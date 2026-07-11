import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

/**
 * Auth 工作台回归测试
 *
 * 测试范围：
 * 1. 授权列表加载和展示
 * 2. 配送方式管理
 * 3. 仓库统计展示
 * 4. Tab 切换功能
 * 5. 功能开关提示
 * 6. 错误处理
 */
test.describe('Auth Workbench Regression', () => {

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
            productWrite: { enabled: false, name: '商品发布', permission: 'write' }
          }
        })
      }),
      'GET /ozon/api/v1/auth/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'auth-1',
              shopId: 'shop-1',
              shopName: 'Test Shop',
              clientId: 'client-1',
              isActive: true,
              createTime: '2026-06-20T10:00:00Z'
            }
          ]
        })
      })
    });
  });

  // ==================== 页面加载和渲染 ====================

  test('应该正确加载授权列表页面', async ({ page }) => {
    await page.goto('/ozon/auth');

    // 验证页面标题
    await expect(page.getByRole('heading', { name: /授权管理/i })).toBeVisible();

    // 验证数据加载
    await expect(page.getByText('Test Shop')).toBeVisible();
    await expect(page.getByText('client-1')).toBeVisible();
  });

  test('应该正确显示授权详情工作台', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/auth/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'auth-1',
            shopName: 'Test Shop',
            clientId: 'client-1',
            isActive: true
          }
        })
      }),
      'GET /ozon/api/v1/seller/delivery-method/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            { id: 'dm-1', methodCode: 'FBS', methodName: 'Fulfillment by Seller', isDefault: true },
            { id: 'dm-2', methodCode: 'FBO', methodName: 'Fulfillment by Ozon', isDefault: false }
          ]
        })
      }),
      'GET /ozon/api/v1/seller/warehouse/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            { id: 'wh-1', warehouseName: 'Moscow Warehouse', isEnabled: true }
          ]
        })
      })
    });

    await page.goto('/ozon/auth?authId=auth-1');

    // 验证配送方式面板
    await expect(page.getByText('配送方式')).toBeVisible();
    await expect(page.getByText('Fulfillment by Seller')).toBeVisible();

    // 验证仓库统计
    await expect(page.getByText('仓库统计')).toBeVisible();
    await expect(page.getByText('Moscow Warehouse')).toBeVisible();
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
            auth: { enabled: false, name: '店铺授权', permission: 'read', disabledReason: '授权功能未开启' }
          }
        })
      })
    });

    await page.goto('/ozon/auth');

    // 验证禁用提示
    await expect(page.getByText(/授权功能未开启/i)).toBeVisible();
  });

  // ==================== 数据加载和展示 ====================

  test('应该正确展示配送方式列表', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/seller/delivery-method/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            { id: 'dm-1', methodCode: 'FBS', methodName: 'Fulfillment by Seller', isDefault: true },
            { id: 'dm-2', methodCode: 'FBO', methodName: 'Fulfillment by Ozon', isDefault: false },
            { id: 'dm-3', methodCode: 'RFBS', methodName: 'Realised FBS', isDefault: false }
          ]
        })
      })
    });

    await page.goto('/ozon/auth?authId=auth-1');

    // 验证所有配送方式
    await expect(page.getByText('Fulfillment by Seller')).toBeVisible();
    await expect(page.getByText('Fulfillment by Ozon')).toBeVisible();
    await expect(page.getByText('Realised FBS')).toBeVisible();

    // 验证默认标记
    const defaultBadge = page.locator('text=默认').first();
    await expect(defaultBadge).toBeVisible();
  });

  // ==================== 用户操作流程 ====================

  test('应该支持创建新授权', async ({ page }) => {
    let createCalled = false;

    await installCommonAppMocks(page, {
      'POST /ozon/api/v1/auth/create': async (request) => {
        createCalled = true;
        const body = await request.postDataJSON();
        expect(body.shopName).toBeTruthy();
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: { id: 'auth-new' } })
        };
      }
    });

    await page.goto('/ozon/auth');

    // 点击新建按钮
    await page.getByRole('button', { name: /新建授权/i }).click();

    // 填写表单
    await page.getByLabel('店铺名称').fill('New Shop');
    await page.getByLabel('Client ID').fill('new-client-id');
    await page.getByLabel('API Key').fill('new-api-key');

    // 提交
    await page.getByRole('button', { name: /确定/i }).click();

    // 验证成功提示
    await expect(page.getByText(/创建成功/i)).toBeVisible();

    expect(createCalled).toBe(true);
  });

  test('应该支持设置默认配送方式', async ({ page }) => {
    let updateCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/seller/delivery-method/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            { id: 'dm-1', methodCode: 'FBS', methodName: 'Fulfillment by Seller', isDefault: true },
            { id: 'dm-2', methodCode: 'FBO', methodName: 'Fulfillment by Ozon', isDefault: false }
          ]
        })
      }),
      'POST /ozon/api/v1/seller/delivery-method/set-default': async (request) => {
        updateCalled = true;
        const body = await request.postDataJSON();
        expect(body.id).toBe('dm-2');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: true })
        };
      }
    });

    await page.goto('/ozon/auth?authId=auth-1');

    // 点击设置默认按钮
    await page.locator('[data-delivery-method-id="dm-2"]').getByRole('button', { name: /设为默认/i }).click();

    // 验证成功提示
    await expect(page.getByText(/设置成功/i)).toBeVisible();

    expect(updateCalled).toBe(true);
  });

  // ==================== 错误处理 ====================

  test('应该正确处理加载失败', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/auth/list': async () => ({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 500,
          message: '服务器错误'
        })
      })
    });

    await page.goto('/ozon/auth');

    // 验证错误提示
    await expect(page.getByText(/加载失败/i)).toBeVisible();
  });

  test('应该正确处理网络超时', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/auth/list': async () => {
        await new Promise(resolve => setTimeout(resolve, 30000)); // 模拟超时
        return { status: 408 };
      }
    });

    await page.goto('/ozon/auth');

    // 验证超时提示
    await expect(page.getByText(/请求超时/i)).toBeVisible({ timeout: 35000 });
  });

  // ==================== 跨页面导航 ====================

  test('应该支持从授权页跳转到商品页', async ({ page }) => {
    await page.goto('/ozon/auth?authId=auth-1');

    // 点击商品管理链接
    await page.getByRole('link', { name: /商品管理/i }).click();

    // 验证跳转成功
    await expect(page).toHaveURL(/\/ozon\/product/);
    await expect(page).toHaveURL(/authId=auth-1/);
  });

  test('应该支持从授权详情返回列表', async ({ page }) => {
    await page.goto('/ozon/auth?authId=auth-1');

    // 点击返回按钮
    await page.getByRole('button', { name: /返回/i }).click();

    // 验证返回列表页
    await expect(page).toHaveURL('/ozon/auth');
  });
});
