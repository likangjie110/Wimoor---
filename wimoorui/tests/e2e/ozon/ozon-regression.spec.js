import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

test.describe('Ozon Page Regressions', () => {
  test('Product page restores deep link and publish context', async ({ page }) => {
    const captures = {
      detailDraftId: null,
      templateHit: false,
      taskDetailHit: false,
      taskListHit: false
    };

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/product/draft/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [{ draftId: 'draft-1', draftName: '测试草稿', descriptionCategoryId: 1001, typeId: 2001 }] })
      }),
      'GET /ozon/api/v1/product/category/tree': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            categories: [{ descriptionCategoryId: 1001, descriptionCategoryName: '服饰', types: [{ typeId: 2001, typeName: 'T恤' }] }]
          }
        })
      }),
      'GET /ozon/api/v1/product/draft/detail': async ({ url }) => {
        captures.detailDraftId = url.searchParams.get('draftId');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              draftId: 'draft-1',
              draftName: '测试草稿',
              descriptionCategoryId: 1001,
              descriptionCategoryName: '服饰',
              typeId: 2001,
              typeName: 'T恤',
              lastPublishTaskId: 'task-1',
              commonAttributes: [],
              commonImages: [],
              variants: [
                {
                  variantId: 'variant-1',
                  materialSku: 'SKU-001',
                  materialName: '测试商品',
                  attributes: [],
                  images: []
                }
              ]
            }
          })
        };
      },
      'GET /ozon/api/v1/product/category/template': async () => {
        captures.templateHit = true;
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              descriptionCategoryId: 1001,
              typeId: 2001,
              commonAttributes: [],
              variantAttributes: [],
              requiredImageCount: 1,
              requiresBarcode: false
            }
          })
        };
      },
      'GET /ozon/api/v1/product/publish/task/detail': async () => {
        captures.taskDetailHit = true;
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              taskStatus: 'SUCCESS',
              remoteTaskId: 'remote-1',
              errorSummary: null,
              normalizedItems: []
            }
          })
        };
      },
      'GET /ozon/api/v1/product/publish/task/list': async () => {
        captures.taskListHit = true;
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [{ taskId: 'task-1', taskStatus: 'SUCCESS', remoteTaskId: 'remote-1' }]
          })
        };
      }
    });

    await page.goto('/ozon/product?authId=auth-1&draftId=draft-1&focus=publish');
    await expect(page.getByText('商品下游操作')).toBeVisible();
    await expect(page.getByText('测试草稿')).toBeVisible();
    await expect.poll(() => captures.detailDraftId).toBe('draft-1');
    await expect.poll(() => captures.templateHit).toBe(true);
    await expect.poll(() => captures.taskDetailHit).toBe(true);
    await expect.poll(() => captures.taskListHit).toBe(true);
  });

  test('Posting page restores route state and auto-loads detail drawer', async ({ page }) => {
    const captures = {
      listStatus: null,
      detailPostingId: null,
      afterSalePostingId: null
    };

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/posting/list': async ({ url }) => {
        captures.listStatus = url.searchParams.get('status');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [
              {
                id: 'posting-1',
                postingNumber: 'posting-no-1',
                fulfillmentType: 'FBS',
                postingStatus: 'awaiting_packaging',
                bridgeStatus: 'SYNCED',
                itemSummary: 'SKU-001 x1',
                authId: 'auth-1'
              }
            ]
          })
        };
      },
      'GET /ozon/api/v1/posting/detail': async ({ url }) => {
        captures.detailPostingId = url.searchParams.get('postingId');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              id: 'posting-1',
              postingNumber: 'posting-no-1',
              fulfillmentType: 'FBS',
              postingStatus: 'awaiting_packaging',
              substatus: 'ready',
              bridgeStatus: 'SYNCED',
              items: [{ itemId: 'item-1', materialSku: 'SKU-001', ozonOfferId: 'offer-1', quantity: 1 }],
              shipments: []
            }
          })
        };
      },
      'GET /ozon/api/v1/posting/aftersale/detail': async ({ url }) => {
        captures.afterSalePostingId = url.searchParams.get('postingId');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: { packages: [], returns: [], cancellations: [] }
          })
        };
      }
    });

    await page.goto('/ozon/posting?authId=auth-1&postingId=posting-1&sinceDays=3&useCursor=false&status=awaiting_packaging');
    await expect(page.getByText('Ozon 订单同步')).toBeVisible();
    await expect(page.getByText('Posting 详情')).toBeVisible();
    await expect.poll(() => captures.listStatus).toBe('awaiting_packaging');
    await expect(page.getByRole('switch')).toHaveAttribute('aria-checked', 'false');
    await expect.poll(() => captures.detailPostingId).toBe('posting-1');
    await expect.poll(() => captures.afterSalePostingId).toBe('posting-1');
  });

  test('Task page restores filters and loads ops summary', async ({ page }) => {
    const captures = {
      jobType: null,
      status: null,
      summaryAuthId: null
    };

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/task/list': async ({ url }) => {
        captures.jobType = url.searchParams.get('jobType');
        captures.status = url.searchParams.get('status');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [
              {
                id: 'job-1',
                authId: 'auth-1',
                jobType: 'POSTING_SYNC',
                status: 'FAILED',
                payload: '{"sinceDays":7}',
                createdAt: '2026-04-11T10:00:00Z',
                updatedAt: '2026-04-11T10:01:00Z'
              }
            ]
          })
        };
      },
      'GET /ozon/api/v1/ops/summary': async ({ url }) => {
        captures.summaryAuthId = url.searchParams.get('authId');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              apiLogTotal: 12,
              apiLogFailed: 2,
              operationAuditTotal: 6,
              operationAuditFailed: 1
            }
          })
        };
      }
    });

    await page.goto('/ozon/task?authId=auth-1&jobType=POSTING_SYNC&status=FAILED');
    await expect(page.getByText('Ozon 任务中心')).toBeVisible();
    await expect(page.getByText('POSTING_SYNC')).toBeVisible();
    await expect.poll(() => captures.jobType).toBe('POSTING_SYNC');
    await expect.poll(() => captures.status).toBe('FAILED');
    await expect.poll(() => captures.summaryAuthId).toBe('auth-1');
    await expect(page.getByText('API 调用总数')).toBeVisible();
  });

  test('Error page restores filters and loads related ops logs in drawer', async ({ page }) => {
    const captures = {
      sourceType: null,
      status: null,
      apiLogObjectId: null,
      auditObjectId: null
    };

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/error/list': async ({ url }) => {
        captures.sourceType = url.searchParams.get('sourceType');
        captures.status = url.searchParams.get('status');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [
              {
                id: 'err-1',
                authId: 'auth-1',
                sourceType: 'POSTING',
                objectId: 'posting-1',
                objectCode: 'posting-no-1',
                status: 'OPEN',
                errorMessage: 'bridge failed',
                requestPayloadJson: '{"postingId":"posting-1"}',
                responsePayloadJson: '{"message":"bridge failed"}'
              }
            ]
          })
        };
      },
      'GET /ozon/api/v1/ops/api-log/list': async ({ url }) => {
        captures.apiLogObjectId = url.searchParams.get('objectId');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [
              {
                id: 'api-1',
                apiGroup: 'POSTING',
                actionName: 'LIST_FBS_POSTINGS',
                status: 'FAILED',
                errorMessage: 'remote 500',
                createTime: '2026-04-11T10:02:00Z'
              }
            ]
          })
        };
      },
      'GET /ozon/api/v1/ops/operation-audit/list': async ({ url }) => {
        captures.auditObjectId = url.searchParams.get('objectId');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [
              {
                id: 'audit-1',
                operationType: 'POSTING_RETRY',
                resultStatus: 'FAILED',
                resultMessage: 'bridge failed again',
                createTime: '2026-04-11T10:03:00Z'
              }
            ]
          })
        };
      }
    });

    await page.goto('/ozon/error?authId=auth-1&sourceType=POSTING&status=OPEN&keyword=posting-no-1');
    await expect(page.getByText('Ozon 错误中心')).toBeVisible();
    await expect(page.getByText('bridge failed')).toBeVisible();
    await expect.poll(() => captures.sourceType).toBe('POSTING');
    await expect.poll(() => captures.status).toBe('OPEN');

    await page.getByRole('button', { name: '查看载荷' }).click();
    await expect(page.getByText('错误载荷')).toBeVisible();
    await expect.poll(() => captures.apiLogObjectId).toBe('posting-1');
    await expect.poll(() => captures.auditObjectId).toBe('posting-1');
    await expect(page.getByText('LIST_FBS_POSTINGS')).toBeVisible();
    await expect(page.getByText('POSTING_RETRY')).toBeVisible();
  });
});
