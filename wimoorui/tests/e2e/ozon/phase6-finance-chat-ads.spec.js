import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

/**
 * Phase 6: Finance/Chat/Ads 双模演进功能测试
 *
 * 测试范围：
 * 1. Finance 页面双模切换测试
 * 2. Chat 页面双模切换测试
 * 3. Ads 页面双模切换测试
 * 4. API 同步按钮测试
 * 5. 功能开关测试
 */
test.describe('Phase 6: Finance/Chat/Ads Dual Mode', () => {

  // ==================== Finance 双模切换测试 ====================

  test('Finance page switches between local import and API sync modes', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/finance/tasks': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'task-1',
              authId: 'auth-1',
              reportId: 'local-report-1',
              taskStatus: 'DONE',
              importedCount: 100,
              sourceMode: 'LOCAL_IMPORT',
              createdAt: '2026-06-20T10:00:00Z'
            }
          ]
        })
      }),
      'GET /ozon/api/v1/config/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            financeApiSync: true,
            chatApiReply: true,
            adsPerformanceApi: true
          }
        })
      })
    });

    await page.goto('/ozon/finance?authId=auth-1');

    // 验证本地导入模式按钮存在
    await expect(page.getByRole('button', { name: /本地导入|导入文件|上传报表/i })).toBeVisible();

    // 验证 API 同步按钮存在（功能开关启用）
    await expect(page.getByRole('button', { name: /API同步|从API同步/i })).toBeVisible();

    // 验证任务列表显示
    await expect(page.getByText('local-report-1')).toBeVisible();
    await expect(page.getByText('DONE')).toBeVisible();
  });

  test('Finance API sync button calls transaction sync API', async ({ page }) => {
    let apiSyncCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/finance/tasks': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [] })
      }),
      'POST /ozon/api/v1/finance/sync/transactions': async () => {
        apiSyncCalled = true;
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              taskId: 'api-task-1',
              reportId: 'api-transactions-2026-06-01-to-2026-06-30',
              importedCount: 50,
              importedAt: '2026-06-25T10:00:00Z'
            }
          })
        };
      },
      'GET /ozon/api/v1/config/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { financeApiSync: true }
        })
      })
    });

    await page.goto('/ozon/finance?authId=auth-1');

    // 点击 API 同步按钮
    await page.getByRole('button', { name: /API同步|从API同步/i }).click();

    // 选择日期范围
    await page.getByPlaceholder('开始日期').fill('2026-06-01');
    await page.getByPlaceholder('结束日期').fill('2026-06-30');

    // 确认同步
    await page.getByRole('button', { name: /确认|开始同步/i }).click();

    // 等待同步完成
    await page.waitForTimeout(500);

    expect(apiSyncCalled).toBe(true);
    await expect(page.getByText(/同步成功|已同步/i)).toBeVisible();
  });

  test('Finance API sync is hidden when feature gate is disabled', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/finance/tasks': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [] })
      }),
      'GET /ozon/api/v1/config/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { financeApiSync: false }
        })
      })
    });

    await page.goto('/ozon/finance?authId=auth-1');

    // 验证本地导入按钮存在
    await expect(page.getByRole('button', { name: /本地导入|导入文件/i })).toBeVisible();

    // 验证 API 同步按钮不存在
    await expect(page.getByRole('button', { name: /API同步|从API同步/i })).not.toBeVisible();
  });

  test('Finance realizations sync button works correctly', async ({ page }) => {
    let realizationsSyncCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/finance/tasks': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [] })
      }),
      'POST /ozon/api/v1/finance/sync/realizations': async () => {
        realizationsSyncCalled = true;
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              taskId: 'api-task-2',
              reportId: 'api-realizations-2026-06-01-to-2026-06-30',
              importedCount: 30
            }
          })
        };
      },
      'GET /ozon/api/v1/config/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { financeApiSync: true }
        })
      })
    });

    await page.goto('/ozon/finance?authId=auth-1');

    // 点击销售明细同步按钮
    await page.getByRole('button', { name: /同步销售明细|Realizations/i }).click();

    await page.getByPlaceholder('开始日期').fill('2026-06-01');
    await page.getByPlaceholder('结束日期').fill('2026-06-30');
    await page.getByRole('button', { name: /确认|开始同步/i }).click();

    await page.waitForTimeout(500);

    expect(realizationsSyncCalled).toBe(true);
  });

  // ==================== Chat 双模切换测试 ====================

  test('Chat page switches between import and API reply modes', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/sessions': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              sessionId: 'session-1',
              customerName: 'Buyer A',
              lastMessageText: 'Hello',
              unreadCount: 2,
              sessionStatus: 'OPEN'
            }
          ]
        })
      }),
      'GET /ozon/api/v1/config/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { chatApiReply: true }
        })
      })
    });

    await page.goto('/ozon/chat?authId=auth-1');

    // 验证聊天列表显示
    await expect(page.getByText('Buyer A')).toBeVisible();
    await expect(page.getByText('Hello')).toBeVisible();

    // 点击进入会话
    await page.getByText('Buyer A').click();

    // 验证回复按钮存在（功能开关启用）
    await expect(page.getByRole('button', { name: /发送回复|API发送|立即发送/i })).toBeVisible();
  });

  test('Chat API reply button sends message via API', async ({ page }) => {
    let apiReplyCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/sessions': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              sessionId: 'session-1',
              customerName: 'Buyer A',
              lastMessageText: 'Hello',
              unreadCount: 2
            }
          ]
        })
      }),
      'GET /ozon/api/v1/chat/messages': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              messageId: 'msg-1',
              senderType: 'BUYER',
              messageText: 'Hello',
              messageTime: '2026-06-20T10:00:00Z'
            }
          ]
        })
      }),
      'POST /ozon/api/v1/chat/reply/send': async () => {
        apiReplyCalled = true;
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              sessionId: 'session-1',
              replyStatus: 'SENT',
              replyText: 'Thanks for your message'
            }
          })
        };
      },
      'GET /ozon/api/v1/config/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { chatApiReply: true }
        })
      })
    });

    await page.goto('/ozon/chat?authId=auth-1');
    await page.getByText('Buyer A').click();

    // 输入回复内容
    await page.getByPlaceholder(/输入回复|回复内容/i).fill('Thanks for your message');

    // 点击 API 发送按钮
    await page.getByRole('button', { name: /API发送|立即发送/i }).click();

    await page.waitForTimeout(500);

    expect(apiReplyCalled).toBe(true);
    await expect(page.getByText(/发送成功|已发送/i)).toBeVisible();
  });

  test('Chat API reply is hidden when feature gate is disabled', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/sessions': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              sessionId: 'session-1',
              customerName: 'Buyer A',
              lastMessageText: 'Hello'
            }
          ]
        })
      }),
      'GET /ozon/api/v1/chat/messages': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [] })
      }),
      'GET /ozon/api/v1/config/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { chatApiReply: false }
        })
      })
    });

    await page.goto('/ozon/chat?authId=auth-1');
    await page.getByText('Buyer A').click();

    // 验证只有记录回复按钮，没有 API 发送按钮
    await expect(page.getByRole('button', { name: /记录回复|保存回复/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /API发送|立即发送/i })).not.toBeVisible();
  });

  // ==================== Ads 双模切换测试 ====================

  test('Ads page switches between import and API sync modes', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/accounts': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              accountId: 'acc-1',
              accountName: 'Main Account',
              status: 'ACTIVE',
              currencyCode: 'RUB'
            }
          ]
        })
      }),
      'GET /ozon/api/v1/ads/campaigns': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              campaignId: 'camp-1',
              campaignName: 'Summer Sale',
              campaignType: 'SEARCH_PROMO',
              campaignStatus: 'ACTIVE'
            }
          ]
        })
      }),
      'GET /ozon/api/v1/config/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { adsPerformanceApi: true }
        })
      })
    });

    await page.goto('/ozon/ads?authId=auth-1');

    // 验证账号和活动显示
    await expect(page.getByText('Main Account')).toBeVisible();
    await expect(page.getByText('Summer Sale')).toBeVisible();

    // 验证本地导入按钮存在
    await expect(page.getByRole('button', { name: /本地导入|导入数据/i })).toBeVisible();

    // 验证 API 同步按钮存在（功能开关启用）
    await expect(page.getByRole('button', { name: /API同步|从API同步/i })).toBeVisible();
  });

  test('Ads API sync button records sync intent', async ({ page }) => {
    let syncIntentCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/accounts': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              accountId: 'acc-1',
              accountName: 'Main Account'
            }
          ]
        })
      }),
      'GET /ozon/api/v1/ads/campaigns': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [] })
      }),
      'POST /ozon/api/v1/ads/sync/intent': async () => {
        syncIntentCalled = true;
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              requestId: 'req-1',
              accountId: 'acc-1',
              requestStatus: 'PENDING',
              message: '已记录同步意图，等待官方 Performance API 接入'
            }
          })
        };
      },
      'GET /ozon/api/v1/config/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { adsPerformanceApi: true }
        })
      })
    });

    await page.goto('/ozon/ads?authId=auth-1');

    // 点击 API 同步按钮
    await page.getByRole('button', { name: /API同步|从API同步/i }).click();

    // 选择账号和日期范围
    await page.getByPlaceholder('开始日期').fill('2026-06-01');
    await page.getByPlaceholder('结束日期').fill('2026-06-30');
    await page.getByRole('button', { name: /确认|提交/i }).click();

    await page.waitForTimeout(500);

    expect(syncIntentCalled).toBe(true);
    await expect(page.getByText(/已记录同步意图|PENDING/i)).toBeVisible();
  });

  test('Ads API sync is hidden when feature gate is disabled', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/accounts': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [] })
      }),
      'GET /ozon/api/v1/ads/campaigns': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [] })
      }),
      'GET /ozon/api/v1/config/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { adsPerformanceApi: false }
        })
      })
    });

    await page.goto('/ozon/ads?authId=auth-1');

    // 验证本地导入按钮存在
    await expect(page.getByRole('button', { name: /本地导入|导入数据/i })).toBeVisible();

    // 验证 API 同步按钮不存在
    await expect(page.getByRole('button', { name: /API同步|从API同步/i })).not.toBeVisible();
  });

  test('Ads reports display metrics correctly', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/accounts': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [{ accountId: 'acc-1', accountName: 'Main Account' }]
        })
      }),
      'GET /ozon/api/v1/ads/campaigns': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [{ campaignId: 'camp-1', campaignName: 'Summer Sale' }]
        })
      }),
      'GET /ozon/api/v1/ads/reports': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              campaignId: 'camp-1',
              reportDate: '2026-06-20',
              impressions: 10000,
              clicks: 250,
              spend: 1500.50,
              orders: 50,
              sales: 8000.00,
              acos: 18.76,
              roas: 5.33
            }
          ]
        })
      }),
      'GET /ozon/api/v1/ads/summary': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            impressions: 10000,
            clicks: 250,
            spend: 1500.50,
            orders: 50,
            sales: 8000.00,
            acos: 18.76,
            roas: 5.33
          }
        })
      }),
      'GET /ozon/api/v1/config/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { adsPerformanceApi: true }
        })
      })
    });

    await page.goto('/ozon/ads?authId=auth-1');

    // 验证汇总指标显示
    await expect(page.getByText(/展示.*10,?000/i)).toBeVisible();
    await expect(page.getByText(/点击.*250/i)).toBeVisible();
    await expect(page.getByText(/花费.*1,?500\.50/i)).toBeVisible();
    await expect(page.getByText(/订单.*50/i)).toBeVisible();
    await expect(page.getByText(/销售.*8,?000/i)).toBeVisible();
  });

  // ==================== 功能开关集成测试 ====================

  test('All features are enabled when gates are on', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/config/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            financeApiSync: true,
            chatApiReply: true,
            adsPerformanceApi: true
          }
        })
      })
    });

    // 测试 Finance 页面
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/finance/tasks': async () => ({
        status: 200,
        body: JSON.stringify({ code: 200, data: [] })
      })
    });
    await page.goto('/ozon/finance?authId=auth-1');
    await expect(page.getByRole('button', { name: /API同步/i })).toBeVisible();

    // 测试 Chat 页面
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/sessions': async () => ({
        status: 200,
        body: JSON.stringify({ code: 200, data: [] })
      })
    });
    await page.goto('/ozon/chat?authId=auth-1');
    // Chat 需要选中会话才显示 API 发送按钮，这里只验证页面加载

    // 测试 Ads 页面
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/accounts': async () => ({
        status: 200,
        body: JSON.stringify({ code: 200, data: [] })
      }),
      'GET /ozon/api/v1/ads/campaigns': async () => ({
        status: 200,
        body: JSON.stringify({ code: 200, data: [] })
      })
    });
    await page.goto('/ozon/ads?authId=auth-1');
    await expect(page.getByRole('button', { name: /API同步/i })).toBeVisible();
  });

  test('All features are disabled when gates are off', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/config/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            financeApiSync: false,
            chatApiReply: false,
            adsPerformanceApi: false
          }
        })
      })
    });

    // 测试 Finance 页面
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/finance/tasks': async () => ({
        status: 200,
        body: JSON.stringify({ code: 200, data: [] })
      })
    });
    await page.goto('/ozon/finance?authId=auth-1');
    await expect(page.getByRole('button', { name: /API同步/i })).not.toBeVisible();

    // 测试 Ads 页面
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/accounts': async () => ({
        status: 200,
        body: JSON.stringify({ code: 200, data: [] })
      }),
      'GET /ozon/api/v1/ads/campaigns': async () => ({
        status: 200,
        body: JSON.stringify({ code: 200, data: [] })
      })
    });
    await page.goto('/ozon/ads?authId=auth-1');
    await expect(page.getByRole('button', { name: /API同步/i })).not.toBeVisible();
  });

});
