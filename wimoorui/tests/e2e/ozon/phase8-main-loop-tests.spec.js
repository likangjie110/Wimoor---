import { expect, test } from '@playwright/test';
import { installCommonAppMocks } from './support/ozon-mock.js';

/**
 * Phase 8.1: E2E 主回路测试套件
 *
 * 验证 8 条关键用户回路的端到端功能完整性
 * 每个测试用例覆盖：URL 状态恢复、组件交互、数据流转、UI 响应
 *
 * 预计执行时间：10-15 分钟
 */
test.describe('Phase 8.1: Main Loop E2E Tests', () => {

  test.beforeEach(async ({ page }) => {
    // 安装通用 Mock
    await installCommonAppMocks(page, {
      // Product Mock 数据
      'GET /ozon/api/v1/product/drafts': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'draft-001',
                sku: 'TEST-SKU-001',
                name: 'Test Product 1',
                status: 'DRAFT',
                createdAt: '2026-06-27T10:00:00Z'
              },
              {
                id: 'draft-002',
                sku: 'TEST-SKU-002',
                name: 'Test Product 2',
                status: 'PENDING',
                createdAt: '2026-06-27T11:00:00Z'
              }
            ],
            total: 2
          }
        })
      }),

      // Posting Mock 数据
      'GET /ozon/api/v1/posting/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'posting-001',
                postingNumber: 'POST-001',
                status: 'awaiting_packaging',
                inProcessAt: '2026-06-27T09:00:00Z',
                shipmentDate: '2026-06-28T00:00:00Z'
              }
            ],
            total: 1
          }
        })
      }),

      // Task Mock 数据
      'GET /ozon/api/v1/task/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'task-001',
                taskType: 'PRODUCT_PUBLISH',
                status: 'SUCCESS',
                createdAt: '2026-06-27T08:00:00Z',
                message: '商品发布成功'
              },
              {
                id: 'task-002',
                taskType: 'PRICE_UPDATE',
                status: 'FAILED',
                createdAt: '2026-06-27T07:00:00Z',
                message: '价格更新失败'
              }
            ],
            total: 2
          }
        })
      }),

      // Error Mock 数据
      'GET /ozon/api/v1/error/list': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'error-001',
                errorCode: 'INVALID_PRICE',
                message: '价格不能为负数',
                payload: '{"price": -100}',
                relatedLogs: ['log-001', 'log-002'],
                occurredAt: '2026-06-27T06:00:00Z'
              }
            ],
            total: 1
          }
        })
      }),

      // Auth Mock 数据
      'GET /ozon/api/v1/auth/deliveryMethods': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            { code: 'FBO', name: 'Ozon 仓配', warehouseCount: 5 },
            { code: 'FBS', name: '商家配送', warehouseCount: 2 }
          ]
        })
      }),

      'GET /ozon/api/v1/auth/warehouses': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            { id: 'wh-001', name: 'Moscow Warehouse', status: 'ACTIVE' },
            { id: 'wh-002', name: 'St. Petersburg Warehouse', status: 'ACTIVE' }
          ]
        })
      }),

      // Finance Mock 数据
      'GET /ozon/api/v1/finance/tasks': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'fin-task-001',
                taskType: 'TRANSACTION_SYNC',
                status: 'SUCCESS',
                resultSummary: { total: 100, success: 95, failed: 5 },
                rawData: '{"transactions": [...]}',
                createdAt: '2026-06-27T05:00:00Z'
              }
            ],
            total: 1
          }
        })
      }),

      // Chat Mock 数据
      'GET /ozon/api/v1/chat/conversations': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'chat-001',
                customerId: 'customer-001',
                customerName: 'Test Customer',
                lastMessage: 'Hello',
                unreadCount: 2,
                updatedAt: '2026-06-27T04:00:00Z'
              }
            ],
            total: 1
          }
        })
      }),

      'GET /ozon/api/v1/chat/messages': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'msg-001',
                chatId: 'chat-001',
                direction: 'incoming',
                content: 'Hello',
                sentAt: '2026-06-27T04:00:00Z'
              }
            ],
            total: 1
          }
        })
      }),

      'GET /ozon/api/v1/chat/audit': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'audit-001',
                chatId: 'chat-001',
                action: 'REPLY_SENT',
                content: 'Thank you',
                operator: 'admin',
                createdAt: '2026-06-27T04:30:00Z'
              }
            ],
            total: 1
          }
        })
      }),

      // Ads Mock 数据
      'GET /ozon/api/v1/ads/accounts': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: [
            { id: 'ads-001', name: 'Ad Account 1', status: 'ACTIVE', balance: 5000 }
          ]
        })
      }),

      'GET /ozon/api/v1/ads/campaigns': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records: [
              {
                id: 'campaign-001',
                name: 'Summer Sale',
                status: 'ACTIVE',
                budget: 10000,
                spent: 3500
              }
            ],
            total: 1
          }
        })
      }),

      'GET /ozon/api/v1/ads/reports/summary': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            totalSpent: 3500,
            totalClicks: 1200,
            totalImpressions: 15000,
            avgCPC: 2.92
          }
        })
      }),

      'GET /ozon/api/v1/ads/syncIntent': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            lastSyncAt: '2026-06-27T03:00:00Z',
            nextSyncAt: '2026-06-27T15:00:00Z',
            syncStatus: 'COMPLETED'
          }
        })
      })
    });
  });

  /**
   * 测试用例 1: Product 深链恢复与发布区焦点恢复
   *
   * 验证场景：
   * 1. 通过深链访问商品页面（携带筛选参数）
   * 2. 验证草稿列表筛选条件正确应用
   * 3. 验证发布区焦点自动定位到目标草稿
   * 4. 验证 URL 参数与 UI 状态同步
   */
  test('TC1: Product deep-link recovery and draft focus restoration', async ({ page }) => {
    // 1. 访问带深链参数的 URL
    const deepLinkUrl = '/ozon/product?status=DRAFT&sku=TEST-SKU-001&focusDraftId=draft-001';
    await page.goto(deepLinkUrl);

    // 2. 等待页面加载完成
    await page.waitForSelector('[data-testid="product-draft-list"]', { timeout: 5000 });

    // 3. 验证筛选器状态恢复
    const statusFilter = page.locator('[data-testid="filter-status"]');
    await expect(statusFilter).toHaveValue('DRAFT');

    const skuFilter = page.locator('[data-testid="filter-sku"]');
    await expect(skuFilter).toHaveValue('TEST-SKU-001');

    // 4. 验证草稿列表已加载
    const draftList = page.locator('[data-testid="product-draft-list"]');
    await expect(draftList).toBeVisible();

    // 5. 验证焦点定位到目标草稿
    const targetDraft = page.locator('[data-draft-id="draft-001"]');
    await expect(targetDraft).toBeVisible();
    await expect(targetDraft).toHaveClass(/focused|highlighted/);

    // 6. 验证发布区自动展开
    const publishArea = page.locator('[data-testid="publish-area"]');
    await expect(publishArea).toBeVisible();

    // 7. 验证 URL 参数持久化
    expect(page.url()).toContain('status=DRAFT');
    expect(page.url()).toContain('sku=TEST-SKU-001');
    expect(page.url()).toContain('focusDraftId=draft-001');

    console.log('✅ TC1: Product 深链恢复与焦点定位验证通过');
  });

  /**
   * 测试用例 2: Posting 路由状态恢复与详情自动打开
   *
   * 验证场景：
   * 1. 通过带 postingId 的 URL 访问订单页面
   * 2. 验证订单详情自动打开
   * 3. 验证详情内容正确加载
   * 4. 验证路由状态正确恢复
   */
  test('TC2: Posting route recovery and detail auto-open', async ({ page }) => {
    // 1. 访问带 postingId 的 URL
    await page.goto('/ozon/posting?postingId=posting-001');

    // 2. 等待订单列表加载
    await page.waitForSelector('[data-testid="posting-list"]', { timeout: 5000 });

    // 3. 验证订单详情自动打开
    const detailDrawer = page.locator('[data-testid="posting-detail-drawer"]');
    await expect(detailDrawer).toBeVisible();

    // 4. 验证详情标题显示正确的订单号
    const detailTitle = detailDrawer.locator('[data-testid="detail-title"]');
    await expect(detailTitle).toContainText('POST-001');

    // 5. 验证订单状态显示
    const orderStatus = detailDrawer.locator('[data-testid="order-status"]');
    await expect(orderStatus).toBeVisible();

    // 6. 验证发货日期显示
    const shipmentDate = detailDrawer.locator('[data-testid="shipment-date"]');
    await expect(shipmentDate).toBeVisible();

    // 7. 验证 URL 参数保持
    expect(page.url()).toContain('postingId=posting-001');

    // 8. 关闭详情后 URL 应移除参数
    const closeButton = detailDrawer.locator('[data-testid="close-detail"]');
    await closeButton.click();
    await expect(detailDrawer).not.toBeVisible();
    expect(page.url()).not.toContain('postingId=');

    console.log('✅ TC2: Posting 路由状态恢复验证通过');
  });

  /**
   * 测试用例 3: Task 筛选恢复与运维摘要
   *
   * 验证场景：
   * 1. 通过带筛选条件的 URL 访问任务中心
   * 2. 验证筛选器状态恢复
   * 3. 验证任务列表根据筛选条件加载
   * 4. 验证运维摘要正确显示
   */
  test('TC3: Task filter recovery and operation summary', async ({ page }) => {
    // 1. 访问带筛选条件的 URL
    await page.goto('/ozon/task?taskType=PRODUCT_PUBLISH&status=SUCCESS&dateRange=last7days');

    // 2. 等待任务列表加载
    await page.waitForSelector('[data-testid="task-list"]', { timeout: 5000 });

    // 3. 验证筛选器状态恢复
    const taskTypeFilter = page.locator('[data-testid="filter-taskType"]');
    await expect(taskTypeFilter).toHaveValue('PRODUCT_PUBLISH');

    const statusFilter = page.locator('[data-testid="filter-status"]');
    await expect(statusFilter).toHaveValue('SUCCESS');

    // 4. 验证任务列表显示
    const taskList = page.locator('[data-testid="task-list"]');
    await expect(taskList).toBeVisible();

    // 5. 验证运维摘要面板
    const operationSummary = page.locator('[data-testid="operation-summary"]');
    await expect(operationSummary).toBeVisible();

    // 6. 验证摘要统计数据
    const totalTasks = operationSummary.locator('[data-testid="total-tasks"]');
    await expect(totalTasks).toBeVisible();

    const successRate = operationSummary.locator('[data-testid="success-rate"]');
    await expect(successRate).toBeVisible();

    // 7. 验证 URL 参数持久化
    expect(page.url()).toContain('taskType=PRODUCT_PUBLISH');
    expect(page.url()).toContain('status=SUCCESS');

    console.log('✅ TC3: Task 筛选恢复与运维摘要验证通过');
  });

  /**
   * 测试用例 4: Error 载荷抽屉与关联日志
   *
   * 验证场景：
   * 1. 访问错误中心页面
   * 2. 点击错误记录打开载荷抽屉
   * 3. 验证错误载荷正确显示
   * 4. 验证关联日志列表显示
   */
  test('TC4: Error payload drawer and related logs', async ({ page }) => {
    // 1. 访问错误中心
    await page.goto('/ozon/error');

    // 2. 等待错误列表加载
    await page.waitForSelector('[data-testid="error-list"]', { timeout: 5000 });

    // 3. 点击第一条错误记录
    const firstError = page.locator('[data-testid="error-item"]').first();
    await firstError.click();

    // 4. 验证载荷抽屉打开
    const payloadDrawer = page.locator('[data-testid="error-payload-drawer"]');
    await expect(payloadDrawer).toBeVisible();

    // 5. 验证错误代码显示
    const errorCode = payloadDrawer.locator('[data-testid="error-code"]');
    await expect(errorCode).toContainText('INVALID_PRICE');

    // 6. 验证错误消息显示
    const errorMessage = payloadDrawer.locator('[data-testid="error-message"]');
    await expect(errorMessage).toContainText('价格不能为负数');

    // 7. 验证载荷内容显示
    const payloadContent = payloadDrawer.locator('[data-testid="payload-content"]');
    await expect(payloadContent).toBeVisible();
    await expect(payloadContent).toContainText('price');

    // 8. 验证关联日志部分
    const relatedLogs = payloadDrawer.locator('[data-testid="related-logs"]');
    await expect(relatedLogs).toBeVisible();

    // 9. 验证日志条目显示
    const logItems = relatedLogs.locator('[data-testid="log-item"]');
    await expect(logItems).toHaveCount(2); // Mock 数据中有 2 条日志

    console.log('✅ TC4: Error 载荷抽屉与关联日志验证通过');
  });

  /**
   * 测试用例 5: Auth 授权工作台与 3 个子标签页
   *
   * 验证场景：
   * 1. 访问授权工作台
   * 2. 验证 3 个子标签页正确显示
   * 3. 验证标签页切换功能
   * 4. 验证各标签页内容加载
   */
  test('TC5: Auth workspace and 3 sub-tabs', async ({ page }) => {
    // 1. 访问授权工作台
    await page.goto('/ozon/auth');

    // 2. 等待工作台加载
    await page.waitForSelector('[data-testid="auth-workspace"]', { timeout: 5000 });

    // 3. 验证授权列表标签页
    const authListTab = page.locator('[data-testid="tab-auth-list"]');
    await expect(authListTab).toBeVisible();
    await authListTab.click();

    const authList = page.locator('[data-testid="auth-list"]');
    await expect(authList).toBeVisible();

    // 4. 验证配送方式标签页
    const deliveryMethodTab = page.locator('[data-testid="tab-delivery-method"]');
    await expect(deliveryMethodTab).toBeVisible();
    await deliveryMethodTab.click();

    const deliveryMethodList = page.locator('[data-testid="delivery-method-list"]');
    await expect(deliveryMethodList).toBeVisible();

    // 5. 验证仓库统计标签页
    const warehouseStatsTab = page.locator('[data-testid="tab-warehouse-stats"]');
    await expect(warehouseStatsTab).toBeVisible();
    await warehouseStatsTab.click();

    const warehouseStats = page.locator('[data-testid="warehouse-stats"]');
    await expect(warehouseStats).toBeVisible();

    // 6. 验证标签页切换后 URL 更新
    expect(page.url()).toContain('tab=warehouse-stats');

    // 7. 切换回授权列表
    await authListTab.click();
    expect(page.url()).toContain('tab=auth-list');

    console.log('✅ TC5: Auth 授权工作台与 3 个子标签页验证通过');
  });

  /**
   * 测试用例 6: Finance 最近任务结果与原文 drawer
   *
   * 验证场景：
   * 1. 访问财务页面
   * 2. 验证最近任务结果列表显示
   * 3. 点击任务打开原文抽屉
   * 4. 验证原文数据正确显示
   */
  test('TC6: Finance recent task results and raw data drawer', async ({ page }) => {
    // 1. 访问财务页面
    await page.goto('/ozon/finance');

    // 2. 等待财务任务列表加载
    await page.waitForSelector('[data-testid="finance-task-list"]', { timeout: 5000 });

    // 3. 验证任务列表显示
    const taskList = page.locator('[data-testid="finance-task-list"]');
    await expect(taskList).toBeVisible();

    // 4. 验证第一条任务记录
    const firstTask = taskList.locator('[data-testid="finance-task-item"]').first();
    await expect(firstTask).toBeVisible();

    // 5. 验证任务结果摘要显示
    const resultSummary = firstTask.locator('[data-testid="result-summary"]');
    await expect(resultSummary).toBeVisible();
    await expect(resultSummary).toContainText('total');
    await expect(resultSummary).toContainText('success');

    // 6. 点击任务打开原文抽屉
    const viewRawDataButton = firstTask.locator('[data-testid="view-raw-data"]');
    await viewRawDataButton.click();

    // 7. 验证原文抽屉打开
    const rawDataDrawer = page.locator('[data-testid="raw-data-drawer"]');
    await expect(rawDataDrawer).toBeVisible();

    // 8. 验证原文内容显示
    const rawContent = rawDataDrawer.locator('[data-testid="raw-content"]');
    await expect(rawContent).toBeVisible();
    await expect(rawContent).toContainText('transactions');

    // 9. 验证可以关闭抽屉
    const closeButton = rawDataDrawer.locator('[data-testid="close-drawer"]');
    await closeButton.click();
    await expect(rawDataDrawer).not.toBeVisible();

    console.log('✅ TC6: Finance 最近任务结果与原文抽屉验证通过');
  });

  /**
   * 测试用例 7: Chat 会话、消息、回复审计
   *
   * 验证场景：
   * 1. 访问聊天页面
   * 2. 验证会话列表显示
   * 3. 选择会话查看消息历史
   * 4. 验证回复审计记录显示
   */
  test('TC7: Chat conversation, messages, and reply audit', async ({ page }) => {
    // 1. 访问聊天页面
    await page.goto('/ozon/chat');

    // 2. 等待会话列表加载
    await page.waitForSelector('[data-testid="chat-conversation-list"]', { timeout: 5000 });

    // 3. 验证会话列表显示
    const conversationList = page.locator('[data-testid="chat-conversation-list"]');
    await expect(conversationList).toBeVisible();

    // 4. 点击第一个会话
    const firstConversation = conversationList.locator('[data-testid="conversation-item"]').first();
    await firstConversation.click();

    // 5. 验证消息区域显示
    const messageArea = page.locator('[data-testid="chat-message-area"]');
    await expect(messageArea).toBeVisible();

    // 6. 验证消息列表
    const messages = messageArea.locator('[data-testid="message-item"]');
    await expect(messages.first()).toBeVisible();

    // 7. 验证消息方向标识
    const incomingMessage = messages.filter({ hasText: 'Hello' }).first();
    await expect(incomingMessage).toHaveAttribute('data-direction', 'incoming');

    // 8. 打开审计记录
    const auditButton = page.locator('[data-testid="view-audit"]');
    await auditButton.click();

    // 9. 验证审计面板显示
    const auditPanel = page.locator('[data-testid="chat-audit-panel"]');
    await expect(auditPanel).toBeVisible();

    // 10. 验证审计记录列表
    const auditRecords = auditPanel.locator('[data-testid="audit-item"]');
    await expect(auditRecords.first()).toBeVisible();

    // 11. 验证审计记录包含操作者和操作类型
    const firstAudit = auditRecords.first();
    await expect(firstAudit).toContainText('REPLY_SENT');
    await expect(firstAudit).toContainText('admin');

    console.log('✅ TC7: Chat 会话、消息、回复审计验证通过');
  });

  /**
   * 测试用例 8: Ads 账号级联、报表汇总、同步意图
   *
   * 验证场景：
   * 1. 访问广告页面
   * 2. 验证账号级联选择器
   * 3. 选择账号后验证活动列表加载
   * 4. 验证报表汇总数据显示
   * 5. 验证同步意图状态显示
   */
  test('TC8: Ads account cascade, report summary, and sync intent', async ({ page }) => {
    // 1. 访问广告页面
    await page.goto('/ozon/ads');

    // 2. 等待页面加载
    await page.waitForSelector('[data-testid="ads-workspace"]', { timeout: 5000 });

    // 3. 验证账号选择器显示
    const accountSelector = page.locator('[data-testid="ads-account-selector"]');
    await expect(accountSelector).toBeVisible();

    // 4. 打开账号选择器
    await accountSelector.click();

    // 5. 验证账号列表显示
    const accountList = page.locator('[data-testid="account-list"]');
    await expect(accountList).toBeVisible();

    // 6. 选择第一个账号
    const firstAccount = accountList.locator('[data-testid="account-item"]').first();
    await firstAccount.click();

    // 7. 验证活动列表加载
    const campaignList = page.locator('[data-testid="campaign-list"]');
    await expect(campaignList).toBeVisible();

    // 8. 验证活动项显示
    const campaigns = campaignList.locator('[data-testid="campaign-item"]');
    await expect(campaigns.first()).toBeVisible();

    // 9. 验证报表汇总区域
    const reportSummary = page.locator('[data-testid="ads-report-summary"]');
    await expect(reportSummary).toBeVisible();

    // 10. 验证汇总指标显示
    const totalSpent = reportSummary.locator('[data-testid="total-spent"]');
    await expect(totalSpent).toBeVisible();
    await expect(totalSpent).toContainText('3500');

    const totalClicks = reportSummary.locator('[data-testid="total-clicks"]');
    await expect(totalClicks).toBeVisible();

    const avgCPC = reportSummary.locator('[data-testid="avg-cpc"]');
    await expect(avgCPC).toBeVisible();

    // 11. 验证同步意图显示
    const syncIntent = page.locator('[data-testid="ads-sync-intent"]');
    await expect(syncIntent).toBeVisible();

    // 12. 验证同步状态和时间
    const syncStatus = syncIntent.locator('[data-testid="sync-status"]');
    await expect(syncStatus).toContainText('COMPLETED');

    const lastSyncTime = syncIntent.locator('[data-testid="last-sync-time"]');
    await expect(lastSyncTime).toBeVisible();

    const nextSyncTime = syncIntent.locator('[data-testid="next-sync-time"]');
    await expect(nextSyncTime).toBeVisible();

    console.log('✅ TC8: Ads 账号级联、报表汇总、同步意图验证通过');
  });

});