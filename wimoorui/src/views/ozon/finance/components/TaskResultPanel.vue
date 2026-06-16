<template>
  <el-card shadow="never" class="task-result-card">
    <template #header>
      <div class="task-result-head">
        <div>最近导入结果</div>
        <el-button link type="primary" @click="$emit('refresh')">刷新</el-button>
      </div>
    </template>

    <el-empty v-if="!task" description="暂无导入任务" />

    <template v-else>
      <div class="task-grid">
        <div class="task-item">
          <div class="task-label">报表ID</div>
          <div class="task-value">{{ task.reportId || '-' }}</div>
        </div>
        <div class="task-item">
          <div class="task-label">状态</div>
          <div class="task-value">{{ task.taskStatus || '-' }}</div>
        </div>
        <div class="task-item">
          <div class="task-label">模式</div>
          <div class="task-value">{{ task.sourceMode || '-' }}</div>
        </div>
        <div class="task-item">
          <div class="task-label">导入条数</div>
          <div class="task-value">{{ task.importedCount ?? '-' }}</div>
        </div>
      </div>

      <el-alert
        :title="task.rawContentReady ? '原文已保留，可随时回看原始报表。' : '当前任务没有可查看的原文。'"
        type="info"
        :closable="false"
        class="task-alert"
      />

      <div class="task-footer">
        <div class="task-footer-text">
          更新时间：{{ task.updatedAt ? dateFormat(task.updatedAt) : '-' }}
        </div>
        <el-button
          v-if="task.rawContentReady"
          link
          type="primary"
          @click="$emit('open-raw', task)"
        >
          查看原文
        </el-button>
      </div>
    </template>
  </el-card>
</template>

<script setup>
import { dateFormat } from '@/utils/index.js';

defineProps({
  task: { type: Object, default: null }
});

defineEmits(['refresh', 'open-raw']);
</script>

<style scoped>
.task-result-card {
  margin-bottom: 16px;
}

.task-result-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.task-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.task-item {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  padding: 12px;
}

.task-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.task-value {
  font-size: 15px;
  font-weight: 600;
  word-break: break-word;
}

.task-alert {
  margin-bottom: 12px;
}

.task-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.task-footer-text {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

@media (max-width: 900px) {
  .task-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
