import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

/**
 * Chat 工作台回归测试
 *
 * 测试范围：
 * 1. 聊天会话列表
 * 2. 消息列表和详情
 * 3. 发送消息
 * 4. 功能开关提示
 * 5. 错误处理
 */
test.describe('Chat Workbench Regression', () => {

  test.beforeEach(async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            chat: { enabled: true, name: '聊天管理', permission: 'read' },
            chatSend: { enabled: true, name: '发送消息', permission: 'write' }
          }
        })
      }),
      'GET /ozon/api/v1/auth/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [{ id: 'auth-1', shopName: 'Test Shop', isActive: true }]
        })
      })
    });
  });

  // ==================== 页面加载和渲染 ====================

  test('应该正确加载聊天会话列表', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/session/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'session-1',
                chatId: 'chat-1',
                customerName: 'Customer A',
                lastMessage: 'Hello, I have a question',
                lastMessageTime: '2026-06-25T10:00:00Z',
                unreadCount: 2,
                status: 'ACTIVE'
              },
              {
                id: 'session-2',
                chatId: 'chat-2',
                customerName: 'Customer B',
                lastMessage: 'Thank you',
                lastMessageTime: '2026-06-24T10:00:00Z',
                unreadCount: 0,
                status: 'CLOSED'
              }
            ],
            total: 2
          }
        })
      })
    });

    await page.goto('/ozon/chat?authId=auth-1');

    // 验证页面标题
    await expect(page.getByRole('heading', { name: /聊天管理/i })).toBeVisible();

    // 验证数据加载
    await expect(page.getByText('Customer A')).toBeVisible();
    await expect(page.getByText('Hello, I have a question')).toBeVisible();
    await expect(page.getByText('2')).toBeVisible(); // 未读数

    await expect(page.getByText('Customer B')).toBeVisible();
    await expect(page.getByText('Thank you')).toBeVisible();
  });

  test('应该正确加载消息详情', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/session/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'session-1',
            chatId: 'chat-1',
            customerName: 'Customer A',
            status: 'ACTIVE'
          }
        })
      }),
      'GET /ozon/api/v1/chat/message/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'msg-1',
              chatId: 'chat-1',
              senderId: 'customer-1',
              senderType: 'CUSTOMER',
              content: 'Hello, I have a question about my order',
              createTime: '2026-06-25T10:00:00Z',
              isRead: true
            },
            {
              id: 'msg-2',
              chatId: 'chat-1',
              senderId: 'seller-1',
              senderType: 'SELLER',
              content: 'Sure, how can I help you?',
              createTime: '2026-06-25T10:05:00Z',
              isRead: true
            },
            {
              id: 'msg-3',
              chatId: 'chat-1',
              senderId: 'customer-1',
              senderType: 'CUSTOMER',
              content: 'When will my order be shipped?',
              createTime: '2026-06-25T10:10:00Z',
              isRead: false
            }
          ]
        })
      })
    });

    await page.goto('/ozon/chat/session/session-1?authId=auth-1');

    // 验证消息加载
    await expect(page.getByText('Hello, I have a question about my order')).toBeVisible();
    await expect(page.getByText('Sure, how can I help you?')).toBeVisible();
    await expect(page.getByText('When will my order be shipped?')).toBeVisible();

    // 验证消息发送者
    const customerMessages = page.locator('[data-sender-type="CUSTOMER"]');
    await expect(customerMessages).toHaveCount(2);

    const sellerMessages = page.locator('[data-sender-type="SELLER"]');
    await expect(sellerMessages).toHaveCount(1);
  });

  // ==================== 功能开关提示 ====================

  test('应该在发送权限未开启时禁用发送按钮', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            chat: { enabled: true, name: '聊天管理', permission: 'read' },
            chatSend: { enabled: false, name: '发送消息', permission: 'write', disabledReason: '聊天发送功能未开启' }
          }
        })
      }),
      'GET /ozon/api/v1/chat/session/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { id: 'session-1', chatId: 'chat-1', customerName: 'Customer A' }
        })
      }),
      'GET /ozon/api/v1/chat/message/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: []
        })
      })
    });

    await page.goto('/ozon/chat/session/session-1?authId=auth-1');

    // 验证发送按钮被禁用
    const sendBtn = page.getByRole('button', { name: /发送/i });
    await expect(sendBtn).toBeDisabled();

    // 验证输入框被禁用
    const textarea = page.getByPlaceholder(/输入消息/i);
    await expect(textarea).toBeDisabled();

    // 悬停查看提示
    await sendBtn.hover();
    await expect(page.getByText(/聊天发送功能未开启/i)).toBeVisible();
  });

  // ==================== 用户操作流程 ====================

  test('应该支持发送文本消息', async ({ page }) => {
    let sendCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/session/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { id: 'session-1', chatId: 'chat-1', customerName: 'Customer A' }
        })
      }),
      'GET /ozon/api/v1/chat/message/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: []
        })
      }),
      'POST /ozon/api/v1/chat/message/send': async (request) => {
        sendCalled = true;
        const body = await request.postDataJSON();
        expect(body.chatId).toBe('chat-1');
        expect(body.content).toBe('Your order will be shipped tomorrow');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { id: 'msg-new', chatId: 'chat-1' }
          })
        };
      }
    });

    await page.goto('/ozon/chat/session/session-1?authId=auth-1');

    // 输入消息
    await page.getByPlaceholder(/输入消息/i).fill('Your order will be shipped tomorrow');

    // 点击发送
    await page.getByRole('button', { name: /发送/i }).click();

    // 验证成功提示
    await expect(page.getByText(/发送成功/i)).toBeVisible();

    // 验证输入框已清空
    await expect(page.getByPlaceholder(/输入消息/i)).toHaveValue('');

    expect(sendCalled).toBe(true);
  });

  test('应该支持筛选未读会话', async ({ page }) => {
    let filterCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/session/list': async (request) => {
        const url = new URL(request.url());
        const unreadOnly = url.searchParams.get('unreadOnly');
        if (unreadOnly === 'true') {
          filterCalled = true;
          return {
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 200,
              data: {
                records: [
                  { id: 'session-1', chatId: 'chat-1', customerName: 'Customer A', unreadCount: 2 }
                ],
                total: 1
              }
            })
          };
        }
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { records: [], total: 0 }
          })
        };
      }
    });

    await page.goto('/ozon/chat?authId=auth-1');

    // 点击"仅显示未读"
    await page.getByRole('checkbox', { name: /仅显示未读/i }).check();

    // 验证筛选结果
    await expect(page.getByText('Customer A')).toBeVisible();
    await expect(page.getByText('2')).toBeVisible(); // 未读数

    expect(filterCalled).toBe(true);
  });

  test('应该支持标记会话为已读', async ({ page }) => {
    let markReadCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/session/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'session-1', chatId: 'chat-1', customerName: 'Customer A', unreadCount: 2 }
            ],
            total: 1
          }
        })
      }),
      'POST /ozon/api/v1/chat/session/mark-read': async (request) => {
        markReadCalled = true;
        const body = await request.postDataJSON();
        expect(body.sessionId).toBe('session-1');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: true })
        };
      }
    });

    await page.goto('/ozon/chat?authId=auth-1');

    // 点击标记已读按钮
    await page.locator('[data-session-id="session-1"]').getByRole('button', { name: /标记已读/i }).click();

    // 验证成功提示
    await expect(page.getByText(/已标记为已读/i)).toBeVisible();

    expect(markReadCalled).toBe(true);
  });

  test('应该支持关闭会话', async ({ page }) => {
    let closeCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/session/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { id: 'session-1', chatId: 'chat-1', customerName: 'Customer A', status: 'ACTIVE' }
        })
      }),
      'GET /ozon/api/v1/chat/message/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: []
        })
      }),
      'POST /ozon/api/v1/chat/session/close': async (request) => {
        closeCalled = true;
        const body = await request.postDataJSON();
        expect(body.sessionId).toBe('session-1');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: true })
        };
      }
    });

    await page.goto('/ozon/chat/session/session-1?authId=auth-1');

    // 点击关闭会话按钮
    await page.getByRole('button', { name: /关闭会话/i }).click();

    // 确认
    await page.getByRole('button', { name: /确定/i }).click();

    // 验证成功提示
    await expect(page.getByText(/会话已关闭/i)).toBeVisible();

    expect(closeCalled).toBe(true);
  });

  // ==================== 错误处理 ====================

  test('应该正确处理会话列表加载失败', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/session/list': async () => ({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 500,
          message: '服务器错误'
        })
      })
    });

    await page.goto('/ozon/chat?authId=auth-1');

    // 验证错误提示
    await expect(page.getByText(/加载失败/i)).toBeVisible();
  });

  test('应该正确处理消息发送失败', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/session/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { id: 'session-1', chatId: 'chat-1', customerName: 'Customer A' }
        })
      }),
      'GET /ozon/api/v1/chat/message/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: []
        })
      }),
      'POST /ozon/api/v1/chat/message/send': async () => ({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 400,
          message: '会话已关闭，无法发送消息'
        })
      })
    });

    await page.goto('/ozon/chat/session/session-1?authId=auth-1');

    // 输入并发送消息
    await page.getByPlaceholder(/输入消息/i).fill('Test message');
    await page.getByRole('button', { name: /发送/i }).click();

    // 验证错误提示
    await expect(page.getByText(/会话已关闭，无法发送消息/i)).toBeVisible();
  });

  // ==================== 深链跳转 ====================

  test('应该支持从会话列表跳转到会话详情', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/session/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'session-1', chatId: 'chat-1', customerName: 'Customer A', unreadCount: 2 }
            ],
            total: 1
          }
        })
      })
    });

    await page.goto('/ozon/chat?authId=auth-1');

    // 点击会话
    await page.locator('[data-session-id="session-1"]').click();

    // 验证跳转
    await expect(page).toHaveURL(/\/ozon\/chat\/session\/session-1/);
  });

  test('应该支持从会话详情返回列表', async ({ page }) => {
    await page.goto('/ozon/chat/session/session-1?authId=auth-1');

    // 点击返回按钮
    await page.getByRole('button', { name: /返回/i }).click();

    // 验证返回列表页
    await expect(page).toHaveURL('/ozon/chat');
  });
});
