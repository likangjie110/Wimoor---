import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, '..');

const checks = [
  {
    source: path.join(root, 'src/main.js'),
    importText: '@/components/FileUpload/Upload.vue'
  },
  {
    source: path.join(root, 'src/components/FileUpload/UploadDialog.vue'),
    importText: '@/components/FileUpload/Upload.vue'
  },
  {
    source: path.join(root, 'src/views/erp/ship/shipment_handing/shipstep/components/three_deliver.vue'),
    importText: '@/api/erp/shipv2/shipmentPlacementApi.js'
  },
  {
    source: path.join(root, 'src/views/erp/shipv2/shipment_handing/shipstep/components/three_deliver.vue'),
    importText: '@/api/erp/shipv2/shipmentPlacementApi.js'
  }
];

for (const check of checks) {
  const content = fs.readFileSync(check.source, 'utf8');
  if (!content.includes(check.importText)) {
    throw new Error(`${path.relative(root, check.source)} 未使用期望的 Linux 大小写路径: ${check.importText}`);
  }
}

console.log('Linux path case check passed');
