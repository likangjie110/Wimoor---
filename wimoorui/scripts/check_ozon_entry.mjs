import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const routerModulePath = path.resolve(__dirname, '../src/router/modules/ozon.js');
const headerPath = path.resolve(__dirname, '../src/layout/components/Header/HeaderPlatform.vue');

const routesModule = await import(pathToFileURL(routerModulePath).href);
const routes = routesModule.default;

if (!Array.isArray(routes) || routes.length === 0) {
  throw new Error('Ozon 路由模块为空');
}

const expectedPaths = [
  'ozon/auth',
  'ozon/product',
  'ozon/stock',
  'ozon/price',
  'ozon/chat',
  'ozon/ads',
  'ozon/finance',
  'ozon/posting',
  'ozon/shipment',
  'ozon/task',
  'ozon/error'
];

for (const routePath of expectedPaths) {
  if (!routes.some((route) => route.path === routePath)) {
    throw new Error(`缺少 Ozon 路由: ${routePath}`);
  }
}

const headerContent = fs.readFileSync(headerPath, 'utf8');

if (!headerContent.includes("plantid:'ozon'") && !headerContent.includes('plantid: \"ozon\"')) {
  throw new Error('HeaderPlatform 缺少 ozon 平台项');
}

if (!headerContent.includes('/ozon/auth')) {
  throw new Error('HeaderPlatform 缺少 Ozon 入口跳转路径');
}

console.log('Ozon entry check passed');
