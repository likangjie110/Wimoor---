<template>
  <div class="main-sty">
    <OzonFeatureNotice
      :item="features.auth"
      title="Ozon 授权功能当前已关闭"
      description="授权、仓库、配送方式和初始化任务都会统一禁用。"
    />
    <OzonFeatureSummaryBar :items="summaryFeatureItems" />

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="授权列表" name="auth">
        <el-card shadow="never" class="bind-card">
          <template #header>
            <div class="card-title">
              <div>
                <h3>Ozon 授权管理</h3>
                <p class="font-extraSmall">录入 Client ID 与 API Key 后保存授权，保存后可执行连接测试与仓库初始化。</p>
              </div>
              <el-space>
                <el-button :disabled="!isEnabled('auth')" @click="showPreBindTip">连接测试</el-button>
                <el-button type="primary" :loading="submitting" :disabled="!isEnabled('auth')" @click="saveAuth">保存授权</el-button>
              </el-space>
            </div>
          </template>

          <el-form :model="bindForm" label-width="110px" class="bind-form">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="授权名称">
                  <el-input v-model="bindForm.name" :disabled="!isEnabled('auth')" placeholder="例如：Ozon RU 主店" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="Client ID">
                  <el-input v-model="bindForm.clientId" :disabled="!isEnabled('auth')" placeholder="请输入 Ozon Client ID" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="API Key">
                  <el-input v-model="bindForm.apiKey" :disabled="!isEnabled('auth')" type="password" show-password placeholder="请输入 Ozon API Key" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-title">
              <div>
                <span>授权列表</span>
                <div v-if="activeAuthName" class="font-extraSmall current-auth">当前工作授权：{{ activeAuthName }}</div>
              </div>
              <el-button :disabled="!isEnabled('auth')" @click="loadList">刷新列表</el-button>
            </div>
          </template>
          <el-table
            v-loading="loading"
            :data="tableData"
            border
            highlight-current-row
            row-key="id"
            :current-row-key="activeAuthId"
            @row-click="handleAuthRowClick"
          >
            <el-table-column prop="name" label="授权名称" min-width="180" />
            <el-table-column prop="clientId" label="Client ID" min-width="140" />
            <el-table-column prop="apiKeyMasked" label="密钥摘要" min-width="120" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column label="最近同步" min-width="180">
              <template #default="scope">
                {{ scope.row.lastSyncTime ? dateFormat(scope.row.lastSyncTime) : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="lastSyncMessage" label="最近结果" min-width="220" show-overflow-tooltip />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="scope">
                <el-space>
                  <el-button link type="primary" :disabled="!isEnabled('auth')" @click.stop="ping(scope.row)">连接测试</el-button>
                  <el-button link type="primary" :disabled="!isEnabled('auth')" @click.stop="openRotate(scope.row)">轮换密钥</el-button>
                  <el-button link type="danger" :disabled="!isEnabled('auth')" @click.stop="disableAuth(scope.row)">停用</el-button>
                </el-space>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="仓库同步" name="warehouse">
        <WarehousePanel
          :auth-options="tableData"
          :auth-id="activeAuthId"
          :warehouses="warehouseData"
          :loading="warehouseLoading"
          :disabled="!isEnabled('auth')"
          @update:authId="setActiveAuth"
          @refresh="loadWarehouses"
          @sync="syncWarehouses"
        />
      </el-tab-pane>

      <el-tab-pane label="配送方式" name="delivery">
        <DeliveryMethodPanel
          :auth-options="tableData"
          :auth-id="activeAuthId"
          :methods="deliveryData"
          :loading="deliveryLoading"
          :disabled="!isEnabled('auth')"
          :form="deliveryForm"
          @update:authId="setActiveAuth"
          @refresh="loadDeliveryMethods"
          @save="saveDeliveryMethod"
          @select-method="selectDeliveryMethod"
          @reset-form="resetDeliveryForm"
        />
      </el-tab-pane>

      <el-tab-pane label="初始化任务" name="initTask">
        <InitTaskPanel
          :auth-options="tableData"
          :auth-id="activeAuthId"
          :tasks="initTaskData"
          :loading="initTaskLoading"
          :disabled="!isEnabled('auth')"
          @update:authId="setActiveAuth"
          @refresh="loadInitTasks"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="rotateDialog.visible" title="轮换 Ozon API Key" width="420px">
      <el-form label-width="90px">
        <el-form-item label="新 API Key">
          <el-input v-model="rotateDialog.apiKey" type="password" show-password placeholder="请输入新的 API Key" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rotateDialog.visible = false">取消</el-button>
        <el-button type="primary" :disabled="!isEnabled('auth')" @click="submitRotate">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import authApi from '@/api/ozon/auth/authApi.js';
import taskApi from '@/api/ozon/task/taskApi.js';
import { dateFormat } from '@/utils/index.js';
import OzonFeatureNotice from '../components/OzonFeatureNotice.vue';
import OzonFeatureSummaryBar from '../components/OzonFeatureSummaryBar.vue';
import { useOzonFeatures } from '../composables/useOzonFeatures.js';
import WarehousePanel from './components/WarehousePanel.vue';
import DeliveryMethodPanel from './components/DeliveryMethodPanel.vue';
import InitTaskPanel from './components/InitTaskPanel.vue';

const loading = ref(false);
const submitting = ref(false);
const warehouseLoading = ref(false);
const deliveryLoading = ref(false);
const initTaskLoading = ref(false);
const tableData = ref([]);
const warehouseData = ref([]);
const deliveryData = ref([]);
const initTaskData = ref([]);
const activeTab = ref('auth');
const activeAuthId = ref('');
const bindForm = reactive({ name: '', clientId: '', apiKey: '' });
const rotateDialog = reactive({ visible: false, authId: '', apiKey: '' });
const deliveryForm = reactive(createDeliveryForm());
const { features, featureItems, loadFeatures, isEnabled, reason } = useOzonFeatures();

const summaryFeatureItems = computed(() => featureItems.value.filter((item) => ['auth', 'productWrite', 'stockWrite', 'priceWrite', 'postingWrite', 'chatSend', 'adsSync'].includes(item.key)));
const activeAuthName = computed(() => tableData.value.find((item) => item.id === activeAuthId.value)?.name || '');

onMounted(() => {
  loadFeatures().finally(() => {
    if (isEnabled('auth')) {
      loadList();
    }
  });
});

function createDeliveryForm() {
  return {
    id: '',
    methodCode: '',
    methodName: '',
    description: '',
    enabled: true,
    defaultMethod: false
  };
}

function resetDeliveryForm() {
  Object.assign(deliveryForm, createDeliveryForm());
}

function loadList() {
  if (!isEnabled('auth')) {
    tableData.value = [];
    activeAuthId.value = '';
    return;
  }
  loading.value = true;
  authApi.list().then(res => {
    tableData.value = res.data || [];
    if (!activeAuthId.value && tableData.value.length > 0) {
      setActiveAuth(tableData.value[0].id);
      return;
    }
    if (activeAuthId.value && !tableData.value.some((item) => item.id === activeAuthId.value)) {
      setActiveAuth(tableData.value[0]?.id || '');
      return;
    }
    loadActiveTabData();
  }).finally(() => {
    loading.value = false;
  });
}

function handleTabChange() {
  loadActiveTabData();
}

function loadActiveTabData() {
  if (!activeAuthId.value || !isEnabled('auth')) {
    warehouseData.value = [];
    deliveryData.value = [];
    initTaskData.value = [];
    return;
  }
  if (activeTab.value === 'warehouse') {
    loadWarehouses();
  } else if (activeTab.value === 'delivery') {
    loadDeliveryMethods();
  } else if (activeTab.value === 'initTask') {
    loadInitTasks();
  }
}

function setActiveAuth(authId) {
  activeAuthId.value = authId || '';
  resetDeliveryForm();
  loadActiveTabData();
}

function handleAuthRowClick(row) {
  setActiveAuth(row?.id || '');
}

function saveAuth() {
  if (!isEnabled('auth')) {
    ElMessage.warning(reason('auth'));
    return;
  }
  if (!bindForm.clientId || !bindForm.apiKey) {
    ElMessage.error('Client ID 和 API Key 不能为空');
    return;
  }
  submitting.value = true;
  authApi.bind({ ...bindForm }).then(() => {
    ElMessage.success('Ozon 授权已保存');
    bindForm.name = '';
    bindForm.clientId = '';
    bindForm.apiKey = '';
    loadList();
  }).finally(() => {
    submitting.value = false;
  });
}

function showPreBindTip() {
  if (!isEnabled('auth')) {
    ElMessage.warning(reason('auth'));
    return;
  }
  ElMessage.info('请先保存授权，再在列表中执行连接测试。');
}

function ping(row) {
  if (!isEnabled('auth')) {
    ElMessage.warning(reason('auth'));
    return;
  }
  setActiveAuth(row?.id || '');
  authApi.ping(row.id).then(res => {
    const count = res.data?.warehouseCount ?? 0;
    ElMessage.success(`连接成功，已同步 ${count} 个仓库`);
    loadList();
    loadWarehouses();
    loadInitTasks();
  });
}

function openRotate(row) {
  if (!isEnabled('auth')) {
    ElMessage.warning(reason('auth'));
    return;
  }
  setActiveAuth(row?.id || '');
  rotateDialog.visible = true;
  rotateDialog.authId = row.id;
  rotateDialog.apiKey = '';
}

function submitRotate() {
  if (!isEnabled('auth')) {
    ElMessage.warning(reason('auth'));
    return;
  }
  if (!rotateDialog.apiKey) {
    ElMessage.error('请输入新的 API Key');
    return;
  }
  authApi.rotateKey({ authId: rotateDialog.authId, apiKey: rotateDialog.apiKey }).then(() => {
    ElMessage.success('API Key 已更新');
    rotateDialog.visible = false;
    rotateDialog.apiKey = '';
    loadList();
  });
}

function disableAuth(row) {
  if (!isEnabled('auth')) {
    ElMessage.warning(reason('auth'));
    return;
  }
  setActiveAuth(row?.id || '');
  ElMessageBox.confirm(`确认停用授权【${row.name}】吗？`, '停用确认', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    authApi.disable(row.id).then(() => {
      ElMessage.success('授权已停用');
      loadList();
    });
  });
}

function loadWarehouses() {
  if (!activeAuthId.value || !isEnabled('auth')) {
    warehouseData.value = [];
    return;
  }
  warehouseLoading.value = true;
  authApi.listWarehouses(activeAuthId.value).then((res) => {
    warehouseData.value = (res.data || []).map((item) => ({
      ...item,
      syncedAt: item.syncedAt ? dateFormat(item.syncedAt) : '-',
      lastWarehouseSyncTime: item.lastWarehouseSyncTime ? dateFormat(item.lastWarehouseSyncTime) : '-'
    }));
  }).finally(() => {
    warehouseLoading.value = false;
  });
}

function syncWarehouses() {
  const current = tableData.value.find((item) => item.id === activeAuthId.value);
  if (!current) {
    ElMessage.error('请先选择授权');
    return;
  }
  ping(current);
}

function loadDeliveryMethods() {
  if (!activeAuthId.value || !isEnabled('auth')) {
    deliveryData.value = [];
    return;
  }
  deliveryLoading.value = true;
  authApi.listDeliveryMethods(activeAuthId.value).then((res) => {
    deliveryData.value = res.data || [];
  }).finally(() => {
    deliveryLoading.value = false;
  });
}

function selectDeliveryMethod(row) {
  if (!row) {
    return;
  }
  deliveryForm.id = row.id || '';
  deliveryForm.methodCode = row.methodCode || '';
  deliveryForm.methodName = row.methodName || '';
  deliveryForm.description = row.description || '';
  deliveryForm.enabled = row.enabled !== false;
  deliveryForm.defaultMethod = !!row.defaultMethod;
}

function saveDeliveryMethod() {
  if (!isEnabled('auth')) {
    ElMessage.warning(reason('auth'));
    return;
  }
  if (!activeAuthId.value) {
    ElMessage.error('请先选择授权');
    return;
  }
  if (!deliveryForm.methodCode || !deliveryForm.methodName) {
    ElMessage.error('方式编码和方式名称不能为空');
    return;
  }
  authApi.saveDeliveryMethod({
    authId: activeAuthId.value,
    id: deliveryForm.id || undefined,
    methodCode: deliveryForm.methodCode,
    methodName: deliveryForm.methodName,
    description: deliveryForm.description || undefined,
    enabled: deliveryForm.enabled,
    defaultMethod: deliveryForm.defaultMethod
  }).then(() => {
    ElMessage.success('配送方式已保存');
    resetDeliveryForm();
    loadDeliveryMethods();
  });
}

function loadInitTasks() {
  if (!activeAuthId.value || !isEnabled('auth')) {
    initTaskData.value = [];
    return;
  }
  initTaskLoading.value = true;
  taskApi.list({ authId: activeAuthId.value }).then((res) => {
    initTaskData.value = (res.data || [])
      .filter((item) => ['INIT_SELLER', 'INIT_WAREHOUSE'].includes(item.jobType))
      .map((item) => ({
        ...item,
        createdAt: item.createdAt ? dateFormat(item.createdAt) : '-',
        updatedAt: item.updatedAt ? dateFormat(item.updatedAt) : '-'
      }));
  }).finally(() => {
    initTaskLoading.value = false;
  });
}
</script>

<style scoped>
.bind-card {
  margin-bottom: 16px;
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.bind-form {
  margin-top: 8px;
}

.current-auth {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
}
</style>
