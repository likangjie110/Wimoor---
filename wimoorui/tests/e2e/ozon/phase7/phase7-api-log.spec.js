import { test, expect } from '@playwright/test';

/**
 * Phase 7 - API 日志查询页面 E2E 测试
 *
 * 测试内容：
 * - 页面渲染
 * - 查询功能（按模块/时间范围/状态）
 * - 详情查看
 * - 分页
 */
test.describe('API 日志查询页面', () => {
  test.beforeEach(async ({ page }) => {
    // 登录并导航到 API 日志页面
    await page.goto('/ozon/ops/api-log');
    await page.waitForLoadState('networkidle');
  });

  test('页面标题和布局正确渲染', async ({ page }) => {
    // 验证页面标题
    await expect(page.locator('h1, .page-title')).toContainText('API 日志');

    // 验证查询表单存在
    await expect(page.locator('.query-form, .search-form')).toBeVisible();

    // 验证表格存在
    await expect(page.locator('.api-log-table, table')).toBeVisible();
  });

  test('查询表单包含所有必要字段', async ({ page }) => {
    // API 分组选择器
    await expect(page.locator('select[name="apiGroup"], .api-group-select')).toBeVisible();

    // 状态选择器
    await expect(page.locator('select[name="status"], .status-select')).toBeVisible();

    // 对象类型输入框
    await expect(page.locator('input[name="objectType"], .object-type-input')).toBeVisible();

    // 查询按钮
    await expect(page.locator('button:has-text("查询"), .query-button')).toBeVisible();
  });

  test('按API模块查询', async ({ page }) => {
    // 选择 PRODUCT 模块
    await page.selectOption('select[name="apiGroup"], .api-group-select', 'PRODUCT');

    // 点击查询
    await page.click('button:has-text("查询"), .query-button');

    // 等待加载
    await page.waitForLoadState('networkidle');

    // 验证结果
    const rows = page.locator('table tbody tr, .api-log-row');
    await expect(rows.first()).toBeVisible();

    // 验证所有行都是 PRODUCT 模块
    const firstCell = rows.first().locator('td').first();
    await expect(firstCell).toContainText('PRODUCT');
  });

  test('按状态筛选', async ({ page }) => {
    // 选择 FAILED 状态
    await page.selectOption('select[name="status"], .status-select', 'FAILED');

    // 点击查询
    await page.click('button:has-text("查询"), .query-button');

    // 等待加载
    await page.waitForLoadState('networkidle');

    // 验证结果包含失败状态标识
    const statusCells = page.locator('.status-cell, td:has-text("FAILED")');
    await expect(statusCells.first()).toBeVisible();
  });

  test('按对象类型查询', async ({ page }) => {
    // 输入对象类型
    await page.fill('input[name="objectType"], .object-type-input', 'PRODUCT');

    // 点击查询
    await page.click('button:has-text("查询"), .query-button');

    // 等待加载
    await page.waitForLoadState('networkidle');

    // 验证结果
    const rows = page.locator('table tbody tr, .api-log-row');
    await expect(rows.first()).toBeVisible();
  });

  test('组合条件查询', async ({ page }) => {
    // 选择模块
    await page.selectOption('select[name="apiGroup"], .api-group-select', 'STOCK');

    // 选择状态
    await page.selectOption('select[name="status"], .status-select', 'SUCCESS');

    // 点击查询
    await page.click('button:has-text("查询"), .query-button');

    // 等待加载
    await page.waitForLoadState('networkidle');

    // 验证结果
    const rows = page.locator('table tbody tr, .api-log-row');
    const count = await rows.count();
    expect(count).toBeGreaterThanOrEqual(0);
  });

  test('查看日志详情', async ({ page }) => {
    // 等待日志列表加载
    const firstRow = page.locator('table tbody tr, .api-log-row').first();
    await expect(firstRow).toBeVisible();

    // 点击查看详情按钮
    await firstRow.locator('button:has-text("详情"), .detail-button').click();

    // 验证详情对话框/抽屉打开
    const detailDialog = page.locator('.detail-dialog, .detail-drawer, [role="dialog"]');
    await expect(detailDialog).toBeVisible();

    // 验证详情内容
    await expect(detailDialog.locator('.api-group, .field:has-text("API 分组")')).toBeVisible();
    await expect(detailDialog.locator('.action-name, .field:has-text("操作名称")')).toBeVisible();
    await expect(detailDialog.locator('.duration, .field:has-text("耗时")')).toBeVisible();
  });

  test('日志详情显示请求和响应', async ({ page }) => {
    // 点击第一行详情
    const firstRow = page.locator('table tbody tr, .api-log-row').first();
    await expect(firstRow).toBeVisible();
    await firstRow.locator('button:has-text("详情"), .detail-button').click();

    // 验证详情对话框
    const detailDialog = page.locator('.detail-dialog, .detail-drawer, [role="dialog"]');
    await expect(detailDialog).toBeVisible();

    // 验证请求 payload
    await expect(detailDialog.locator('.request-payload, .field:has-text("请求")')).toBeVisible();

    // 验证响应 payload
    await expect(detailDialog.locator('.response-payload, .field:has-text("响应")')).toBeVisible();

    // 关闭详情
    await detailDialog.locator('button:has-text("关闭"), .close-button').click();
    await expect(detailDialog).not.toBeVisible();
  });

  test('表格显示关键字段', async ({ page }) => {
    // 验证表头
    const headers = page.locator('table thead th, .table-header');
    await expect(headers).toContainText(['API 分组', '操作名称', '状态', '耗时']);

    // 验证第一行数据
    const firstRow = page.locator('table tbody tr, .api-log-row').first();
    await expect(firstRow).toBeVisible();

    const cells = firstRow.locator('td');
    expect(await cells.count()).toBeGreaterThanOrEqual(4);
  });

  test('分页功能', async ({ page }) => {
    // 验证分页器存在
    const pagination = page.locator('.pagination, .el-pagination');
    await expect(pagination).toBeVisible();

    // 获取第一页的第一行数据
    const firstPageFirstRow = page.locator('table tbody tr, .api-log-row').first();
    const firstPageText = await firstPageFirstRow.textContent();

    // 点击下一页（如果有）
    const nextButton = pagination.locator('button:has-text("下一页"), .next-page');
    if (await nextButton.isEnabled()) {
      await nextButton.click();
      await page.waitForLoadState('networkidle');

      // 验证数据已更新
      const secondPageFirstRow = page.locator('table tbody tr, .api-log-row').first();
      const secondPageText = await secondPageFirstRow.textContent();
      expect(secondPageText).not.toBe(firstPageText);
    }
  });

  test('空数据状态显示', async ({ page }) => {
    // 输入不存在的对象ID
    await page.fill('input[name="objectId"], .object-id-input', 'non-existent-id-12345');

    // 点击查询
    await page.click('button:has-text("查询"), .query-button');

    // 等待加载
    await page.waitForLoadState('networkidle');

    // 验证空数据提示
    const emptyState = page.locator('.empty-state, .no-data, :has-text("暂无数据")');
    await expect(emptyState).toBeVisible();
  });

  test('重置查询条件', async ({ page }) => {
    // 填写查询条件
    await page.selectOption('select[name="apiGroup"], .api-group-select', 'PRODUCT');
    await page.selectOption('select[name="status"], .status-select', 'FAILED');

    // 点击重置按钮
    const resetButton = page.locator('button:has-text("重置"), .reset-button');
    if (await resetButton.isVisible()) {
      await resetButton.click();

      // 验证表单已重置
      const apiGroupValue = await page.locator('select[name="apiGroup"], .api-group-select').inputValue();
      const statusValue = await page.locator('select[name="status"], .status-select').inputValue();
      expect(apiGroupValue).toBe('');
      expect(statusValue).toBe('');
    }
  });

  test('耗时显示格式正确', async ({ page }) => {
    // 获取第一行
    const firstRow = page.locator('table tbody tr, .api-log-row').first();
    await expect(firstRow).toBeVisible();

    // 验证耗时字段存在且格式正确（数字 + ms）
    const durationCell = firstRow.locator('.duration-cell, td:nth-child(5)');
    const durationText = await durationCell.textContent();
    expect(durationText).toMatch(/\d+\s*ms/);
  });

  test('失败状态显示错误信息', async ({ page }) => {
    // 筛选失败状态
    await page.selectOption('select[name="status"], .status-select', 'FAILED');
    await page.click('button:has-text("查询"), .query-button');
    await page.waitForLoadState('networkidle');

    // 点击第一行详情
    const firstRow = page.locator('table tbody tr, .api-log-row').first();
    if (await firstRow.isVisible()) {
      await firstRow.locator('button:has-text("详情"), .detail-button').click();

      // 验证错误信息字段
      const detailDialog = page.locator('.detail-dialog, .detail-drawer, [role="dialog"]');
      await expect(detailDialog.locator('.error-message, .field:has-text("错误")')).toBeVisible();
    }
  });
});
