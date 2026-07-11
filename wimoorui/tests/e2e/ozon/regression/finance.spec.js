import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

/**
 * Finance 工作台回归测试
 *
 * 测试范围：
 * 1. 交易记录列表
 * 2. 销售明细列表
 * 3. 财务报告
 * 4. 功能开关提示
 * 5. 错误处理
 */
test.describe('Finance Workbench Regression', () => {

  test.beforeEach(async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            finance: { enabled: true, name: '财务管理', permission: 'read' }
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

  test('应该正确加载交易记录列表', async ({ page }) => {
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
                currency: 'RUB',
                status: 'SUCCESS'
              },
              {
                id: 'trans-2',
                transactionDate: '2026-06-24',
                postingNumber: 'POST-002',
                type: 'REFUND',
                amount: -50.00,
                currency: 'RUB',
                status: 'SUCCESS'
              }
            ],
            total: 2
          }
        })
      })
    });

    await page.goto('/ozon/finance?authId=auth-1&tab=transaction');

    // 验证页面标题
    await expect(page.getByRole('heading', { name: /财务管理/i })).toBeVisible();

    // 验证数据加载
    await expect(page.getByText('POST-001')).toBeVisible();
    await expect(page.getByText('SALE')).toBeVisible();
    await expect(page.getByText('250.00')).toBeVisible();

    await expect(page.getByText('POST-002')).toBeVisible();
    await expect(page.getByText('REFUND')).toBeVisible();
    await expect(page.getByText('-50.00')).toBeVisible();
  });

  test('应该正确加载销售明细列表', async ({ page }) => {
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
                productName: 'Test Product',
                quantity: 2,
                price: 100.00,
                commission: 15.00,
                netAmount: 185.00,
                currency: 'RUB'
              },
              {
                id: 'detail-2',
                saleDate: '2026-06-24',
                sku: 'TEST-SKU-002',
                productName: 'Another Product',
                quantity: 1,
                price: 50.00,
                commission: 7.50,
                netAmount: 42.50,
                currency: 'RUB'
              }
            ],
            total: 2
          }
        })
      })
    });

    await page.goto('/ozon/finance?authId=auth-1&tab=sales');

    // 验证 Tab 切换
    await expect(page.getByRole('tab', { name: /销售明细/i })).toHaveAttribute('aria-selected', 'true');

    // 验证数据
    await expect(page.getByText('TEST-SKU-001')).toBeVisible();
    await expect(page.getByText('Test Product')).toBeVisible();
    await expect(page.getByText('185.00')).toBeVisible();

    await expect(page.getByText('TEST-SKU-002')).toBeVisible();
    await expect(page.getByText('Another Product')).toBeVisible();
  });

  // ==================== 功能开关提示 ====================

  test('应该在功能未开启时显示提示', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            finance: { enabled: false, name: '财务管理', permission: 'read', disabledReason: '财务功能未开启' }
          }
        })
      })
    });

    await page.goto('/ozon/finance?authId=auth-1');

    // 验证禁用提示
    await expect(page.getByText(/财务功能未开启/i)).toBeVisible();
  });

  // ==================== 用户操作流程 ====================

  test('应该支持按日期范围筛选交易', async ({ page }) => {
    let filterCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/finance/transaction/list': async (request) => {
        const url = new URL(request.url());
        const startDate = url.searchParams.get('startDate');
        const endDate = url.searchParams.get('endDate');
        if (startDate && endDate) {
          filterCalled = true;
          expect(startDate).toBe('2026-06-01');
          expect(endDate).toBe('2026-06-30');
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

    await page.goto('/ozon/finance?authId=auth-1&tab=transaction');

    // 选择日期范围
    await page.getByLabel('开始日期').fill('2026-06-01');
    await page.getByLabel('结束日期').fill('2026-06-30');

    // 点击查询
    await page.getByRole('button', { name: /查询/i }).click();

    expect(filterCalled).toBe(true);
  });

  test('应该支持导出财务数据', async ({ page }) => {
    let exportCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/finance/transaction/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [{ id: 'trans-1', amount: 100 }], total: 1 }
        })
      }),
      'POST /ozon/api/v1/finance/transaction/export': async (request) => {
        exportCalled = true;
        return {
          status: 200,
          contentType: 'application/octet-stream',
          body: 'mock-excel-data'
        };
      }
    });

    await page.goto('/ozon/finance?authId=auth-1&tab=transaction');

    // 点击导出按钮
    const downloadPromise = page.waitForEvent('download');
    await page.getByRole('button', { name: /导出/i }).click();

    // 验证下载
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toContain('.xlsx');

    expect(exportCalled).toBe(true);
  });

  // ==================== 错误处理 ====================

  test('应该正确处理交易记录加载失败', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/finance/transaction/list': async () => ({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 500,
          message: '服务器错误'
        })
      })
    });

    await page.goto('/ozon/finance?authId=auth-1&tab=transaction');

    // 验证错误提示
    await expect(page.getByText(/加载失败/i)).toBeVisible();
  });
});
