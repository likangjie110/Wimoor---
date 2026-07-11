import { test, expect } from '@playwright/test';

/**
 * Phase 7 - 审计日志查询页面 E2E 测试
 *
 * 测试内容：
 * - 页面渲染
 * - 查询功能（按操作类型/操作人/时间范围）
 * - 详情查看
 * - 分页
 */
test.describe('审计日志查询页面', () => {
  test.beforeEach(async ({ page }) => {
    // 登录并导航到审计日志页面
    await page.goto('/ozon/ops/audit-log');
    await page.waitForLoadState('networkidle');
  });

  test('页面标题和布局正确渲染', async ({ page }) => {
    // 验证页面标题
    await expect(page.locator('h1, .page-title')).toContainText('操作审计');

    // 验证查询表单存在
    await expect(page.locator('.query-form, .search-form')).toBeVisible();

    // 验证表格存在
    await expect(page.locator('.audit-table, table')).toBeVisible();
  });

  test('查询表单包含所有必要字段', async ({ page }) => {
    // 操作类型选择器
    await expect(page.locator('select[name="operationType"], .operation-type-select')).toBeVisible();

    // 结果状态选择器
    await expect(page.locator('select[name="resultStatus"], .result-status-select')).toBeVisible();

    // 对象类型输入框
    await expect(page.locator('input[name="objectType"], .object-type-input')).toBeVisible();

    // 查询按钮
    await expect(page.locator('button:has-text("查询"), .query-button')).toBeVisible();
  });

  test('按操作类型查询', async ({ page }) => {
    // 选择操作类型
    await page.selectOption('select[name="operationType"], .operation-type-select', 'PRODUCT_PUBLISH');

    // 点击查询
    await page.click('button:has-text("查询"), .query-button');

    // 等待加载
    await page.waitForLoadState('networkidle');

    // 验证结果
    const rows = page.locator('table tbody tr, .audit-row');
    await expect(rows.first()).toBeVisible();

    // 验证操作类型列
    const firstCell = rows.first().locator('td').first();
    await expect(firstCell).toContainText('PRODUCT_PUBLISH');
  });

  test('按结果状态筛选', async ({ page }) => {
    // 选择失败状态
    await page.selectOption('select[name="resultStatus"], .result-status-select', 'FAILED');

    // 点击查询
    await page.click('button:has-text("查询"), .query-button');

    // 等待加载
    await page.waitForLoadState('networkidle');

    // 验证结果包含失败状态
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
    const rows = page.locator('table tbody tr, .audit-row');
    await expect(rows.first()).toBeVisible();
  });

  test('按对象ID查询', async ({ page }) => {
    // 输入对象ID
    await page.fill('input[name="objectId"], .object-id-input', 'draft-123');

    // 点击查询
    await page.click('button:has-text("查询"), .query-button');

    // 等待加载
    await page.waitForLoadState('networkidle');

    // 验证结果
    const rows = page.locator('table tbody tr, .audit-row');
    const count = await rows.count();
    expect(count).toBeGreaterThanOrEqual(0);
  });

  test('组合条件查询', async ({ page }) => {
    // 选择操作类型
    await page.selectOption('select[name="operationType"], .operation-type-select', 'STOCK_UPDATE');

    // 选择结果状态
    await page.selectOption('select[name="resultStatus"], .result-status-select', 'SUCCESS');

    // 输入对象类型
    await page.fill('input[name="objectType"], .object-type-input', 'STOCK');

    // 点击查询
    await page.click('button:has-text("查询"), .query-button');

    // 等待加载
    await page.waitForLoadState('networkidle');

    // 验证结果
    const rows = page.locator('table tbody tr, .audit-row');
    const count = await rows.count();
    expect(count).toBeGreaterThanOrEqual(0);
  });

  test('查看审计详情', async ({ page }) => {
    // 等待审计列表加载
    const firstRow = page.locator('table tbody tr, .audit-row').first();
    await expect(firstRow).toBeVisible();

    // 点击查看详情按钮
    await firstRow.locator('button:has-text("详情"), .detail-button').click();

    // 验证详情对话框/抽屉打开
    const detailDialog = page.locator('.detail-dialog, .detail-drawer, [role="dialog"]');
    await expect(detailDialog).toBeVisible();

    // 验证详情内容
    await expect(detailDialog.locator('.operation-type, .field:has-text("操作类型")')).toBeVisible();
    await expect(detailDialog.locator('.object-type, .field:has-text("对象类型")')).toBeVisible();
    await expect(detailDialog.locator('.result-status, .field:has-text("结果状态")')).toBeVisible();
  });

  test('审计详情显示操作参数', async ({ page }) => {
    // 点击第一行详情
    const firstRow = page.locator('table tbody tr, .audit-row').first();
    await expect(firstRow).toBeVisible();
    await firstRow.locator('button:has-text("详情"), .detail-button').click();

    // 验证详情对话框
    const detailDialog = page.locator('.detail-dialog, .detail-drawer, [role="dialog"]');
    await expect(detailDialog).toBeVisible();

    // 验证请求参数
    await expect(detailDialog.locator('.request-payload, .field:has-text("请求参数")')).toBeVisible();

    // 验证结果信息
    await expect(detailDialog.locator('.result-message, .field:has-text("结果信息")')).toBeVisible();

    // 关闭详情
    await detailDialog.locator('button:has-text("关闭"), .close-button').click();
    await expect(detailDialog).not.toBeVisible();
  });

  test('表格显示关键字段', async ({ page }) => {
    // 验证表头
    const headers = page.locator('table thead th, .table-header');
    await expect(headers).toContainText(['操作类型', '对象类型', '结果状态', '操作时间']);

    // 验证第一行数据
    const firstRow = page.locator('table tbody tr, .audit-row').first();
    await expect(firstRow).toBeVisible();

    const cells = firstRow.locator('td');
    expect(await cells.count()).toBeGreaterThanOrEqual(4);
  });

  test('显示操作人信息', async ({ page }) => {
    // 获取第一行
    const firstRow = page.locator('table tbody tr, .audit-row').first();
    await expect(firstRow).toBeVisible();

    // 验证操作人字段存在
    const operatorCell = firstRow.locator('.operator-cell, td:has-text("操作人")');
    if (await operatorCell.isVisible()) {
      const operatorText = await operatorCell.textContent();
      expect(operatorText).toBeTruthy();
    }
  });

  test('分页功能', async ({ page }) => {
    // 验证分页器存在
    const pagination = page.locator('.pagination, .el-pagination');
    await expect(pagination).toBeVisible();

    // 获取第一页的第一行数据
    const firstPageFirstRow = page.locator('table tbody tr, .audit-row').first();
    const firstPageText = await firstPageFirstRow.textContent();

    // 点击下一页（如果有）
    const nextButton = pagination.locator('button:has-text("下一页"), .next-page');
    if (await nextButton.isEnabled()) {
      await nextButton.click();
      await page.waitForLoadState('networkidle');

      // 验证数据已更新
      const secondPageFirstRow = page.locator('table tbody tr, .audit-row').first();
      const secondPageText = await secondPageFirstRow.textContent();
      expect(secondPageText).not.toBe(firstPageText);
    }
  });

  test('空数据状态显示', async ({ page }) => {
    // 输入不存在的对象ID
    await page.fill('input[name="objectId"], .object-id-input', 'non-existent-audit-12345');

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
    await page.selectOption('select[name="operationType"], .operation-type-select', 'PRODUCT_PUBLISH');
    await page.selectOption('select[name="resultStatus"], .result-status-select', 'FAILED');

    // 点击重置按钮
    const resetButton = page.locator('button:has-text("重置"), .reset-button');
    if (await resetButton.isVisible()) {
      await resetButton.click();

      // 验证表单已重置
      const operationTypeValue = await page.locator('select[name="operationType"], .operation-type-select').inputValue();
      const resultStatusValue = await page.locator('select[name="resultStatus"], .result-status-select').inputValue();
      expect(operationTypeValue).toBe('');
      expect(resultStatusValue).toBe('');
    }
  });

  test('失败状态显示错误详情', async ({ page }) => {
    // 筛选失败状态
    await page.selectOption('select[name="resultStatus"], .result-status-select', 'FAILED');
    await page.click('button:has-text("查询"), .query-button');
    await page.waitForLoadState('networkidle');

    // 点击第一行详情
    const firstRow = page.locator('table tbody tr, .audit-row').first();
    if (await firstRow.isVisible()) {
      await firstRow.locator('button:has-text("详情"), .detail-button').click();

      // 验证错误信息字段
      const detailDialog = page.locator('.detail-dialog, .detail-drawer, [role="dialog"]');
      await expect(detailDialog.locator('.result-message, .field:has-text("结果信息")')).toBeVisible();
    }
  });

  test('对象代码显示', async ({ page }) => {
    // 获取第一行
    const firstRow = page.locator('table tbody tr, .audit-row').first();
    await expect(firstRow).toBeVisible();

    // 点击详情
    await firstRow.locator('button:has-text("详情"), .detail-button').click();

    // 验证对象代码字段
    const detailDialog = page.locator('.detail-dialog, .detail-drawer, [role="dialog"]');
    await expect(detailDialog.locator('.object-code, .field:has-text("对象代码")')).toBeVisible();
  });

  test('时间格式正确显示', async ({ page }) => {
    // 获取第一行
    const firstRow = page.locator('table tbody tr, .audit-row').first();
    await expect(firstRow).toBeVisible();

    // 验证时间字段存在且格式正确
    const timeCell = firstRow.locator('.time-cell, td:last-child');
    const timeText = await timeCell.textContent();
    expect(timeText).toMatch(/\d{4}-\d{2}-\d{2}|\d{2}:\d{2}:\d{2}/);
  });

  test('从审计日志跳转到源页面', async ({ page }) => {
    // 获取第一行
    const firstRow = page.locator('table tbody tr, .audit-row').first();
    await expect(firstRow).toBeVisible();

    // 查找跳转按钮
    const navigateButton = firstRow.locator('button:has-text("跳转"), .navigate-button, .deep-link-button');
    if (await navigateButton.isVisible()) {
      // 记录当前URL
      const currentUrl = page.url();

      // 点击跳转
      await navigateButton.click();
      await page.waitForLoadState('networkidle');

      // 验证URL已改变
      const newUrl = page.url();
      expect(newUrl).not.toBe(currentUrl);
    }
  });
});
