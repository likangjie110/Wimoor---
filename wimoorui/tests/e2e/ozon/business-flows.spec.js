import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

/**
 * 关键业务流程端到端测试
 *
 * 测试完整的业务链路，确保各模块协同工作正常
 */
test.describe('Business Flow E2E Tests', () => {

  test.beforeEach(async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            auth: { enabled: true, name: '店铺授权', permission: 'read' },
            product: { enabled: true, name: '商品管理', permission: 'read' },
            productWrite: { enabled: true, name: '商品发布', permission: 'write' },
            stockWrite: { enabled: true, name: '库存推送', permission: 'write' },
            priceWrite: { enabled: true, name: '价格推送', permission: 'write' },
            task: { enabled: true, name: '任务中心', permission: 'read' }
          }
        })
      }),
      'GET /ozon/api/v1/auth/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            { id: 'auth-1', shopName: 'Test Shop', clientId: 'client-1', isActive: true }
          ]
        })
      })
    });
  });

  // ==================== 流程 1: 授权到商品发布 ====================

  test('完整流程：创建授权 → 设置配送方式 → 导入草稿 → 编辑草稿 → 发布商品 → 查看任务', async ({ page }) => {
    let authId = 'auth-new';
    let draftId = 'draft-new';
    let publishTaskId = 'task-publish-1';

    // Step 1: 创建授权
    await installCommonAppMocks(page, {
      'POST /ozon/api/v1/auth/create': async (request) => {
        const body = await request.postDataJSON();
        expect(body.shopName).toBe('New Test Shop');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { id: authId, shopName: 'New Test Shop', clientId: body.clientId }
          })
        };
      }
    });

    await page.goto('/ozon/auth');
    await page.getByRole('button', { name: /新建授权/i }).click();
    await page.getByLabel('店铺名称').fill('New Test Shop');
    await page.getByLabel('Client ID').fill('new-client-123');
    await page.getByLabel('API Key').fill('new-api-key-456');
    await page.getByRole('button', { name: /确定/i }).click();
    await expect(page.getByText(/创建成功/i)).toBeVisible();

    // Step 2: 设置配送方式
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/auth/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { id: authId, shopName: 'New Test Shop', clientId: 'new-client-123' }
        })
      }),
      'GET /ozon/api/v1/seller/delivery-method/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            { id: 'dm-1', methodCode: 'FBS', methodName: 'Fulfillment by Seller', isDefault: false },
            { id: 'dm-2', methodCode: 'FBO', methodName: 'Fulfillment by Ozon', isDefault: false }
          ]
        })
      }),
      'POST /ozon/api/v1/seller/delivery-method/set-default': async (request) => {
        const body = await request.postDataJSON();
        expect(body.id).toBe('dm-1');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: true })
        };
      }
    });

    await page.goto(`/ozon/auth?authId=${authId}`);
    await expect(page.getByText('Fulfillment by Seller')).toBeVisible();
    await page.locator('[data-delivery-method-id="dm-1"]').getByRole('button', { name: /设为默认/i }).click();
    await expect(page.getByText(/设置成功/i)).toBeVisible();

    // Step 3: 导入草稿
    await installCommonAppMocks(page, {
      'POST /ozon/api/v1/product/draft/import': async (request) => {
        const body = await request.postDataJSON();
        expect(body.authId).toBe(authId);
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { taskId: 'import-task-1', total: 1 }
          })
        };
      },
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: draftId, sku: 'NEW-SKU-001', productName: 'New Product', status: 'DRAFT', categoryId: 'cat-1' }
            ],
            total: 1
          }
        })
      })
    });

    await page.getByRole('link', { name: /商品管理/i }).click();
    await expect(page).toHaveURL(new RegExp(`/ozon/product.*authId=${authId}`));
    await page.getByRole('button', { name: /导入草稿/i }).click();
    await page.getByLabel('SKU列表').fill('NEW-SKU-001');
    await page.getByRole('button', { name: /确定/i }).click();
    await expect(page.getByText(/导入任务已创建/i)).toBeVisible();

    // 等待导入完成，刷新列表
    await page.getByRole('button', { name: /刷新/i }).click();
    await expect(page.getByText('NEW-SKU-001')).toBeVisible();

    // Step 4: 编辑草稿
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: draftId,
            sku: 'NEW-SKU-001',
            productName: 'New Product',
            description: 'Original description',
            categoryId: 'cat-1',
            status: 'DRAFT'
          }
        })
      }),
      'POST /ozon/api/v1/product/draft/update': async (request) => {
        const body = await request.postDataJSON();
        expect(body.id).toBe(draftId);
        expect(body.status).toBe('READY');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: true })
        };
      }
    });

    await page.locator(`[data-draft-id="${draftId}"]`).getByRole('link', { name: /编辑/i }).click();
    await expect(page).toHaveURL(new RegExp(`/ozon/product/draft/${draftId}`));

    // 填写必填字段并标记为准备发布
    await page.getByLabel('描述').fill('Updated description with details');
    await page.getByLabel('状态').click();
    await page.getByRole('option', { name: /准备发布/i }).click();
    await page.getByRole('button', { name: /保存/i }).click();
    await expect(page.getByText(/保存成功/i)).toBeVisible();

    // Step 5: 发布商品
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: draftId, sku: 'NEW-SKU-001', productName: 'New Product', status: 'READY' }
            ],
            total: 1
          }
        })
      }),
      'POST /ozon/api/v1/product/publish': async (request) => {
        const body = await request.postDataJSON();
        expect(body.draftIds).toContain(draftId);
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { taskId: publishTaskId, total: 1 }
          })
        };
      }
    });

    await page.goto(`/ozon/product?authId=${authId}&tab=draft`);
    await page.locator(`[data-draft-id="${draftId}"] input[type="checkbox"]`).check();
    await page.getByRole('button', { name: /批量发布/i }).click();
    await page.getByRole('button', { name: /确定/i }).click();
    await expect(page.getByText(/发布任务已创建/i)).toBeVisible();

    // Step 6: 查看任务
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/publish-task/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: publishTaskId,
              draftId: draftId,
              status: 'PROCESSING',
              progress: 50,
              createTime: new Date().toISOString()
            }
          ]
        })
      })
    });

    await page.getByRole('button', { name: /发布任务/i }).click();
    await expect(page.getByText(publishTaskId)).toBeVisible();
    await expect(page.getByText('PROCESSING')).toBeVisible();
    await expect(page.getByText('50%')).toBeVisible();
  });

  // ==================== 流程 2: 库存价格管理 ====================

  test('完整流程：选择授权 → 推送库存 → 推送价格 → 查看任务', async ({ page }) => {
    const authId = 'auth-1';
    const productId = 'prod-1';
    const stockTaskId = 'stock-task-1';
    const priceTaskId = 'price-task-1';

    // Step 1: 推送库存
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'stock-1', productId: productId, sku: 'TEST-SKU-001', stock: 100, warehouseId: 'wh-1' }
            ],
            total: 1
          }
        })
      }),
      'POST /ozon/api/v1/stock/push': async (request) => {
        const body = await request.postDataJSON();
        expect(body.stockIds).toContain('stock-1');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { taskId: stockTaskId, total: 1 }
          })
        };
      }
    });

    await page.goto(`/ozon/stock?authId=${authId}`);
    await page.locator('[data-stock-id="stock-1"] input[type="checkbox"]').check();
    await page.getByRole('button', { name: /推送库存/i }).click();
    await page.getByRole('button', { name: /确定/i }).click();
    await expect(page.getByText(/推送任务已创建/i)).toBeVisible();

    // Step 2: 推送价格
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/price/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'price-1', productId: productId, sku: 'TEST-SKU-001', price: 100.00 }
            ],
            total: 1
          }
        })
      }),
      'POST /ozon/api/v1/price/push': async (request) => {
        const body = await request.postDataJSON();
        expect(body.priceIds).toContain('price-1');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { taskId: priceTaskId, total: 1 }
          })
        };
      }
    });

    await page.goto(`/ozon/price?authId=${authId}`);
    await page.locator('[data-price-id="price-1"] input[type="checkbox"]').check();
    await page.getByRole('button', { name: /推送价格/i }).click();
    await page.getByRole('button', { name: /确定/i }).click();
    await expect(page.getByText(/推送任务已创建/i)).toBeVisible();

    // Step 3: 查看任务
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/task/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: stockTaskId, type: 'STOCK_PUSH', status: 'SUCCESS', total: 1, success: 1, failed: 0 },
              { id: priceTaskId, type: 'PRICE_PUSH', status: 'SUCCESS', total: 1, success: 1, failed: 0 }
            ],
            total: 2
          }
        })
      })
    });

    await page.goto(`/ozon/task?authId=${authId}`);
    await expect(page.getByText(stockTaskId)).toBeVisible();
    await expect(page.getByText(priceTaskId)).toBeVisible();
    await expect(page.getByText('SUCCESS').first()).toBeVisible();
  });

  // ==================== 流程 3: 订单售后 ====================

  test('完整流程：同步订单 → 查看订单详情 → 处理售后 → 同步包裹', async ({ page }) => {
    const authId = 'auth-1';
    const postingId = 'posting-1';
    const aftersaleId = 'aftersale-1';

    // Step 1: 查看订单详情
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/posting/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: postingId,
                postingNumber: 'POST-001',
                orderNumber: 'ORDER-001',
                status: 'DELIVERED'
              }
            ],
            total: 1
          }
        })
      }),
      'GET /ozon/api/v1/posting/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: postingId,
            postingNumber: 'POST-001',
            orderNumber: 'ORDER-001',
            status: 'DELIVERED',
            products: [
              { sku: 'TEST-SKU-001', name: 'Test Product', quantity: 1, price: 100.00 }
            ]
          }
        })
      })
    });

    await page.goto(`/ozon/posting?authId=${authId}`);
    await page.locator(`[data-posting-id="${postingId}"]`).getByRole('link', { name: /详情/i }).click();
    await expect(page).toHaveURL(new RegExp(`/ozon/posting/detail/${postingId}`));
    await expect(page.getByText('POST-001')).toBeVisible();

    // Step 2: 申请售后
    await installCommonAppMocks(page, {
      'POST /ozon/api/v1/aftersale/create': async (request) => {
        const body = await request.postDataJSON();
        expect(body.postingId).toBe(postingId);
        expect(body.type).toBe('RETURN');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { id: aftersaleId }
          })
        };
      }
    });

    await page.getByRole('button', { name: /申请售后/i }).click();
    await page.getByLabel('售后类型').click();
    await page.getByRole('option', { name: /退货/i }).click();
    await page.getByLabel('售后原因').fill('商品有质量问题');
    await page.getByRole('button', { name: /提交/i }).click();
    await expect(page.getByText(/售后申请已提交/i)).toBeVisible();

    // Step 3: 查看售后状态
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/aftersale/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: aftersaleId,
                postingId: postingId,
                postingNumber: 'POST-001',
                type: 'RETURN',
                status: 'PENDING',
                reason: '商品有质量问题'
              }
            ],
            total: 1
          }
        })
      })
    });

    await page.goto(`/ozon/aftersale?authId=${authId}`);
    await expect(page.getByText(aftersaleId)).toBeVisible();
    await expect(page.getByText('RETURN')).toBeVisible();
    await expect(page.getByText('PENDING')).toBeVisible();
  });

  // ==================== 流程 4: 财务数据 ====================

  test('完整流程：同步交易 → 同步销售明细 → 查看财务数据', async ({ page }) => {
    const authId = 'auth-1';

    // Step 1: 查看交易记录
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/finance/transaction/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'trans-1',
                transactionDate: '2026-06-25',
                postingNumber: 'POST-001',
                type: 'SALE',
                amount: 250.00,
                currency: 'RUB'
              }
            ],
            total: 1
          }
        })
      })
    });

    await page.goto(`/ozon/finance?authId=${authId}&tab=transaction`);
    await expect(page.getByText('POST-001')).toBeVisible();
    await expect(page.getByText('250.00')).toBeVisible();

    // Step 2: 查看销售明细
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/finance/sales-detail/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'detail-1',
                saleDate: '2026-06-25',
                sku: 'TEST-SKU-001',
                quantity: 2,
                price: 100.00,
                commission: 15.00,
                netAmount: 185.00
              }
            ],
            total: 1
          }
        })
      })
    });

    await page.getByRole('tab', { name: /销售明细/i }).click();
    await expect(page.getByText('TEST-SKU-001')).toBeVisible();
    await expect(page.getByText('185.00')).toBeVisible();

    // Step 3: 导出财务报告
    await installCommonAppMocks(page, {
      'POST /ozon/api/v1/finance/transaction/export': async () => ({
        status: 200,
        contentType: 'application/octet-stream',
        body: 'mock-excel-data'
      })
    });

    const downloadPromise = page.waitForEvent('download');
    await page.getByRole('button', { name: /导出/i }).click();
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toContain('.xlsx');
  });

  // ==================== 流程 5: 客服聊天 ====================

  test('完整流程：同步会话 → 同步消息 → 发送回复', async ({ page }) => {
    const authId = 'auth-1';
    const sessionId = 'session-1';
    const chatId = 'chat-1';

    // Step 1: 查看会话列表
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/session/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: sessionId,
                chatId: chatId,
                customerName: 'Customer A',
                lastMessage: 'I have a question',
                unreadCount: 1,
                status: 'ACTIVE'
              }
            ],
            total: 1
          }
        })
      })
    });

    await page.goto(`/ozon/chat?authId=${authId}`);
    await expect(page.getByText('Customer A')).toBeVisible();
    await expect(page.getByText('1')).toBeVisible(); // 未读数

    // Step 2: 查看消息详情
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/session/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { id: sessionId, chatId: chatId, customerName: 'Customer A' }
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
              chatId: chatId,
              senderType: 'CUSTOMER',
              content: 'I have a question about shipping',
              isRead: false
            }
          ]
        })
      })
    });

    await page.locator(`[data-session-id="${sessionId}"]`).click();
    await expect(page).toHaveURL(new RegExp(`/ozon/chat/session/${sessionId}`));
    await expect(page.getByText('I have a question about shipping')).toBeVisible();

    // Step 3: 发送回复
    await installCommonAppMocks(page, {
      'POST /ozon/api/v1/chat/message/send': async (request) => {
        const body = await request.postDataJSON();
        expect(body.chatId).toBe(chatId);
        expect(body.content).toBe('Shipping usually takes 3-5 business days');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { id: 'msg-2', chatId: chatId }
          })
        };
      }
    });

    await page.getByPlaceholder(/输入消息/i).fill('Shipping usually takes 3-5 business days');
    await page.getByRole('button', { name: /发送/i }).click();
    await expect(page.getByText(/发送成功/i)).toBeVisible();
  });

  // ==================== 流程 6: 广告管理 ====================

  test('完整流程：同步广告活动 → 查看报告 → 查看广告数据', async ({ page }) => {
    const authId = 'auth-1';
    const campaignId = 'campaign-1';

    // Step 1: 同步广告
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/campaign/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [], total: 0 }
        })
      }),
      'POST /ozon/api/v1/ads/sync': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { taskId: 'sync-task-1', total: 5 }
        })
      })
    });

    await page.goto(`/ozon/ads?authId=${authId}`);
    await page.getByRole('button', { name: /同步广告/i }).click();
    await page.getByRole('button', { name: /确定/i }).click();
    await expect(page.getByText(/同步任务已创建/i)).toBeVisible();

    // Step 2: 查看广告活动
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/campaign/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: campaignId,
                campaignName: 'Test Campaign',
                status: 'ACTIVE',
                budget: 10000,
                spent: 2500,
                impressions: 50000,
                clicks: 1000,
                conversions: 50
              }
            ],
            total: 1
          }
        })
      })
    });

    await page.getByRole('button', { name: /刷新/i }).click();
    await expect(page.getByText('Test Campaign')).toBeVisible();
    await expect(page.getByText('ACTIVE')).toBeVisible();

    // Step 3: 查看报告摘要
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/report/summary': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            totalBudget: 10000.00,
            totalSpent: 2500.00,
            totalImpressions: 50000,
            totalClicks: 1000,
            avgCtr: 2.0,
            totalConversions: 50,
            avgRoas: 2.0
          }
        })
      })
    });

    await page.getByRole('button', { name: /查看报告/i }).click();
    await expect(page.getByText('总预算')).toBeVisible();
    await expect(page.getByText('10000.00')).toBeVisible();
    await expect(page.getByText('平均CTR')).toBeVisible();
    await expect(page.getByText('2.0%')).toBeVisible();
  });
});
