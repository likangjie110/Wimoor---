<template>
  <div class="reply-composer">
    <el-alert
      :title="sendEnabled ? '当前会真实发送到 Ozon，并保留本地审计。' : (sendReason || '当前仅支持本地回复审计，不会真正发送到 Ozon。')"
      :type="sendEnabled ? 'success' : 'warning'"
      :closable="false"
      class="composer-alert"
    />

    <el-input
      :model-value="modelValue"
      type="textarea"
      :rows="4"
      :placeholder="sendEnabled ? '输入回复内容，提交后会发送到 Ozon。' : '输入回复内容，当前只保留本地审计记录。'"
      @update:model-value="$emit('update:modelValue', $event)"
    />

    <div class="composer-actions">
      <el-button type="primary" :loading="loading" @click="$emit('submit')">{{ sendEnabled ? '发送并记录' : '保存回复审计' }}</el-button>
    </div>

    <el-card shadow="never" class="audit-card">
      <template #header>最近回复审计</template>
      <el-empty v-if="!audits || audits.length === 0" description="暂无回复审计" />
      <el-table v-else :data="audits" border size="small">
        <el-table-column prop="replyStatus" label="状态" width="100" />
        <el-table-column prop="replyText" label="回复内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column label="时间" min-width="160">
          <template #default="scope">
            {{ scope.row.createTime ? dateFormat(scope.row.createTime) : '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { dateFormat } from '@/utils/index.js';

defineProps({
  modelValue: { type: String, default: '' },
  sendEnabled: { type: Boolean, default: false },
  sendReason: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  audits: { type: Array, default: () => [] }
});

defineEmits(['update:modelValue', 'submit']);
</script>

<style scoped>
.composer-alert {
  margin-bottom: 12px;
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.audit-card {
  margin-top: 16px;
}
</style>
