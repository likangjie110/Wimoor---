import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from '../support/ozon-mock.js';

test.describe('Ozon Workbench Regressions', () => {
  test('Auth page loads auth list and tab workbenches', async ({ page }) => {
    const captures = {
      warehouseAuthId: null,
      deliveryAuthId: null,
      initTaskAuthId: null
    };

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/seller/warehouse/list': async ({ url }) => {
        captures.warehouseAuthId = url.searchParams.get('authId');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [
              {
                id: 'warehouse-1',
                authId: 'auth-1',
                warehouseId: '1001',
                name: 'Main Warehouse',
                warehouseType: 'FULFILLMENT',
                status: 'ACTIVE',
                defaultWarehouse: true,
                syncedAt: '2026-04-11T10:00:00Z',
                lastWarehouseSyncTime: '2026-04-11T10:00:00Z'
              }
            ]
          })
        };
      },
      'GET /ozon/api/v1/seller/deliveryMethod/list': async ({ url }) => {
        captures.deliveryAuthId = url.searchParams.get('authId');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [
              {
                id: 'dm-1',
                authId: 'auth-1',
                methodCode: 'fbs-main',
                methodName: 'FBS 主配送',
                description: '默认配送',
                enabled: true,
                defaultMethod: true
              }
            ]
          })
        };
      },
      'GET /ozon/api/v1/task/list': async ({ url }) => {
        captures.initTaskAuthId = url.searchParams.get('authId');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [
              {
                id: 'job-1',
                authId: 'auth-1',
                jobType: 'INIT_WAREHOUSE',
                status: 'DONE',
                payload: '{}',
                createdAt: '2026-04-11T10:00:00Z',
                updatedAt: '2026-04-11T10:01:00Z'
              }
            ]
          })
        };
      }
    });

    await page.goto('/ozon/auth');
    await expect(page.getByText('Ozon 授权管理')).toBeVisible();
    await expect(page.getByText('当前工作授权：Ozon E2E Shop')).toBeVisible();

    await page.getByRole('tab', { name: '仓库同步' }).click();
    await expect(page.getByText('Main Warehouse')).toBeVisible();
    await expect.poll(() => captures.warehouseAuthId).toBe('auth-1');

    await page.getByRole('tab', { name: '配送方式' }).click();
    await expect(page.getByText('FBS 主配送')).toBeVisible();
    await expect.poll(() => captures.deliveryAuthId).toBe('auth-1');

    await page.getByRole('tab', { name: '初始化任务' }).click();
    await expect(page.getByText('INIT_WAREHOUSE')).toBeVisible();
    await expect.poll(() => captures.initTaskAuthId).toBe('auth-1');
  });

  test('Finance page renders latest task result and raw drawer', async ({ page }) => {
    const captures = {
      taskAuthId: null,
      transactionAuthId: null,
      rawTaskId: null
    };

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/finance/task/list': async ({ url }) => {
        captures.taskAuthId = url.searchParams.get('authId');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [
              {
                id: 'task-1',
                authId: 'auth-1',
                reportId: 'report-1',
                reportDate: '2026-04-11T00:00:00Z',
                taskStatus: 'DONE',
                importedCount: 2,
                sourceMode: 'LOCAL_IMPORT',
                rawContentReady: true,
                updatedAt: '2026-04-11T10:00:00Z'
              }
            ]
          })
        };
      },
      'GET /ozon/api/v1/finance/transaction/list': async ({ url }) => {
        captures.transactionAuthId = url.searchParams.get('authId');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [
              {
                id: 'txn-row-1',
                transactionId: 'txn-1',
                operationType: 'sale',
                postingNumber: 'posting-1',
                amount: 12.5,
                currencyCode: 'RUB',
                rawLineJson: '{"transactionId":"txn-1"}'
              }
            ]
          })
        };
      },
      'GET /ozon/api/v1/finance/task/raw': async ({ url }) => {
        captures.rawTaskId = url.searchParams.get('taskId');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: '{"transactions":[{"transactionId":"txn-1"}]}'
          })
        };
      }
    });

    await page.goto('/ozon/finance');
    await expect(page.getByText('Ozon 财务导入')).toBeVisible();
    await expect(page.getByText('最近导入结果')).toBeVisible();
    await expect(page.getByText('LOCAL_IMPORT')).toBeVisible();
    await expect.poll(() => captures.taskAuthId).toBe('auth-1');
    await expect.poll(() => captures.transactionAuthId).toBe('auth-1');

    await page.getByRole('button', { name: '查看原文' }).first().click();
    await expect(page.getByRole('heading', { name: '报表原文' })).toBeVisible();
    await expect(page.getByText('transactionId')).toBeVisible();
    await expect.poll(() => captures.rawTaskId).toBe('task-1');
  });

  test('Chat page loads sessions, messages and reply audits', async ({ page }) => {
    let replySaved = false;

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/chat/session/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'session-row-1',
              authId: 'auth-1',
              sessionId: 'session-1',
              customerName: 'Buyer A',
              unreadCount: 1,
              sessionStatus: 'OPEN',
              lastMessageText: 'hello',
              lastMessageAt: '2026-04-11T10:00:00Z'
            }
          ]
        })
      }),
      'GET /ozon/api/v1/chat/message/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'msg-row-1',
              messageId: 'msg-1',
              senderType: 'BUYER',
              messageText: 'hello',
              messageTime: '2026-04-11T10:00:00Z'
            }
          ]
        })
      }),
      'GET /ozon/api/v1/chat/reply/audit/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: replySaved
            ? [{
                id: 'audit-2',
                sessionId: 'session-1',
                replyStatus: 'RECORDED',
                replyText: '新回复',
                operator: 'tester',
                createTime: '2026-04-11T10:02:00Z'
              }]
            : [{
                id: 'audit-1',
                sessionId: 'session-1',
                replyStatus: 'RECORDED',
                replyText: '历史回复',
                operator: 'tester',
                createTime: '2026-04-11T10:01:00Z'
              }]
        })
      }),
      'POST /ozon/api/v1/chat/reply/record': async () => {
        replySaved = true;
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              id: 'audit-2',
              sessionId: 'session-1',
              replyStatus: 'RECORDED',
              replyText: '新回复',
              operator: 'tester',
              createTime: '2026-04-11T10:02:00Z'
            }
          })
        };
      }
    });

    await page.goto('/ozon/chat');
    await expect(page.getByText('Ozon 聊天导入')).toBeVisible();
    await page.getByText('Buyer A').click();
    await expect(page.locator('.message-text').filter({ hasText: 'hello' })).toBeVisible();
    await expect(page.getByText('历史回复')).toBeVisible();

    await page.getByPlaceholder('输入回复内容，当前会保留审计记录以便后续接入官方发送。').fill('新回复');
    await page.getByRole('button', { name: '保存回复审计' }).click();
    await expect(page.getByText('新回复')).toBeVisible();
  });

  test('Ads page loads account cascade and records sync intent', async ({ page }) => {
    const captures = {
      accountIdInCampaign: null,
      accountIdInReport: null,
      syncPayload: null
    };

    await installCommonAppMocks(page, {
      'GET /ozon/api/v1/ads/account/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'account-row-1',
              authId: 'auth-1',
              accountId: 'acc-1',
              accountName: 'Main Account',
              status: 'ACTIVE',
              currencyCode: 'RUB'
            }
          ]
        })
      }),
      'GET /ozon/api/v1/ads/campaign/list': async ({ url }) => {
        captures.accountIdInCampaign = url.searchParams.get('accountId');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [
              {
                id: 'campaign-row-1',
                accountId: 'acc-1',
                campaignId: 'camp-1',
                campaignName: 'Spring Campaign',
                campaignType: 'SEARCH_PROMO',
                campaignStatus: 'ACTIVE',
                budget: 1000
              }
            ]
          })
        };
      },
      'GET /ozon/api/v1/ads/report/list': async ({ url }) => {
        captures.accountIdInReport = url.searchParams.get('accountId');
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: [
              {
                id: 'report-row-1',
                accountId: 'acc-1',
                campaignId: 'camp-1',
                reportDate: '2026-04-11T00:00:00Z',
                impressions: 1000,
                clicks: 50,
                spend: 120.5,
                orders: 5,
                sales: 800,
                ctr: 5,
                cpc: 2.4,
                acos: 15.06,
                roas: 6.64,
                rawLineJson: '{}'
              }
            ]
          })
        };
      },
      'GET /ozon/api/v1/ads/summary': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            impressions: 1000,
            clicks: 50,
            spend: 120.5,
            orders: 5,
            sales: 800,
            acos: 15.06,
            roas: 6.64
          }
        })
      }),
      'GET /ozon/api/v1/ops/operation-audit/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            {
              id: 'audit-1',
              operationType: 'ADS_SYNC_INTENT',
              resultStatus: 'PENDING',
              objectCode: 'Main Account',
              resultMessage: '等待接入官方 Performance API',
              createTime: '2026-04-11T10:00:00Z'
            }
          ]
        })
      }),
      'POST /ozon/api/v1/ads/sync/intent': async ({ request }) => {
        captures.syncPayload = await request.postDataJSON();
        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            data: {
              requestId: 'sync-1',
              accountId: 'acc-1',
              requestStatus: 'PENDING',
              message: '已记录同步意图，等待官方 Performance API 接入'
            }
          })
        };
      }
    });

    await page.goto('/ozon/ads');
    await expect(page.getByText('Ozon 广告导入')).toBeVisible();
    await expect(page.locator('.el-table').getByText('Spring Campaign', { exact: true }).first()).toBeVisible();
    await expect(page.getByText('ACOS / ROAS')).toBeVisible();
    await expect.poll(() => captures.accountIdInCampaign).toBe('acc-1');
    await expect.poll(() => captures.accountIdInReport).toBe('acc-1');

    await page.getByRole('button', { name: '记录同步意图' }).click();
    await expect(page.getByText('已记录同步意图，等待官方 Performance API 接入')).toBeVisible();
    await expect.poll(() => captures.syncPayload?.accountId).toBe('acc-1');
  });
});
