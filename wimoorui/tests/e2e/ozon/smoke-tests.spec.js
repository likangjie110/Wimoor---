import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from './support/ozon-mock.js';

/**
 * 冒烟测试套件
 *
 * 快速验证系统核心功能可用性，适用于生产环境部署后的快速验证
 * 预计执行时间：2-3 分钟
 */
test.describe('Smoke Tests', () => {

  test.beforeEach(async ({ page }) => {
    // 设置基础 Mock，确保所有功能开关开启
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
            postingWrite: { enabled: true, name: '履约操作', permission: 'write' },
            finance: { enabled: true, name: '财务管理', permission: 'read' },
            chat: { enabled: true, name: '聊天管理', permission: 'read' },
            chatSend: { enabled: true, name: '发送消息', permission: 'write' },
            ads: { enabled: true, name: '广告管理', permission: 'read' },
            adsSync: { enabled: true, name: '广告同步', permission: 'write' },
            task: { enabled: true, name: '任务中心', permission: 'read' },
            error: { enabled: true, name: '错误中心', permission: 'read' }
          }
        })
      }),
      'GET /ozon/api/v1/auth/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            { id: 'auth-smoke-1', shopName: 'Smoke Test Shop', isActive: true }
          ]
        })
      })
    });
  });

  // ==================== 页面可访问性 ====================

  test('所有核心页面应该可以正常访问', async ({ page }) => {
    const authId = 'auth-smoke-1';

    const pages = [
      { path: '/ozon/auth', title: /授权管理/i },
      { path: `/ozon/product?authId=${authId}`, title: /商品管理/i },
      { path: `/ozon/stock?authId=${authId}`, title: /库存管理/i },
      { path: `/ozon/price?authId=${authId}`, title: /价格管理/i },
      { path: `/ozon/posting?authId=${authId}`, title: /订单管理/i },
      { path: `/ozon/aftersale?authId=${authId}`, title: /售后管理/i },
      { path: `/ozon/finance?authId=${authId}`, title: /财务管理/i },
      { path: `/ozon/chat?authId=${authId}`, title: /聊天管理/i },
      { path: `/ozon/ads?authId=${authId}`, title: /广告管理/i },
      { path: `/ozon/task?authId=${authId}`, title: /任务中心/i },
      { path: `/ozon/error?authId=${authId}`, title: /错误中心/i }
    ];

    for (const { path, title } of pages) {
      await page.goto(path);
      await expect(page.getByRole('heading', { name: title })).toBeVisible({ timeout: 5000 });
    }
  });

  // ==================== 功能开关验证 ====================

  test('功能开关API应该正常响应', async ({ page }) => {
    await page.goto('/ozon/auth');

    // 验证功能开关数据已加载
    await page.waitForResponse(response =>
      response.url().includes('/api/v1/features') && response.status() === 200
    );

    // 验证页面正常显示（功能开关已生效）
    await expect(page.getByRole('heading', { name: /授权管理/i })).toBeVisible();
  });

  // ==================== 授权模块核心功能 ====================

  test('授权列表应该可以正常加载', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/auth/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            { id: 'auth-1', shopName: 'Test Shop 1', clientId: 'client-1', isActive: true },
            { id: 'auth-2', shopName: 'Test Shop 2', clientId: 'client-2', isActive: true }
          ]
        })
      })
    });

    await page.goto('/ozon/auth');

    await expect(page.getByText('Test Shop 1')).toBeVisible();
    await expect(page.getByText('Test Shop 2')).toBeVisible();
  });

  // ==================== 商品模块核心功能 ====================

  test('商品草稿列表应该可以正常加载', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'draft-1', sku: 'SMOKE-SKU-001', productName: 'Smoke Test Product', status: 'DRAFT' }
            ],
            total: 1
          }
        })
      })
    });

    await page.goto('/ozon/product?authId=auth-smoke-1&tab=draft');

    await expect(page.getByText('SMOKE-SKU-001')).toBeVisible();
    await expect(page.getByText('Smoke Test Product')).toBeVisible();
  });

  // ==================== 库存模块核心功能 ====================

  test('库存列表应该可以正常加载', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'stock-1', sku: 'SMOKE-SKU-001', stock: 100, available: 90 }
            ],
            total: 1
          }
        })
      })
    });

    await page.goto('/ozon/stock?authId=auth-smoke-1');

    await expect(page.getByText('SMOKE-SKU-001')).toBeVisible();
    await expect(page.getByText('100')).toBeVisible();
  });

  // ==================== 价格模块核心功能 ====================

  test('价格列表应该可以正常加载', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/price/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'price-1', sku: 'SMOKE-SKU-001', price: 100.00, currency: 'RUB' }
            ],
            total: 1
          }
        })
      })
    });

    await page.goto('/ozon/price?authId=auth-smoke-1');

    await expect(page.getByText('SMOKE-SKU-001')).toBeVisible();
    await expect(page.getByText('100.00')).toBeVisible();
  });

  // ==================== 订单模块核心功能 ====================

  test('订单列表应该可以正常加载', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/posting/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'posting-1', postingNumber: 'SMOKE-POST-001', status: 'AWAITING_PACKAGING' }
            ],
            total: 1
          }
        })
      })
    });

    await page.goto('/ozon/posting?authId=auth-smoke-1');

    await expect(page.getByText('SMOKE-POST-001')).toBeVisible();
    await expect(page.getByText('AWAITING_PACKAGING')).toBeVisible();
  });

  // ==================== 财务模块核心功能 ====================

  test('财务交易记录应该可以正常加载', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/finance/transaction/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'trans-1', transactionDate: '2026-06-25', type: 'SALE', amount: 250.00 }
            ],
            total: 1
          }
        })
      })
    });

    await page.goto('/ozon/finance?authId=auth-smoke-1&tab=transaction');

    await expect(page.getByText('SALE')).toBeVisible();
    await expect(page.getByText('250.00')).toBeVisible();
  });

  // ==================== 聊天模块核心功能 ====================

  test('聊天会话列表应该可以正常加载', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/session/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'session-1', customerName: 'Smoke Test Customer', lastMessage: 'Hello', unreadCount: 1 }
            ],
            total: 1
          }
        })
      })
    });

    await page.goto('/ozon/chat?authId=auth-smoke-1');

    await expect(page.getByText('Smoke Test Customer')).toBeVisible();
    await expect(page.getByText('Hello')).toBeVisible();
  });

  // ==================== 广告模块核心功能 ====================

  test('广告活动列表应该可以正常加载', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/campaign/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'campaign-1', campaignName: 'Smoke Test Campaign', status: 'ACTIVE', budget: 10000 }
            ],
            total: 1
          }
        })
      })
    });

    await page.goto('/ozon/ads?authId=auth-smoke-1');

    await expect(page.getByText('Smoke Test Campaign')).toBeVisible();
    await expect(page.getByText('ACTIVE')).toBeVisible();
  });

  // ==================== 任务中心核心功能 ====================

  test('任务列表应该可以正常加载', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/task/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'task-1', type: 'PRODUCT_PUBLISH', status: 'SUCCESS', total: 10, success: 10, failed: 0 }
            ],
            total: 1
          }
        })
      })
    });

    await page.goto('/ozon/task?authId=auth-smoke-1');

    await expect(page.getByText('PRODUCT_PUBLISH')).toBeVisible();
    await expect(page.getByText('SUCCESS')).toBeVisible();
  });

  // ==================== 错误中心核心功能 ====================

  test('错误列表应该可以正常加载', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/error/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'error-1', errorCode: 'VALIDATION_ERROR', errorMessage: '验证失败', taskType: 'PRODUCT_PUBLISH' }
            ],
            total: 1
          }
        })
      })
    });

    await page.goto('/ozon/error?authId=auth-smoke-1');

    await expect(page.getByText('VALIDATION_ERROR')).toBeVisible();
    await expect(page.getByText('验证失败')).toBeVisible();
  });

  // ==================== 关键操作按钮可用性 ====================

  test('关键操作按钮应该可见且可用', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'draft-1', sku: 'TEST-SKU', status: 'READY' }
            ],
            total: 1
          }
        })
      })
    });

    await page.goto('/ozon/product?authId=auth-smoke-1&tab=draft');

    // 验证关键按钮存在且启用
    await expect(page.getByRole('button', { name: /导入草稿/i })).toBeEnabled();
    await expect(page.getByRole('button', { name: /批量发布/i })).toBeEnabled();
    await expect(page.getByRole('button', { name: /刷新/i })).toBeEnabled();
  });

  // ==================== API 错误处理 ====================

  test('应该能够正确处理API错误', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 500,
          message: '服务器错误'
        })
      })
    });

    await page.goto('/ozon/product?authId=auth-smoke-1&tab=draft');

    // 验证错误提示显示
    await expect(page.getByText(/加载失败|服务器错误/i)).toBeVisible({ timeout: 5000 });
  });

  // ==================== 导航和路由 ====================

  test('主导航菜单应该正常工作', async ({ page }) => {
    await page.goto('/ozon/auth');

    // 验证导航到商品页
    await page.getByRole('link', { name: /商品管理/i }).click();
    await expect(page).toHaveURL(/\/ozon\/product/);

    // 验证导航到库存页
    await page.getByRole('link', { name: /库存管理/i }).click();
    await expect(page).toHaveURL(/\/ozon\/stock/);

    // 验证导航到订单页
    await page.getByRole('link', { name: /订单管理/i }).click();
    await expect(page).toHaveURL(/\/ozon\/posting/);
  });

  // ==================== 数据筛选和搜索 ====================

  test('基本搜索功能应该可用', async ({ page }) => {
    let searchCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async (request) => {
        const url = new URL(request.url());
        const keyword = url.searchParams.get('keyword');
        if (keyword) {
          searchCalled = true;
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

    await page.goto('/ozon/product?authId=auth-smoke-1&tab=draft');

    // 输入搜索关键词
    await page.getByPlaceholder(/搜索/i).fill('TEST-SKU');
    await page.getByRole('button', { name: /搜索/i }).click();

    // 验证搜索请求已发送
    await page.waitForTimeout(500);
    expect(searchCalled).toBe(true);
  });

  // ==================== 性能基准 ====================

  test('页面加载时间应该在可接受范围内', async ({ page }) => {
    const startTime = Date.now();

    await page.goto('/ozon/product?authId=auth-smoke-1');
    await expect(page.getByRole('heading', { name: /商品管理/i })).toBeVisible();

    const loadTime = Date.now() - startTime;

    // 页面加载应该在 3 秒内完成
    expect(loadTime).toBeLessThan(3000);
  });
});
