<template>
  <el-card shadow="never" class="section-card">
    <template #header>
      <div class="section-title">
        <span>初始化任务</span>
        <el-button :disabled="disabled || !authId" @click="$emit('refresh')">刷新任务</el-button>
      </div>
    </template>

    <el-form-item label="当前授权">
      <el-select
        :model-value="authId"
        :disabled="disabled"
        placeholder="请选择 Ozon 授权"
        style="width: 100%"
        @change="$emit('update:authId', $event)"
      >
        <el-option v-for="item in authOptions" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
    </el-form-item>

    <el-table v-loading="loading" :data="tasks" border>
      <el-table-column prop="jobType" label="任务类型" min-width="160" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="operator" label="操作人" width="120" />
      <el-table-column prop="payload" label="任务载荷" min-width="220" show-overflow-tooltip />
      <el-table-column label="创建时间" min-width="180">
        <template #default="scope">
          {{ scope.row.createdAt || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="更新时间" min-width="180">
        <template #default="scope">
          {{ scope.row.updatedAt || '-' }}
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
defineProps({
  authOptions: { type: Array, default: () => [] },
  authId: { type: String, default: '' },
  tasks: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
});

defineEmits(['update:authId', 'refresh']);
</script>

<style scoped>
.section-card {
  margin-top: 16px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
