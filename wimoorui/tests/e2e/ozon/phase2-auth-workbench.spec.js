import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

/**
 * Phase 2: Auth 工作台完整化功能测试
 *
 * 测试范围：
 * 1. DeliveryMethodPanel 配送方式管理
 * 2. Auth 页面 Tab 切换
 * 3. 功能开关和仓库统计显示
 */
test.describe('Phase 2: Auth Workbench Features', () => {

  // ==================== DeliveryMethodPanel 配送方式列表测试 ====================

  test('DeliveryMethodPanel displays delivery methods list', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/seller/delivery-method/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'method-1',
              authId: 'auth-1',
              methodCode: 'FBS',
              methodName: 'Fulfillment by Seller',
              isDefault: true,
              createTime: '2026-06-20T10:00:00Z'
            },
            {
              id: 'method-2',
              authId: 'auth-1',
              methodCode: 'FBO',
              methodName: 'Fulfillment by Ozon',
              isDefault: false,
              createTime: '2026-06-21T10:00:00Z'
            }
          ]
        })
      })
    });

    await page.goto('/ozon/auth?authId=auth-1');

    // 等待配送方式面板加载
    await expect(page.getByText('配送方式')).toBeVisible();
    await expect(page.getByText('Fulfillment by Seller')).toBeVisible();
    await expect(page.getByText('Fulfillment by Ozon')).toBeVisible();

    // 验证默认标记
    await expect(page.getByText('默认').first()).toBeVisible();
  });

  test('DeliveryMethodPanel creates new delivery method', async ({ page }) => {
    let createdMethod = null;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/seller/delivery-method/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: []
        })
      }),
      'POST /ozon/api/v1/seller/delivery-method/save': async (request) => {
        createdMethod = await request.postDataJSON();
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              id: 'method-new',
              ...createdMethod,
              createTime: new Date().toISOString()
            }
          })
        };
      }
    });

    await page.goto('/ozon/auth?authId=auth-1');

    // 点击新增按钮
    await page.getByRole('button', { name: '新增配送方式' }).click();

    // 填写表单
    await page.getByLabel('配送代码').fill('FBS');
    await page.getByLabel('配送名称').fill('Test Delivery Method');

    // 保存
    await page.getByRole('button', { name: '确定' }).click();

    // 验证请求数据
    await page.waitForTimeout(500);
    expect(createdMethod).not.toBeNull();
    expect(createdMethod.methodCode).toBe('FBS');
    expect(createdMethod.methodName).toBe('Test Delivery Method');
  });

  test('DeliveryMethodPanel edits existing delivery method', async ({ page }) => {
    let updatedMethod = null;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/seller/delivery-method/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'method-1',
              authId: 'auth-1',
              methodCode: 'FBS',
              methodName: 'Old Name',
              isDefault: false
            }
          ]
        })
      }),
      'POST /ozon/api/v1/seller/delivery-method/save': async (request) => {
        updatedMethod = await request.postDataJSON();
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: updatedMethod
          })
        };
      }
    });

    await page.goto('/ozon/auth?authId=auth-1');

    // 点击编辑按钮
    await page.getByRole('button', { name: '编辑' }).first().click();

    // 修改名称
    await page.getByLabel('配送名称').clear();
    await page.getByLabel('配送名称').fill('Updated Name');

    // 保存
    await page.getByRole('button', { name: '确定' }).click();

    // 验证
    await page.waitForTimeout(500);
    expect(updatedMethod).not.toBeNull();
    expect(updatedMethod.methodName).toBe('Updated Name');
  });

  test('DeliveryMethodPanel sets default delivery method', async ({ page }) => {
    let setDefaultCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/seller/delivery-method/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'method-1',
              methodCode: 'FBS',
              methodName: 'Method 1',
              isDefault: true
            },
            {
              id: 'method-2',
              methodCode: 'FBO',
              methodName: 'Method 2',
              isDefault: false
            }
          ]
        })
      }),
      'POST /ozon/api/v1/seller/delivery-method/set-default': async (request) => {
        setDefaultCalled = true;
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: true })
        };
      }
    });

    await page.goto('/ozon/auth?authId=auth-1');

    // 点击第二个配送方式的设为默认按钮
    const setDefaultButtons = page.getByRole('button', { name: '设为默认' });
    await setDefaultButtons.first().click();

    // 验证
    await page.waitForTimeout(500);
    expect(setDefaultCalled).toBe(true);
  });

  test('DeliveryMethodPanel deletes non-default delivery method', async ({ page }) => {
    let deletedMethodId = null;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/seller/delivery-method/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'method-1',
              methodCode: 'FBS',
              methodName: 'Default Method',
              isDefault: true
            },
            {
              id: 'method-2',
              methodCode: 'FBO',
              methodName: 'To Delete',
              isDefault: false
            }
          ]
        })
      }),
      'DELETE /ozon/api/v1/seller/delivery-method/delete': async (request) => {
        const url = new URL(request.url());
        deletedMethodId = url.searchParams.get('id');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: true })
        };
      }
    });

    await page.goto('/ozon/auth?authId=auth-1');

    // 点击删除按钮
    await page.getByRole('button', { name: '删除' }).first().click();

    // 确认删除
    await page.getByRole('button', { name: '确认' }).click();

    // 验证
    await page.waitForTimeout(500);
    expect(deletedMethodId).toBe('method-2');
  });

  test('DeliveryMethodPanel prevents deleting default method', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/seller/delivery-method/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'method-1',
              methodCode: 'FBS',
              methodName: 'Default Method',
              isDefault: true
            }
          ]
        })
      }),
      'DELETE /ozon/api/v1/seller/delivery-method/delete': async () => ({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 400,
          message: '不能删除默认配送方式'
        })
      })
    });

    await page.goto('/ozon/auth?authId=auth-1');

    // 尝试删除默认方式
    await page.getByRole('button', { name: '删除' }).first().click();
    await page.getByRole('button', { name: '确认' }).click();

    // 验证错误提示
    await expect(page.getByText('不能删除默认配送方式')).toBeVisible();
  });

  // ==================== Auth 页面 Tab 切换测试 ====================

  test('Auth page displays tab navigation', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/auth/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'auth-1',
            name: 'Ozon RU',
            clientId: 'client-1',
            status: 'ACTIVE',
            warehouseCount: 3,
            defaultWarehouse: 'Main Warehouse',
            writeGatesEnabled: 2
          }
        })
      })
    });

    await page.goto('/ozon/auth?authId=auth-1');

    // 验证 Tab 导航存在
    await expect(page.getByRole('tab', { name: '基本信息' })).toBeVisible();
    await expect(page.getByRole('tab', { name: '配送方式' })).toBeVisible();
    await expect(page.getByRole('tab', { name: '功能开关' })).toBeVisible();
  });

  test('Auth page switches between tabs', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/auth/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'auth-1',
            name: 'Ozon RU',
            warehouseCount: 3
          }
        })
      }),
      'GET /ozon/api/v1/seller/delivery-method/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [] })
      })
    });

    await page.goto('/ozon/auth?authId=auth-1');

    // 默认显示基本信息
    await expect(page.getByText('授权信息')).toBeVisible();

    // 切换到配送方式
    await page.getByRole('tab', { name: '配送方式' }).click();
    await expect(page.getByText('配送方式')).toBeVisible();

    // 切换回基本信息
    await page.getByRole('tab', { name: '基本信息' }).click();
    await expect(page.getByText('授权信息')).toBeVisible();
  });

  test('Auth page displays warehouse statistics', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/auth/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'auth-1',
            name: 'Ozon RU',
            warehouseCount: 5,
            defaultWarehouse: 'Moscow Warehouse'
          }
        })
      })
    });

    await page.goto('/ozon/auth?authId=auth-1');

    // 验证仓库统计显示
    await expect(page.getByText('5')).toBeVisible(); // 仓库数量
    await expect(page.getByText('Moscow Warehouse')).toBeVisible(); // 默认仓库
  });

  test('Auth page displays feature gate summary', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/auth/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'auth-1',
            name: 'Ozon RU',
            writeGatesEnabled: 3
          }
        })
      })
    });

    await page.goto('/ozon/auth?authId=auth-1');

    // 验证功能开关摘要
    await expect(page.getByText('写操作已启用: 3')).toBeVisible();
  });
});
