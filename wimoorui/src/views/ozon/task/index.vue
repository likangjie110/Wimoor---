<template>
  <div class="main-sty">
    <OzonFeatureNotice
      :item="features.task"
      title="Ozon 任务中心当前已关闭"
      description="关闭状态下不会加载同步任务，也无法查看任务状态摘要。"
    />
    <OzonFeatureSummaryBar :items="summaryFeatureItems" />

    <el-card shadow="never" class="toolbar-card">
      <template #header>
        <div class="card-title">
          <div>
            <h3>Ozon 任务中心</h3>
            <p class="font-extraSmall">查看 Ozon 同步任务的最近状态，支持按授权、任务类型和状态快速筛选。</p>
          </div>
          <el-button :disabled="!isEnabled('task')" data-testid="btn-refresh" @click="loadTasks">刷新任务</el-button>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="授权店铺">
            <el-select v-model="query.authId" placeholder="请选择 Ozon 授权" style="width: 100%" data-testid="filter-auth" @change="loadTasks">
              <el-option v-for="item in authOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="任务类型">
            <el-select v-model="query.jobType" clearable placeholder="全部" style="width: 100%" data-testid="filter-job-type" @change="loadTasks">
              <el-option v-for="item in jobTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="任务状态">
            <el-select v-model="query.status" clearable placeholder="全部" style="width: 100%" data-testid="filter-status" @change="loadTasks">
              <el-option label="PENDING" value="PENDING" />
              <el-option label="RUNNING" value="RUNNING" />
              <el-option label="DONE" value="DONE" />
              <el-option label="FAILED" value="FAILED" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16" class="summary-row" data-testid="operation-summary">
      <el-col :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">最近执行</div>
          <div class="summary-value" data-testid="last-run-time">{{ summary.lastRunTime ? dateFormat(summary.lastRunTime) : '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">待处理</div>
          <div class="summary-value" data-testid="total-tasks">{{ summary.backlog }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">平均耗时</div>
          <div class="summary-value" data-testid="avg-latency">{{ summary.averageLatencyText }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">失败数</div>
          <div class="summary-value" data-testid="failure-count">{{ summary.failureCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="summary-row" data-testid="ops-summary">
      <el-col :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">API 调用总数</div>
          <div class="summary-value" data-testid="api-total">{{ opsSummary.apiLogTotal }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">API 失败数</div>
          <div class="summary-value" data-testid="api-failed">{{ opsSummary.apiLogFailed }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">人工操作总数</div>
          <div class="summary-value" data-testid="ops-total">{{ opsSummary.operationAuditTotal }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">人工失败数</div>
          <div class="summary-value" data-testid="ops-failed">{{ opsSummary.operationAuditFailed }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border data-testid="task-list">
        <el-table-column prop="jobType" label="任务类型" min-width="160" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="业务摘要" min-width="220" show-overflow-tooltip>
          <template #default="scope">
            {{ renderTaskSummary(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column label="创建时间" min-width="180">
          <template #default="scope">
            {{ scope.row.createdAt ? dateFormat(scope.row.createdAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="180">
          <template #default="scope">
            {{ scope.row.updatedAt ? dateFormat(scope.row.updatedAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="120">
          <template #default="scope">
            {{ formatLatency(scope.row.createdAt, scope.row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="payload" label="任务载荷" min-width="240" show-overflow-tooltip />
        <el-table-column label="来源" width="120" fixed="right">
          <template #default="scope">
            <el-button
              v-if="resolveTaskTarget(scope.row)"
              link
              type="primary"
              @click="goToTaskSource(scope.row)"
            >
              前往源页
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import authApi from '@/api/ozon/auth/authApi.js';
import opsApi from '@/api/ozon/ops/opsApi.js';
import taskApi from '@/api/ozon/task/taskApi.js';
import { dateFormat } from '@/utils/index.js';
import OzonFeatureNotice from '../components/OzonFeatureNotice.vue';
import OzonFeatureSummaryBar from '../components/OzonFeatureSummaryBar.vue';
import { useOzonFeatures } from '../composables/useOzonFeatures.js';

const loading = ref(false);
const authOptions = ref([]);
const tableData = ref([]);
const opsLoading = ref(false);
const route = useRoute();
const query = reactive({
  authId: '',
  jobType: '',
  status: ''
});
const opsSummary = reactive({
  apiLogTotal: 0,
  apiLogFailed: 0,
  operationAuditTotal: 0,
  operationAuditFailed: 0
});
const { features, featureItems, loadFeatures, isEnabled } = useOzonFeatures();
const router = useRouter();

const jobTypeOptions = [
  { label: '初始化店铺', value: 'INIT_SELLER' },
  { label: '初始化仓库', value: 'INIT_WAREHOUSE' },
  { label: '订单同步', value: 'POSTING_SYNC' },
  { label: '库存同步', value: 'STOCK_SYNC' },
  { label: '价格同步', value: 'PRICE_SYNC' },
  { label: '财务导入', value: 'FINANCE_IMPORT' },
  { label: '追踪号推送', value: 'TRACKING_PUSH' }
];

const summaryFeatureItems = computed(() => featureItems.value.filter((item) => ['task', 'productWrite', 'stockWrite', 'priceWrite', 'postingWrite', 'chatSend', 'adsSync'].includes(item.key)));

const summary = computed(() => {
  const items = tableData.value || [];
  const backlog = items.filter(item => ['PENDING', 'RUNNING'].includes(item.status)).length;
  const failureCount = items.filter(item => item.status === 'FAILED').length;
  const lastRunTime = items.reduce((latest, item) => {
    if (!item.updatedAt) return latest;
    return !latest || new Date(item.updatedAt).getTime() > new Date(latest).getTime() ? item.updatedAt : latest;
  }, null);
  const latencyValues = items
    .map(item => diffMs(item.createdAt, item.updatedAt))
    .filter(value => value > 0);
  const averageLatency = latencyValues.length
    ? Math.round(latencyValues.reduce((sum, value) => sum + value, 0) / latencyValues.length)
    : 0;
  return {
    lastRunTime,
    backlog,
    failureCount,
    averageLatencyText: averageLatency > 0 ? `${Math.round(averageLatency / 1000)}s` : '-'
  };
});

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
      loadTasks();
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
  const routeJobType = normalizeQuery(route.query.jobType);
  const routeStatus = normalizeQuery(route.query.status);
  if (routeAuthId && routeAuthId !== query.authId && authOptions.value.some((item) => item.id === routeAuthId)) {
    query.authId = routeAuthId;
  }
  query.jobType = routeJobType || '';
  query.status = routeStatus || '';
}

function loadTasks() {
  if (!query.authId || !isEnabled('task')) {
    tableData.value = [];
    resetOpsSummary();
    return;
  }
  syncRoute({
    authId: query.authId,
    jobType: query.jobType,
    status: query.status
  });
  loadOpsSummary();
  loading.value = true;
  taskApi.list({
    authId: query.authId,
    jobType: query.jobType || undefined,
    status: query.status || undefined
  }).then(res => {
    tableData.value = res.data || [];
  }).finally(() => {
    loading.value = false;
  });
}

function loadOpsSummary() {
  if (!query.authId || !isEnabled('task')) {
    resetOpsSummary();
    return;
  }
  opsLoading.value = true;
  opsApi.summary(query.authId).then(res => {
    const data = res.data || {};
    opsSummary.apiLogTotal = data.apiLogTotal || 0;
    opsSummary.apiLogFailed = data.apiLogFailed || 0;
    opsSummary.operationAuditTotal = data.operationAuditTotal || 0;
    opsSummary.operationAuditFailed = data.operationAuditFailed || 0;
  }).finally(() => {
    opsLoading.value = false;
  });
}

function resetOpsSummary() {
  opsSummary.apiLogTotal = 0;
  opsSummary.apiLogFailed = 0;
  opsSummary.operationAuditTotal = 0;
  opsSummary.operationAuditFailed = 0;
}

function diffMs(createdAt, updatedAt) {
  if (!createdAt || !updatedAt) {
    return 0;
  }
  return Math.max(new Date(updatedAt).getTime() - new Date(createdAt).getTime(), 0);
}

function formatLatency(createdAt, updatedAt) {
  const value = diffMs(createdAt, updatedAt);
  return value > 0 ? `${Math.round(value / 1000)}s` : '-';
}

function parsePayload(payload) {
  if (!payload) {
    return {};
  }
  try {
    return JSON.parse(payload);
  } catch (error) {
    return {};
  }
}

function renderTaskSummary(row) {
  const payload = parsePayload(row.payload);
  if (row.jobType === 'PRODUCT_PUBLISH') {
    return payload.draftId ? `草稿 ${payload.draftId} 发布任务` : '商品发布任务';
  }
  if (row.jobType === 'POSTING_SYNC') {
    return `回溯 ${payload.sinceDays || '-'} 天，同步 ${payload.imported || 0} 条 posting`;
  }
  if (row.jobType === 'TRACKING_PUSH') {
    return payload.postingId ? `履约单 ${payload.postingId} 推送追踪号` : '追踪号推送任务';
  }
  if (row.jobType === 'PRICE_SYNC') {
    return payload.count ? `价格推送 ${payload.count} 条` : '价格推送任务';
  }
  if (row.jobType === 'STOCK_SYNC') {
    return payload.count ? `库存推送 ${payload.count} 条` : '库存推送任务';
  }
  if (row.jobType === 'FINANCE_IMPORT') {
    return payload.reportId ? `财务报表 ${payload.reportId}` : '财务导入任务';
  }
  return row.payload || '-';
}

function resolveTaskTarget(row) {
  const payload = parsePayload(row.payload);
  if (row.jobType === 'PRODUCT_PUBLISH' && payload.draftId) {
    return { path: '/ozon/product', query: { authId: row.authId, draftId: payload.draftId, focus: 'publish' } };
  }
  if (row.jobType === 'POSTING_SYNC') {
    return {
      path: '/ozon/posting',
      query: {
        authId: row.authId,
        sinceDays: payload.sinceDays,
        useCursor: payload.useCursor ? 'true' : undefined
      }
    };
  }
  if (row.jobType === 'TRACKING_PUSH' && payload.postingId) {
    return { path: '/ozon/shipment', query: { authId: row.authId, postingId: payload.postingId } };
  }
  if (row.jobType === 'PRICE_SYNC') {
    return { path: '/ozon/price', query: { authId: row.authId } };
  }
  if (row.jobType === 'STOCK_SYNC') {
    return { path: '/ozon/stock', query: { authId: row.authId } };
  }
  if (row.jobType === 'FINANCE_IMPORT') {
    return { path: '/ozon/finance', query: { authId: row.authId, reportId: payload.reportId } };
  }
  return null;
}

function goToTaskSource(row) {
  const target = resolveTaskTarget(row);
  if (target) {
    router.push(target);
  }
}

function syncRoute(nextQuery) {
  const normalized = {
    authId: nextQuery.authId || undefined,
    jobType: nextQuery.jobType || undefined,
    status: nextQuery.status || undefined
  };
  const current = {
    authId: normalizeQuery(route.query.authId) || undefined,
    jobType: normalizeQuery(route.query.jobType) || undefined,
    status: normalizeQuery(route.query.status) || undefined
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

.summary-row {
  margin-bottom: 16px;
}

.summary-card {
  min-height: 96px;
}

.summary-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
}
</style>
