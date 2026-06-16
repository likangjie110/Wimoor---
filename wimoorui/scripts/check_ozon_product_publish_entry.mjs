import fs from 'node:fs';
import path from 'node:path';

const scriptDir = path.dirname(new URL(import.meta.url).pathname);
const apiPath = path.resolve(scriptDir, '../src/api/ozon/product/productApi.js');
const pagePath = path.resolve(scriptDir, '../src/views/ozon/product/index.vue');

const apiContent = fs.readFileSync(apiPath, 'utf8');
const pageContent = fs.readFileSync(pagePath, 'utf8');

const expectedApiExports = [
  'listDrafts',
  'saveDraft',
  'detail',
  'preview',
  'publish',
  'publishTaskDetail'
];

for (const item of expectedApiExports) {
  if (!apiContent.includes(`function ${item}`)) {
    throw new Error(`productApi 缺少函数: ${item}`);
  }
}

const expectedComponents = [
  'DraftSidebar',
  'DraftBaseForm',
  'CommonAttributePanel',
  'CommonImagePanel',
  'VariantMatrix',
  'PreviewPanel',
  'PublishTaskPanel'
];

for (const item of expectedComponents) {
  if (!pageContent.includes(item)) {
    throw new Error(`商品工作台缺少组件引用: ${item}`);
  }
}

console.log('Ozon product publish entry check passed');
