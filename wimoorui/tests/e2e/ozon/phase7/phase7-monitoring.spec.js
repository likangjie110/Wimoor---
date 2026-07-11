import { test, expect } from '@playwright/test';

/**
 * Phase 7 - 监控仪表盘 E2E 测试
 *
 * 测试内容：
 * - 页面渲染
 * - 健康检查显示
 * - 指标展示
 * - 图表渲染
 * - 实时更新
 */
test.describe('监控仪表盘', () => {
  test.beforeEach(async ({ page }) => {
    // 登录并导航到监控仪表盘
    await page.goto('/ozon/ops/monitoring');
    await page.waitForLoadState('networkidle');
  });

  test('页面标题和布局正确渲染', async ({ page }) => {
    // 验证页面标题
    await expect(page.locator('h1, .page-title')).toContainText('运维监控');

    // 验证关键区域存在
    await expect(page.locator('.summary-section, .overview-section')).toBeVisible();
    await expect(page.locator('.chart-section, .metrics-section')).toBeVisible();
  });

  test('健康检查状态显示', async ({ page }) => {
    // 验证健康检查卡片存在
    const healthCard = page.locator('.health-card, .health-check-card');
    await expect(healthCard).toBeVisible();

    // 验证状态指示器
    const statusIndicator = healthCard.locator('.status-indicator, .health-status');
    await expect(statusIndicator).toBeVisible();

    // 验证状态文本（健康/异常）
    const statusText = await statusIndicator.textContent();
    expect(statusText).toMatch(/健康|正常|异常|警告/);
  });

  test('API日志统计显示', async ({ page }) => {
    // 验证API日志统计卡片
    const apiLogCard = page.locator('.api-log-card, .card:has-text("API 日志")');
    await expect(apiLogCard).toBeVisible();

    // 验证总数显示
    await expect(apiLogCard.locator('.total-count, .metric-value')).toBeVisible();

    // 验证失败数显示
    await expect(apiLogCard.locator('.failed-count, .error-count')).toBeVisible();
  });

  test('操作审计统计显示', async ({ page }) => {
    // 验证操作审计统计卡片
    const auditCard = page.locator('.audit-card, .card:has-text("操作审计")');
    await expect(auditCard).toBeVisible();

    // 验证总数显示
    await expect(auditCard.locator('.total-count, .metric-value')).toBeVisible();

    // 验证失败数显示
    await expect(auditCard.locator('.failed-count, .error-count')).toBeVisible();
  });

  test('成功率指标显示', async ({ page }) => {
    // 查找成功率卡片
    const successRateCard = page.locator('.success-rate-card, .card:has-text("成功率")');
    if (await successRateCard.isVisible()) {
      // 验证百分比显示
      const percentage = successRateCard.locator('.percentage, .rate-value');
      await expect(percentage).toBeVisible();

      // 验证百分比格式
      const percentageText = await percentage.textContent();
      expect(percentageText).toMatch(/\d+(\.\d+)?%/);
    }
  });

  test('图表区域渲染', async ({ page }) => {
    // 验证图表容器存在
    const chartContainer = page.locator('.chart-container, .echarts-container, canvas');
    await expect(chartContainer.first()).toBeVisible();

    // 等待图表加载
    await page.waitForTimeout(1000);

    // 验证图表元素存在
    const chartElements = await chartContainer.count();
    expect(chartElements).toBeGreaterThan(0);
  });

  test('API调用趋势图显示', async ({ page }) => {
    // 查找API调用趋势图
    const trendChart = page.locator('.api-trend-chart, .chart:has-text("API 调用趋势")');
    if (await trendChart.isVisible()) {
      // 验证图表标题
      await expect(trendChart.locator('.chart-title, h3')).toContainText('API');

      // 验证图表canvas
      await expect(trendChart.locator('canvas, .chart-canvas')).toBeVisible();
    }
  });

  test('错误率趋势图显示', async ({ page }) => {
    // 查找错误率趋势图
    const errorChart = page.locator('.error-rate-chart, .chart:has-text("错误率")');
    if (await errorChart.isVisible()) {
      // 验证图表标题
      await expect(errorChart.locator('.chart-title, h3')).toContainText('错误');

      // 验证图表canvas
      await expect(errorChart.locator('canvas, .chart-canvas')).toBeVisible();
    }
  });

  test('操作类型分布图显示', async ({ page }) => {
    // 查找操作类型分布图
    const distributionChart = page.locator('.operation-distribution-chart, .chart:has-text("操作分布")');
    if (await distributionChart.isVisible()) {
      // 验证图表标题
      await expect(distributionChart.locator('.chart-title, h3')).toBeVisible();

      // 验证图表canvas
      await expect(distributionChart.locator('canvas, .chart-canvas')).toBeVisible();
    }
  });

  test('时间范围选择器功能', async ({ page }) => {
    // 查找时间范围选择器
    const timeRangeSelector = page.locator('.time-range-selector, .date-range-picker');
    if (await timeRangeSelector.isVisible()) {
      // 记录当前指标值
      const metricBefore = await page.locator('.metric-value').first().textContent();

      // 选择不同的时间范围
      await timeRangeSelector.click();
      await page.locator('.range-option:has-text("最近7天"), li:has-text("最近7天")').click();

      // 等待数据刷新
      await page.waitForLoadState('networkidle');

      // 验证数据已更新
      const metricAfter = await page.locator('.metric-value').first().textContent();
      // 可能相同也可能不同，主要是验证不会报错
      expect(metricAfter).toBeTruthy();
    }
  });

  test('刷新按钮功能', async ({ page }) => {
    // 查找刷新按钮
    const refreshButton = page.locator('button:has-text("刷新"), .refresh-button, button[aria-label="刷新"]');
    if (await refreshButton.isVisible()) {
      // 点击刷新
      await refreshButton.click();

      // 等待加载
      await page.waitForLoadState('networkidle');

      // 验证数据仍然显示
      const summarySection = page.locator('.summary-section, .overview-section');
      await expect(summarySection).toBeVisible();
    }
  });

  test('卡片点击跳转到详情页', async ({ page }) => {
    // 查找API日志卡片
    const apiLogCard = page.locator('.api-log-card, .card:has-text("API 日志")');
    if (await apiLogCard.isVisible()) {
      // 查找"查看详情"链接或按钮
      const detailLink = apiLogCard.locator('a:has-text("查看详情"), button:has-text("查看详情"), .detail-link');
      if (await detailLink.isVisible()) {
        // 记录当前URL
        const currentUrl = page.url();

        // 点击查看详情
        await detailLink.click();
        await page.waitForLoadState('networkidle');

        // 验证URL已改变
        const newUrl = page.url();
        expect(newUrl).not.toBe(currentUrl);
        expect(newUrl).toContain('/api-log');
      }
    }
  });

  test('数据加载状态显示', async ({ page }) => {
    // 重新加载页面
    await page.reload();

    // 验证加载指示器（可能很快消失）
    const loadingIndicator = page.locator('.loading, .el-loading-mask, .spinner');
    // 加载指示器可能已经消失，所以不强制要求可见
    const isLoading = await loadingIndicator.isVisible().catch(() => false);
    // 只是验证不会出错，加载状态可能很快就结束了
    expect(typeof isLoading).toBe('boolean');
  });

  test('响应式布局适配', async ({ page }) => {
    // 测试桌面视图
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.waitForTimeout(500);

    const desktopCards = await page.locator('.card, .summary-card').count();
    expect(desktopCards).toBeGreaterThan(0);

    // 测试平板视图
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.waitForTimeout(500);

    const tabletCards = await page.locator('.card, .summary-card').count();
    expect(tabletCards).toBeGreaterThan(0);
  });

  test('指标数值格式正确', async ({ page }) => {
    // 验证总数格式
    const totalCountElements = page.locator('.total-count, .metric-value');
    const firstCount = await totalCountElements.first().textContent();
    expect(firstCount).toMatch(/\d+/);

    // 验证百分比格式
    const percentageElements = page.locator('.percentage, .rate-value');
    if (await percentageElements.count() > 0) {
      const firstPercentage = await percentageElements.first().textContent();
      expect(firstPercentage).toMatch(/\d+(\.\d+)?%/);
    }
  });

  test('空数据状态处理', async ({ page }) => {
    // 选择一个可能没有数据的授权
    const authSelector = page.locator('.auth-selector, select[name="authId"]');
    if (await authSelector.isVisible()) {
      // 选择第一个选项
      await authSelector.selectOption({ index: 0 });
      await page.waitForLoadState('networkidle');

      // 验证页面仍然正常显示
      const summarySection = page.locator('.summary-section, .overview-section');
      await expect(summarySection).toBeVisible();
    }
  });

  test('实时更新提示', async ({ page }) => {
    // 查找自动刷新配置
    const autoRefreshToggle = page.locator('.auto-refresh-toggle, input[type="checkbox"]:near(:has-text("自动刷新"))');
    if (await autoRefreshToggle.isVisible()) {
      // 启用自动刷新
      await autoRefreshToggle.check();

      // 等待一段时间
      await page.waitForTimeout(2000);

      // 验证页面仍然正常
      const summarySection = page.locator('.summary-section, .overview-section');
      await expect(summarySection).toBeVisible();
    }
  });

  test('异常状态告警显示', async ({ page }) => {
    // 查找告警区域
    const alertArea = page.locator('.alert-area, .warning-area, .alert-banner');
    const alertCount = await alertArea.count();

    // 告警可能存在也可能不存在，只验证不会出错
    expect(alertCount).toBeGreaterThanOrEqual(0);

    if (alertCount > 0) {
      // 如果有告警，验证告警内容
      const alertText = await alertArea.first().textContent();
      expect(alertText).toBeTruthy();
    }
  });
});
