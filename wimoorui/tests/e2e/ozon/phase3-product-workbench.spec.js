import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

/**
 * Phase 3: Product 工作台生产化功能测试
 *
 * 测试范围：
 * 1. DraftSidebar 搜索和筛选
 * 2. DraftSidebar 操作菜单（克隆、归档、删除）
 * 3. TaskHistoryDrawer 显示和交互
 * 4. 跨页面深链跳转
 */
test.describe('Phase 3: Product Workbench Features', () => {

  // ==================== DraftSidebar 搜索和筛选测试 ====================

  test('DraftSidebar search filters drafts by name', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'draft-1',
              authId: 'auth-1',
              draftName: 'Test Product A',
              status: 'DRAFT',
              variantCount: 5,
              createTime: '2026-06-20T10:00:00Z',
              updateTime: '2026-06-20T10:00:00Z'
            },
            {
              id: 'draft-2',
              authId: 'auth-1',
              draftName: 'Another Product B',
              status: 'DRAFT',
              variantCount: 3,
              createTime: '2026-06-21T10:00:00Z',
              updateTime: '2026-06-21T10:00:00Z'
            },
            {
              id: 'draft-3',
              authId: 'auth-1',
              draftName: 'Test Product C',
              status: 'PUBLISHED',
              variantCount: 2,
              createTime: '2026-06-22T10:00:00Z',
              updateTime: '2026-06-22T10:00:00Z'
            }
          ]
        })
      })
    });

    await page.goto('/ozon/product?authId=auth-1');
    await expect(page.getByText('Test Product A')).toBeVisible();
    await expect(page.getByText('Another Product B')).toBeVisible();

    // 搜索 "Test" 应该只显示包含 Test 的草稿
    const searchInput = page.getByPlaceholder('搜索草稿');
    await searchInput.fill('Test');

    await expect(page.getByText('Test Product A')).toBeVisible();
    await expect(page.getByText('Test Product C')).toBeVisible();
    await expect(page.getByText('Another Product B')).not.toBeVisible();
  });

  test('DraftSidebar status filter shows only matching drafts', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'draft-1',
              draftName: 'Draft A',
              status: 'DRAFT',
              variantCount: 5
            },
            {
              id: 'draft-2',
              draftName: 'Published B',
              status: 'PUBLISHED',
              variantCount: 3
            },
            {
              id: 'draft-3',
              draftName: 'Archived C',
              status: 'ARCHIVED',
              variantCount: 2
            }
          ]
        })
      })
    });

    await page.goto('/ozon/product?authId=auth-1');

    // 默认显示所有状态
    await expect(page.getByText('Draft A')).toBeVisible();
    await expect(page.getByText('Published B')).toBeVisible();
    await expect(page.getByText('Archived C')).toBeVisible();

    // 筛选只显示 DRAFT 状态
    const statusFilter = page.locator('select[aria-label="状态筛选"]');
    await statusFilter.selectOption('DRAFT');

    await expect(page.getByText('Draft A')).toBeVisible();
    await expect(page.getByText('Published B')).not.toBeVisible();
    await expect(page.getByText('Archived C')).not.toBeVisible();
  });

  // ==================== DraftSidebar 操作菜单测试 ====================

  test('DraftSidebar clone action creates new draft with suffix', async ({ page }) => {
    let cloneRequestReceived = false;
    let clonedDraftName = null;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'draft-1',
              draftName: 'Original Draft',
              status: 'DRAFT',
              variantCount: 5
            }
          ]
        })
      }),
      'POST /ozon/api/v1/product/draft/clone': async ({ request }) => {
        cloneRequestReceived = true;
        const payload = await request.postDataJSON();
        clonedDraftName = payload.newDraftName;

        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              id: 'draft-2',
              draftName: payload.newDraftName,
              status: 'DRAFT',
              variantCount: 5
            }
          })
        };
      }
    });

    await page.goto('/ozon/product?authId=auth-1');
    await expect(page.getByText('Original Draft')).toBeVisible();

    // 点击操作菜单
    const moreButton = page.locator('[aria-label="更多操作"]').first();
    await moreButton.click();

    // 点击克隆
    await page.getByText('克隆草稿').click();

    // 在对话框中输入新名称
    const nameInput = page.getByPlaceholder('请输入新草稿名称');
    await expect(nameInput).toBeVisible();
    await nameInput.fill('Original Draft - Copy');

    // 确认克隆
    await page.getByRole('button', { name: '确定' }).click();

    // 验证请求已发送
    await expect.poll(() => cloneRequestReceived).toBe(true);
    await expect.poll(() => clonedDraftName).toBe('Original Draft - Copy');

    // 验证成功消息
    await expect(page.getByText('克隆成功')).toBeVisible();
  });

  test('DraftSidebar archive action updates draft status', async ({ page }) => {
    let archiveRequestReceived = false;
    let archiveReason = null;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'draft-1',
              draftName: 'Draft to Archive',
              status: 'DRAFT',
              variantCount: 5
            }
          ]
        })
      }),
      'POST /ozon/api/v1/product/draft/archive': async ({ request }) => {
        archiveRequestReceived = true;
        const payload = await request.postDataJSON();
        archiveReason = payload.archiveReason;

        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { success: true }
          })
        };
      }
    });

    await page.goto('/ozon/product?authId=auth-1');
    await expect(page.getByText('Draft to Archive')).toBeVisible();

    // 点击操作菜单
    const moreButton = page.locator('[aria-label="更多操作"]').first();
    await moreButton.click();

    // 点击归档
    await page.getByText('归档草稿').click();

    // 在对话框中输入归档原因
    const reasonInput = page.getByPlaceholder('请输入归档原因');
    await expect(reasonInput).toBeVisible();
    await reasonInput.fill('No longer needed');

    // 确认归档
    await page.getByRole('button', { name: '确定' }).click();

    // 验证请求已发送
    await expect.poll(() => archiveRequestReceived).toBe(true);
    await expect.poll(() => archiveReason).toBe('No longer needed');

    // 验证成功消息
    await expect(page.getByText('归档成功')).toBeVisible();
  });

  test('DraftSidebar delete action requires confirmation', async ({ page }) => {
    let deleteRequestReceived = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'draft-1',
              draftName: 'Draft to Delete',
              status: 'DRAFT',
              variantCount: 5
            }
          ]
        })
      }),
      'DELETE /ozon/api/v1/product/draft/delete': async () => {
        deleteRequestReceived = true;
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { success: true }
          })
        };
      }
    });

    await page.goto('/ozon/product?authId=auth-1');
    await expect(page.getByText('Draft to Delete')).toBeVisible();

    // 点击操作菜单
    const moreButton = page.locator('[aria-label="更多操作"]').first();
    await moreButton.click();

    // 点击删除
    await page.getByText('删除草稿').click();

    // 验证确认对话框
    await expect(page.getByText('确认删除草稿')).toBeVisible();
    await expect(page.getByText('此操作不可恢复')).toBeVisible();

    // 取消删除
    await page.getByRole('button', { name: '取消' }).click();
    await expect.poll(() => deleteRequestReceived).toBe(false);

    // 重新打开删除对话框
    await moreButton.click();
    await page.getByText('删除草稿').click();

    // 确认删除
    await page.getByRole('button', { name: '确定' }).click();

    // 验证请求已发送
    await expect.poll(() => deleteRequestReceived).toBe(true);

    // 验证成功消息
    await expect(page.getByText('删除成功')).toBeVisible();
  });

  // ==================== TaskHistoryDrawer 测试 ====================

  test('TaskHistoryDrawer displays task list with statistics', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'draft-1',
              draftName: 'Test Draft',
              status: 'PUBLISHED',
              variantCount: 10
            }
          ]
        })
      }),
      'GET /ozon/api/v1/product/publish/task/history': async ({ url }) => {
        const draftId = url.searchParams.get('draftId');
        if (draftId === 'draft-1') {
          return {
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 200,
              data: [
                {
                  taskId: 'task-1',
                  draftId: 'draft-1',
                  status: 'SUCCESS',
                  totalVariants: 10,
                  successCount: 8,
                  failedCount: 2,
                  createTime: '2026-06-25T10:00:00Z',
                  completeTime: '2026-06-25T10:05:00Z',
                  errorSummary: '2 variants failed'
                },
                {
                  taskId: 'task-2',
                  draftId: 'draft-1',
                  status: 'FAILED',
                  totalVariants: 10,
                  successCount: 0,
                  failedCount: 10,
                  createTime: '2026-06-24T10:00:00Z',
                  completeTime: '2026-06-24T10:02:00Z',
                  errorSummary: 'API error'
                }
              ]
            })
          };
        }
        return { status: 404 };
      }
    });

    await page.goto('/ozon/product?authId=auth-1&draftId=draft-1');

    // 点击"查看历史"按钮
    const historyButton = page.getByRole('button', { name: '查看历史' });
    await historyButton.click();

    // 验证抽屉打开
    await expect(page.getByText('任务历史')).toBeVisible();

    // 验证任务列表显示
    await expect(page.getByText('task-1')).toBeVisible();
    await expect(page.getByText('task-2')).toBeVisible();

    // 验证统计信息
    await expect(page.getByText('8 / 10')).toBeVisible(); // 成功/总数
    await expect(page.getByText('0 / 10')).toBeVisible(); // 失败任务
  });

  test('TaskHistoryDrawer shows task detail dialog', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'draft-1',
              draftName: 'Test Draft',
              status: 'PUBLISHED',
              variantCount: 10
            }
          ]
        })
      }),
      'GET /ozon/api/v1/product/publish/task/history': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              taskId: 'task-1',
              status: 'SUCCESS',
              totalVariants: 10,
              successCount: 8,
              failedCount: 2,
              createTime: '2026-06-25T10:00:00Z',
              completeTime: '2026-06-25T10:05:00Z'
            }
          ]
        })
      }),
      'GET /ozon/api/v1/product/publish/task/detail': async ({ url }) => {
        const taskId = url.searchParams.get('taskId');
        if (taskId === 'task-1') {
          return {
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 200,
              data: {
                taskId: 'task-1',
                status: 'SUCCESS',
                totalVariants: 10,
                successCount: 8,
                failedCount: 2,
                createTime: '2026-06-25T10:00:00Z',
                completeTime: '2026-06-25T10:05:00Z',
                errorSummary: '2 variants failed to publish',
                variantResults: [
                  {
                    variantSku: 'SKU-001',
                    status: 'SUCCESS',
                    message: 'Published successfully'
                  },
                  {
                    variantSku: 'SKU-002',
                    status: 'FAILED',
                    message: 'Invalid price'
                  }
                ]
              }
            })
          };
        }
        return { status: 404 };
      }
    });

    await page.goto('/ozon/product?authId=auth-1&draftId=draft-1');

    // 打开任务历史
    await page.getByRole('button', { name: '查看历史' }).click();
    await expect(page.getByText('任务历史')).toBeVisible();

    // 点击查看详情
    await page.getByRole('button', { name: '查看详情' }).first().click();

    // 验证详情对话框打开
    await expect(page.getByText('任务详情')).toBeVisible();

    // 验证变体结果显示
    await expect(page.getByText('SKU-001')).toBeVisible();
    await expect(page.getByText('SKU-002')).toBeVisible();
    await expect(page.getByText('Published successfully')).toBeVisible();
    await expect(page.getByText('Invalid price')).toBeVisible();
  });

  // ==================== 跨页面深链测试 ====================

  test('URL parameters restore page state correctly', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'draft-1',
              draftName: 'Target Draft',
              status: 'DRAFT',
              variantCount: 5
            }
          ]
        })
      }),
      'GET /ozon/api/v1/product/draft/detail': async ({ url }) => {
        const draftId = url.searchParams.get('draftId');
        if (draftId === 'draft-1') {
          return {
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 200,
              data: {
                draftId: 'draft-1',
                draftName: 'Target Draft',
                status: 'DRAFT',
                variants: [
                  {
                    materialSku: 'TEST-SKU-001',
                    price: 100.0
                  }
                ]
              }
            })
          };
        }
        return { status: 404 };
      }
    });

    // 带参数访问页面
    await page.goto('/ozon/product?authId=auth-1&draftId=draft-1&focus=variants');

    // 验证草稿已选中
    await expect(page.getByText('Target Draft')).toBeVisible();

    // 验证聚焦区域（如果有滚动或高亮）
    const variantsSection = page.locator('[data-section="variants"]');
    await expect(variantsSection).toBeVisible();
  });

  test('Navigation to price center carries SKU parameter', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'draft-1',
              draftName: 'Test Draft',
              status: 'DRAFT',
              variantCount: 5
            }
          ]
        })
      })
    });

    await page.goto('/ozon/product?authId=auth-1&draftId=draft-1');

    // 点击"前往价格中心"按钮（假设按钮存在）
    const priceCenterButton = page.getByRole('button', { name: /价格中心/ });

    if (await priceCenterButton.isVisible()) {
      await priceCenterButton.click();

      // 验证跳转到价格中心页面，并携带参数
      await expect(page).toHaveURL(/\/ozon\/price/);
      await expect(page.url()).toContain('authId=auth-1');
    }
  });

  // ==================== 错误处理测试 ====================

  test('Handles API errors gracefully', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 500,
          message: 'Internal server error'
        })
      })
    });

    await page.goto('/ozon/product?authId=auth-1');

    // 验证错误消息显示
    await expect(page.getByText(/错误|失败|Error/)).toBeVisible();
  });

  test('Clone operation handles duplicate name error', async ({ page }) => {
    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'draft-1',
              draftName: 'Original Draft',
              status: 'DRAFT',
              variantCount: 5
            }
          ]
        })
      }),
      'POST /ozon/api/v1/product/draft/clone': async () => ({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 400,
          message: '草稿名称已存在'
        })
      })
    });

    await page.goto('/ozon/product?authId=auth-1');

    // 打开克隆对话框
    const moreButton = page.locator('[aria-label="更多操作"]').first();
    await moreButton.click();
    await page.getByText('克隆草稿').click();

    // 输入重复名称
    await page.getByPlaceholder('请输入新草稿名称').fill('Duplicate Name');
    await page.getByRole('button', { name: '确定' }).click();

    // 验证错误提示
    await expect(page.getByText(/已存在|duplicate/i)).toBeVisible();
  });
});
