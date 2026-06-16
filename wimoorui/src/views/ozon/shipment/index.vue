<template>
  <div class="main-sty">
    <OzonFeatureNotice
      :item="features.postingWrite"
      title="Ozon 履约写操作当前已关闭"
      description="你仍然可以查看已有履约记录，但推送追踪号按钮会保持禁用。"
    />

    <el-card shadow="never" class="toolbar-card">
      <template #header>
        <div class="card-title">
          <div>
            <h3>Ozon 履约追踪</h3>
            <p class="font-extraSmall">按 posting 查询履约记录，并在写开关开启后手动推送追踪号。支持从 Posting 页面带参直达。</p>
          </div>
          <el-space>
            <el-button @click="loadHistory">刷新记录</el-button>
            <el-button type="primary" :loading="submitting" :disabled="!isEnabled('postingWrite')" @click="submitTracking">推送追踪号</el-button>
          </el-space>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="授权店铺">
            <el-select v-model="form.authId" placeholder="请选择 Ozon 授权" style="width: 100%">
              <el-option v-for="item in authOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="Posting ID">
            <el-input v-model="form.postingId" placeholder="请输入 postingId" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="Posting Number">
            <el-input :model-value="form.postingNumber || '-'" readonly />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="追踪号">
            <el-input v-model="form.trackingNumber" placeholder="请输入追踪号" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="物流商">
            <el-input v-model="form.deliveryService" placeholder="例如 CDEK / Boxberry" clearable />
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="history" border>
        <el-table-column prop="trackingNumber" label="追踪号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="deliveryService" label="物流商" min-width="140" show-overflow-tooltip />
        <el-table-column prop="shipmentStatus" label="状态" width="140" />
        <el-table-column label="推送时间" min-width="180">
          <template #default="scope">
            {{ scope.row.createTime ? dateFormat(scope.row.createTime) : '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import authApi from '@/api/ozon/auth/authApi.js';
import shipmentApi from '@/api/ozon/shipment/shipmentApi.js';
import { dateFormat } from '@/utils/index.js';
import OzonFeatureNotice from '../components/OzonFeatureNotice.vue';
import { useOzonFeatures } from '../composables/useOzonFeatures.js';

const loading = ref(false);
const submitting = ref(false);
const authOptions = ref([]);
const history = ref([]);
const route = useRoute();
const router = useRouter();
const form = reactive({
  authId: '',
  postingId: '',
  postingNumber: '',
  trackingNumber: '',
  deliveryService: ''
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
  authApi.list().then((res) => {
    authOptions.value = res.data || [];
    if (!form.authId && authOptions.value.length > 0) {
      form.authId = resolveInitialAuthId();
      applyRouteState();
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
  const routePostingId = normalizeQuery(route.query.postingId);
  const routePostingNumber = normalizeQuery(route.query.postingNumber);
  if (routeAuthId && routeAuthId !== form.authId && authOptions.value.some((item) => item.id === routeAuthId)) {
    form.authId = routeAuthId;
  }
  if (routePostingId) {
    form.postingId = routePostingId;
  }
  if (routePostingNumber) {
    form.postingNumber = routePostingNumber;
  }
  if (form.authId && form.postingId) {
    loadHistory();
  }
}

function loadHistory() {
  if (!form.authId || !form.postingId) {
    ElMessage.info('请先选择授权并输入 postingId');
    return;
  }
  syncRoute({
    authId: form.authId,
    postingId: form.postingId,
    postingNumber: form.postingNumber
  });
  loading.value = true;
  shipmentApi.list({ authId: form.authId, postingId: form.postingId }).then((res) => {
    history.value = res.data || [];
  }).finally(() => {
    loading.value = false;
  });
}

function submitTracking() {
  if (!isEnabled('postingWrite')) {
    ElMessage.warning(reason('postingWrite'));
    return;
  }
  if (!form.authId || !form.postingId || !form.trackingNumber) {
    ElMessage.error('授权、postingId、追踪号不能为空');
    return;
  }
  submitting.value = true;
  shipmentApi.pushTracking({
    authId: form.authId,
    postingId: form.postingId,
    trackingNumber: form.trackingNumber,
    deliveryService: form.deliveryService || undefined
  }).then(() => {
    ElMessage.success('追踪号已提交');
    loadHistory();
  }).finally(() => {
    submitting.value = false;
  });
}

function normalizeQuery(value) {
  if (Array.isArray(value)) {
    return value[0] || '';
  }
  return value || '';
}

function syncRoute({ authId, postingId, postingNumber }) {
  const normalized = {
    authId: authId || undefined,
    postingId: postingId || undefined,
    postingNumber: postingNumber || undefined
  };
  const current = {
    authId: normalizeQuery(route.query.authId) || undefined,
    postingId: normalizeQuery(route.query.postingId) || undefined,
    postingNumber: normalizeQuery(route.query.postingNumber) || undefined
  };
  if (JSON.stringify(normalized) !== JSON.stringify(current)) {
    router.replace({ path: route.path, query: { ...route.query, ...normalized } });
  }
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
</style>
