import { test, expect } from '@playwright/test';

test.describe('Phase 6 Ads 模块完整功能', () => {
  test.beforeEach(async ({ page }) => {
    // 假设已登录，跳转到 Ads 页面
    await page.goto('/ozon/ads');
  });

  test('应该正确加载 Ads 页面和功能开关提示', async ({ page }) => {
    // 验证页面标题
    await expect(page.locator('h3:has-text("Ozon 广告导入")')).toBeVisible();

    // 验证功能开关提示存在
    const featureNotice = page.locator('.el-alert');
    await expect(featureNotice.first()).toBeVisible();
  });

  test('模式切换 - 验证双模切换 Banner', async ({ page }) => {
    // 验证双模切换 Banner 存在
    const modeBanner = page.locator(':has-text("广告双模工作台")');
    await expect(modeBanner).toBeVisible();

    // 验证本地导入模式描述
    await expect(page.locator(':has-text("本地广告导入")')).toBeVisible();

    // 验证官方同步模式描述
    await expect(page.locator(':has-text("官方同步模式")')).toBeVisible();
  });

  test('本地导入功能 - 导入按钮状态', async ({ page }) => {
    // 验证导入按钮存在
    const importButton = page.locator('button:has-text("导入广告数据")');
    await expect(importButton).toBeVisible();

    // 验证授权选择框
    const authSelect = page.locator('.el-select').first();
    await expect(authSelect).toBeVisible();

    // 验证 JSON 输入框
    const jsonInput = page.locator('textarea[placeholder*="原始 JSON"]');
    await expect(jsonInput).toBeVisible();
  });

  test('API 同步面板 - 验证同步广告活动按钮', async ({ page }) => {
    // 验证官方 API 同步卡片
    await expect(page.locator(':has-text("官方 API 同步")')).toBeVisible();

    // 验证同步广告活动按钮
    const syncCampaignsButton = page.locator('button:has-text("同步广告活动")');
    await expect(syncCampaignsButton).toBeVisible();

    // 默认应该是禁用状态（如果功能开关未开启）
    const isDisabled = await syncCampaignsButton.isDisabled();
    expect(isDisabled).toBe(true);
  });

  test('API 同步面板 - 验证同步广告报告功能', async ({ page }) => {
    // 验证同步广告报告部分
    await expect(page.locator(':has-text("同步广告报告")')).toBeVisible();

    // 验证日期选择器
    const datePicker = page.locator('.el-date-editor').last();
    await expect(datePicker).toBeVisible();

    // 验证同步报告按钮
    const syncReportsButton = page.locator('button:has-text("同步广告报告")');
    await expect(syncReportsButton).toBeVisible();
  });

  test('数据展示 - 验证汇总卡片', async ({ page }) => {
    // 验证汇总卡片存在
    await expect(page.locator(':has-text("曝光")')).toBeVisible();
    await expect(page.locator(':has-text("点击")')).toBeVisible();
    await expect(page.locator(':has-text("花费")')).toBeVisible();
    await expect(page.locator(':has-text("订单")')).toBeVisible();
    await expect(page.locator(':has-text("销售额")')).toBeVisible();
    await expect(page.locator(':has-text("ACOS / ROAS")')).toBeVisible();
  });

  test('数据展示 - 验证活动列表表格', async ({ page }) => {
    // 验证活动列表卡片
    await expect(page.locator(':has-text("活动列表")')).toBeVisible();

    // 验证表格列标题
    await expect(page.locator('th:has-text("活动名")')).toBeVisible();
    await expect(page.locator('th:has-text("类型")')).toBeVisible();
    await expect(page.locator('th:has-text("状态")')).toBeVisible();
    await expect(page.locator('th:has-text("预算")')).toBeVisible();

    // 验证刷新按钮
    await expect(page.locator('button:has-text("刷新活动")')).toBeVisible();
  });

  test('数据展示 - 验证日报数据表格', async ({ page }) => {
    // 验证日报数据卡片
    await expect(page.locator(':has-text("日报数据")')).toBeVisible();

    // 验证表格列标题
    await expect(page.locator('th:has-text("日期")')).toBeVisible();
    await expect(page.locator('th:has-text("曝光")')).toBeVisible();
    await expect(page.locator('th:has-text("点击")')).toBeVisible();
    await expect(page.locator('th:has-text("花费")')).toBeVisible();

    // 验证刷新按钮
    await expect(page.locator('button:has-text("刷新报表")')).toBeVisible();
  });

  test('功能开关 - 验证 adsSync 开关控制', async ({ page }) => {
    // 验证同步功能提示信息
    const syncAlert = page.locator('.sync-alert');
    await expect(syncAlert).toBeVisible();

    // 验证提示文字包含功能状态信息
    const alertText = await syncAlert.textContent();
    expect(alertText).toMatch(/API 同步|官方同步/);
  });

  test('筛选功能 - 验证广告账号筛选', async ({ page }) => {
    // 验证广告账号筛选框
    const accountFilter = page.locator('label:has-text("广告账号")').locator('..').locator('.el-select');
    await expect(accountFilter).toBeVisible();
  });

  test('筛选功能 - 验证活动关键词搜索', async ({ page }) => {
    // 验证活动筛选输入框
    const keywordInput = page.locator('input[placeholder*="活动名"]');
    await expect(keywordInput).toBeVisible();
  });
});

test.describe('Phase 6 Ads 模块集成测试', () => {
  test('完整流程 - 导入后同步', async ({ page }) => {
    await page.goto('/ozon/ads');

    // 1. 验证初始状态
    await expect(page.locator('h3:has-text("Ozon 广告导入")')).toBeVisible();

    // 2. 选择授权（如果有数据）
    const authSelect = page.locator('.el-select').first();
    if (await authSelect.isVisible()) {
      await authSelect.click();
      const firstOption = page.locator('.el-select-dropdown__item').first();
      if (await firstOption.isVisible()) {
        await firstOption.click();
      }
    }

    // 3. 验证同步面板可见
    await expect(page.locator(':has-text("官方 API 同步")')).toBeVisible();

    // 4. 验证数据展示区域
    await expect(page.locator(':has-text("活动列表")')).toBeVisible();
    await expect(page.locator(':has-text("日报数据")')).toBeVisible();
  });
});
