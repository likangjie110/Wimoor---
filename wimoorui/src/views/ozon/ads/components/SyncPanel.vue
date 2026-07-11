<template>
  <el-card shadow="never" class="sync-card">
    <template #header>
      <div class="sync-head">官方 API 同步</div>
    </template>

    <el-alert
      :title="syncEnabled ? 'API 同步已开启，可以直接同步广告活动和报告数据。' : (syncReason || '官方同步未开启，当前仅可使用本地导入模式。')"
      :type="syncEnabled ? 'success' : 'warning'"
      :closable="false"
      class="sync-alert"
    />

    <div class="sync-section">
      <h4>同步广告活动</h4>
      <p class="section-desc">从 OZON Performance API 同步广告活动列表</p>
      <el-button
        type="primary"
        :disabled="!syncEnabled || !authId"
        :loading="loadingCampaigns"
        @click="$emit('syncCampaigns')"
      >
        同步广告活动
      </el-button>
    </div>

    <el-divider />

    <div class="sync-section">
      <h4>同步广告报告</h4>
      <p class="section-desc">同步指定日期范围内的广告性能报告</p>
      <div class="sync-form">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="form.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-button
          type="primary"
          :disabled="!syncEnabled || !authId || !form.dateRange || form.dateRange.length !== 2"
          :loading="loadingReports"
          @click="$emit('syncReports')"
        >
          同步广告报告
        </el-button>
      </div>
    </div>

    <el-divider />

    <div class="sync-section" v-if="false">
      <h4>记录同步意图（旧功能）</h4>
      <div class="sync-grid">
        <el-form-item label="广告账号">
          <el-select v-model="form.accountId" placeholder="请选择广告账号" style="width: 100%">
            <el-option v-for="item in accountOptions" :key="item.accountId" :label="item.accountName || item.accountId" :value="item.accountId" />
          </el-select>
        </el-form-item>

        <el-form-item label="广告活动">
          <el-select v-model="form.campaignId" clearable placeholder="全部活动" style="width: 100%">
            <el-option v-for="item in campaignOptions" :key="item.campaignId" :label="item.campaignName || item.campaignId" :value="item.campaignId" />
          </el-select>
        </el-form-item>

        <div class="sync-actions">
          <el-button type="primary" :disabled="!syncEnabled" :loading="loading" @click="$emit('submit')">
            记录同步意图
          </el-button>
        </div>
      </div>
    </div>

    <el-table v-if="results.length > 0" :data="results" border size="small" style="margin-top: 16px">
      <el-table-column prop="resultStatus" label="状态" width="100" />
      <el-table-column prop="objectCode" label="对象" min-width="180" show-overflow-tooltip />
      <el-table-column prop="resultMessage" label="结果说明" min-width="260" show-overflow-tooltip />
      <el-table-column label="记录时间" min-width="160">
        <template #default="scope">
          {{ scope.row.createTime ? dateFormat(scope.row.createTime) : '-' }}
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { dateFormat } from '@/utils/index.js';

defineProps({
  syncEnabled: { type: Boolean, default: false },
  syncReason: { type: String, default: '' },
  authId: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  loadingCampaigns: { type: Boolean, default: false },
  loadingReports: { type: Boolean, default: false },
  form: { type: Object, required: true },
  accountOptions: { type: Array, default: () => [] },
  campaignOptions: { type: Array, default: () => [] },
  results: { type: Array, default: () => [] }
});

defineEmits(['submit', 'syncCampaigns', 'syncReports']);
</script>

<style scoped>
.sync-card {
  margin-bottom: 16px;
}

.sync-head {
  font-weight: 600;
}

.sync-alert {
  margin-bottom: 16px;
}

.sync-section {
  margin-bottom: 16px;
}

.sync-section h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
}

.section-desc {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: #666;
}

.sync-form {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.sync-form .el-form-item {
  flex: 1;
  margin-bottom: 0;
}

.sync-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  align-items: end;
  margin-bottom: 16px;
}

.sync-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .sync-grid {
    grid-template-columns: 1fr;
  }

  .sync-form {
    flex-direction: column;
  }
}
</style>
