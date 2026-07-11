import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

/**
 * 跨模块集成测试
 *
 * 测试模块间的深链跳转、数据联动和协同工作
 */
test.describe('Cross-Module Integration Tests', () => {

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
          data: [{ id: 'auth-1', shopName: 'Test Shop', isActive: true }]
        })
      })
    });
  });

  // ==================== Product → Stock/Price 深链跳转 ====================

  test('应该支持从商品详情跳转到库存管理并保持上下文', async ({ page }) => {
    const draftId = 'draft-1';
    const productId = 'prod-1';

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: draftId,
            sku: 'TEST-SKU-001',
            productId: productId,
            productName: 'Test Product'
          }
        })
      }),
      'GET /ozon/api/v1/stock/list': async (request) => {
        const url = new URL(request.url());
        expect(url.searchParams.get('productId')).toBe(productId);
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              records: [
                { id: 'stock-1', productId: productId, sku: 'TEST-SKU-001', stock: 100 }
              ],
              total: 1
            }
          })
        };
      }
    });

    await page.goto(`/ozon/product/draft/${draftId}?authId=auth-1`);
    await expect(page.getByText('TEST-SKU-001')).toBeVisible();

    // 点击库存管理链接
    await page.getByRole('link', { name: /库存管理/i }).click();

    // 验证跳转并过滤到当前商品
    await expect(page).toHaveURL(/\/ozon\/stock/);
    await expect(page).toHaveURL(/productId=prod-1/);
    await expect(page.getByText('TEST-SKU-001')).toBeVisible();
    await expect(page.getByText('100')).toBeVisible();
  });

  test('应该支持从商品详情跳转到价格管理并保持上下文', async ({ page }) => {
    const draftId = 'draft-1';
    const productId = 'prod-1';

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: draftId,
            sku: 'TEST-SKU-001',
            productId: productId
          }
        })
      }),
      'GET /ozon/api/v1/price/list': async (request) => {
        const url = new URL(request.url());
        expect(url.searchParams.get('productId')).toBe(productId);
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              records: [
                { id: 'price-1', productId: productId, sku: 'TEST-SKU-001', price: 100.00 }
              ],
              total: 1
            }
          })
        };
      }
    });

    await page.goto(`/ozon/product/draft/${draftId}?authId=auth-1`);

    // 点击价格管理链接
    await page.getByRole('link', { name: /价格管理/i }).click();

    // 验证跳转并过滤到当前商品
    await expect(page).toHaveURL(/\/ozon\/price/);
    await expect(page).toHaveURL(/productId=prod-1/);
    await expect(page.getByText('TEST-SKU-001')).toBeVisible();
    await expect(page.getByText('100.00')).toBeVisible();
  });

  // ==================== Posting → AfterSale 集成 ====================

  test('应该支持从订单详情创建售后并跳转查看', async ({ page }) => {
    const postingId = 'posting-1';
    const aftersaleId = 'aftersale-new';

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/posting/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: postingId,
            postingNumber: 'POST-001',
            status: 'DELIVERED',
            products: [
              { sku: 'TEST-SKU-001', quantity: 1 }
            ]
          }
        })
      }),
      'POST /ozon/api/v1/aftersale/create': async (request) => {
        const body = await request.postDataJSON();
        expect(body.postingId).toBe(postingId);
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { id: aftersaleId, postingId: postingId }
          })
        };
      },
      'GET /ozon/api/v1/aftersale/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: aftersaleId,
            postingId: postingId,
            postingNumber: 'POST-001',
            type: 'RETURN',
            status: 'PENDING'
          }
        })
      })
    });

    await page.goto(`/ozon/posting/detail/${postingId}?authId=auth-1`);

    // 创建售后
    await page.getByRole('button', { name: /申请售后/i }).click();
    await page.getByLabel('售后类型').click();
    await page.getByRole('option', { name: /退货/i }).click();
    await page.getByLabel('售后原因').fill('商品损坏');
    await page.getByRole('button', { name: /提交/i }).click();
    await expect(page.getByText(/售后申请已提交/i)).toBeVisible();

    // 点击查看售后详情
    await page.getByRole('link', { name: /查看售后/i }).click();

    // 验证跳转到售后详情页
    await expect(page).toHaveURL(/\/ozon\/aftersale\/detail/);
    await expect(page.getByText('POST-001')).toBeVisible();
    await expect(page.getByText('RETURN')).toBeVisible();
    await expect(page.getByText('PENDING')).toBeVisible();
  });

  test('应该支持从售后列表跳转回订单详情', async ({ page }) => {
    const postingId = 'posting-1';
    const aftersaleId = 'aftersale-1';

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/aftersale/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: aftersaleId,
                postingId: postingId,
                postingNumber: 'POST-001',
                type: 'RETURN',
                status: 'PENDING'
              }
            ],
            total: 1
          }
        })
      }),
      'GET /ozon/api/v1/posting/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: postingId,
            postingNumber: 'POST-001',
            status: 'DELIVERED'
          }
        })
      })
    });

    await page.goto('/ozon/aftersale?authId=auth-1');
    await expect(page.getByText('POST-001')).toBeVisible();

    // 点击订单号跳转
    await page.getByRole('link', { name: 'POST-001' }).click();

    // 验证跳转到订单详情页
    await expect(page).toHaveURL(/\/ozon\/posting\/detail/);
    await expect(page.getByText('POST-001')).toBeVisible();
    await expect(page.getByText('DELIVERED')).toBeVisible();
  });

  // ==================== 错误中心 → 源页面跳转 ====================

  test('应该支持从错误中心跳转到商品发布任务源页面', async ({ page }) => {
    const taskId = 'publish-task-1';
    const draftId = 'draft-1';

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/error/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'error-1',
                taskId: taskId,
                taskType: 'PRODUCT_PUBLISH',
                objectId: draftId,
                errorCode: 'VALIDATION_ERROR',
                errorMessage: '缺少必填属性',
                createTime: '2026-06-25T10:00:00Z'
              }
            ],
            total: 1
          }
        })
      }),
      'GET /ozon/api/v1/product/draft/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: draftId,
            sku: 'TEST-SKU-001',
            status: 'DRAFT'
          }
        })
      })
    });

    await page.goto('/ozon/error?authId=auth-1');
    await expect(page.getByText('VALIDATION_ERROR')).toBeVisible();

    // 点击跳转到源对象
    await page.locator('[data-error-id="error-1"]').getByRole('link', { name: /查看详情/i }).click();

    // 验证跳转到商品草稿详情
    await expect(page).toHaveURL(/\/ozon\/product\/draft/);
    await expect(page.getByText('TEST-SKU-001')).toBeVisible();
  });

  test('应该支持从错误中心跳转到库存推送任务源页面', async ({ page }) => {
    const taskId = 'stock-task-1';
    const stockId = 'stock-1';

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/error/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'error-1',
                taskId: taskId,
                taskType: 'STOCK_PUSH',
                objectId: stockId,
                errorCode: 'WAREHOUSE_NOT_FOUND',
                errorMessage: '仓库未配置'
              }
            ],
            total: 1
          }
        })
      }),
      'GET /ozon/api/v1/stock/list': async (request) => {
        const url = new URL(request.url());
        expect(url.searchParams.get('id')).toBe(stockId);
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              records: [
                { id: stockId, sku: 'TEST-SKU-001', stock: 100 }
              ],
              total: 1
            }
          })
        };
      }
    });

    await page.goto('/ozon/error?authId=auth-1');
    await expect(page.getByText('WAREHOUSE_NOT_FOUND')).toBeVisible();

    // 点击跳转
    await page.locator('[data-error-id="error-1"]').getByRole('link', { name: /查看详情/i }).click();

    // 验证跳转到库存页面并高亮错误记录
    await expect(page).toHaveURL(/\/ozon\/stock/);
    await expect(page.getByText('TEST-SKU-001')).toBeVisible();
  });

  // ==================== 任务中心 → 源页面跳转 ====================

  test('应该支持从任务中心跳转到商品发布页面', async ({ page }) => {
    const taskId = 'publish-task-1';

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/task/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: taskId,
                type: 'PRODUCT_PUBLISH',
                status: 'PROCESSING',
                total: 10,
                success: 5,
                failed: 2,
                createTime: '2026-06-25T10:00:00Z'
              }
            ],
            total: 1
          }
        })
      }),
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [], total: 0 }
        })
      }),
      'GET /ozon/api/v1/product/publish-task/list': async (request) => {
        const url = new URL(request.url());
        expect(url.searchParams.get('taskId')).toBe(taskId);
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [
              { id: taskId, status: 'PROCESSING', progress: 50 }
            ]
          })
        };
      }
    });

    await page.goto('/ozon/task?authId=auth-1');
    await expect(page.getByText('PRODUCT_PUBLISH')).toBeVisible();

    // 点击跳转到源页面
    await page.locator(`[data-task-id="${taskId}"]`).getByRole('link', { name: /查看/i }).click();

    // 验证跳转到商品页面并打开任务面板
    await expect(page).toHaveURL(/\/ozon\/product/);
    await expect(page).toHaveURL(/taskId=${taskId}/);
  });

  // ==================== 监控仪表盘 → 日志查询 ====================

  test('应该支持从监控仪表盘跳转到错误日志', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/monitor/summary': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            totalTasks: 100,
            successTasks: 85,
            failedTasks: 10,
            processingTasks: 5,
            errorCount: 15
          }
        })
      }),
      'GET /ozon/api/v1/error/list': async (request) => {
        const url = new URL(request.url());
        const startDate = url.searchParams.get('startDate');
        expect(startDate).toBeTruthy();
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              records: [
                { id: 'error-1', errorCode: 'VALIDATION_ERROR', errorMessage: '验证失败' }
              ],
              total: 15
            }
          })
        };
      }
    });

    await page.goto('/ozon/monitor?authId=auth-1');
    await expect(page.getByText('15')).toBeVisible(); // 错误数

    // 点击错误数跳转
    await page.getByRole('link', { name: /15.*错误/i }).click();

    // 验证跳转到错误中心
    await expect(page).toHaveURL(/\/ozon\/error/);
    await expect(page.getByText('VALIDATION_ERROR')).toBeVisible();
  });

  // ==================== 全局授权切换联动 ====================

  test('应该支持全局授权切换时各页面联动更新', async ({ page }) => {
    const auth1 = 'auth-1';
    const auth2 = 'auth-2';

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/auth/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            { id: auth1, shopName: 'Shop A', isActive: true },
            { id: auth2, shopName: 'Shop B', isActive: true }
          ]
        })
      }),
      'GET /ozon/api/v1/product/draft/list': async (request) => {
        const url = new URL(request.url());
        const authId = url.searchParams.get('authId');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              records: authId === auth1 ? [
                { id: 'draft-1', sku: 'SHOP-A-SKU', authId: auth1 }
              ] : [
                { id: 'draft-2', sku: 'SHOP-B-SKU', authId: auth2 }
              ],
              total: 1
            }
          })
        };
      }
    });

    // 访问 Shop A 的商品页
    await page.goto(`/ozon/product?authId=${auth1}`);
    await expect(page.getByText('SHOP-A-SKU')).toBeVisible();
    await expect(page.getByText('Shop A')).toBeVisible();

    // 切换到 Shop B
    await page.getByRole('combobox', { name: /选择授权/i }).click();
    await page.getByRole('option', { name: 'Shop B' }).click();

    // 验证 URL 更新
    await expect(page).toHaveURL(/authId=auth-2/);

    // 验证数据更新
    await expect(page.getByText('SHOP-B-SKU')).toBeVisible();
    await expect(page.getByText('Shop B')).toBeVisible();
    await expect(page.getByText('SHOP-A-SKU')).not.toBeVisible();
  });

  // ==================== 批量操作跨页面协同 ====================

  test('应该支持商品批量发布后在任务中心查看进度', async ({ page }) => {
    const draftIds = ['draft-1', 'draft-2', 'draft-3'];
    const taskId = 'publish-task-batch';

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: draftIds.map(id => ({
              id,
              sku: `SKU-${id}`,
              status: 'READY'
            })),
            total: 3
          }
        })
      }),
      'POST /ozon/api/v1/product/publish': async (request) => {
        const body = await request.postDataJSON();
        expect(body.draftIds).toEqual(draftIds);
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { taskId, total: 3 }
          })
        };
      },
      'GET /ozon/api/v1/task/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: taskId,
                type: 'PRODUCT_PUBLISH',
                status: 'PROCESSING',
                total: 3,
                success: 1,
                failed: 0
              }
            ],
            total: 1
          }
        })
      })
    });

    // 在商品页批量发布
    await page.goto('/ozon/product?authId=auth-1');
    await page.locator('thead input[type="checkbox"]').check(); // 全选
    await page.getByRole('button', { name: /批量发布/i }).click();
    await page.getByRole('button', { name: /确定/i }).click();
    await expect(page.getByText(/发布任务已创建/i)).toBeVisible();

    // 跳转到任务中心
    await page.getByRole('link', { name: /任务中心/i }).click();
    await expect(page).toHaveURL(/\/ozon\/task/);

    // 验证任务存在
    await expect(page.getByText(taskId)).toBeVisible();
    await expect(page.getByText('PRODUCT_PUBLISH')).toBeVisible();
    await expect(page.getByText('1 / 3')).toBeVisible();
  });
});
