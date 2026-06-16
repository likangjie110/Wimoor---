import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const checks = [
  {
    file: path.resolve(__dirname, '../src/views/ozon/product/index.vue'),
    patterns: ['route.query.draftId', 'route.query.focus', 'syncRoute({ authId: query.authId, draftId: selectedDraftId.value, focus: \'publish\' })']
  },
  {
    file: path.resolve(__dirname, '../src/views/ozon/posting/index.vue'),
    patterns: ['route.query.postingId', 'route.query.sinceDays', 'route.query.useCursor', 'router.push({', 'path: \'/ozon/shipment\'']
  },
  {
    file: path.resolve(__dirname, '../src/views/ozon/task/index.vue'),
    patterns: ['route.query.jobType', 'route.query.status', 'opsApi.summary', 'router.replace({ path: route.path']
  },
  {
    file: path.resolve(__dirname, '../src/views/ozon/error/index.vue'),
    patterns: ['route.query.sourceType', 'route.query.status', 'opsApi.listApiLogs', 'opsApi.listOperationAudits']
  }
];

for (const { file, patterns } of checks) {
  const content = fs.readFileSync(file, 'utf8');
  for (const pattern of patterns) {
    if (!content.includes(pattern)) {
      throw new Error(`${path.basename(file)} 缺少回归关键片段: ${pattern}`);
    }
  }
}

console.log('Ozon route smoke check passed');
