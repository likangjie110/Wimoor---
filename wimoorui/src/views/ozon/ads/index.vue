<template>
  <div class="main-sty">
    <OzonFeatureNotice
      :item="features.ads"
      title="Ozon 广告模块当前已关闭"
      description="关闭状态下不会加载广告活动、日报和汇总，也无法导入本地广告 JSON。"
    />
    <OzonFeatureNotice
      :item="features.adsSync"
      type="info"
      title="Ozon 广告官方同步未开启"
      description="当前页面仅支持本地 JSON 导入和汇总浏览，尚未启用官方 Performance API 同步。"
    />
    <OzonFeatureSummaryBar :items="summaryFeatureItems" />
    <ModeSwitchBanner
      title="广告双模工作台"
      description="本地导入仍是当前稳定模式，账号/活动/报表分析继续沿用；官方同步能力会直接嵌入同页。"
      local-title="本地广告导入"
      local-description="导入账号、活动和日报 JSON，完成基础分析和汇总浏览。"
      remote-title="官方同步模式"
      remote-description="当广告同步开关开启后，可在同页记录同步意图并逐步接入 Performance API。"
      :remote-enabled="isEnabled('adsSync')"
      :remote-reason="reason('adsSync')"
    />

    <el-card shadow="never" class="toolbar-card">
      <template #header>
        <div class="card-title">
          <div>
            <h3>Ozon 广告导入</h3>
            <p class="font-extraSmall">导入本地 JSON 广告数据，查看活动列表、日报数据和汇总卡片。</p>
          </div>
          <el-button type="primary" :loading="importing" :disabled="!isEnabled('ads')" @click="submitImport">导入广告数据</el-button>
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
          <el-form-item label="广告账号">
            <el-select v-model="filters.accountId" clearable placeholder="全部账号" style="width: 100%" @change="handleAccountChange">
              <el-option v-for="item in accountData" :key="item.accountId" :label="item.accountName || item.accountId" :value="item.accountId" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="活动筛选">
            <el-input v-model="filters.keyword" placeholder="活动名/活动ID/类型" @keyup.enter="loadCampaigns" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="原始 JSON">
        <el-input
          v-model="form.rawContent"
          type="textarea"
          :rows="8"
          placeholder='{"account":{"accountId":"acc-1","accountName":"Ozon Ads Account","status":"ACTIVE","currencyCode":"RUB"},"campaigns":[{"campaignId":"camp-1","campaignName":"Spring Campaign","campaignType":"SEARCH_PROMO","campaignStatus":"ACTIVE","budget":1000}],"reports":[{"campaignId":"camp-1","reportDate":"2026-03-26","impressions":1000,"clicks":50,"spend":120.5,"orders":5,"sales":800,"ctr":5,"cpc":2.41,"acos":15.06,"roas":6.64}]}'
        />
      </el-form-item>
    </el-card>

    <SyncPanel
      :sync-enabled="isEnabled('adsSync')"
      :sync-reason="reason('adsSync')"
      :auth-id="form.authId"
      :loading="syncing"
      :loading-campaigns="syncingCampaigns"
      :loading-reports="syncingReports"
      :form="syncForm"
      :account-options="accountData"
      :campaign-options="campaignData"
      :results="syncResults"
      @submit="submitSyncIntent"
      @sync-campaigns="handleSyncCampaigns"
      @sync-reports="handleSyncReports"
    />

    <el-row :gutter="16" class="summary-row">
      <el-col :span="4"><el-card shadow="never" class="summary-card"><div class="summary-label">曝光</div><div class="summary-value">{{ summary.impressions }}</div></el-card></el-col>
      <el-col :span="4"><el-card shadow="never" class="summary-card"><div class="summary-label">点击</div><div class="summary-value">{{ summary.clicks }}</div></el-card></el-col>
      <el-col :span="4"><el-card shadow="never" class="summary-card"><div class="summary-label">花费</div><div class="summary-value">{{ summary.spend }}</div></el-card></el-col>
      <el-col :span="4"><el-card shadow="never" class="summary-card"><div class="summary-label">订单</div><div class="summary-value">{{ summary.orders }}</div></el-card></el-col>
      <el-col :span="4"><el-card shadow="never" class="summary-card"><div class="summary-label">销售额</div><div class="summary-value">{{ summary.sales }}</div></el-card></el-col>
      <el-col :span="4"><el-card shadow="never" class="summary-card"><div class="summary-label">ACOS / ROAS</div><div class="summary-value">{{ summary.acos }} / {{ summary.roas }}</div></el-card></el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="9">
        <el-card shadow="never" class="campaign-card">
          <template #header>
            <div class="card-title">
              <div>活动列表</div>
              <el-button @click="loadCampaigns">刷新活动</el-button>
            </div>
          </template>
          <el-table
            v-loading="campaignLoading"
            :data="campaignData"
            border
            highlight-current-row
            @current-change="handleCampaignChange"
          >
            <el-table-column prop="campaignName" label="活动名" min-width="180" show-overflow-tooltip />
            <el-table-column prop="campaignType" label="类型" min-width="120" />
            <el-table-column prop="campaignStatus" label="状态" width="100" />
            <el-table-column prop="budget" label="预算" width="100" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="15">
        <el-card shadow="never">
          <template #header>
            <div class="card-title">
              <div>日报数据</div>
              <el-space>
                <el-date-picker
                  v-model="filters.dateRange"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                />
                <el-button @click="loadReports">刷新报表</el-button>
              </el-space>
            </div>
          </template>
          <el-table v-loading="reportLoading" :data="reportData" border>
            <el-table-column label="日期" min-width="140">
              <template #default="scope">
                {{ scope.row.reportDate ? dateFormat(scope.row.reportDate) : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="impressions" label="曝光" width="90" />
            <el-table-column prop="clicks" label="点击" width="90" />
            <el-table-column prop="spend" label="花费" width="110" />
            <el-table-column prop="orders" label="订单" width="90" />
            <el-table-column prop="sales" label="销售额" width="110" />
            <el-table-column prop="ctr" label="CTR" width="90" />
            <el-table-column prop="cpc" label="CPC" width="90" />
            <el-table-column prop="acos" label="ACOS" width="90" />
            <el-table-column prop="roas" label="ROAS" width="90" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="openRaw(scope.row)">查看原文</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-drawer v-model="rawDrawer.visible" title="广告原始行" size="720px">
      <el-scrollbar max-height="520px">
        <pre class="payload-pre">{{ rawDrawer.content || '-' }}</pre>
      </el-scrollbar>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import authApi from '@/api/ozon/auth/authApi.js';
import adsApi from '@/api/ozon/ads/adsApi.js';
import opsApi from '@/api/ozon/ops/opsApi.js';
import { dateFormat } from '@/utils/index.js';
import OzonFeatureNotice from '../components/OzonFeatureNotice.vue';
import OzonFeatureSummaryBar from '../components/OzonFeatureSummaryBar.vue';
import { useOzonFeatures } from '../composables/useOzonFeatures.js';
import ModeSwitchBanner from '../shared/components/ModeSwitchBanner.vue';
import SyncPanel from './components/SyncPanel.vue';

const importing = ref(false);
const campaignLoading = ref(false);
const reportLoading = ref(false);
const syncing = ref(false);
const syncingCampaigns = ref(false);
const syncingReports = ref(false);
const authOptions = ref([]);
const accountData = ref([]);
const campaignData = ref([]);
const reportData = ref([]);
const syncResults = ref([]);
const selectedCampaign = ref(null);
const summary = reactive({
  impressions: 0,
  clicks: 0,
  spend: 0,
  orders: 0,
  sales: 0,
  acos: 0,
  roas: 0
});
const rawDrawer = reactive({
  visible: false,
  content: ''
});
const form = reactive({
  authId: '',
  rawContent: ''
});
const filters = reactive({
  accountId: '',
  keyword: '',
  dateRange: []
});
const syncForm = reactive({
  accountId: '',
  campaignId: '',
  dateRange: []
});
const { features, featureItems, loadFeatures, isEnabled, reason } = useOzonFeatures();
const summaryFeatureItems = computed(() => featureItems.value.filter((item) => ['ads', 'adsSync'].includes(item.key)));

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
  selectedCampaign.value = null;
  filters.accountId = '';
  syncForm.accountId = '';
  syncForm.campaignId = '';
  campaignData.value = [];
  accountData.value = [];
  reportData.value = [];
  syncResults.value = [];
  resetSummary();
  loadAccounts().finally(() => {
    loadCampaigns();
    loadReports();
    loadSyncResults();
  });
}

function loadAccounts() {
  if (!form.authId || !isEnabled('ads')) {
    accountData.value = [];
    return Promise.resolve();
  }
  return adsApi.listAccounts(form.authId).then(res => {
    accountData.value = res.data || [];
    if (!filters.accountId && accountData.value.length > 0) {
      filters.accountId = accountData.value[0].accountId;
    }
    if (!syncForm.accountId && accountData.value.length > 0) {
      syncForm.accountId = accountData.value[0].accountId;
    }
  });
}

function submitImport() {
  if (!isEnabled('ads')) {
    ElMessage.warning(reason('ads'));
    return;
  }
  if (!form.authId || !form.rawContent) {
    ElMessage.error('请先选择授权并输入原始 JSON');
    return;
  }
  importing.value = true;
  adsApi.importAds({
    authId: form.authId,
    rawContent: form.rawContent
  }).then(res => {
    ElMessage.success(`已导入 ${res.data?.campaignCount || 0} 个活动，${res.data?.reportCount || 0} 条报表`);
    loadAccounts().finally(() => {
      loadCampaigns();
      loadReports();
      loadSyncResults();
    });
  }).finally(() => {
    importing.value = false;
  });
}

function loadCampaigns() {
  if (!form.authId || !isEnabled('ads')) {
    campaignData.value = [];
    return;
  }
  campaignLoading.value = true;
  adsApi.listCampaigns({
    authId: form.authId,
    accountId: filters.accountId || undefined,
    keyword: filters.keyword || undefined
  }).then(res => {
    campaignData.value = res.data || [];
    if (selectedCampaign.value && !campaignData.value.some((item) => item.campaignId === selectedCampaign.value.campaignId)) {
      selectedCampaign.value = null;
    }
  }).finally(() => {
    campaignLoading.value = false;
  });
}

function handleAccountChange() {
  selectedCampaign.value = null;
  syncForm.accountId = filters.accountId || accountData.value[0]?.accountId || '';
  syncForm.campaignId = '';
  loadCampaigns();
  loadReports();
}

function handleCampaignChange(row) {
  selectedCampaign.value = row || null;
  syncForm.campaignId = row?.campaignId || '';
  loadReports();
}

function loadReports() {
  if (!form.authId || !isEnabled('ads')) {
    reportData.value = [];
    resetSummary();
    return;
  }
  reportLoading.value = true;
  const params = {
    authId: form.authId,
    accountId: filters.accountId || undefined,
    campaignId: selectedCampaign.value?.campaignId || undefined,
    fromDate: filters.dateRange?.[0] || undefined,
    toDate: filters.dateRange?.[1] || undefined
  };
  adsApi.listReports(params).then(res => {
    reportData.value = res.data || [];
  }).finally(() => {
    reportLoading.value = false;
  });
  adsApi.getSummary(params).then(res => {
    Object.assign(summary, res.data || {});
  });
}

function loadSyncResults() {
  if (!form.authId) {
    syncResults.value = [];
    return;
  }
  opsApi.listOperationAudits({
    authId: form.authId,
    operationType: 'ADS_SYNC_INTENT'
  }).then(res => {
    syncResults.value = res.data || [];
  });
}

function submitSyncIntent() {
  if (!isEnabled('adsSync')) {
    ElMessage.warning(reason('adsSync'));
    return;
  }
  if (!form.authId || !syncForm.accountId) {
    ElMessage.error('请先选择广告账号');
    return;
  }
  syncing.value = true;
  adsApi.createSyncIntent({
    authId: form.authId,
    accountId: syncForm.accountId,
    campaignId: syncForm.campaignId || undefined,
    fromDate: syncForm.dateRange?.[0] || undefined,
    toDate: syncForm.dateRange?.[1] || undefined
  }).then(res => {
    ElMessage.success(res.data?.message || '已记录同步意图');
    loadSyncResults();
  }).finally(() => {
    syncing.value = false;
  });
}

function handleSyncCampaigns() {
  if (!isEnabled('adsSync')) {
    ElMessage.warning(reason('adsSync'));
    return;
  }
  if (!form.authId) {
    ElMessage.error('请先选择授权店铺');
    return;
  }
  syncingCampaigns.value = true;
  adsApi.syncCampaigns(form.authId).then(res => {
    ElMessage.success(`成功同步 ${res.data || 0} 个广告活动`);
    loadCampaigns();
  }).catch(err => {
    ElMessage.error(err.message || '同步失败');
  }).finally(() => {
    syncingCampaigns.value = false;
  });
}

function handleSyncReports() {
  if (!isEnabled('adsSync')) {
    ElMessage.warning(reason('adsSync'));
    return;
  }
  if (!form.authId) {
    ElMessage.error('请先选择授权店铺');
    return;
  }
  if (!syncForm.dateRange || syncForm.dateRange.length !== 2) {
    ElMessage.error('请选择日期范围');
    return;
  }
  syncingReports.value = true;
  adsApi.syncReports(form.authId, syncForm.dateRange[0], syncForm.dateRange[1]).then(res => {
    ElMessage.success(`成功同步 ${res.data || 0} 条广告报告`);
    loadReports();
  }).catch(err => {
    ElMessage.error(err.message || '同步失败');
  }).finally(() => {
    syncingReports.value = false;
  });
}

function resetSummary() {
  summary.impressions = 0;
  summary.clicks = 0;
  summary.spend = 0;
  summary.orders = 0;
  summary.sales = 0;
  summary.acos = 0;
  summary.roas = 0;
}

function openRaw(row) {
  rawDrawer.content = row.rawLineJson || '';
  rawDrawer.visible = true;
}
</script>

<style scoped>
.toolbar-card {
  margin-bottom: 16px;
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
  font-size: 22px;
  font-weight: 600;
}

.campaign-card {
  height: 100%;
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
