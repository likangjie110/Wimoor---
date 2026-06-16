<template>
  <div class="main-sty">
    <OzonFeatureNotice
      :item="features.postingWrite"
      title="Ozon Posting 写操作当前已关闭"
      description="订单列表仍可查看，但同步订单、重试桥接和推送追踪号按钮会保持禁用。"
    />

    <el-card shadow="never" class="toolbar-card">
      <template #header>
        <div class="card-title">
          <div>
            <h3>Ozon 订单同步</h3>
            <p class="font-extraSmall">拉取 Ozon FBS posting，按商品映射写入 ERP 订单事实，不自动触发出库。</p>
          </div>
          <el-space>
            <el-button @click="loadList">刷新列表</el-button>
            <el-button type="primary" :loading="syncing" :disabled="!isEnabled('postingWrite')" @click="syncPostings">同步订单</el-button>
          </el-space>
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
        <el-col :span="4">
          <el-form-item label="回溯天数">
            <el-input-number v-model="query.sinceDays" :min="1" :max="30" :disabled="query.useCursor" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="增量游标">
            <el-switch v-model="query.useCursor" />
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="履约类型">
            <el-select v-model="query.fulfillmentType" clearable placeholder="全部" style="width: 100%">
              <el-option label="FBS" value="FBS" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="5">
          <el-form-item label="订单状态">
            <el-input v-model="query.status" placeholder="例如 awaiting_packaging" clearable @keyup.enter="loadList" />
          </el-form-item>
        </el-col>
        <el-col :span="5">
          <el-form-item label="关键字">
            <el-input v-model="query.keyword" placeholder="Posting Number" clearable @keyup.enter="loadList" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="postingNumber" label="Posting Number" min-width="180" show-overflow-tooltip />
        <el-table-column prop="fulfillmentType" label="履约" width="90" />
        <el-table-column prop="postingStatus" label="订单状态" min-width="160" show-overflow-tooltip />
        <el-table-column prop="bridgeStatus" label="桥接状态" width="110" />
        <el-table-column prop="latestTrackingNumber" label="追踪号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="latestDeliveryService" label="物流商" min-width="120" show-overflow-tooltip />
        <el-table-column prop="latestShipmentStatus" label="履约状态" width="120" />
        <el-table-column prop="erpOrderId" label="ERP订单ID" min-width="220" show-overflow-tooltip />
        <el-table-column prop="itemSummary" label="商品摘要" min-width="260" show-overflow-tooltip />
        <el-table-column label="下单时间" min-width="170">
          <template #default="scope">
            {{ scope.row.orderCreatedAt ? dateFormat(scope.row.orderCreatedAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="发货时限" min-width="170">
          <template #default="scope">
            {{ scope.row.shipmentDeadlineAt ? dateFormat(scope.row.shipmentDeadlineAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="syncVersion" label="版本" width="80" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="scope">
            <el-space>
              <el-button
                v-if="scope.row.bridgeStatus !== 'SYNCED'"
                link
                type="warning"
                :disabled="!isEnabled('postingWrite')"
                :loading="retryingId === scope.row.id"
                @click="retryPosting(scope.row)"
              >
                重试桥接
              </el-button>
              <el-button
                v-if="scope.row.fulfillmentType === 'FBS'"
                link
                type="success"
                :disabled="!isEnabled('postingWrite')"
                @click="openTrackingDialog(scope.row)"
              >
                推送追踪号
              </el-button>
              <el-button
                v-if="scope.row.fulfillmentType === 'FBS'"
                link
                @click="openShipmentHistory(scope.row)"
              >
                履约记录
              </el-button>
              <el-button link @click="openDetail(scope.row)">详情</el-button>
              <el-button link type="primary" @click="openPayload(scope.row)">查看原文</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="payloadDialog.visible" title="Ozon Posting 原始载荷" width="760px">
      <el-scrollbar max-height="420px">
        <pre class="payload-pre">{{ payloadDialog.content }}</pre>
      </el-scrollbar>
      <template #footer>
        <el-button @click="payloadDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="trackingDialog.visible" title="推送追踪号" width="460px">
      <el-form label-width="100px">
        <el-form-item label="Posting">
          <span>{{ trackingDialog.postingNumber || '-' }}</span>
        </el-form-item>
        <el-form-item label="追踪号">
          <el-input v-model="trackingDialog.trackingNumber" placeholder="请输入物流追踪号" />
        </el-form-item>
        <el-form-item label="物流商">
          <el-input v-model="trackingDialog.deliveryService" placeholder="例如 CDEK、Boxberry" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="trackingDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="trackingDialog.submitting" :disabled="!isEnabled('postingWrite')" @click="submitTracking">确认推送</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyDialog.visible" title="履约记录" width="760px">
      <el-table v-loading="historyDialog.loading" :data="historyDialog.items" border>
        <el-table-column prop="trackingNumber" label="追踪号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="deliveryService" label="物流商" min-width="140" show-overflow-tooltip />
        <el-table-column prop="shipmentStatus" label="状态" width="140" />
        <el-table-column label="推送时间" min-width="180">
          <template #default="scope">
            {{ scope.row.createTime ? dateFormat(scope.row.createTime) : '-' }}
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="historyDialog.visible = false">关闭</el-button>
        <el-button v-if="historyDialog.postingId" type="primary" @click="goToShipmentWorkbench(historyDialog.postingId, historyDialog.postingNumber)">进入履约工作台</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailDialog.visible" title="Posting 详情" size="760px">
      <el-skeleton v-if="detailDialog.loading" animated :rows="10" />
      <template v-else>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="Posting Number">{{ detailDialog.detail?.postingNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="履约类型">{{ detailDialog.detail?.fulfillmentType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{ detailDialog.detail?.postingStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="子状态">{{ detailDialog.detail?.substatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="仓库ID">{{ detailDialog.detail?.warehouseId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="桥接状态">{{ detailDialog.detail?.bridgeStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="ERP订单ID">{{ detailDialog.detail?.erpOrderId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="同步版本">{{ detailDialog.detail?.syncVersion || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">商品行</el-divider>
        <el-table :data="detailDialog.detail?.items || []" border size="small">
          <el-table-column prop="materialSku" label="ERP SKU" min-width="160" />
          <el-table-column prop="ozonOfferId" label="Offer ID" min-width="160" />
          <el-table-column prop="quantity" label="数量" width="100" />
        </el-table>

        <el-divider content-position="left">履约记录</el-divider>
        <el-table :data="detailDialog.detail?.shipments || []" border size="small">
          <el-table-column prop="trackingNumber" label="追踪号" min-width="180" />
          <el-table-column prop="deliveryService" label="物流商" min-width="140" />
          <el-table-column prop="shipmentStatus" label="状态" width="120" />
          <el-table-column label="创建时间" min-width="180">
            <template #default="scope">
              {{ scope.row.createdAt ? dateFormat(scope.row.createdAt) : '-' }}
            </template>
          </el-table-column>
        </el-table>

        <AfterSalePanel
          :detail="detailDialog.afterSale"
          :disabled="!isEnabled('postingWrite')"
          @refresh="loadAfterSaleDetail"
          @save-package="savePackageRecord"
          @save-return="saveReturnRecord"
          @save-cancellation="saveCancellationRecord"
        />

        <el-divider content-position="left">原始载荷</el-divider>
        <el-scrollbar max-height="260px">
          <pre class="payload-pre">{{ formatPayload(detailDialog.detail?.rawPayloadJson) }}</pre>
        </el-scrollbar>
      </template>
      <template #footer>
        <el-button @click="detailDialog.visible = false">关闭</el-button>
        <el-button
          v-if="detailDialog.detail?.id"
          type="primary"
          @click="goToShipmentWorkbench(detailDialog.detail.id, detailDialog.detail.postingNumber)"
        >
          进入履约工作台
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import authApi from '@/api/ozon/auth/authApi.js';
import postingApi from '@/api/ozon/posting/postingApi.js';
import { dateFormat } from '@/utils/index.js';
import OzonFeatureNotice from '../components/OzonFeatureNotice.vue';
import { useOzonFeatures } from '../composables/useOzonFeatures.js';
import AfterSalePanel from './components/AfterSalePanel.vue';

const loading = ref(false);
const syncing = ref(false);
const retryingId = ref('');
const authOptions = ref([]);
const tableData = ref([]);
const route = useRoute();
const router = useRouter();
const query = reactive({
  authId: '',
  sinceDays: 7,
  useCursor: true,
  status: '',
  fulfillmentType: 'FBS',
  keyword: ''
});
const payloadDialog = reactive({
  visible: false,
  content: ''
});
const trackingDialog = reactive({
  visible: false,
  postingId: '',
  postingNumber: '',
  trackingNumber: '',
  deliveryService: '',
  submitting: false
});
const historyDialog = reactive({
  visible: false,
  loading: false,
  items: [],
  postingId: '',
  postingNumber: ''
});
const detailDialog = reactive({
  visible: false,
  loading: false,
  detail: null,
  afterSale: null
});
const { features, loadFeatures, isEnabled, reason } = useOzonFeatures();

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
  const postingId = normalizeQuery(route.query.postingId);
  const sinceDays = normalizeNumberQuery(route.query.sinceDays, 7);
  const useCursor = normalizeBooleanQuery(route.query.useCursor, true);
  const status = normalizeQuery(route.query.status);
  const fulfillmentType = normalizeQuery(route.query.fulfillmentType);
  const keyword = normalizeQuery(route.query.keyword);
  if (routeAuthId && routeAuthId !== query.authId && authOptions.value.some((item) => item.id === routeAuthId)) {
    query.authId = routeAuthId;
    loadList();
  }
  query.sinceDays = sinceDays;
  query.useCursor = useCursor;
  query.status = status || '';
  query.fulfillmentType = fulfillmentType || 'FBS';
  query.keyword = keyword || '';
  if (postingId) {
    openDetail({ id: postingId }, false);
  }
}

function loadList() {
  if (!query.authId) {
    return;
  }
  syncRoute({
    authId: query.authId,
    postingId: normalizeQuery(route.query.postingId),
    sinceDays: query.sinceDays,
    useCursor: query.useCursor,
    status: query.status,
    fulfillmentType: query.fulfillmentType,
    keyword: query.keyword
  });
  loading.value = true;
  postingApi.list({
    authId: query.authId,
    status: query.status || undefined,
    fulfillmentType: query.fulfillmentType || undefined,
    keyword: query.keyword || undefined
  }).then(res => {
    tableData.value = res.data || [];
  }).finally(() => {
    loading.value = false;
  });
}

function syncPostings() {
  if (!isEnabled('postingWrite')) {
    ElMessage.warning(reason('postingWrite'));
    return;
  }
  if (!query.authId) {
    ElMessage.error('请先选择 Ozon 授权');
    return;
  }
  syncing.value = true;
  postingApi.sync({
    authId: query.authId,
    sinceDays: query.useCursor ? null : query.sinceDays,
    useCursor: query.useCursor
  }).then(res => {
    const imported = res.data?.imported || 0;
    const bridged = res.data?.erpOrderIds?.length || 0;
    const modeText = res.data?.cursorUsed ? '增量游标' : `回溯${query.sinceDays}天`;
    ElMessage.success(`已按${modeText}同步 ${imported} 条 posting，桥接 ${bridged} 条 ERP 订单事实`);
    loadList();
  }).finally(() => {
    syncing.value = false;
  });
}

function retryPosting(row) {
  if (!isEnabled('postingWrite')) {
    ElMessage.warning(reason('postingWrite'));
    return;
  }
  if (!query.authId || !row?.id) {
    ElMessage.error('缺少重试上下文');
    return;
  }
  retryingId.value = row.id;
  postingApi.retryOne(query.authId, row.id).then(res => {
    const bridged = res.data?.erpOrderIds?.length || 0;
    if (bridged > 0) {
      ElMessage.success(`已重试并桥接 ${bridged} 条 ERP 订单事实`);
    } else {
      ElMessage.warning('已重试桥接，但仍未生成 ERP 订单');
    }
    loadList();
  }).finally(() => {
    retryingId.value = '';
  });
}

function openTrackingDialog(row) {
  if (!isEnabled('postingWrite')) {
    ElMessage.warning(reason('postingWrite'));
    return;
  }
  trackingDialog.visible = true;
  trackingDialog.postingId = row.id;
  trackingDialog.postingNumber = row.postingNumber;
  trackingDialog.trackingNumber = '';
  trackingDialog.deliveryService = '';
}

function openShipmentHistory(row) {
  if (!query.authId || !row?.id) {
    ElMessage.error('缺少履约上下文');
    return;
  }
  historyDialog.visible = true;
  historyDialog.loading = true;
  historyDialog.items = [];
  historyDialog.postingId = row.id;
  historyDialog.postingNumber = row.postingNumber;
  postingApi.listShipmentHistory(query.authId, row.id).then(res => {
    historyDialog.items = res.data || [];
  }).finally(() => {
    historyDialog.loading = false;
  });
}

function openDetail(row, updateRoute = true) {
  if (!query.authId || !row?.id) {
    return;
  }
  if (updateRoute) {
    syncRoute({ authId: query.authId, postingId: row.id });
  }
  detailDialog.visible = true;
  detailDialog.loading = true;
  detailDialog.detail = null;
  detailDialog.afterSale = null;
  Promise.all([
    postingApi.detail(query.authId, row.id),
    postingApi.afterSaleDetail(query.authId, row.id)
  ]).then(([detailRes, afterSaleRes]) => {
    detailDialog.detail = detailRes.data || null;
    detailDialog.afterSale = afterSaleRes.data || null;
  }).finally(() => {
    detailDialog.loading = false;
  });
}

function submitTracking() {
  if (!isEnabled('postingWrite')) {
    ElMessage.warning(reason('postingWrite'));
    return;
  }
  if (!query.authId || !trackingDialog.postingId) {
    ElMessage.error('缺少推送上下文');
    return;
  }
  if (!trackingDialog.trackingNumber) {
    ElMessage.error('请先输入追踪号');
    return;
  }
  trackingDialog.submitting = true;
  postingApi.pushTracking({
    authId: query.authId,
    postingId: trackingDialog.postingId,
    trackingNumber: trackingDialog.trackingNumber,
    deliveryService: trackingDialog.deliveryService || undefined
  }).then(() => {
    ElMessage.success('追踪号已推送');
    trackingDialog.visible = false;
  }).finally(() => {
    trackingDialog.submitting = false;
  });
}

function goToShipmentWorkbench(postingId, postingNumber) {
  if (!query.authId || !postingId) {
    return;
  }
  router.push({
    path: '/ozon/shipment',
    query: {
      authId: query.authId,
      postingId,
      postingNumber
    }
  });
}

function openPayload(row) {
  payloadDialog.visible = true;
  payloadDialog.content = formatPayload(row.rawPayloadJson);
}

function formatPayload(value) {
  if (!value) {
    return '-';
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch (error) {
    return value;
  }
}

function syncRoute({ authId, postingId, sinceDays, useCursor, status, fulfillmentType, keyword }) {
  router.replace({
    path: route.path,
    query: {
      ...route.query,
      authId: authId || undefined,
      postingId: postingId || undefined,
      sinceDays: sinceDays || undefined,
      useCursor: useCursor ? 'true' : 'false',
      status: status || undefined,
      fulfillmentType: fulfillmentType || undefined,
      keyword: keyword || undefined
    }
  });
}

function normalizeQuery(value) {
  if (Array.isArray(value)) {
    return value[0] || '';
  }
  return value || '';
}

function normalizeNumberQuery(value, fallback) {
  const normalized = Number(normalizeQuery(value));
  return Number.isFinite(normalized) && normalized > 0 ? normalized : fallback;
}

function normalizeBooleanQuery(value, fallback) {
  const normalized = normalizeQuery(value);
  if (normalized === 'true') {
    return true;
  }
  if (normalized === 'false') {
    return false;
  }
  return fallback;
}

function loadAfterSaleDetail() {
  if (!query.authId || !detailDialog.detail?.id) {
    return;
  }
  postingApi.afterSaleDetail(query.authId, detailDialog.detail.id).then((res) => {
    detailDialog.afterSale = res.data || null;
  });
}

function savePackageRecord(payload) {
  if (!isEnabled('postingWrite')) {
    ElMessage.warning(reason('postingWrite'));
    return;
  }
  postingApi.savePackage({
    authId: query.authId,
    postingId: detailDialog.detail?.id,
    ...payload
  }).then(() => {
    ElMessage.success('包裹记录已保存');
    loadAfterSaleDetail();
  });
}

function saveReturnRecord(payload) {
  if (!isEnabled('postingWrite')) {
    ElMessage.warning(reason('postingWrite'));
    return;
  }
  postingApi.saveReturn({
    authId: query.authId,
    postingId: detailDialog.detail?.id,
    ...payload
  }).then(() => {
    ElMessage.success('退货记录已保存');
    loadAfterSaleDetail();
  });
}

function saveCancellationRecord(payload) {
  if (!isEnabled('postingWrite')) {
    ElMessage.warning(reason('postingWrite'));
    return;
  }
  postingApi.saveCancellation({
    authId: query.authId,
    postingId: detailDialog.detail?.id,
    ...payload
  }).then(() => {
    ElMessage.success('取消记录已保存');
    loadAfterSaleDetail();
  });
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

.payload-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  line-height: 1.5;
}
</style>
