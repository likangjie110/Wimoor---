import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

/**
 * Price 工作台回归测试
 *
 * 测试范围：
 * 1. 价格列表加载和展示
 * 2. 价格推送功能
 * 3. 推送任务管理
 * 4. 功能开关提示
 * 5. 错误处理
 */
test.describe('Price Workbench Regression', () => {

  test.beforeEach(async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            priceWrite: { enabled: true, name: '价格推送', permission: 'write' }
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

  test('应该正确加载价格列表', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/price/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'price-1',
                productId: 'prod-1',
                sku: 'TEST-SKU-001',
                productName: 'Test Product',
                price: 100.00,
                oldPrice: 120.00,
                marketingPrice: 90.00,
                currency: 'RUB',
                updateTime: '2026-06-25T10:00:00Z'
              },
              {
                id: 'price-2',
                productId: 'prod-2',
                sku: 'TEST-SKU-002',
                productName: 'Another Product',
                price: 50.00,
                oldPrice: 60.00,
                marketingPrice: 45.00,
                currency: 'RUB',
                updateTime: '2026-06-24T10:00:00Z'
              }
            ],
            total: 2
          }
        })
      })
    });

    await page.goto('/ozon/price?authId=auth-1');

    // 验证页面标题
    await expect(page.getByRole('heading', { name: /价格管理/i })).toBeVisible();

    // 验证数据加载
    await expect(page.getByText('TEST-SKU-001')).toBeVisible();
    await expect(page.getByText('Test Product')).toBeVisible();
    await expect(page.getByText('100.00')).toBeVisible();
    await expect(page.getByText('120.00')).toBeVisible();

    await expect(page.getByText('TEST-SKU-002')).toBeVisible();
    await expect(page.getByText('Another Product')).toBeVisible();
  });

  // ==================== 功能开关提示 ====================

  test('应该在写权限未开启时禁用推送按钮', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            priceWrite: { enabled: false, name: '价格推送', permission: 'write', disabledReason: '价格写操作未开启' }
          }
        })
      }),
      'GET /ozon/api/v1/price/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [{ id: 'price-1', sku: 'TEST-001', price: 100 }], total: 1 }
        })
      })
    });

    await page.goto('/ozon/price?authId=auth-1');

    // 验证推送按钮被禁用
    const pushBtn = page.getByRole('button', { name: /推送价格/i });
    await expect(pushBtn).toBeDisabled();

    // 悬停查看提示
    await pushBtn.hover();
    await expect(page.getByText(/价格写操作未开启/i)).toBeVisible();
  });

  // ==================== 用户操作流程 ====================

  test('应该支持批量价格推送', async ({ page }) => {
    let pushCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/price/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'price-1', sku: 'TEST-001', price: 100 },
              { id: 'price-2', sku: 'TEST-002', price: 50 }
            ],
            total: 2
          }
        })
      }),
      'POST /ozon/api/v1/price/push': async (request) => {
        pushCalled = true;
        const body = await request.postDataJSON();
        expect(body.priceIds.length).toBe(2);
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { taskId: 'push-task-1', total: 2 }
          })
        };
      }
    });

    await page.goto('/ozon/price?authId=auth-1');

    // 全选
    await page.locator('thead input[type="checkbox"]').check();

    // 点击批量推送
    await page.getByRole('button', { name: /批量推送/i }).click();
    await page.getByRole('button', { name: /确定/i }).click();

    // 验证成功提示
    await expect(page.getByText(/推送任务已创建/i)).toBeVisible();

    expect(pushCalled).toBe(true);
  });

  // ==================== 错误处理 ====================

  test('应该正确处理推送失败', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/price/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [{ id: 'price-1', sku: 'TEST-001', price: 100 }], total: 1 }
        })
      }),
      'POST /ozon/api/v1/price/push': async () => ({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 400,
          message: '价格不能低于成本价'
        })
      })
    });

    await page.goto('/ozon/price?authId=auth-1');

    // 选择价格并推送
    await page.locator('[data-price-id="price-1"] input[type="checkbox"]').check();
    await page.getByRole('button', { name: /推送价格/i }).click();
    await page.getByRole('button', { name: /确定/i }).click();

    // 验证错误提示
    await expect(page.getByText(/价格不能低于成本价/i)).toBeVisible();
  });
});
