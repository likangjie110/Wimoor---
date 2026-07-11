import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

/**
 * Phase 5: AfterSale Panel 售后功能测试
 *
 * 测试范围：
 * 1. AfterSalePanel 组件渲染
 * 2. Tab 切换（退货、包裹、取消）
 * 3. "同步退货"按钮功能
 * 4. "同步包裹"按钮功能
 * 5. "取消订单"按钮功能（含二次确认）
 * 6. Loading 状态
 * 7. 成功/错误提示
 */
test.describe('Phase 5: AfterSale Panel Features', () => {

  // ==================== 组件渲染测试 ====================

  test('AfterSalePanel renders with three tabs', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/posting/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'posting-1',
            postingNumber: 'POSTING-001',
            postingStatus: 'delivered',
            packages: [],
            returns: [],
            cancellations: []
          }
        })
      }),
      'GET /ozon/api/v1/aftersale/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            packages: [],
            returns: [],
            cancellations: []
          }
        })
      })
    });

    await page.goto('/ozon/posting/detail?authId=auth-1&postingId=posting-1');

    // 验证 AfterSale 标签页存在
    await expect(page.getByRole('tab', { name: '退货' })).toBeVisible();
    await expect(page.getByRole('tab', { name: '包裹' })).toBeVisible();
    await expect(page.getByRole('tab', { name: '取消' })).toBeVisible();
  });

  test('AfterSalePanel displays returns list', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/aftersale/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            packages: [],
            returns: [
              {
                id: 'ret-1',
                returnNumber: 'RET-001',
                returnStatus: 'PENDING',
                reason: 'damaged',
                quantity: 1,
                createdAt: '2026-06-20T10:00:00Z'
              },
              {
                id: 'ret-2',
                returnNumber: 'RET-002',
                returnStatus: 'COMPLETED',
                reason: 'wrong item',
                quantity: 2,
                createdAt: '2026-06-21T10:00:00Z'
              }
            ],
            cancellations: []
          }
        })
      })
    });

    await page.goto('/ozon/posting/detail?authId=auth-1&postingId=posting-1');
    await page.getByRole('tab', { name: '退货' }).click();

    await expect(page.getByText('RET-001')).toBeVisible();
    await expect(page.getByText('RET-002')).toBeVisible();
    await expect(page.getByText('damaged')).toBeVisible();
    await expect(page.getByText('wrong item')).toBeVisible();
  });

  test('AfterSalePanel displays packages list', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/aftersale/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            packages: [
              {
                id: 'pkg-1',
                packageNumber: 'PKG-001',
                packageStatus: 'DELIVERED',
                trackingNumber: 'TRACK-123',
                createdAt: '2026-06-20T10:00:00Z'
              }
            ],
            returns: [],
            cancellations: []
          }
        })
      })
    });

    await page.goto('/ozon/posting/detail?authId=auth-1&postingId=posting-1');
    await page.getByRole('tab', { name: '包裹' }).click();

    await expect(page.getByText('PKG-001')).toBeVisible();
    await expect(page.getByText('DELIVERED')).toBeVisible();
    await expect(page.getByText('TRACK-123')).toBeVisible();
  });

  // ==================== Tab 切换测试 ====================

  test('AfterSalePanel tab switching works correctly', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/aftersale/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            packages: [{ id: 'pkg-1', packageNumber: 'PKG-001' }],
            returns: [{ id: 'ret-1', returnNumber: 'RET-001' }],
            cancellations: [{ id: 'can-1', cancellationNumber: 'CAN-001' }]
          }
        })
      })
    });

    await page.goto('/ozon/posting/detail?authId=auth-1&postingId=posting-1');

    // 默认显示退货
    await page.getByRole('tab', { name: '退货' }).click();
    await expect(page.getByText('RET-001')).toBeVisible();

    // 切换到包裹
    await page.getByRole('tab', { name: '包裹' }).click();
    await expect(page.getByText('PKG-001')).toBeVisible();

    // 切换到取消
    await page.getByRole('tab', { name: '取消' }).click();
    await expect(page.getByText('CAN-001')).toBeVisible();
  });

  // ==================== 同步退货测试 ====================

  test('Sync Returns button triggers API call and shows success message', async ({ page }) => {
    let syncReturnsCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/aftersale/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { packages: [], returns: [], cancellations: [] }
        })
      }),
      'POST /ozon/api/v1/aftersale/sync-returns': async () => {
        syncReturnsCalled = true;
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            message: '退货同步成功'
          })
        };
      }
    });

    await page.goto('/ozon/posting/detail?authId=auth-1&postingId=posting-1');
    await page.getByRole('tab', { name: '退货' }).click();

    const syncButton = page.getByRole('button', { name: '同步退货' });
    await syncButton.click();

    // 验证 Loading 状态
    await expect(syncButton).toBeDisabled();

    // 等待成功消息
    await expect(page.getByText('退货同步成功')).toBeVisible({ timeout: 5000 });

    // 验证 API 被调用
    expect(syncReturnsCalled).toBe(true);
  });

  test('Sync Returns handles API failure gracefully', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/aftersale/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { packages: [], returns: [], cancellations: [] }
        })
      }),
      'POST /ozon/api/v1/aftersale/sync-returns': async () => ({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 500,
          message: 'API 调用失败'
        })
      })
    });

    await page.goto('/ozon/posting/detail?authId=auth-1&postingId=posting-1');
    await page.getByRole('tab', { name: '退货' }).click();

    await page.getByRole('button', { name: '同步退货' }).click();

    // 验证错误消息
    await expect(page.getByText(/API 调用失败|同步失败/)).toBeVisible({ timeout: 5000 });
  });

  // ==================== 同步包裹测试 ====================

  test('Sync Packages button triggers API call and shows success message', async ({ page }) => {
    let syncPackagesCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/aftersale/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { packages: [], returns: [], cancellations: [] }
        })
      }),
      'POST /ozon/api/v1/aftersale/sync-packages': async () => {
        syncPackagesCalled = true;
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            message: '包裹同步成功'
          })
        };
      }
    });

    await page.goto('/ozon/posting/detail?authId=auth-1&postingId=posting-1');
    await page.getByRole('tab', { name: '包裹' }).click();

    const syncButton = page.getByRole('button', { name: '同步包裹' });
    await syncButton.click();

    // 验证成功消息
    await expect(page.getByText('包裹同步成功')).toBeVisible({ timeout: 5000 });
    expect(syncPackagesCalled).toBe(true);
  });

  // ==================== 取消订单测试 ====================

  test('Cancel Posting shows confirmation dialog', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/aftersale/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { packages: [], returns: [], cancellations: [] }
        })
      })
    });

    await page.goto('/ozon/posting/detail?authId=auth-1&postingId=posting-1');
    await page.getByRole('tab', { name: '取消' }).click();

    await page.getByRole('button', { name: '取消订单' }).click();

    // 验证确认对话框出现
    await expect(page.getByText(/确认取消订单|是否确认/)).toBeVisible();
    await expect(page.getByRole('button', { name: '确定' })).toBeVisible();
    await expect(page.getByRole('button', { name: '取消' })).toBeVisible();
  });

  test('Cancel Posting executes when confirmed', async ({ page }) => {
    let cancelCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/aftersale/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { packages: [], returns: [], cancellations: [] }
        })
      }),
      'POST /ozon/api/v1/aftersale/cancel-posting': async () => {
        cancelCalled = true;
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            message: '订单取消成功'
          })
        };
      }
    });

    await page.goto('/ozon/posting/detail?authId=auth-1&postingId=posting-1');
    await page.getByRole('tab', { name: '取消' }).click();

    await page.getByRole('button', { name: '取消订单' }).click();

    // 点击确认
    await page.getByRole('button', { name: '确定' }).click();

    // 验证成功消息
    await expect(page.getByText('订单取消成功')).toBeVisible({ timeout: 5000 });
    expect(cancelCalled).toBe(true);
  });

  test('Cancel Posting does not execute when cancelled in dialog', async ({ page }) => {
    let cancelCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/aftersale/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { packages: [], returns: [], cancellations: [] }
        })
      }),
      'POST /ozon/api/v1/aftersale/cancel-posting': async () => {
        cancelCalled = true;
        return { status: 200 };
      }
    });

    await page.goto('/ozon/posting/detail?authId=auth-1&postingId=posting-1');
    await page.getByRole('tab', { name: '取消' }).click();

    await page.getByRole('button', { name: '取消订单' }).click();

    // 点击取消按钮
    await page.getByRole('button', { name: '取消' }).click();

    // 等待一小段时间确保没有 API 调用
    await page.waitForTimeout(1000);
    expect(cancelCalled).toBe(false);
  });

  // ==================== Loading 状态测试 ====================

  test('Sync buttons show loading state during API call', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/aftersale/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { packages: [], returns: [], cancellations: [] }
        })
      }),
      'POST /ozon/api/v1/aftersale/sync-returns': async () => {
        // 模拟慢速 API
        await new Promise(resolve => setTimeout(resolve, 2000));
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, message: '成功' })
        };
      }
    });

    await page.goto('/ozon/posting/detail?authId=auth-1&postingId=posting-1');
    await page.getByRole('tab', { name: '退货' }).click();

    const syncButton = page.getByRole('button', { name: '同步退货' });
    await syncButton.click();

    // 验证按钮被禁用（Loading 状态）
    await expect(syncButton).toBeDisabled();

    // 等待完成后按钮恢复
    await expect(syncButton).toBeEnabled({ timeout: 5000 });
  });

  // ==================== 空状态测试 ====================

  test('AfterSalePanel shows empty state when no data', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/aftersale/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            packages: [],
            returns: [],
            cancellations: []
          }
        })
      })
    });

    await page.goto('/ozon/posting/detail?authId=auth-1&postingId=posting-1');

    // 检查退货空状态
    await page.getByRole('tab', { name: '退货' }).click();
    await expect(page.getByText(/暂无退货记录|无数据/)).toBeVisible();

    // 检查包裹空状态
    await page.getByRole('tab', { name: '包裹' }).click();
    await expect(page.getByText(/暂无包裹记录|无数据/)).toBeVisible();

    // 检查取消空状态
    await page.getByRole('tab', { name: '取消' }).click();
    await expect(page.getByText(/暂无取消记录|无数据/)).toBeVisible();
  });
});
