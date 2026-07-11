import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

/**
 * Product 工作台回归测试
 *
 * 测试范围：
 * 1. 草稿列表和详情
 * 2. 商品映射管理
 * 3. 类目属性查询
 * 4. 发布任务面板
 * 5. 任务历史抽屉
 * 6. 功能开关提示
 * 7. 错误处理
 * 8. 深链跳转
 */
test.describe('Product Workbench Regression', () => {

  test.beforeEach(async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            product: { enabled: true, name: '商品管理', permission: 'read' },
            productWrite: { enabled: true, name: '商品发布', permission: 'write' }
          }
        })
      }),
      'GET /ozon/api/v1/auth/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            { id: 'auth-1', shopName: 'Test Shop', isActive: true }
          ]
        })
      })
    });
  });

  // ==================== 页面加载和渲染 ====================

  test('应该正确加载商品草稿列表', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'draft-1',
                sku: 'TEST-SKU-001',
                productName: 'Test Product',
                categoryId: 'cat-1',
                status: 'DRAFT',
                createTime: '2026-06-20T10:00:00Z'
              },
              {
                id: 'draft-2',
                sku: 'TEST-SKU-002',
                productName: 'Another Product',
                categoryId: 'cat-2',
                status: 'READY',
                createTime: '2026-06-21T10:00:00Z'
              }
            ],
            total: 2
          }
        })
      })
    });

    await page.goto('/ozon/product?authId=auth-1&tab=draft');

    // 验证页面标题
    await expect(page.getByRole('heading', { name: /商品管理/i })).toBeVisible();

    // 验证 Tab
    await expect(page.getByRole('tab', { name: /商品草稿/i })).toHaveAttribute('aria-selected', 'true');

    // 验证数据加载
    await expect(page.getByText('TEST-SKU-001')).toBeVisible();
    await expect(page.getByText('Test Product')).toBeVisible();
    await expect(page.getByText('TEST-SKU-002')).toBeVisible();
    await expect(page.getByText('Another Product')).toBeVisible();
  });

  test('应该正确加载商品映射列表', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/mapping/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'mapping-1',
                localSku: 'LOCAL-001',
                ozonSku: 'OZON-001',
                productId: 'prod-1',
                productName: 'Mapped Product',
                isActive: true
              }
            ],
            total: 1
          }
        })
      })
    });

    await page.goto('/ozon/product?authId=auth-1&tab=mapping');

    // 验证 Tab 切换
    await expect(page.getByRole('tab', { name: /商品映射/i })).toHaveAttribute('aria-selected', 'true');

    // 验证数据
    await expect(page.getByText('LOCAL-001')).toBeVisible();
    await expect(page.getByText('OZON-001')).toBeVisible();
    await expect(page.getByText('Mapped Product')).toBeVisible();
  });

  // ==================== 功能开关提示 ====================

  test('应该在写权限未开启时禁用发布按钮', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/features': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            product: { enabled: true, name: '商品管理', permission: 'read' },
            productWrite: { enabled: false, name: '商品发布', permission: 'write', disabledReason: '商品发布功能未开启' }
          }
        })
      }),
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [{ id: 'draft-1', sku: 'TEST-001', status: 'READY' }], total: 1 }
        })
      })
    });

    await page.goto('/ozon/product?authId=auth-1&tab=draft');

    // 验证发布按钮被禁用
    const publishBtn = page.getByRole('button', { name: /发布/i });
    await expect(publishBtn).toBeDisabled();

    // 悬停查看提示
    await publishBtn.hover();
    await expect(page.getByText(/商品发布功能未开启/i)).toBeVisible();
  });

  // ==================== 数据加载和展示 ====================

  test('应该正确展示草稿详情', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'draft-1',
            sku: 'TEST-SKU-001',
            productName: 'Test Product',
            description: 'Product description',
            categoryId: 'cat-1',
            categoryName: 'Electronics',
            images: ['img1.jpg', 'img2.jpg'],
            attributes: [
              { key: 'Color', value: 'Red' },
              { key: 'Size', value: 'Large' }
            ],
            status: 'DRAFT'
          }
        })
      })
    });

    await page.goto('/ozon/product/draft/draft-1?authId=auth-1');

    // 验证基本信息
    await expect(page.getByText('TEST-SKU-001')).toBeVisible();
    await expect(page.getByText('Test Product')).toBeVisible();
    await expect(page.getByText('Product description')).toBeVisible();

    // 验证类目
    await expect(page.getByText('Electronics')).toBeVisible();

    // 验证属性
    await expect(page.getByText('Color')).toBeVisible();
    await expect(page.getByText('Red')).toBeVisible();
    await expect(page.getByText('Size')).toBeVisible();
    await expect(page.getByText('Large')).toBeVisible();

    // 验证图片
    const images = page.locator('img[src*="img1.jpg"]');
    await expect(images.first()).toBeVisible();
  });

  test('应该正确展示发布任务面板', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [{ id: 'draft-1', sku: 'TEST-001', status: 'READY' }], total: 1 }
        })
      }),
      'GET /ozon/api/v1/product/publish-task/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'task-1',
              draftId: 'draft-1',
              status: 'PROCESSING',
              progress: 50,
              createTime: '2026-06-25T10:00:00Z'
            },
            {
              id: 'task-2',
              draftId: 'draft-1',
              status: 'SUCCESS',
              progress: 100,
              createTime: '2026-06-24T10:00:00Z'
            }
          ]
        })
      })
    });

    await page.goto('/ozon/product?authId=auth-1&tab=draft');

    // 打开任务面板
    await page.getByRole('button', { name: /发布任务/i }).click();

    // 验证任务列表
    await expect(page.getByText('task-1')).toBeVisible();
    await expect(page.getByText('PROCESSING')).toBeVisible();
    await expect(page.getByText('50%')).toBeVisible();

    await expect(page.getByText('task-2')).toBeVisible();
    await expect(page.getByText('SUCCESS')).toBeVisible();
    await expect(page.getByText('100%')).toBeVisible();
  });

  // ==================== 用户操作流程 ====================

  test('应该支持导入草稿', async ({ page }) => {
    let importCalled = false;

    await installCommonAppMocks(page, {
      'POST /ozon/api/v1/product/draft/import': async (request) => {
        importCalled = true;
        const body = await request.postDataJSON();
        expect(body.authId).toBe('auth-1');
        expect(body.skus).toContain('SKU-001');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { taskId: 'import-task-1', total: 1 }
          })
        };
      }
    });

    await page.goto('/ozon/product?authId=auth-1&tab=draft');

    // 点击导入按钮
    await page.getByRole('button', { name: /导入草稿/i }).click();

    // 填写 SKU
    await page.getByLabel('SKU列表').fill('SKU-001\nSKU-002');

    // 提交
    await page.getByRole('button', { name: /确定/i }).click();

    // 验证成功提示
    await expect(page.getByText(/导入任务已创建/i)).toBeVisible();

    expect(importCalled).toBe(true);
  });

  test('应该支持编辑草稿', async ({ page }) => {
    let updateCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'draft-1',
            sku: 'TEST-SKU-001',
            productName: 'Test Product',
            description: 'Old description',
            categoryId: 'cat-1'
          }
        })
      }),
      'POST /ozon/api/v1/product/draft/update': async (request) => {
        updateCalled = true;
        const body = await request.postDataJSON();
        expect(body.id).toBe('draft-1');
        expect(body.description).toBe('New description');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: true })
        };
      }
    });

    await page.goto('/ozon/product/draft/draft-1?authId=auth-1');

    // 点击编辑按钮
    await page.getByRole('button', { name: /编辑/i }).click();

    // 修改描述
    await page.getByLabel('描述').clear();
    await page.getByLabel('描述').fill('New description');

    // 保存
    await page.getByRole('button', { name: /保存/i }).click();

    // 验证成功提示
    await expect(page.getByText(/保存成功/i)).toBeVisible();

    expect(updateCalled).toBe(true);
  });

  test('应该支持发布草稿', async ({ page }) => {
    let publishCalled = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              { id: 'draft-1', sku: 'TEST-001', status: 'READY' },
              { id: 'draft-2', sku: 'TEST-002', status: 'READY' }
            ],
            total: 2
          }
        })
      }),
      'POST /ozon/api/v1/product/publish': async (request) => {
        publishCalled = true;
        const body = await request.postDataJSON();
        expect(body.draftIds).toContain('draft-1');
        expect(body.draftIds).toContain('draft-2');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { taskId: 'publish-task-1', total: 2 }
          })
        };
      }
    });

    await page.goto('/ozon/product?authId=auth-1&tab=draft');

    // 选择草稿
    await page.locator('[data-draft-id="draft-1"] input[type="checkbox"]').check();
    await page.locator('[data-draft-id="draft-2"] input[type="checkbox"]').check();

    // 点击发布按钮
    await page.getByRole('button', { name: /批量发布/i }).click();

    // 确认
    await page.getByRole('button', { name: /确定/i }).click();

    // 验证成功提示
    await expect(page.getByText(/发布任务已创建/i)).toBeVisible();

    expect(publishCalled).toBe(true);
  });

  test('应该支持查询类目属性', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/category/attributes': async (request) => {
        const url = new URL(request.url());
        expect(url.searchParams.get('categoryId')).toBe('cat-1');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [
              { id: 'attr-1', name: 'Color', type: 'String', required: true, values: ['Red', 'Blue'] },
              { id: 'attr-2', name: 'Size', type: 'String', required: false, values: ['S', 'M', 'L'] }
            ]
          })
        };
      }
    });

    await page.goto('/ozon/product?authId=auth-1&tab=category');

    // 输入类目 ID
    await page.getByLabel('类目ID').fill('cat-1');

    // 查询
    await page.getByRole('button', { name: /查询属性/i }).click();

    // 验证结果
    await expect(page.getByText('Color')).toBeVisible();
    await expect(page.getByText('必填')).toBeVisible();
    await expect(page.getByText('Red')).toBeVisible();
    await expect(page.getByText('Blue')).toBeVisible();

    await expect(page.getByText('Size')).toBeVisible();
    await expect(page.getByText('可选')).toBeVisible();
  });

  // ==================== 错误处理 ====================

  test('应该正确处理草稿加载失败', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 500,
          message: '服务器错误'
        })
      })
    });

    await page.goto('/ozon/product?authId=auth-1&tab=draft');

    // 验证错误提示
    await expect(page.getByText(/加载失败/i)).toBeVisible();
  });

  test('应该正确处理发布失败', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [{ id: 'draft-1', sku: 'TEST-001', status: 'READY' }], total: 1 }
        })
      }),
      'POST /ozon/api/v1/product/publish': async () => ({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 400,
          message: '草稿验证失败：缺少必填属性'
        })
      })
    });

    await page.goto('/ozon/product?authId=auth-1&tab=draft');

    // 选择草稿
    await page.locator('[data-draft-id="draft-1"] input[type="checkbox"]').check();

    // 点击发布
    await page.getByRole('button', { name: /批量发布/i }).click();
    await page.getByRole('button', { name: /确定/i }).click();

    // 验证错误提示
    await expect(page.getByText(/草稿验证失败：缺少必填属性/i)).toBeVisible();
  });

  // ==================== 深链跳转 ====================

  test('应该支持从任务面板跳转到错误中心', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [], total: 0 }
        })
      }),
      'GET /ozon/api/v1/product/publish-task/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'task-1',
              status: 'FAILED',
              errorCount: 5
            }
          ]
        })
      })
    });

    await page.goto('/ozon/product?authId=auth-1&tab=draft');

    // 打开任务面板
    await page.getByRole('button', { name: /发布任务/i }).click();

    // 点击错误链接
    await page.locator('[data-task-id="task-1"]').getByRole('link', { name: /查看错误/i }).click();

    // 验证跳转到错误中心
    await expect(page).toHaveURL(/\/ozon\/error/);
    await expect(page).toHaveURL(/taskId=task-1/);
  });

  test('应该支持从商品详情跳转到库存页', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'draft-1',
            sku: 'TEST-SKU-001',
            productId: 'prod-1'
          }
        })
      })
    });

    await page.goto('/ozon/product/draft/draft-1?authId=auth-1');

    // 点击库存管理链接
    await page.getByRole('link', { name: /库存管理/i }).click();

    // 验证跳转
    await expect(page).toHaveURL(/\/ozon\/stock/);
    await expect(page).toHaveURL(/productId=prod-1/);
  });

  test('应该支持从商品详情跳转到价格页', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/detail': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            id: 'draft-1',
            sku: 'TEST-SKU-001',
            productId: 'prod-1'
          }
        })
      })
    });

    await page.goto('/ozon/product/draft/draft-1?authId=auth-1');

    // 点击价格管理链接
    await page.getByRole('link', { name: /价格管理/i }).click();

    // 验证跳转
    await expect(page).toHaveURL(/\/ozon\/price/);
    await expect(page).toHaveURL(/productId=prod-1/);
  });
});
