import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useDeepLink } from '@/views/ozon/composables/useDeepLink';

/**
 * useDeepLink Composable 测试
 *
 * 测试深链跳转功能：
 * - buildDeepLink() 参数构建和 URL 生成
 * - navigateToDeepLink() 路由跳转和参数传递
 * - 各种专用导航方法
 * - 边界情况处理
 */
describe('useDeepLink', () => {
  let mockRouter;
  let deepLink;

  beforeEach(() => {
    mockRouter = {
      push: vi.fn()
    };

    // Mock useRouter
    vi.mock('vue-router', () => ({
      useRouter: () => mockRouter
    }));

    deepLink = useDeepLink();
  });

  describe('buildDeepLink', () => {
    it('构建产品模块深链', () => {
      const result = deepLink.buildDeepLink('product', 'product-123');

      expect(result).toEqual({
        path: '/ozon/product',
        query: { id: 'product-123' }
      });
    });

    it('构建库存模块深链', () => {
      const result = deepLink.buildDeepLink('stock', 'stock-456');

      expect(result).toEqual({
        path: '/ozon/stock',
        query: { id: 'stock-456' }
      });
    });

    it('构建价格模块深链', () => {
      const result = deepLink.buildDeepLink('price', 'price-789');

      expect(result).toEqual({
        path: '/ozon/price',
        query: { id: 'price-789' }
      });
    });

    it('构建订单模块深链', () => {
      const result = deepLink.buildDeepLink('posting', 'posting-001');

      expect(result).toEqual({
        path: '/ozon/posting',
        query: { id: 'posting-001' }
      });
    });

    it('构建深链时包含focus参数', () => {
      const result = deepLink.buildDeepLink('product', 'product-123', 'stock');

      expect(result).toEqual({
        path: '/ozon/product',
        query: {
          id: 'product-123',
          focus: 'stock'
        }
      });
    });

    it('大小写不敏感', () => {
      const result = deepLink.buildDeepLink('PRODUCT', 'product-123');

      expect(result).toEqual({
        path: '/ozon/product',
        query: { id: 'product-123' }
      });
    });

    it('未知模块返回null并警告', () => {
      const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

      const result = deepLink.buildDeepLink('unknown', 'entity-123');

      expect(result).toBeNull();
      expect(consoleWarnSpy).toHaveBeenCalledWith('Unknown module: unknown');

      consoleWarnSpy.mockRestore();
    });

    it('AfterSale映射到Posting页面', () => {
      const result = deepLink.buildDeepLink('aftersale', 'aftersale-123');

      expect(result).toEqual({
        path: '/ozon/posting',
        query: { id: 'aftersale-123' }
      });
    });

    it('Warehouse映射到Auth页面', () => {
      const result = deepLink.buildDeepLink('warehouse', 'warehouse-123');

      expect(result).toEqual({
        path: '/ozon/auth',
        query: { id: 'warehouse-123' }
      });
    });
  });

  describe('navigateToDeepLink', () => {
    it('调用router.push进行跳转', () => {
      deepLink.navigateToDeepLink('product', 'product-123');

      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/ozon/product',
        query: { id: 'product-123' }
      });
    });

    it('带focus参数跳转', () => {
      deepLink.navigateToDeepLink('stock', 'stock-456', 'product');

      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/ozon/stock',
        query: {
          id: 'stock-456',
          focus: 'product'
        }
      });
    });

    it('未知模块不跳转', () => {
      vi.spyOn(console, 'warn').mockImplementation(() => {});

      deepLink.navigateToDeepLink('unknown', 'entity-123');

      expect(mockRouter.push).not.toHaveBeenCalled();
    });
  });

  describe('navigateFromError', () => {
    it('从错误记录跳转到源页面', () => {
      const errorRecord = {
        module: 'product',
        entityId: 'product-123',
        entityType: 'DRAFT'
      };

      deepLink.navigateFromError(errorRecord);

      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/ozon/product',
        query: {
          id: 'product-123',
          focus: 'DRAFT'
        }
      });
    });

    it('缺少module时警告', () => {
      const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
      const errorRecord = {
        entityId: 'product-123'
      };

      deepLink.navigateFromError(errorRecord);

      expect(consoleWarnSpy).toHaveBeenCalledWith(
        'Invalid error record for deep link',
        errorRecord
      );
      expect(mockRouter.push).not.toHaveBeenCalled();

      consoleWarnSpy.mockRestore();
    });

    it('缺少entityId时警告', () => {
      const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
      const errorRecord = {
        module: 'product'
      };

      deepLink.navigateFromError(errorRecord);

      expect(consoleWarnSpy).toHaveBeenCalled();
      expect(mockRouter.push).not.toHaveBeenCalled();

      consoleWarnSpy.mockRestore();
    });
  });

  describe('navigateFromTask', () => {
    it('从任务记录跳转到源页面', () => {
      const taskRecord = {
        module: 'stock',
        entityId: 'stock-456',
        taskType: 'SYNC'
      };

      deepLink.navigateFromTask(taskRecord);

      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/ozon/stock',
        query: {
          id: 'stock-456',
          focus: 'SYNC'
        }
      });
    });

    it('缺少必要字段时警告', () => {
      const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
      const taskRecord = {
        module: 'stock'
      };

      deepLink.navigateFromTask(taskRecord);

      expect(consoleWarnSpy).toHaveBeenCalled();
      expect(mockRouter.push).not.toHaveBeenCalled();

      consoleWarnSpy.mockRestore();
    });
  });

  describe('navigateFromApiLog', () => {
    it('从API日志跳转到源页面', () => {
      const apiLog = {
        objectType: 'product',
        objectId: 'product-123',
        apiGroup: 'PRODUCT_API'
      };

      deepLink.navigateFromApiLog(apiLog);

      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/ozon/product',
        query: {
          id: 'product-123',
          focus: 'PRODUCT_API'
        }
      });
    });

    it('缺少objectType时警告', () => {
      const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
      const apiLog = {
        objectId: 'product-123'
      };

      deepLink.navigateFromApiLog(apiLog);

      expect(consoleWarnSpy).toHaveBeenCalled();
      expect(mockRouter.push).not.toHaveBeenCalled();

      consoleWarnSpy.mockRestore();
    });
  });

  describe('navigateFromAudit', () => {
    it('从审计记录跳转到源页面', () => {
      const auditRecord = {
        objectType: 'price',
        objectId: 'price-789',
        operationType: 'IMPORT'
      };

      deepLink.navigateFromAudit(auditRecord);

      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/ozon/price',
        query: {
          id: 'price-789',
          focus: 'IMPORT'
        }
      });
    });

    it('缺少必要字段时警告', () => {
      const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
      const auditRecord = {
        operationType: 'IMPORT'
      };

      deepLink.navigateFromAudit(auditRecord);

      expect(consoleWarnSpy).toHaveBeenCalled();
      expect(mockRouter.push).not.toHaveBeenCalled();

      consoleWarnSpy.mockRestore();
    });
  });

  describe('专用导航方法', () => {
    it('navigateProductToStock - 产品到库存', () => {
      deepLink.navigateProductToStock('product-123');

      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/ozon/stock',
        query: {
          id: 'product-123',
          focus: 'product'
        }
      });
    });

    it('navigateProductToPrice - 产品到价格', () => {
      deepLink.navigateProductToPrice('product-123');

      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/ozon/price',
        query: {
          id: 'product-123',
          focus: 'product'
        }
      });
    });

    it('navigateStockToProduct - 库存到产品', () => {
      deepLink.navigateStockToProduct('stock-456');

      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/ozon/product',
        query: {
          id: 'stock-456',
          focus: 'stock'
        }
      });
    });

    it('navigatePriceToProduct - 价格到产品', () => {
      deepLink.navigatePriceToProduct('price-789');

      expect(mockRouter.push).toHaveBeenCalledWith({
        path: '/ozon/product',
        query: {
          id: 'price-789',
          focus: 'price'
        }
      });
    });
  });

  describe('边界情况', () => {
    it('null entityId处理', () => {
      const result = deepLink.buildDeepLink('product', null);

      expect(result).toEqual({
        path: '/ozon/product',
        query: { id: null }
      });
    });

    it('空字符串entityId处理', () => {
      const result = deepLink.buildDeepLink('product', '');

      expect(result).toEqual({
        path: '/ozon/product',
        query: { id: '' }
      });
    });

    it('null focus参数被忽略', () => {
      const result = deepLink.buildDeepLink('product', 'product-123', null);

      expect(result).toEqual({
        path: '/ozon/product',
        query: { id: 'product-123' }
      });
    });
  });
});
