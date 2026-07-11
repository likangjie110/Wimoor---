import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

/**
 * Ads 工作台回归测试
 *
 * 测试范围：
 * 1. 广告活动列表
 * 2. 广告报告展示
 * 3. 功能开关提示
 * 4. 错误处理
 */
test.describe('Ads Workbench Regression', () => {

  test.beforeEach(async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            ads: { enabled: true, name: '广告管理', permission: 'read' },
            adsSync: { enabled: true, name: '广告同步', permission: 'write' }
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

  test('应该正确加载广告活动列表', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/campaign/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'campaign-1',
                campaignId: 'camp-1',
                campaignName: 'Summer Sale Campaign',
                type: 'SEARCH',
                status: 'ACTIVE',
                budget: 10000.00,
                spent: 2500.00,
                impressions: 50000,
                clicks: 1000,
                ctr: 2.0,
                conversions: 50,
                revenue: 5000.00,
                roas: 2.0,
                startDate: '2026-06-01',
                endDate: '2026-06-30'
              },
              {
                id: 'campaign-2',
                campaignId: 'camp-2',
                campaignName: 'Brand Awareness',
                type: 'DISPLAY',
                status: 'PAUSED',
                budget: 5000.00,
                spent: 1000.00,
                impressions: 20000,
                clicks: 300,
                ctr: 1.5,
                conversions: 10,
                revenue: 1000.00,
                roas: 1.0,
                startDate: '2026-06-01',
                endDate: '2026-06-30'
              }
            ],
            total: 2
          }
        })
      })
    });

    await page.goto('/ozon/ads?authId=auth-1');

    // 验证页面标题
    await expect(page.getByRole('heading', { name: /广告管理/i })).toBeVisible();

    // 验证数据加载
    await expect(page.getByText('Summer Sale Campaign')).toBeVisible();
    await expect(page.getByText('SEARCH')).toBeVisible();
    await expect(page.getByText('ACTIVE')).toBeVisible();
    await expect(page.getByText('10000.00')).toBeVisible(); // 预算
    await expect(page.getByText('2500.00')).toBeVisible();  // 花费

    await expect(page.getByText('Brand Awareness')).toBeVisible();
    await expect(page.getByText('DISPLAY')).toBeVisible();
    await expect(page.getByText('PAUSED')).toBeVisible();
  });

  test('应该正确展示广告报告', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/campaign/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [], total: 0 }
        })
      }),
      'GET /ozon/api/v1/ads/report/summary': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            totalBudget: 15000.00,
            totalSpent: 3500.00,
            totalImpressions: 70000,
            totalClicks: 1300,
            avgCtr: 1.86,
            totalConversions: 60,
            totalRevenue: 6000.00,
            avgRoas: 1.71,
            currency: 'RUB'
          }
        })
      })
    });

    await page.goto('/ozon/ads?authId=auth-1');

    // 验证报告数据
    await expect(page.getByText('总预算')).toBeVisible();
    await expect(page.getByText('15000.00')).toBeVisible();

    await expect(page.getByText('总花费')).toBeVisible();
    await expect(page.getByText('3500.00')).toBeVisible();

    await expect(page.getByText('总展示')).toBeVisible();
    await expect(page.getByText('70000')).toBeVisible();

    await expect(page.getByText('总点击')).toBeVisible();
    await expect(page.getByText('1300')).toBeVisible();

    await expect(page.getByText('平均CTR')).toBeVisible();
    await expect(page.getByText('1.86%')).toBeVisible();

    await expect(page.getByText('总转化')).toBeVisible();
    await expect(page.getByText('60')).toBeVisible();

    await expect(page.getByText('平均ROAS')).toBeVisible();
    await expect(page.getByText('1.71')).toBeVisible();
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
            ads: { enabled: false, name: '广告管理', permission: 'read', disabledReason: '广告功能未开启' }
          }
        })
      })
    });

    await page.goto('/ozon/ads?authId=auth-1');

    // 验证禁用提示
    await expect(page.getByText(/广告功能未开启/i)).toBeVisible();
  });

  test('应该在同步权限未开启时禁用同步按钮', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            ads: { enabled: true, name: '广告管理', permission: 'read' },
            adsSync: { enabled: false, name: '广告同步', permission: 'write', disabledReason: '广告同步功能未开启' }
          }
        })
      }),
      'GET /ozon/api/v1/ads/campaign/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [], total: 0 }
        })
      })
    });

    await page.goto('/ozon/ads?authId=auth-1');

    // 验证同步按钮被禁用
    const syncBtn = page.getByRole('button', { name: /同步广告/i });
    await expect(syncBtn).toBeDisabled();

    // 悬停查看提示
    await syncBtn.hover();
    await expect(page.getByText(/广告同步功能未开启/i)).toBeVisible();
  });

  // ==================== 用户操作流程 ====================

  test('应该支持同步广告数据', async ({ page }) => {
    let syncCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/campaign/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [], total: 0 }
        })
      }),
      'POST /ozon/api/v1/ads/sync': async (request) => {
        syncCalled = true;
        const body = await request.postDataJSON();
        expect(body.authId).toBe('auth-1');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { taskId: 'sync-task-1', total: 10 }
          })
        };
      }
    });

    await page.goto('/ozon/ads?authId=auth-1');

    // 点击同步按钮
    await page.getByRole('button', { name: /同步广告/i }).click();

    // 确认
    await page.getByRole('button', { name: /确定/i }).click();

    // 验证成功提示
    await expect(page.getByText(/同步任务已创建/i)).toBeVisible();

    expect(syncCalled).toBe(true);
  });

  test('应该支持按日期范围筛选广告数据', async ({ page }) => {
    let filterCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/campaign/list': async (request) => {
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

    await page.goto('/ozon/ads?authId=auth-1');

    // 选择日期范围
    await page.getByLabel('开始日期').fill('2026-06-01');
    await page.getByLabel('结束日期').fill('2026-06-30');

    // 点击查询
    await page.getByRole('button', { name: /查询/i }).click();

    expect(filterCalled).toBe(true);
  });

  test('应该支持按状态筛选广告活动', async ({ page }) => {
    let filterCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/campaign/list': async (request) => {
        const url = new URL(request.url());
        const status = url.searchParams.get('status');
        if (status) {
          filterCalled = true;
          expect(status).toBe('ACTIVE');
        }
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              records: status === 'ACTIVE' ? [
                { id: 'campaign-1', campaignName: 'Active Campaign', status: 'ACTIVE' }
              ] : [],
              total: status === 'ACTIVE' ? 1 : 0
            }
          })
        };
      }
    });

    await page.goto('/ozon/ads?authId=auth-1');

    // 选择状态筛选
    await page.getByLabel(/活动状态/i).click();
    await page.getByRole('option', { name: /活动中/i }).click();

    // 验证筛选结果
    await expect(page.getByText('Active Campaign')).toBeVisible();
    await expect(page.getByText('ACTIVE')).toBeVisible();

    expect(filterCalled).toBe(true);
  });

  test('应该支持查看广告活动详情', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/campaign/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'campaign-1', campaignId: 'camp-1', campaignName: 'Test Campaign', status: 'ACTIVE' }
            ],
            total: 1
          }
        })
      }),
      'GET /ozon/api/v1/ads/campaign/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'campaign-1',
            campaignId: 'camp-1',
            campaignName: 'Test Campaign',
            type: 'SEARCH',
            status: 'ACTIVE',
            budget: 10000.00,
            spent: 2500.00,
            dailyBudget: 500.00,
            targetProducts: [
              { sku: 'TEST-SKU-001', productName: 'Test Product' }
            ],
            keywords: [
              { keyword: 'test keyword', bid: 10.00, status: 'ACTIVE' }
            ]
          }
        })
      })
    });

    await page.goto('/ozon/ads?authId=auth-1');

    // 点击查看详情
    await page.locator('[data-campaign-id="campaign-1"]').getByRole('button', { name: /详情/i }).click();

    // 验证详情展示
    await expect(page.getByText('Test Campaign')).toBeVisible();
    await expect(page.getByText('SEARCH')).toBeVisible();
    await expect(page.getByText('500.00')).toBeVisible(); // 日预算

    // 验证目标商品
    await expect(page.getByText('TEST-SKU-001')).toBeVisible();
    await expect(page.getByText('Test Product')).toBeVisible();

    // 验证关键词
    await expect(page.getByText('test keyword')).toBeVisible();
    await expect(page.getByText('10.00')).toBeVisible(); // 出价
  });

  test('应该支持导出广告报告', async ({ page }) => {
    let exportCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/campaign/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'campaign-1', campaignName: 'Test Campaign' }
            ],
            total: 1
          }
        })
      }),
      'POST /ozon/api/v1/ads/report/export': async (request) => {
        exportCalled = true;
        return {
          status: 200,
          contentType: 'application/octet-stream',
          body: 'mock-excel-data'
        };
      }
    });

    await page.goto('/ozon/ads?authId=auth-1');

    // 点击导出按钮
    const downloadPromise = page.waitForEvent('download');
    await page.getByRole('button', { name: /导出报告/i }).click();

    // 验证下载
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toContain('.xlsx');

    expect(exportCalled).toBe(true);
  });

  // ==================== 错误处理 ====================

  test('应该正确处理广告列表加载失败', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/campaign/list': async () => ({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 500,
          message: '服务器错误'
        })
      })
    });

    await page.goto('/ozon/ads?authId=auth-1');

    // 验证错误提示
    await expect(page.getByText(/加载失败/i)).toBeVisible();
  });

  test('应该正确处理同步失败', async ({ page }) => {
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
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 400,
          message: 'API 密钥无效'
        })
      })
    });

    await page.goto('/ozon/ads?authId=auth-1');

    // 点击同步并确认
    await page.getByRole('button', { name: /同步广告/i }).click();
    await page.getByRole('button', { name: /确定/i }).click();

    // 验证错误提示
    await expect(page.getByText(/API 密钥无效/i)).toBeVisible();
  });

  // ==================== 深链跳转 ====================

  test('应该支持从广告活动跳转到商品详情', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/campaign/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [], total: 0 }
        })
      }),
      'GET /ozon/api/v1/ads/campaign/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'campaign-1',
            campaignName: 'Test Campaign',
            targetProducts: [
              { sku: 'TEST-SKU-001', draftId: 'draft-1' }
            ]
          }
        })
      })
    });

    await page.goto('/ozon/ads?authId=auth-1');

    // 打开活动详情
    await page.locator('[data-campaign-id="campaign-1"]').getByRole('button', { name: /详情/i }).click();

    // 点击商品链接
    await page.getByRole('link', { name: 'TEST-SKU-001' }).click();

    // 验证跳转
    await expect(page).toHaveURL(/\/ozon\/product\/draft\/draft-1/);
  });
});
