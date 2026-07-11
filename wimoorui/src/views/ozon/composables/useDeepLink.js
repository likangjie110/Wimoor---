import { useRouter } from 'vue-router';

/**
 * 深链跳转 Composable
 *
 * 提供从错误中心、任务中心、日志中心跳回源页面的能力
 */
export function useDeepLink() {
  const router = useRouter();

  /**
   * 构建深链 URL
   * @param {string} module - 模块名称（product, stock, price, posting 等）
   * @param {string} entityId - 实体 ID
   * @param {string} focus - 聚焦参数（可选）
   * @returns {string} 深链 URL
   */
  function buildDeepLink(module, entityId, focus = null) {
    const moduleRouteMap = {
      product: '/ozon/product',
      stock: '/ozon/stock',
      price: '/ozon/price',
      posting: '/ozon/posting',
      aftersale: '/ozon/posting', // AfterSale 也是在 Posting 页面
      finance: '/ozon/finance',
      chat: '/ozon/chat',
      ads: '/ozon/ads',
      warehouse: '/ozon/auth', // Warehouse 在授权页面管理
      auth: '/ozon/auth'
    };

    const basePath = moduleRouteMap[module.toLowerCase()];
    if (!basePath) {
      console.warn(`Unknown module: ${module}`);
      return null;
    }

    const query = { id: entityId };
    if (focus) {
      query.focus = focus;
    }

    return {
      path: basePath,
      query
    };
  }

  /**
   * 跳转到深链
   * @param {string} module - 模块名称
   * @param {string} entityId - 实体 ID
   * @param {string} focus - 聚焦参数（可选）
   */
  function navigateToDeepLink(module, entityId, focus = null) {
    const route = buildDeepLink(module, entityId, focus);
    if (route) {
      router.push(route);
    }
  }

  /**
   * 从错误记录跳转
   * @param {Object} errorRecord - 错误记录对象
   */
  function navigateFromError(errorRecord) {
    if (!errorRecord || !errorRecord.module || !errorRecord.entityId) {
      console.warn('Invalid error record for deep link', errorRecord);
      return;
    }

    navigateToDeepLink(
      errorRecord.module,
      errorRecord.entityId,
      errorRecord.entityType || null
    );
  }

  /**
   * 从任务记录跳转
   * @param {Object} taskRecord - 任务记录对象
   */
  function navigateFromTask(taskRecord) {
    if (!taskRecord || !taskRecord.module || !taskRecord.entityId) {
      console.warn('Invalid task record for deep link', taskRecord);
      return;
    }

    navigateToDeepLink(
      taskRecord.module,
      taskRecord.entityId,
      taskRecord.taskType || null
    );
  }

  /**
   * 从 API 日志跳转
   * @param {Object} apiLog - API 日志记录对象
   */
  function navigateFromApiLog(apiLog) {
    if (!apiLog || !apiLog.objectType || !apiLog.objectId) {
      console.warn('Invalid API log for deep link', apiLog);
      return;
    }

    // API 日志的 objectType 就是模块名
    navigateToDeepLink(
      apiLog.objectType,
      apiLog.objectId,
      apiLog.apiGroup || null
    );
  }

  /**
   * 从操作审计跳转
   * @param {Object} auditRecord - 审计记录对象
   */
  function navigateFromAudit(auditRecord) {
    if (!auditRecord || !auditRecord.objectType || !auditRecord.objectId) {
      console.warn('Invalid audit record for deep link', auditRecord);
      return;
    }

    navigateToDeepLink(
      auditRecord.objectType,
      auditRecord.objectId,
      auditRecord.operationType || null
    );
  }

  /**
   * Product → Stock 深链
   * @param {string} productId - 产品 ID
   */
  function navigateProductToStock(productId) {
    navigateToDeepLink('stock', productId, 'product');
  }

  /**
   * Product → Price 深链
   * @param {string} productId - 产品 ID
   */
  function navigateProductToPrice(productId) {
    navigateToDeepLink('price', productId, 'product');
  }

  /**
   * Stock → Product 深链
   * @param {string} stockId - 库存 ID
   */
  function navigateStockToProduct(stockId) {
    navigateToDeepLink('product', stockId, 'stock');
  }

  /**
   * Price → Product 深链
   * @param {string} priceId - 价格 ID
   */
  function navigatePriceToProduct(priceId) {
    navigateToDeepLink('product', priceId, 'price');
  }

  return {
    buildDeepLink,
    navigateToDeepLink,
    navigateFromError,
    navigateFromTask,
    navigateFromApiLog,
    navigateFromAudit,
    navigateProductToStock,
    navigateProductToPrice,
    navigateStockToProduct,
    navigatePriceToProduct
  };
}
