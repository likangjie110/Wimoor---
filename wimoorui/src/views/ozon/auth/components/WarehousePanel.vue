<template>
  <el-card shadow="never" class="section-card">
    <template #header>
      <div class="section-title">
        <span>仓库同步</span>
        <el-space>
          <el-button :disabled="disabled || !authId" @click="$emit('refresh')">刷新列表</el-button>
          <el-button type="primary" :disabled="disabled || !authId" @click="$emit('sync')">同步仓库</el-button>
        </el-space>
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

    <el-table v-loading="loading" :data="warehouses" border>
      <el-table-column prop="warehouseId" label="仓库ID" min-width="140" />
      <el-table-column prop="name" label="仓库名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="warehouseType" label="仓库类型" min-width="120" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column label="默认仓" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.defaultWarehouse" type="success">默认</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="同步时间" min-width="170">
        <template #default="scope">
          {{ scope.row.lastWarehouseSyncTime || scope.row.syncedAt || '-' }}
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
defineProps({
  authOptions: { type: Array, default: () => [] },
  authId: { type: String, default: '' },
  warehouses: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
});

defineEmits(['update:authId', 'refresh', 'sync']);
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
