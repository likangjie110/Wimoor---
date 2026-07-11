import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

/**
 * Posting 工作台回归测试
 *
 * 测试范围：
 * 1. 订单列表和详情
 * 2. 包裹管理
 * 3. 售后处理
 * 4. 功能开关提示
 * 5. 错误处理
 */
test.describe('Posting Workbench Regression', () => {

  test.beforeEach(async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            postingWrite: { enabled: true, name: '履约操作', permission: 'write' }
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

  test('应该正确加载订单列表', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/posting/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'posting-1',
                postingNumber: 'POST-001',
                orderId: 'order-1',
                orderNumber: 'ORDER-001',
                status: 'AWAITING_PACKAGING',
                inProcessAt: '2026-06-25T10:00:00Z',
                shipmentDate: '2026-06-26T10:00:00Z'
              },
              {
                id: 'posting-2',
                postingNumber: 'POST-002',
                orderId: 'order-2',
                orderNumber: 'ORDER-002',
                status: 'DELIVERED',
                inProcessAt: '2026-06-24T10:00:00Z',
                deliveredDate: '2026-06-25T10:00:00Z'
              }
            ],
            total: 2
          }
        })
      })
    });

    await page.goto('/ozon/posting?authId=auth-1');

    // 验证页面标题
    await expect(page.getByRole('heading', { name: /订单管理/i })).toBeVisible();

    // 验证数据加载
    await expect(page.getByText('POST-001')).toBeVisible();
    await expect(page.getByText('ORDER-001')).toBeVisible();
    await expect(page.getByText('AWAITING_PACKAGING')).toBeVisible();

    await expect(page.getByText('POST-002')).toBeVisible();
    await expect(page.getByText('ORDER-002')).toBeVisible();
    await expect(page.getByText('DELIVERED')).toBeVisible();
  });

  test('应该正确加载订单详情', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/posting/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'posting-1',
            postingNumber: 'POST-001',
            orderNumber: 'ORDER-001',
            status: 'AWAITING_PACKAGING',
            products: [
              { sku: 'TEST-SKU-001', name: 'Test Product', quantity: 2, price: 100.00 },
              { sku: 'TEST-SKU-002', name: 'Another Product', quantity: 1, price: 50.00 }
            ],
            customer: {
              name: 'Test Customer',
              phone: '+7 123 456 7890',
              address: 'Moscow, Russia'
            },
            totalAmount: 250.00,
            currency: 'RUB'
          }
        })
      })
    });

    await page.goto('/ozon/posting/detail/posting-1?authId=auth-1');

    // 验证订单信息
    await expect(page.getByText('POST-001')).toBeVisible();
    await expect(page.getByText('ORDER-001')).toBeVisible();
    await expect(page.getByText('AWAITING_PACKAGING')).toBeVisible();

    // 验证商品信息
    await expect(page.getByText('TEST-SKU-001')).toBeVisible();
    await expect(page.getByText('Test Product')).toBeVisible();
    await expect(page.getByText('2')).toBeVisible(); // 数量

    // 验证客户信息
    await expect(page.getByText('Test Customer')).toBeVisible();
    await expect(page.getByText('+7 123 456 7890')).toBeVisible();

    // 验证金额
    await expect(page.getByText('250.00')).toBeVisible();
  });

  // ==================== 功能开关提示 ====================

  test('应该在写权限未开启时禁用履约操作按钮', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            postingWrite: { enabled: false, name: '履约操作', permission: 'write', disabledReason: '履约写操作未开启' }
          }
        })
      }),
      'GET /ozon/api/v1/posting/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [{ id: 'posting-1', postingNumber: 'POST-001', status: 'AWAITING_PACKAGING' }], total: 1 }
        })
      })
    });

    await page.goto('/ozon/posting?authId=auth-1');

    // 验证发货按钮被禁用
    const shipBtn = page.getByRole('button', { name: /发货/i });
    await expect(shipBtn).toBeDisabled();

    // 悬停查看提示
    await shipBtn.hover();
    await expect(page.getByText(/履约写操作未开启/i)).toBeVisible();
  });

  // ==================== 用户操作流程 ====================

  test('应该支持订单搜索', async ({ page }) => {
    let searchCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/posting/list': async (request) => {
        const url = new URL(request.url());
        const keyword = url.searchParams.get('keyword');
        if (keyword) {
          searchCalled = true;
          expect(keyword).toBe('POST-001');
        }
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              records: keyword ? [
                { id: 'posting-1', postingNumber: 'POST-001', status: 'AWAITING_PACKAGING' }
              ] : [],
              total: keyword ? 1 : 0
            }
          })
        };
      }
    });

    await page.goto('/ozon/posting?authId=auth-1');

    // 输入搜索关键词
    await page.getByPlaceholder(/搜索订单号/i).fill('POST-001');

    // 点击搜索
    await page.getByRole('button', { name: /搜索/i }).click();

    // 验证搜索结果
    await expect(page.getByText('POST-001')).toBeVisible();

    expect(searchCalled).toBe(true);
  });

  test('应该支持订单状态筛选', async ({ page }) => {
    let filterCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/posting/list': async (request) => {
        const url = new URL(request.url());
        const status = url.searchParams.get('status');
        if (status) {
          filterCalled = true;
          expect(status).toBe('AWAITING_PACKAGING');
        }
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              records: status ? [
                { id: 'posting-1', postingNumber: 'POST-001', status: 'AWAITING_PACKAGING' }
              ] : [],
              total: status ? 1 : 0
            }
          })
        };
      }
    });

    await page.goto('/ozon/posting?authId=auth-1');

    // 选择状态筛选
    await page.getByLabel(/订单状态/i).click();
    await page.getByRole('option', { name: /待打包/i }).click();

    // 验证筛选结果
    await expect(page.getByText('AWAITING_PACKAGING')).toBeVisible();

    expect(filterCalled).toBe(true);
  });

  test('应该支持订单发货', async ({ page }) => {
    let shipCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/posting/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'posting-1',
            postingNumber: 'POST-001',
            status: 'AWAITING_PACKAGING'
          }
        })
      }),
      'POST /ozon/api/v1/posting/ship': async (request) => {
        shipCalled = true;
        const body = await request.postDataJSON();
        expect(body.postingId).toBe('posting-1');
        expect(body.trackingNumber).toBe('TRACK-001');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: true })
        };
      }
    });

    await page.goto('/ozon/posting/detail/posting-1?authId=auth-1');

    // 点击发货按钮
    await page.getByRole('button', { name: /发货/i }).click();

    // 填写运单号
    await page.getByLabel('运单号').fill('TRACK-001');

    // 提交
    await page.getByRole('button', { name: /确定/i }).click();

    // 验证成功提示
    await expect(page.getByText(/发货成功/i)).toBeVisible();

    expect(shipCalled).toBe(true);
  });

  test('应该支持申请售后', async ({ page }) => {
    let aftersaleCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/posting/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'posting-1',
            postingNumber: 'POST-001',
            status: 'DELIVERED'
          }
        })
      }),
      'POST /ozon/api/v1/aftersale/create': async (request) => {
        aftersaleCalled = true;
        const body = await request.postDataJSON();
        expect(body.postingId).toBe('posting-1');
        expect(body.type).toBe('RETURN');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: { id: 'aftersale-1' } })
        };
      }
    });

    await page.goto('/ozon/posting/detail/posting-1?authId=auth-1');

    // 点击售后按钮
    await page.getByRole('button', { name: /申请售后/i }).click();

    // 选择售后类型
    await page.getByLabel('售后类型').click();
    await page.getByRole('option', { name: /退货/i }).click();

    // 填写原因
    await page.getByLabel('售后原因').fill('商品损坏');

    // 提交
    await page.getByRole('button', { name: /提交/i }).click();

    // 验证成功提示
    await expect(page.getByText(/售后申请已提交/i)).toBeVisible();

    expect(aftersaleCalled).toBe(true);
  });

  // ==================== 错误处理 ====================

  test('应该正确处理订单加载失败', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/posting/list': async () => ({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 500,
          message: '服务器错误'
        })
      })
    });

    await page.goto('/ozon/posting?authId=auth-1');

    // 验证错误提示
    await expect(page.getByText(/加载失败/i)).toBeVisible();
  });

  test('应该正确处理发货失败', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/posting/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'posting-1',
            postingNumber: 'POST-001',
            status: 'AWAITING_PACKAGING'
          }
        })
      }),
      'POST /ozon/api/v1/posting/ship': async () => ({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 400,
          message: '运单号格式错误'
        })
      })
    });

    await page.goto('/ozon/posting/detail/posting-1?authId=auth-1');

    // 点击发货并提交
    await page.getByRole('button', { name: /发货/i }).click();
    await page.getByLabel('运单号').fill('INVALID');
    await page.getByRole('button', { name: /确定/i }).click();

    // 验证错误提示
    await expect(page.getByText(/运单号格式错误/i)).toBeVisible();
  });

  // ==================== 深链跳转 ====================

  test('应该支持从订单详情跳转到商品详情', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/posting/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'posting-1',
            postingNumber: 'POST-001',
            products: [
              { sku: 'TEST-SKU-001', draftId: 'draft-1' }
            ]
          }
        })
      })
    });

    await page.goto('/ozon/posting/detail/posting-1?authId=auth-1');

    // 点击商品链接
    await page.getByRole('link', { name: 'TEST-SKU-001' }).click();

    // 验证跳转
    await expect(page).toHaveURL(/\/ozon\/product\/draft\/draft-1/);
  });

  test('应该支持从订单列表返回', async ({ page }) => {
    await page.goto('/ozon/posting/detail/posting-1?authId=auth-1');

    // 点击返回按钮
    await page.getByRole('button', { name: /返回/i }).click();

    // 验证返回列表页
    await expect(page).toHaveURL('/ozon/posting');
  });
});
