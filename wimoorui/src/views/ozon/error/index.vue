<template>
  <div class="main-sty">
    <OzonFeatureNotice
      :item="features.error"
      title="Ozon 错误中心当前已关闭"
      description="关闭状态下不会加载错误记录，也不能执行重试或忽略。"
    />
    <OzonFeatureSummaryBar :items="summaryFeatureItems" />

    <el-card shadow="never" class="toolbar-card">
      <template #header>
        <div class="card-title">
          <div>
            <h3>Ozon 错误中心</h3>
            <p class="font-extraSmall">查看对象级失败记录，支持对单个 posting 或 tracking 推送失败进行重试与忽略。</p>
          </div>
          <el-button :disabled="!isEnabled('error')" @click="loadList">刷新列表</el-button>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col :span="6">
          <el-form-item label="授权店铺">
            <el-select v-model="query.authId" placeholder="请选择 Ozon 授权" style="width: 100%" @change="loadList">
              <el-option v-for="item in authOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="5">
          <el-form-item label="来源类型">
            <el-select v-model="query.sourceType" clearable placeholder="全部" style="width: 100%" @change="loadList">
              <el-option label="POSTING" value="POSTING" />
              <el-option label="SHIPMENT" value="SHIPMENT" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="5">
          <el-form-item label="状态">
            <el-select v-model="query.status" clearable placeholder="全部" style="width: 100%" @change="loadList">
              <el-option label="OPEN" value="OPEN" />
              <el-option label="RESOLVED" value="RESOLVED" />
              <el-option label="IGNORED" value="IGNORED" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="关键字">
            <el-input v-model="query.keyword" placeholder="对象编码、对象ID、错误消息" clearable @keyup.enter="loadList" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="sourceType" label="来源" width="110" />
        <el-table-column label="对象" min-width="220" show-overflow-tooltip>
          <template #default="scope">
            <div>{{ scope.row.objectCode || '-' }}</div>
            <div class="font-extraSmall text-muted">{{ scope.row.objectId }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="errorMessage" label="错误消息" min-width="280" show-overflow-tooltip />
        <el-table-column prop="retryCount" label="重试次数" width="100" />
        <el-table-column label="最后重试" min-width="170">
          <template #default="scope">
            {{ scope.row.lastRetryAt ? dateFormat(scope.row.lastRetryAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template #default="scope">
            {{ scope.row.updatedAt ? dateFormat(scope.row.updatedAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column label="来源跳转" width="120">
          <template #default="scope">
            <el-button
              v-if="resolveErrorTarget(scope.row)"
              link
              type="primary"
              @click="goToErrorSource(scope.row)"
            >
              前往源页
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-space>
              <el-button link type="primary" @click="openPayload(scope.row)">查看载荷</el-button>
              <el-button
                v-if="scope.row.status === 'OPEN'"
                link
                type="warning"
                :disabled="!isEnabled('error')"
                :loading="retryingId === scope.row.id"
                @click="retryOne(scope.row)"
              >
                重试
              </el-button>
              <el-button
                v-if="scope.row.status === 'OPEN'"
                link
                :disabled="!isEnabled('error')"
                :loading="ignoringId === scope.row.id"
                @click="ignoreOne(scope.row)"
              >
                忽略
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="payloadDialog.visible" title="错误载荷" size="720px">
      <el-card shadow="never" class="payload-card">
        <template #header>重试请求载荷</template>
        <el-scrollbar max-height="220px">
          <pre class="payload-pre">{{ payloadDialog.requestPayload || '-' }}</pre>
        </el-scrollbar>
      </el-card>
      <el-card shadow="never">
        <template #header>补充上下文载荷</template>
        <el-scrollbar max-height="220px">
          <pre class="payload-pre">{{ payloadDialog.responsePayload || '-' }}</pre>
        </el-scrollbar>
      </el-card>
      <el-card shadow="never" class="payload-card">
        <template #header>关联 API 日志</template>
        <el-table v-loading="payloadDialog.opsLoading" :data="payloadDialog.apiLogs" border size="small">
          <el-table-column prop="apiGroup" label="分组" width="120" />
          <el-table-column prop="actionName" label="动作" min-width="160" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="时间" min-width="160">
            <template #default="scope">
              {{ scope.row.createTime ? dateFormat(scope.row.createTime) : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="errorMessage" label="错误信息" min-width="220" show-overflow-tooltip />
        </el-table>
      </el-card>
      <el-card shadow="never">
        <template #header>关联操作审计</template>
        <el-table v-loading="payloadDialog.opsLoading" :data="payloadDialog.operationAudits" border size="small">
          <el-table-column prop="operationType" label="操作" min-width="180" show-overflow-tooltip />
          <el-table-column prop="resultStatus" label="结果" width="100" />
          <el-table-column label="时间" min-width="160">
            <template #default="scope">
              {{ scope.row.createTime ? dateFormat(scope.row.createTime) : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="resultMessage" label="说明" min-width="240" show-overflow-tooltip />
        </el-table>
      </el-card>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import authApi from '@/api/ozon/auth/authApi.js';
import errorApi from '@/api/ozon/error/errorApi.js';
import opsApi from '@/api/ozon/ops/opsApi.js';
import { dateFormat } from '@/utils/index.js';
import OzonFeatureNotice from '../components/OzonFeatureNotice.vue';
import OzonFeatureSummaryBar from '../components/OzonFeatureSummaryBar.vue';
import { useOzonFeatures } from '../composables/useOzonFeatures.js';

const loading = ref(false);
const retryingId = ref('');
const ignoringId = ref('');
const authOptions = ref([]);
const tableData = ref([]);
const route = useRoute();
const query = reactive({
  authId: '',
  sourceType: '',
  status: 'OPEN',
  keyword: ''
});
const payloadDialog = reactive({
  visible: false,
  requestPayload: '',
  responsePayload: '',
  opsLoading: false,
  apiLogs: [],
  operationAudits: []
});
const { features, featureItems, loadFeatures, isEnabled, reason } = useOzonFeatures();
const summaryFeatureItems = computed(() => featureItems.value.filter((item) => ['error', 'postingWrite', 'productWrite', 'stockWrite', 'priceWrite', 'chatSend', 'adsSync'].includes(item.key)));
const router = useRouter();

onMounted(() => {
  loadFeatures().finally(loadAuths);
});

watch(
  () => route.query,
  () => {
    if (authOptions.value.length > 0) {
      applyRouteState();
    }
  },
  { deep: true }
);

function loadAuths() {
  authApi.list().then(res => {
    authOptions.value = res.data || [];
    if (!query.authId && authOptions.value.length > 0) {
      query.authId = resolveInitialAuthId();
      applyRouteState();
      loadList();
    }
  });
}

function resolveInitialAuthId() {
  const routeAuthId = normalizeQuery(route.query.authId);
  if (routeAuthId && authOptions.value.some((item) => item.id === routeAuthId)) {
    return routeAuthId;
  }
  return authOptions.value[0]?.id || '';
}

function applyRouteState() {
  const routeAuthId = normalizeQuery(route.query.authId);
  const routeSourceType = normalizeQuery(route.query.sourceType);
  const routeStatus = normalizeQuery(route.query.status);
  const routeKeyword = normalizeQuery(route.query.keyword);
  if (routeAuthId && routeAuthId !== query.authId && authOptions.value.some((item) => item.id === routeAuthId)) {
    query.authId = routeAuthId;
  }
  query.sourceType = routeSourceType || '';
  query.status = routeStatus || 'OPEN';
  query.keyword = routeKeyword || '';
}

function loadList() {
  if (!query.authId || !isEnabled('error')) {
    tableData.value = [];
    return;
  }
  syncRoute({
    authId: query.authId,
    sourceType: query.sourceType,
    status: query.status,
    keyword: query.keyword
  });
  loading.value = true;
  errorApi.list({
    authId: query.authId,
    sourceType: query.sourceType || undefined,
    status: query.status || undefined,
    keyword: query.keyword || undefined
  }).then(res => {
    tableData.value = res.data || [];
  }).finally(() => {
    loading.value = false;
  });
}

function retryOne(row) {
  if (!isEnabled('error')) {
    ElMessage.warning(reason('error'));
    return;
  }
  retryingId.value = row.id;
  errorApi.retryOne(row.id).then(() => {
    ElMessage.success('已触发单对象重试');
    loadList();
  }).finally(() => {
    retryingId.value = '';
  });
}

function ignoreOne(row) {
  if (!isEnabled('error')) {
    ElMessage.warning(reason('error'));
    return;
  }
  ignoringId.value = row.id;
  errorApi.ignore(row.id).then(() => {
    ElMessage.success('已忽略该错误记录');
    loadList();
  }).finally(() => {
    ignoringId.value = '';
  });
}

function openPayload(row) {
  payloadDialog.visible = true;
  payloadDialog.requestPayload = row.requestPayloadJson || '';
  payloadDialog.responsePayload = row.responsePayloadJson || '';
  payloadDialog.apiLogs = [];
  payloadDialog.operationAudits = [];
  loadRelatedOps(row);
}

function loadRelatedOps(row) {
  if (!row?.authId || !row?.objectId || !row?.sourceType) {
    payloadDialog.apiLogs = [];
    payloadDialog.operationAudits = [];
    return;
  }
  payloadDialog.opsLoading = true;
  Promise.all([
    opsApi.listApiLogs({
      authId: row.authId,
      objectType: row.sourceType,
      objectId: row.objectId
    }),
    opsApi.listOperationAudits({
      authId: row.authId,
      objectType: row.sourceType,
      objectId: row.objectId
    })
  ]).then(([apiLogRes, auditRes]) => {
    payloadDialog.apiLogs = apiLogRes.data || [];
    payloadDialog.operationAudits = auditRes.data || [];
  }).finally(() => {
    payloadDialog.opsLoading = false;
  });
}

function resolveErrorTarget(row) {
  if (row.sourceType === 'PRODUCT' && row.objectId) {
    return { path: '/ozon/product', query: { authId: row.authId, draftId: row.objectId, focus: 'publish' } };
  }
  if (row.sourceType === 'POSTING' && row.objectId) {
    return { path: '/ozon/posting', query: { authId: row.authId, postingId: row.objectId } };
  }
  if (row.sourceType === 'SHIPMENT' && row.objectId) {
    return { path: '/ozon/shipment', query: { authId: row.authId, postingId: row.objectId, postingNumber: row.objectCode } };
  }
  if (row.sourceType === 'FINANCE') {
    return { path: '/ozon/finance', query: { authId: row.authId } };
  }
  if (row.sourceType === 'CHAT') {
    return { path: '/ozon/chat', query: { authId: row.authId } };
  }
  if (row.sourceType === 'ADS') {
    return { path: '/ozon/ads', query: { authId: row.authId } };
  }
  return null;
}

function goToErrorSource(row) {
  const target = resolveErrorTarget(row);
  if (target) {
    router.push(target);
  }
}

function syncRoute(nextQuery) {
  const normalized = {
    authId: nextQuery.authId || undefined,
    sourceType: nextQuery.sourceType || undefined,
    status: nextQuery.status || undefined,
    keyword: nextQuery.keyword || undefined
  };
  const current = {
    authId: normalizeQuery(route.query.authId) || undefined,
    sourceType: normalizeQuery(route.query.sourceType) || undefined,
    status: normalizeQuery(route.query.status) || undefined,
    keyword: normalizeQuery(route.query.keyword) || undefined
  };
  if (JSON.stringify(normalized) !== JSON.stringify(current)) {
    router.replace({ path: route.path, query: { ...route.query, ...normalized } });
  }
}

function normalizeQuery(value) {
  if (Array.isArray(value)) {
    return value[0] || '';
  }
  return value || '';
}
</script>

<style scoped>
.toolbar-card {
  margin-bottom: 16px;
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.text-muted {
  color: var(--el-text-color-secondary);
}

.payload-card {
  margin-bottom: 16px;
}

.payload-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.6;
}
</style>
