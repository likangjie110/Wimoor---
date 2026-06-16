<template>
  <el-card shadow="never" class="mode-card">
    <div class="mode-header">
      <div>
        <div class="mode-title">{{ title }}</div>
        <div class="mode-description">{{ description }}</div>
      </div>
      <el-tag type="success">当前主模式</el-tag>
    </div>

    <div class="mode-grid">
      <div class="mode-item mode-item-local">
        <div class="mode-item-head">
          <span>{{ localTitle }}</span>
          <el-tag size="small" type="success">可用</el-tag>
        </div>
        <div class="mode-item-text">{{ localDescription }}</div>
      </div>

      <div class="mode-item" :class="remoteEnabled ? 'mode-item-remote-enabled' : 'mode-item-remote-disabled'">
        <div class="mode-item-head">
          <span>{{ remoteTitle }}</span>
          <el-tag size="small" :type="remoteEnabled ? 'primary' : 'warning'">
            {{ remoteEnabled ? '已预留' : '待开启' }}
          </el-tag>
        </div>
        <div class="mode-item-text">{{ remoteDescription }}</div>
        <div v-if="remoteReason" class="mode-item-reason">{{ remoteReason }}</div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  localTitle: { type: String, default: '本地模式' },
  localDescription: { type: String, default: '' },
  remoteTitle: { type: String, default: '官方模式' },
  remoteDescription: { type: String, default: '' },
  remoteEnabled: { type: Boolean, default: false },
  remoteReason: { type: String, default: '' }
});
</script>

<style scoped>
.mode-card {
  margin-bottom: 16px;
}

.mode-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.mode-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 6px;
}

.mode-description {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.mode-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.mode-item {
  border-radius: 12px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-light);
}

.mode-item-local {
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.12), rgba(103, 194, 58, 0.04));
}

.mode-item-remote-enabled {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.12), rgba(64, 158, 255, 0.04));
}

.mode-item-remote-disabled {
  background: linear-gradient(135deg, rgba(230, 162, 60, 0.12), rgba(230, 162, 60, 0.04));
}

.mode-item-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  font-weight: 600;
}

.mode-item-text {
  font-size: 13px;
  line-height: 1.7;
  color: var(--el-text-color-regular);
}

.mode-item-reason {
  margin-top: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

@media (max-width: 900px) {
  .mode-grid {
    grid-template-columns: 1fr;
  }
}
</style>
