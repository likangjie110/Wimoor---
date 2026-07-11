import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

/**
 * Stock 工作台回归测试
 *
 * 测试范围：
 * 1. 库存列表加载和展示
 * 2. 库存推送功能
 * 3. 推送任务管理
 * 4. 功能开关提示
 * 5. 错误处理
 */
test.describe('Stock Workbench Regression', () => {

  test.beforeEach(async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            stockWrite: { enabled: true, name: '库存推送', permission: 'write' }
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

  test('应该正确加载库存列表', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'stock-1',
                productId: 'prod-1',
                sku: 'TEST-SKU-001',
                productName: 'Test Product',
                warehouseId: 'wh-1',
                warehouseName: 'Moscow Warehouse',
                stock: 100,
                reserved: 10,
                available: 90,
                updateTime: '2026-06-25T10:00:00Z'
              },
              {
                id: 'stock-2',
                productId: 'prod-2',
                sku: 'TEST-SKU-002',
                productName: 'Another Product',
                warehouseId: 'wh-1',
                warehouseName: 'Moscow Warehouse',
                stock: 50,
                reserved: 5,
                available: 45,
                updateTime: '2026-06-24T10:00:00Z'
              }
            ],
            total: 2
          }
        })
      })
    });

    await page.goto('/ozon/stock?authId=auth-1');

    // 验证页面标题
    await expect(page.getByRole('heading', { name: /库存管理/i })).toBeVisible();

    // 验证数据加载
    await expect(page.getByText('TEST-SKU-001')).toBeVisible();
    await expect(page.getByText('Test Product')).toBeVisible();
    await expect(page.getByText('100')).toBeVisible(); // 总库存
    await expect(page.getByText('90')).toBeVisible();  // 可用库存

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
            stockWrite: { enabled: false, name: '库存推送', permission: 'write', disabledReason: '库存写操作未开启' }
          }
        })
      }),
      'GET /ozon/api/v1/stock/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [{ id: 'stock-1', sku: 'TEST-001', stock: 100 }], total: 1 }
        })
      })
    });

    await page.goto('/ozon/stock?authId=auth-1');

    // 验证推送按钮被禁用
    const pushBtn = page.getByRole('button', { name: /推送库存/i });
    await expect(pushBtn).toBeDisabled();

    // 悬停查看提示
    await pushBtn.hover();
    await expect(page.getByText(/库存写操作未开启/i)).toBeVisible();
  });

  // ==================== 用户操作流程 ====================

  test('应该支持单个库存推送', async ({ page }) => {
    let pushCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'stock-1', productId: 'prod-1', sku: 'TEST-001', stock: 100, warehouseId: 'wh-1' }
            ],
            total: 1
          }
        })
      }),
      'POST /ozon/api/v1/stock/push': async (request) => {
        pushCalled = true;
        const body = await request.postDataJSON();
        expect(body.stockIds).toContain('stock-1');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { taskId: 'push-task-1', total: 1 }
          })
        };
      }
    });

    await page.goto('/ozon/stock?authId=auth-1');

    // 选择库存
    await page.locator('[data-stock-id="stock-1"] input[type="checkbox"]').check();

    // 点击推送按钮
    await page.getByRole('button', { name: /推送库存/i }).click();

    // 确认
    await page.getByRole('button', { name: /确定/i }).click();

    // 验证成功提示
    await expect(page.getByText(/推送任务已创建/i)).toBeVisible();

    expect(pushCalled).toBe(true);
  });

  test('应该支持批量库存推送', async ({ page }) => {
    let pushCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'stock-1', sku: 'TEST-001', stock: 100 },
              { id: 'stock-2', sku: 'TEST-002', stock: 50 },
              { id: 'stock-3', sku: 'TEST-003', stock: 75 }
            ],
            total: 3
          }
        })
      }),
      'POST /ozon/api/v1/stock/push': async (request) => {
        pushCalled = true;
        const body = await request.postDataJSON();
        expect(body.stockIds.length).toBe(3);
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { taskId: 'push-task-1', total: 3 }
          })
        };
      }
    });

    await page.goto('/ozon/stock?authId=auth-1');

    // 全选
    await page.locator('thead input[type="checkbox"]').check();

    // 点击批量推送
    await page.getByRole('button', { name: /批量推送/i }).click();
    await page.getByRole('button', { name: /确定/i }).click();

    // 验证成功提示
    await expect(page.getByText(/推送任务已创建/i)).toBeVisible();
    await expect(page.getByText(/共 3 条/i)).toBeVisible();

    expect(pushCalled).toBe(true);
  });

  test('应该支持查看推送任务', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [], total: 0 }
        })
      }),
      'GET /ozon/api/v1/stock/push-task/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'task-1',
              authId: 'auth-1',
              status: 'PROCESSING',
              total: 10,
              success: 5,
              failed: 0,
              createTime: '2026-06-25T10:00:00Z'
            },
            {
              id: 'task-2',
              authId: 'auth-1',
              status: 'SUCCESS',
              total: 20,
              success: 20,
              failed: 0,
              createTime: '2026-06-24T10:00:00Z'
            }
          ]
        })
      })
    });

    await page.goto('/ozon/stock?authId=auth-1');

    // 点击任务按钮
    await page.getByRole('button', { name: /推送任务/i }).click();

    // 验证任务列表
    await expect(page.getByText('task-1')).toBeVisible();
    await expect(page.getByText('PROCESSING')).toBeVisible();
    await expect(page.getByText('5 / 10')).toBeVisible();

    await expect(page.getByText('task-2')).toBeVisible();
    await expect(page.getByText('SUCCESS')).toBeVisible();
    await expect(page.getByText('20 / 20')).toBeVisible();
  });

  // ==================== 错误处理 ====================

  test('应该正确处理库存加载失败', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/list': async () => ({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 500,
          message: '服务器错误'
        })
      })
    });

    await page.goto('/ozon/stock?authId=auth-1');

    // 验证错误提示
    await expect(page.getByText(/加载失败/i)).toBeVisible();
  });

  test('应该正确处理推送失败', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [{ id: 'stock-1', sku: 'TEST-001', stock: 100 }], total: 1 }
        })
      }),
      'POST /ozon/api/v1/stock/push': async () => ({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 400,
          message: '仓库未配置'
        })
      })
    });

    await page.goto('/ozon/stock?authId=auth-1');

    // 选择库存并推送
    await page.locator('[data-stock-id="stock-1"] input[type="checkbox"]').check();
    await page.getByRole('button', { name: /推送库存/i }).click();
    await page.getByRole('button', { name: /确定/i }).click();

    // 验证错误提示
    await expect(page.getByText(/仓库未配置/i)).toBeVisible();
  });

  // ==================== 深链跳转 ====================

  test('应该支持从库存页跳转到商品详情', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'stock-1', productId: 'prod-1', sku: 'TEST-001', draftId: 'draft-1' }
            ],
            total: 1
          }
        })
      })
    });

    await page.goto('/ozon/stock?authId=auth-1');

    // 点击商品链接
    await page.getByRole('link', { name: 'TEST-001' }).click();

    // 验证跳转
    await expect(page).toHaveURL(/\/ozon\/product\/draft\/draft-1/);
  });

  test('应该支持从任务跳转到错误中心', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [], total: 0 }
        })
      }),
      'GET /ozon/api/v1/stock/push-task/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            { id: 'task-1', status: 'FAILED', failed: 5 }
          ]
        })
      })
    });

    await page.goto('/ozon/stock?authId=auth-1');

    // 打开任务面板
    await page.getByRole('button', { name: /推送任务/i }).click();

    // 点击错误链接
    await page.locator('[data-task-id="task-1"]').getByRole('link', { name: /查看错误/i }).click();

    // 验证跳转
    await expect(page).toHaveURL(/\/ozon\/error/);
    await expect(page).toHaveURL(/taskId=task-1/);
  });
});
