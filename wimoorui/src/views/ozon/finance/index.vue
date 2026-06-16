<template>
  <div class="main-sty">
    <OzonFeatureNotice
      :item="features.finance"
      title="Ozon 财务模块当前已关闭"
      description="关闭状态下不会加载导入任务和交易明细，也无法提交新的本地报表。"
    />
    <OzonFeatureSummaryBar :items="summaryFeatureItems" />
    <ModeSwitchBanner
      title="财务双模工作台"
      description="当前继续使用本地报表导入做可验证闭环，官方在线拉取入口保留在同页，不会再单独拆页面。"
      local-title="本地报表导入"
      local-description="导入原始 JSON、保留任务和原文、解析交易明细，是当前稳定工作模式。"
      remote-title="官方在线拉取"
      remote-description="合同和远端接口确认后，直接在本页增加在线拉取，不迁移现有任务和明细视图。"
      :remote-enabled="false"
      remote-reason="当前仅保留入口占位，尚未启用远端官方拉取。"
    />

    <el-card shadow="never" class="toolbar-card">
      <template #header>
        <div class="card-title">
          <div>
            <h3>Ozon 财务导入</h3>
            <p class="font-extraSmall">导入本地 Ozon 财务 JSON 报表原文，保留导入任务、原始内容和解析后的交易明细。</p>
          </div>
          <el-button type="primary" :loading="importing" :disabled="!isEnabled('finance')" @click="submitImport">导入报表</el-button>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="授权店铺">
            <el-select v-model="form.authId" placeholder="请选择 Ozon 授权" style="width: 100%" @change="reloadAll">
              <el-option v-for="item in authOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="报表ID">
            <el-input v-model="form.reportId" placeholder="例如 report-20260326-001" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="报表日期">
            <el-date-picker v-model="form.reportDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="原始 JSON">
        <el-input
          v-model="form.rawContent"
          type="textarea"
          :rows="8"
          placeholder='{"transactions":[{"transactionId":"txn-1","operationType":"sale","postingNumber":"posting-1","amount":12.5,"currencyCode":"RUB","transactionTime":"2026-03-26T08:00:00Z"}]}'
        />
      </el-form-item>
    </el-card>

    <TaskResultPanel
      :task="latestTask"
      @refresh="loadTasks"
      @open-raw="openRaw"
    />

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-title">
          <div>导入任务</div>
          <el-button @click="loadTasks">刷新任务</el-button>
        </div>
      </template>
      <el-table v-loading="taskLoading" :data="taskData" border>
        <el-table-column prop="reportId" label="报表ID" min-width="160" show-overflow-tooltip />
        <el-table-column label="报表日期" min-width="140">
          <template #default="scope">
            {{ scope.row.reportDate ? dateFormat(scope.row.reportDate) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="taskStatus" label="状态" width="120" />
        <el-table-column prop="importedCount" label="导入条数" width="100" />
        <el-table-column prop="errorMessage" label="错误消息" min-width="220" show-overflow-tooltip />
        <el-table-column label="更新时间" min-width="160">
          <template #default="scope">
            {{ scope.row.updatedAt ? dateFormat(scope.row.updatedAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openRaw(scope.row)">查看原文</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-title">
          <div>交易明细</div>
          <el-space>
            <el-date-picker
              v-model="transactionQuery.dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
            />
            <el-input v-model="transactionQuery.reportId" placeholder="按报表ID筛选" style="width: 220px" />
            <el-button @click="loadTransactions">刷新明细</el-button>
          </el-space>
        </div>
      </template>
      <el-table v-loading="transactionLoading" :data="transactionData" border>
        <el-table-column prop="transactionId" label="交易ID" min-width="160" show-overflow-tooltip />
        <el-table-column prop="operationType" label="业务类型" min-width="140" show-overflow-tooltip />
        <el-table-column prop="postingNumber" label="订单号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="amount" label="金额" width="120" />
        <el-table-column prop="currencyCode" label="币种" width="100" />
        <el-table-column label="交易时间" min-width="170">
          <template #default="scope">
            {{ scope.row.transactionTime ? dateFormat(scope.row.transactionTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openLine(scope.row)">查看原行</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="rawDrawer.visible" title="报表原文" size="720px">
      <el-scrollbar max-height="520px">
        <pre class="payload-pre">{{ rawDrawer.content || '-' }}</pre>
      </el-scrollbar>
    </el-drawer>

    <el-drawer v-model="lineDrawer.visible" title="原始交易行" size="720px">
      <el-scrollbar max-height="520px">
        <pre class="payload-pre">{{ lineDrawer.content || '-' }}</pre>
      </el-scrollbar>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import authApi from '@/api/ozon/auth/authApi.js';
import financeApi from '@/api/ozon/finance/financeApi.js';
import { dateFormat } from '@/utils/index.js';
import OzonFeatureNotice from '../components/OzonFeatureNotice.vue';
import OzonFeatureSummaryBar from '../components/OzonFeatureSummaryBar.vue';
import { useOzonFeatures } from '../composables/useOzonFeatures.js';
import ModeSwitchBanner from '../shared/components/ModeSwitchBanner.vue';
import TaskResultPanel from './components/TaskResultPanel.vue';

const importing = ref(false);
const taskLoading = ref(false);
const transactionLoading = ref(false);
const authOptions = ref([]);
const taskData = ref([]);
const transactionData = ref([]);
const form = reactive({
  authId: '',
  reportId: '',
  reportDate: '',
  rawContent: ''
});
const transactionQuery = reactive({
  reportId: '',
  dateRange: []
});
const rawDrawer = reactive({
  visible: false,
  content: ''
});
const lineDrawer = reactive({
  visible: false,
  content: ''
});
const { features, featureItems, loadFeatures, isEnabled, reason } = useOzonFeatures();
const summaryFeatureItems = computed(() => featureItems.value.filter((item) => ['finance', 'task'].includes(item.key)));
const latestTask = computed(() => (taskData.value && taskData.value.length > 0 ? taskData.value[0] : null));

onMounted(() => {
  loadFeatures().finally(loadAuths);
});

function loadAuths() {
  authApi.list().then(res => {
    authOptions.value = res.data || [];
    if (!form.authId && authOptions.value.length > 0) {
      form.authId = authOptions.value[0].id;
      reloadAll();
    }
  });
}

function reloadAll() {
  loadTasks();
  loadTransactions();
}

function submitImport() {
  if (!isEnabled('finance')) {
    ElMessage.warning(reason('finance'));
    return;
  }
  if (!form.authId || !form.reportId || !form.reportDate || !form.rawContent) {
    ElMessage.error('请完整填写授权、报表ID、报表日期和原始 JSON');
    return;
  }
  importing.value = true;
  financeApi.importReport({
    authId: form.authId,
    reportId: form.reportId,
    reportDate: form.reportDate,
    rawContent: form.rawContent
  }).then(res => {
    ElMessage.success(`财务报表已导入 ${res.data?.importedCount || 0} 条交易`);
    transactionQuery.reportId = form.reportId;
    loadTasks();
    loadTransactions();
  }).finally(() => {
    importing.value = false;
  });
}

function loadTasks() {
  if (!form.authId || !isEnabled('finance')) {
    taskData.value = [];
    return;
  }
  taskLoading.value = true;
  financeApi.listTasks(form.authId).then(res => {
    taskData.value = res.data || [];
  }).finally(() => {
    taskLoading.value = false;
  });
}

function loadTransactions() {
  if (!form.authId || !isEnabled('finance')) {
    transactionData.value = [];
    return;
  }
  transactionLoading.value = true;
  financeApi.listTransactions({
    authId: form.authId,
    reportId: transactionQuery.reportId || undefined,
    fromDate: transactionQuery.dateRange?.[0] || undefined,
    toDate: transactionQuery.dateRange?.[1] || undefined
  }).then(res => {
    transactionData.value = res.data || [];
  }).finally(() => {
    transactionLoading.value = false;
  });
}

function openRaw(row) {
  if (!isEnabled('finance')) {
    ElMessage.warning(reason('finance'));
    return;
  }
  financeApi.getRawContent(form.authId, row.id).then(res => {
    rawDrawer.content = res.data || '';
    rawDrawer.visible = true;
  });
}

function openLine(row) {
  lineDrawer.content = row.rawLineJson || '';
  lineDrawer.visible = true;
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

.payload-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.6;
}
</style>
