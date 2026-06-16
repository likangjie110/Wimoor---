<template>
  <el-card shadow="never" class="sync-card">
    <template #header>
      <div class="sync-head">官方同步预留位</div>
    </template>

    <el-alert
      :title="syncEnabled ? '同步位已开放，可以记录同步意图并追踪结果。' : (syncReason || '官方同步未开启，当前仅可使用本地导入模式。')"
      :type="syncEnabled ? 'success' : 'warning'"
      :closable="false"
      class="sync-alert"
    />

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

      <div class="sync-actions">
        <el-button type="primary" :disabled="!syncEnabled" :loading="loading" @click="$emit('submit')">
          记录同步意图
        </el-button>
      </div>
    </div>

    <el-table :data="results" border size="small">
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
  loading: { type: Boolean, default: false },
  form: { type: Object, required: true },
  accountOptions: { type: Array, default: () => [] },
  campaignOptions: { type: Array, default: () => [] },
  results: { type: Array, default: () => [] }
});

defineEmits(['submit']);
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
}
</style>
