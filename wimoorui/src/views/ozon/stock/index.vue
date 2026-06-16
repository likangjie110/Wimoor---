<template>
  <div class="main-sty">
    <OzonFeatureNotice
      :item="features.stockWrite"
      title="Ozon 库存写操作当前已关闭"
      description="你仍然可以查看历史库存快照和库存任务，但库存推送按钮会保持禁用。"
    />

    <el-card shadow="never" class="toolbar-card">
      <template #header>
        <div class="card-title">
          <div>
            <h3>Ozon 库存推送</h3>
            <p class="font-extraSmall">按行输入 `SKU,数量`，提交后会生成本地任务与快照记录。</p>
          </div>
          <el-space>
            <el-button @click="refreshAll">刷新全部</el-button>
            <el-button v-if="materialSkuHint" @click="goToProduct">返回商品工作台</el-button>
            <el-button type="primary" :disabled="!isEnabled('stockWrite')" @click="submitPush">提交库存</el-button>
          </el-space>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="授权店铺">
            <el-select v-model="form.authId" placeholder="请选择 Ozon 授权" style="width: 100%" @change="refreshAll">
              <el-option v-for="item in authOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="仓库ID">
            <el-input v-model="form.warehouseId" placeholder="例如：1020003216782000" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="当前联动SKU">
            <el-input :model-value="materialSkuHint || '-'" readonly />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="库存明细">
        <el-input
          v-model="payloadText"
          type="textarea"
          :rows="6"
          placeholder="每行一个明细，格式：SKU,数量&#10;ERP-SKU-1,11&#10;ERP-SKU-2,0"
        />
      </el-form-item>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-title">
          <span>库存任务</span>
          <el-button @click="loadTasks">刷新任务</el-button>
        </div>
      </template>

      <el-table v-loading="taskLoading" :data="taskData" border>
        <el-table-column prop="taskId" label="任务ID" min-width="180" />
        <el-table-column prop="warehouseId" label="仓库ID" min-width="140" />
        <el-table-column prop="taskStatus" label="状态" width="120" />
        <el-table-column prop="requestedCount" label="请求条数" width="100" />
        <el-table-column prop="successCount" label="成功条数" width="100" />
        <el-table-column prop="errorMessage" label="错误信息" min-width="220" show-overflow-tooltip />
        <el-table-column label="创建时间" min-width="170">
          <template #default="scope">
            {{ scope.row.createdAt ? dateFormat(scope.row.createdAt) : '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-title">
          <span>库存快照</span>
          <el-button @click="loadSnapshots">刷新快照</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="snapshotData" border>
        <el-table-column prop="materialSku" label="ERP SKU" min-width="150" />
        <el-table-column prop="ozonOfferId" label="Offer ID" min-width="180" show-overflow-tooltip />
        <el-table-column prop="warehouseId" label="仓库ID" min-width="150" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="syncStatus" label="状态" width="110" />
        <el-table-column label="提交时间" min-width="170">
          <template #default="scope">
            {{ scope.row.syncedAt ? dateFormat(scope.row.syncedAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="syncMessage" label="结果" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import authApi from '@/api/ozon/auth/authApi.js';
import stockApi from '@/api/ozon/stock/stockApi.js';
import { dateFormat } from '@/utils/index.js';
import OzonFeatureNotice from '../components/OzonFeatureNotice.vue';
import { useOzonFeatures } from '../composables/useOzonFeatures.js';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const taskLoading = ref(false);
const authOptions = ref([]);
const snapshotData = ref([]);
const taskData = ref([]);
const payloadText = ref('');
const form = reactive({ authId: '', warehouseId: '' });
const { features, loadFeatures, isEnabled, reason } = useOzonFeatures();

const materialSkuHint = computed(() => normalizeQuery(route.query.materialSku));

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
    if (!form.authId && authOptions.value.length > 0) {
      form.authId = resolveInitialAuthId();
      applyRouteState();
      refreshAll();
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
  if (routeAuthId && routeAuthId !== form.authId && authOptions.value.some((item) => item.id === routeAuthId)) {
    form.authId = routeAuthId;
  }
  if (materialSkuHint.value) {
    prefillSku(materialSkuHint.value);
  }
}

function prefillSku(materialSku) {
  const rows = (payloadText.value || '').split(/\r?\n/).map((row) => row.trim()).filter(Boolean);
  if (rows.some((row) => row.startsWith(`${materialSku},`))) {
    return;
  }
  payloadText.value = `${materialSku},\n${rows.join('\n')}`.trim();
}

function refreshAll() {
  loadTasks();
  loadSnapshots();
}

function loadSnapshots() {
  if (!form.authId) {
    snapshotData.value = [];
    return;
  }
  loading.value = true;
  stockApi.listSnapshots(form.authId).then(res => {
    snapshotData.value = res.data || [];
  }).finally(() => {
    loading.value = false;
  });
}

function loadTasks() {
  if (!form.authId) {
    taskData.value = [];
    return;
  }
  taskLoading.value = true;
  stockApi.listTasks(form.authId).then((res) => {
    taskData.value = res.data || [];
  }).finally(() => {
    taskLoading.value = false;
  });
}

function submitPush() {
  if (!isEnabled('stockWrite')) {
    ElMessage.warning(reason('stockWrite'));
    return;
  }
  if (!form.authId) {
    ElMessage.error('请先选择 Ozon 授权');
    return;
  }
  let items = [];
  try {
    items = parseItems(payloadText.value);
  } catch (error) {
    ElMessage.error(error.message);
    return;
  }
  if (items.length === 0) {
    ElMessage.error('请输入库存明细');
    return;
  }
  stockApi.push({
    authId: form.authId,
    warehouseId: form.warehouseId || null,
    items
  }).then(res => {
    ElMessage.success(`库存任务已提交，受理 ${res.data?.accepted || 0} 条`);
    payloadText.value = '';
    if (materialSkuHint.value) {
      prefillSku(materialSkuHint.value);
    }
    refreshAll();
  });
}

function parseItems(text) {
  return (text || '')
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(Boolean)
    .map(line => {
      const parts = line.split(',').map(item => item.trim());
      if (parts.length < 2 || !parts[0]) {
        throw new Error(`库存明细格式错误: ${line}`);
      }
      const quantity = Number(parts[1]);
      if (!Number.isInteger(quantity) || quantity < 0) {
        throw new Error(`库存数量必须是大于等于 0 的整数: ${line}`);
      }
      return {
        materialSku: parts[0],
        quantity
      };
    });
}

function goToProduct() {
  router.push({
    path: '/ozon/product',
    query: {
      authId: form.authId,
      materialSku: materialSkuHint.value,
      focus: 'publish'
    }
  });
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

.section-card {
  margin-bottom: 16px;
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}
</style>
