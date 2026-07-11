import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

/**
 * Phase 4: Stock/Price 工作台生产化功能测试
 *
 * 测试范围：
 * 1. StockTaskHistoryPanel 任务历史面板
 * 2. PriceTaskHistoryPanel 任务历史面板
 * 3. 跨页面导航和参数传递
 */
test.describe('Phase 4: Stock/Price Workbench Features', () => {

  // ==================== StockTaskHistoryPanel 测试 ====================

  test('StockTaskHistoryPanel displays task history list', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/task/history': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              taskId: 'task-1',
              taskStatus: 'SUCCESS',
              requestedCount: 100,
              successCount: 95,
              failedCount: 5,
              createTime: '2026-06-20T10:00:00Z',
              updateTime: '2026-06-20T10:30:00Z'
            },
            {
              taskId: 'task-2',
              taskStatus: 'FAILED',
              requestedCount: 50,
              successCount: 0,
              failedCount: 50,
              errorSummary: '授权失败',
              createTime: '2026-06-21T09:00:00Z',
              updateTime: '2026-06-21T09:15:00Z'
            },
            {
              taskId: 'task-3',
              taskStatus: 'RUNNING',
              requestedCount: 80,
              successCount: 40,
              failedCount: 0,
              createTime: '2026-06-22T08:00:00Z',
              updateTime: '2026-06-22T08:10:00Z'
            }
          ]
        })
      })
    });

    await page.goto('/ozon/stock?authId=auth-1');

    // 打开任务历史面板
    await page.getByRole('button', { name: '任务历史' }).click();

    // 验证任务列表显示
    await expect(page.getByText('task-1')).toBeVisible();
    await expect(page.getByText('task-2')).toBeVisible();
    await expect(page.getByText('task-3')).toBeVisible();

    // 验证状态显示
    await expect(page.getByText('SUCCESS')).toBeVisible();
    await expect(page.getByText('FAILED')).toBeVisible();
    await expect(page.getByText('RUNNING')).toBeVisible();

    // 验证统计数据
    await expect(page.getByText('95/100')).toBeVisible(); // 成功/总数
  });

  test('StockTaskHistoryPanel displays error summary', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/task/history': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [] })
      }),
      'GET /ozon/api/v1/stock/task/error-summary': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            '请求超时': 5,
            '授权失败': 3,
            '参数错误': 2
          }
        })
      })
    });

    await page.goto('/ozon/stock?authId=auth-1');

    // 打开任务历史面板
    await page.getByRole('button', { name: '任务历史' }).click();

    // 验证错误摘要显示
    await expect(page.getByText('错误摘要')).toBeVisible();
    await expect(page.getByText('请求超时: 5')).toBeVisible();
    await expect(page.getByText('授权失败: 3')).toBeVisible();
    await expect(page.getByText('参数错误: 2')).toBeVisible();
  });

  test('StockTaskHistoryPanel opens task detail dialog', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/task/history': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              taskId: 'task-1',
              taskStatus: 'SUCCESS',
              requestedCount: 100,
              successCount: 95,
              failedCount: 5
            }
          ]
        })
      }),
      'GET /ozon/api/v1/stock/task/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            taskId: 'task-1',
            taskStatus: 'SUCCESS',
            requestedCount: 100,
            successCount: 95,
            failedCount: 5,
            errorMessage: '5 items failed validation',
            createTime: '2026-06-20T10:00:00Z',
            updateTime: '2026-06-20T10:30:00Z'
          }
        })
      })
    });

    await page.goto('/ozon/stock?authId=auth-1');

    // 打开任务历史面板
    await page.getByRole('button', { name: '任务历史' }).click();

    // 点击任务查看详情
    await page.getByText('task-1').click();

    // 验证详情对话框显示
    await expect(page.getByText('任务详情')).toBeVisible();
    await expect(page.getByText('task-1')).toBeVisible();
    await expect(page.getByText('5 items failed validation')).toBeVisible();
  });

  test('StockTaskHistoryPanel filters tasks by SKU', async ({ page }) => {
    let skuParam = null;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/task/list-by-sku': async (request) => {
        const url = new URL(request.url());
        skuParam = url.searchParams.get('sku');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [
              {
                taskId: 'task-sku-1',
                taskStatus: 'SUCCESS',
                requestedCount: 10,
                successCount: 10,
                failedCount: 0
              }
            ]
          })
        };
      }
    });

    await page.goto('/ozon/stock?authId=auth-1&sku=TEST-SKU-001');

    // 验证 SKU 筛选生效
    await page.waitForTimeout(500);
    expect(skuParam).toBe('TEST-SKU-001');

    // 验证筛选后的任务列表
    await expect(page.getByText('task-sku-1')).toBeVisible();
  });

  // ==================== PriceTaskHistoryPanel 测试 ====================

  test('PriceTaskHistoryPanel displays task history list', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/price/task/history': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              taskId: 'price-task-1',
              taskStatus: 'SUCCESS',
              requestedCount: 200,
              successCount: 195,
              failedCount: 5,
              createTime: '2026-06-20T11:00:00Z',
              updateTime: '2026-06-20T11:30:00Z'
            },
            {
              taskId: 'price-task-2',
              taskStatus: 'FAILED',
              requestedCount: 100,
              successCount: 0,
              failedCount: 100,
              errorSummary: '价格格式错误',
              createTime: '2026-06-21T10:00:00Z',
              updateTime: '2026-06-21T10:20:00Z'
            }
          ]
        })
      })
    });

    await page.goto('/ozon/price?authId=auth-1');

    // 打开任务历史面板
    await page.getByRole('button', { name: '任务历史' }).click();

    // 验证任务列表显示
    await expect(page.getByText('price-task-1')).toBeVisible();
    await expect(page.getByText('price-task-2')).toBeVisible();

    // 验证统计数据
    await expect(page.getByText('195/200')).toBeVisible();
  });

  test('PriceTaskHistoryPanel displays error summary', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/price/task/history': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [] })
      }),
      'GET /ozon/api/v1/price/task/error-summary': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            '参数错误': 8,
            '商品不存在': 4,
            '服务器错误': 2
          }
        })
      })
    });

    await page.goto('/ozon/price?authId=auth-1');

    // 打开任务历史面板
    await page.getByRole('button', { name: '任务历史' }).click();

    // 验证错误摘要显示
    await expect(page.getByText('错误摘要')).toBeVisible();
    await expect(page.getByText('参数错误: 8')).toBeVisible();
    await expect(page.getByText('商品不存在: 4')).toBeVisible();
    await expect(page.getByText('服务器错误: 2')).toBeVisible();
  });

  test('PriceTaskHistoryPanel opens task detail dialog', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/price/task/history': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              taskId: 'price-task-1',
              taskStatus: 'SUCCESS',
              requestedCount: 150,
              successCount: 145,
              failedCount: 5
            }
          ]
        })
      }),
      'GET /ozon/api/v1/price/task/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            taskId: 'price-task-1',
            taskStatus: 'SUCCESS',
            requestedCount: 150,
            successCount: 145,
            failedCount: 5,
            errorMessage: '5 prices out of range',
            createTime: '2026-06-20T11:00:00Z',
            updateTime: '2026-06-20T11:30:00Z'
          }
        })
      })
    });

    await page.goto('/ozon/price?authId=auth-1');

    // 打开任务历史面板
    await page.getByRole('button', { name: '任务历史' }).click();

    // 点击任务查看详情
    await page.getByText('price-task-1').click();

    // 验证详情对话框显示
    await expect(page.getByText('任务详情')).toBeVisible();
    await expect(page.getByText('5 prices out of range')).toBeVisible();
  });

  // ==================== 跨页面导航测试 ====================

  test('Navigation from Product to Stock with SKU parameter', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'draft-1',
              draftName: 'Test Product',
              sku: 'TEST-SKU-001'
            }
          ]
        })
      }),
      'GET /ozon/api/v1/stock/task/list-by-sku': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [] })
      })
    });

    await page.goto('/ozon/product?authId=auth-1');

    // 点击跳转到库存管理
    await page.getByRole('button', { name: '查看库存' }).click();

    // 验证 URL 包含 SKU 参数
    await expect(page).toHaveURL(/\/ozon\/stock\?authId=auth-1&sku=TEST-SKU-001/);
  });

  test('Navigation from Product to Price with SKU parameter', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'draft-1',
              draftName: 'Test Product',
              sku: 'TEST-SKU-002'
            }
          ]
        })
      }),
      'GET /ozon/api/v1/price/task/list-by-sku': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [] })
      })
    });

    await page.goto('/ozon/product?authId=auth-1');

    // 点击跳转到价格管理
    await page.getByRole('button', { name: '查看价格' }).click();

    // 验证 URL 包含 SKU 参数
    await expect(page).toHaveURL(/\/ozon\/price\?authId=auth-1&sku=TEST-SKU-002/);
  });

  test('Back navigation from Stock to Product preserves context', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/task/list-by-sku': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [] })
      }),
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'draft-1',
              draftName: 'Test Product',
              sku: 'TEST-SKU-001'
            }
          ]
        })
      })
    });

    // 从 Stock 页面开始（带 SKU 参数）
    await page.goto('/ozon/stock?authId=auth-1&sku=TEST-SKU-001&from=product');

    // 点击返回按钮
    await page.getByRole('button', { name: '返回' }).click();

    // 验证返回到 Product 页面
    await expect(page).toHaveURL(/\/ozon\/product\?authId=auth-1/);
  });

  test('Task history panel refreshes on interval', async ({ page }) => {
    let requestCount = 0;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/task/history': async () => {
        requestCount++;
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [
              {
                taskId: `task-${requestCount}`,
                taskStatus: 'SUCCESS',
                requestedCount: 100,
                successCount: 100,
                failedCount: 0
              }
            ]
          })
        };
      }
    });

    await page.goto('/ozon/stock?authId=auth-1');

    // 打开任务历史面板
    await page.getByRole('button', { name: '任务历史' }).click();

    // 初始加载
    await expect(page.getByText('task-1')).toBeVisible();

    // 等待自动刷新（假设刷新间隔为 5 秒）
    await page.waitForTimeout(6000);

    // 验证已发起多次请求
    expect(requestCount).toBeGreaterThan(1);
  });

  test('Task history panel handles empty state', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/task/history': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [] })
      }),
      'GET /ozon/api/v1/stock/task/error-summary': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: {} })
      })
    });

    await page.goto('/ozon/stock?authId=auth-1');

    // 打开任务历史面板
    await page.getByRole('button', { name: '任务历史' }).click();

    // 验证空状态提示
    await expect(page.getByText('暂无任务历史')).toBeVisible();
  });

  test('Task history panel handles loading state', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/stock/task/history': async () => {
        // 延迟响应以显示加载状态
        await new Promise(resolve => setTimeout(resolve, 1000));
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: [] })
        };
      }
    });

    await page.goto('/ozon/stock?authId=auth-1');

    // 打开任务历史面板
    await page.getByRole('button', { name: '任务历史' }).click();

    // 验证加载状态显示
    await expect(page.getByText('加载中')).toBeVisible();
  });
});
