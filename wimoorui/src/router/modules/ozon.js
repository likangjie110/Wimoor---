export default [
  {
    path: 'ozon/auth',
    name: 'ozon_auth',
    meta: { keepAlive: true },
    component: () => import('@/views/ozon/auth/index.vue')
  },
  {
    path: 'ozon/product',
    name: 'ozon_product',
    meta: { keepAlive: true },
    component: () => import('@/views/ozon/product/index.vue')
  },
  {
    path: 'ozon/stock',
    name: 'ozon_stock',
    meta: { keepAlive: true },
    component: () => import('@/views/ozon/stock/index.vue')
  },
  {
    path: 'ozon/price',
    name: 'ozon_price',
    meta: { keepAlive: true },
    component: () => import('@/views/ozon/price/index.vue')
  },
  {
    path: 'ozon/chat',
    name: 'ozon_chat',
    meta: { keepAlive: true },
    component: () => import('@/views/ozon/chat/index.vue')
  },
  {
    path: 'ozon/ads',
    name: 'ozon_ads',
    meta: { keepAlive: true },
    component: () => import('@/views/ozon/ads/index.vue')
  },
  {
    path: 'ozon/finance',
    name: 'ozon_finance',
    meta: { keepAlive: true },
    component: () => import('@/views/ozon/finance/index.vue')
  },
  {
    path: 'ozon/posting',
    name: 'ozon_posting',
    meta: { keepAlive: true },
    component: () => import('@/views/ozon/posting/index.vue')
  },
  {
    path: 'ozon/shipment',
    name: 'ozon_shipment',
    meta: { keepAlive: true },
    component: () => import('@/views/ozon/shipment/index.vue')
  },
  {
    path: 'ozon/task',
    name: 'ozon_task',
    meta: { keepAlive: true },
    component: () => import('@/views/ozon/task/index.vue')
  },
  {
    path: 'ozon/error',
    name: 'ozon_error',
    meta: { keepAlive: true },
    component: () => import('@/views/ozon/error/index.vue')
  },
  {
    path: 'ozon/monitoring',
    name: 'ozon_monitoring',
    meta: { keepAlive: true },
    component: () => import('@/views/ozon/monitoring/Dashboard.vue')
  }
]
