<template>
  <el-card shadow="never" class="summary-card">
    <template #header>
      <div class="summary-title">当前 Ozon 功能开关</div>
    </template>
    <el-space wrap>
      <el-tooltip
        v-for="item in items"
        :key="item.key"
        :content="item.enabled ? `${item.label}已开启` : (item.reason || `${item.label}未开启`)"
        placement="top"
      >
        <el-tag :type="item.enabled ? 'success' : 'warning'">
          {{ item.label }} · {{ item.enabled ? '开' : '关' }}
        </el-tag>
      </el-tooltip>
    </el-space>
  </el-card>
</template>

<script setup>
defineProps({
  items: { type: Array, default: () => [] }
});
</script>

<style scoped>
.summary-card {
  margin-bottom: 16px;
}

.summary-title {
  font-weight: 600;
}
</style>
