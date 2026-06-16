<template>
  <el-card shadow="never" class="section-card">
    <template #header>
      <div class="section-title">
        <span>发布任务</span>
        <el-space>
          <el-button @click="$emit('preview')" :disabled="moduleDisabled" :title="moduleDisabled ? moduleDisabledReason : ''">预览</el-button>
          <el-button @click="$emit('refresh-task')" :disabled="!taskId || moduleDisabled" :title="moduleDisabled ? moduleDisabledReason : ''">刷新任务</el-button>
          <el-button
            type="primary"
            @click="$emit('publish')"
            :disabled="!draftId || publishDisabled"
            :title="publishDisabled ? publishDisabledReason : ''"
          >
            发布
          </el-button>
        </el-space>
      </div>
    </template>

    <el-descriptions :column="2" border size="small">
      <el-descriptions-item label="草稿ID">{{ draftId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="任务ID">{{ taskId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="任务状态">{{ taskResult?.taskStatus || '-' }}</el-descriptions-item>
      <el-descriptions-item label="远端任务ID">{{ taskResult?.remoteTaskId || '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-alert
      v-if="taskResult?.errorSummary"
      :title="taskResult.errorSummary"
      type="warning"
      :closable="false"
      style="margin-top: 12px"
    />

    <el-table v-if="taskResult?.normalizedItems?.length" :data="taskResult.normalizedItems" border size="small" style="margin-top: 12px">
      <el-table-column prop="offerId" label="Offer ID" min-width="140" />
      <el-table-column prop="productId" label="Product ID" min-width="160" />
      <el-table-column prop="remoteStatus" label="远端状态" width="120" />
      <el-table-column label="错误数" width="100">
        <template #default="scope">
          {{ scope.row.errors?.length || 0 }}
        </template>
      </el-table-column>
    </el-table>

    <el-divider content-position="left">发布任务历史</el-divider>

    <el-table
      v-if="taskHistory?.length"
      :data="taskHistory"
      border
      size="small"
      style="margin-top: 12px"
    >
      <el-table-column prop="taskId" label="本地任务ID" min-width="180" />
      <el-table-column prop="taskStatus" label="状态" width="120" />
      <el-table-column prop="remoteTaskId" label="远端任务ID" min-width="160" />
      <el-table-column prop="errorSummary" label="错误摘要" min-width="200" show-overflow-tooltip />
      <el-table-column label="创建时间" min-width="170">
        <template #default="scope">
          {{ formatTime(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button link type="primary" @click="$emit('select-task', scope.row.taskId)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-else-if="taskHistoryLoaded" description="暂无发布任务历史" :image-size="56" />
  </el-card>
</template>

<script setup>
import { dateFormat } from '@/utils/index.js';

defineProps({
  draftId: { type: String, default: '' },
  taskId: { type: String, default: '' },
  taskResult: { type: Object, default: null },
  taskHistory: { type: Array, default: () => [] },
  taskHistoryLoaded: { type: Boolean, default: false },
  moduleDisabled: { type: Boolean, default: false },
  moduleDisabledReason: { type: String, default: '' },
  publishDisabled: { type: Boolean, default: false },
  publishDisabledReason: { type: String, default: '' }
});

defineEmits(['preview', 'publish', 'refresh-task', 'select-task']);

function formatTime(value) {
  return value ? dateFormat(value) : '-';
}
</script>

<style scoped>
.section-card {
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
