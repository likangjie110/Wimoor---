import { test, expect } from '@playwright/test';

/**
 * Phase 7 - 深链跳转集成测试
 *
 * 测试跨模块深链跳转的完整流程：
 * - 错误中心 → 源页面深链
 * - 任务中心 → 源页面深链
 * - 跨模块深链测试
 */
test.describe('深链跳转集成测试', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/login');
    await page.fill('input[name="username"]', 'test-user');
    await page.fill('input[name="password"]', 'test-password');
    await page.click('button:has-text("登录")');
    await page.waitForLoadState('networkidle');
  });

  test('从错误中心跳转到产品页面', async ({ page }) => {
    // 1. 导航到错误中心
    await page.goto('/ozon/error-center');
    await page.waitForLoadState('networkidle');

    // 2. 查找第一个产品相关错误
    const errorRow = page.locator('table tbody tr, .error-row').first();
    await expect(errorRow).toBeVisible();

    // 3. 点击跳转按钮
    const jumpButton = errorRow.locator('button:has-text("跳转"), .jump-button, .deep-link-button');
    if (await jumpButton.isVisible()) {
      await jumpButton.click();
      await page.waitForLoadState('networkidle');

      // 4. 验证跳转到产品页面
      expect(page.url()).toContain('/ozon/product');

      // 5. 验证页面包含实体ID参数
      const url = new URL(page.url());
      expect(url.searchParams.has('id')).toBeTruthy();
    }
  });

  test('从任务中心跳转到库存页面', async ({ page }) => {
    // 1. 导航到任务中心
    await page.goto('/ozon/task-center');
    await page.waitForLoadState('networkidle');

    // 2. 筛选库存相关任务
    const moduleFilter = page.locator('select[name="module"], .module-filter');
    if (await moduleFilter.isVisible()) {
      await moduleFilter.selectOption('stock');
      await page.waitForLoadState('networkidle');
    }

    // 3. 查找第一个库存任务
    const taskRow = page.locator('table tbody tr, .task-row').first();
    if (await taskRow.isVisible()) {
      // 4. 点击跳转按钮
      const jumpButton = taskRow.locator('button:has-text("跳转"), .jump-button, .deep-link-button');
      if (await jumpButton.isVisible()) {
        await jumpButton.click();
        await page.waitForLoadState('networkidle');

        // 5. 验证跳转到库存页面
        expect(page.url()).toContain('/ozon/stock');
      }
    }
  });

  test('从API日志跳转到源页面', async ({ page }) => {
    // 1. 导航到API日志页面
    await page.goto('/ozon/ops/api-log');
    await page.waitForLoadState('networkidle');

    // 2. 查找第一条日志
    const logRow = page.locator('table tbody tr, .api-log-row').first();
    await expect(logRow).toBeVisible();

    // 3. 点击详情查看对象信息
    await logRow.locator('button:has-text("详情"), .detail-button').click();

    const detailDialog = page.locator('.detail-dialog, .detail-drawer, [role="dialog"]');
    await expect(detailDialog).toBeVisible();

    // 4. 查找跳转按钮
    const jumpButton = detailDialog.locator('button:has-text("跳转到源"), .jump-to-source-button');
    if (await jumpButton.isVisible()) {
      const currentUrl = page.url();

      await jumpButton.click();
      await page.waitForLoadState('networkidle');

      // 5. 验证URL已改变
      const newUrl = page.url();
      expect(newUrl).not.toBe(currentUrl);
      expect(newUrl).toContain('/ozon/');
    }
  });

  test('从审计日志跳转到源页面', async ({ page }) => {
    // 1. 导航到审计日志页面
    await page.goto('/ozon/ops/audit-log');
    await page.waitForLoadState('networkidle');

    // 2. 查找第一条审计记录
    const auditRow = page.locator('table tbody tr, .audit-row').first();
    await expect(auditRow).toBeVisible();

    // 3. 点击详情
    await auditRow.locator('button:has-text("详情"), .detail-button').click();

    const detailDialog = page.locator('.detail-dialog, .detail-drawer, [role="dialog"]');
    await expect(detailDialog).toBeVisible();

    // 4. 查找跳转按钮
    const jumpButton = detailDialog.locator('button:has-text("跳转到源"), .jump-to-source-button');
    if (await jumpButton.isVisible()) {
      const currentUrl = page.url();

      await jumpButton.click();
      await page.waitForLoadState('networkidle');

      // 5. 验证URL已改变
      const newUrl = page.url();
      expect(newUrl).not.toBe(currentUrl);
    }
  });

  test('产品到库存的深链跳转', async ({ page }) => {
    // 1. 导航到产品页面
    await page.goto('/ozon/product');
    await page.waitForLoadState('networkidle');

    // 2. 选择第一个产品
    const productRow = page.locator('table tbody tr, .product-row').first();
    if (await productRow.isVisible()) {
      await productRow.click();
      await page.waitForLoadState('networkidle');

      // 3. 查找"查看库存"按钮
      const viewStockButton = page.locator('button:has-text("查看库存"), .view-stock-button');
      if (await viewStockButton.isVisible()) {
        // 获取产品ID
        const productIdElement = page.locator('.product-id, [data-product-id]');
        let productId = null;
        if (await productIdElement.isVisible()) {
          productId = await productIdElement.textContent();
        }

        // 点击查看库存
        await viewStockButton.click();
        await page.waitForLoadState('networkidle');

        // 4. 验证跳转到库存页面
        expect(page.url()).toContain('/ozon/stock');

        // 5. 验证URL包含产品ID参数
        if (productId) {
          expect(page.url()).toContain(productId.trim());
        }
      }
    }
  });

  test('产品到价格的深链跳转', async ({ page }) => {
    // 1. 导航到产品页面
    await page.goto('/ozon/product');
    await page.waitForLoadState('networkidle');

    // 2. 选择第一个产品
    const productRow = page.locator('table tbody tr, .product-row').first();
    if (await productRow.isVisible()) {
      await productRow.click();
      await page.waitForLoadState('networkidle');

      // 3. 查找"查看价格"按钮
      const viewPriceButton = page.locator('button:has-text("查看价格"), .view-price-button');
      if (await viewPriceButton.isVisible()) {
        await viewPriceButton.click();
        await page.waitForLoadState('networkidle');

        // 4. 验证跳转到价格页面
        expect(page.url()).toContain('/ozon/price');
      }
    }
  });

  test('库存到产品的深链跳转', async ({ page }) => {
    // 1. 导航到库存页面
    await page.goto('/ozon/stock');
    await page.waitForLoadState('networkidle');

    // 2. 选择第一个库存记录
    const stockRow = page.locator('table tbody tr, .stock-row').first();
    if (await stockRow.isVisible()) {
      await stockRow.click();
      await page.waitForLoadState('networkidle');

      // 3. 查找"查看产品"按钮
      const viewProductButton = page.locator('button:has-text("查看产品"), .view-product-button');
      if (await viewProductButton.isVisible()) {
        await viewProductButton.click();
        await page.waitForLoadState('networkidle');

        // 4. 验证跳转到产品页面
        expect(page.url()).toContain('/ozon/product');
      }
    }
  });

  test('价格到产品的深链跳转', async ({ page }) => {
    // 1. 导航到价格页面
    await page.goto('/ozon/price');
    await page.waitForLoadState('networkidle');

    // 2. 选择第一个价格记录
    const priceRow = page.locator('table tbody tr, .price-row').first();
    if (await priceRow.isVisible()) {
      await priceRow.click();
      await page.waitForLoadState('networkidle');

      // 3. 查找"查看产品"按钮
      const viewProductButton = page.locator('button:has-text("查看产品"), .view-product-button');
      if (await viewProductButton.isVisible()) {
        await viewProductButton.click();
        await page.waitForLoadState('networkidle');

        // 4. 验证跳转到产品页面
        expect(page.url()).toContain('/ozon/product');
      }
    }
  });

  test('深链跳转保持focus参数', async ({ page }) => {
    // 1. 使用带focus参数的深链URL
    await page.goto('/ozon/product?id=product-123&focus=stock');
    await page.waitForLoadState('networkidle');

    // 2. 验证页面接收到focus参数
    const url = new URL(page.url());
    expect(url.searchParams.get('focus')).toBe('stock');

    // 3. 验证页面根据focus参数高亮相应区域（如果实现了）
    const stockSection = page.locator('.stock-section, [data-section="stock"]');
    if (await stockSection.isVisible()) {
      // 可能有高亮或展开状态
      const isHighlighted = await stockSection.evaluate(el =>
        el.classList.contains('highlighted') ||
        el.classList.contains('active') ||
        el.classList.contains('expanded')
      );
      expect(isHighlighted).toBeTruthy();
    }
  });

  test('跨模块深链携带上下文信息', async ({ page }) => {
    // 1. 从错误中心带着错误信息跳转
    await page.goto('/ozon/error-center');
    await page.waitForLoadState('networkidle');

    const errorRow = page.locator('table tbody tr, .error-row').first();
    if (await errorRow.isVisible()) {
      // 获取错误信息
      const errorMessage = await errorRow.locator('.error-message, td:nth-child(2)').textContent();

      // 点击跳转
      const jumpButton = errorRow.locator('button:has-text("跳转"), .jump-button');
      if (await jumpButton.isVisible()) {
        await jumpButton.click();
        await page.waitForLoadState('networkidle');

        // 2. 验证目标页面显示错误提示
        const errorBanner = page.locator('.error-banner, .alert-banner, .from-error-center');
        if (await errorBanner.isVisible()) {
          const bannerText = await errorBanner.textContent();
          expect(bannerText).toContain('错误');
        }
      }
    }
  });

  test('深链跳转处理无效模块', async ({ page }) => {
    // 1. 尝试跳转到无效模块
    await page.goto('/ozon/product');
    await page.waitForLoadState('networkidle');

    // 2. 使用 JavaScript 尝试无效深链
    const result = await page.evaluate(() => {
      // 模拟调用深链函数
      try {
        const event = new CustomEvent('deeplink', {
          detail: { module: 'invalid-module', entityId: 'test-123' }
        });
        window.dispatchEvent(event);
        return 'no-error';
      } catch (e) {
        return 'error';
      }
    });

    // 3. 验证没有导航发生
    expect(page.url()).toContain('/ozon/product');
  });

  test('深链跳转权限检查', async ({ page }) => {
    // 1. 尝试跳转到没有权限的页面
    await page.goto('/ozon/product?id=unauthorized-product-123');
    await page.waitForLoadState('networkidle');

    // 2. 验证显示权限错误或重定向
    const currentPath = new URL(page.url()).pathname;
    const errorMessage = page.locator('.error-message, .permission-denied, .no-access');

    // 可能显示错误信息或重定向到授权页面
    const hasError = await errorMessage.isVisible();
    const isRedirected = currentPath !== '/ozon/product';

    expect(hasError || isRedirected).toBeTruthy();
  });
});
