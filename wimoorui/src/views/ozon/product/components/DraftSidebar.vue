<template>
  <el-card shadow="never" class="section-card">
    <template #header>
      <div class="section-title">
        <span>草稿列表</span>
        <el-button link type="primary" :disabled="disabled" :title="disabled ? disabledReason : ''" @click="$emit('refresh')">刷新</el-button>
      </div>
    </template>

    <el-form-item label="授权店铺">
      <el-select :model-value="authId" placeholder="请选择 Ozon 授权" style="width: 100%" :disabled="disabled" @change="$emit('update:authId', $event)">
        <el-option v-for="item in authOptions" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
    </el-form-item>

    <el-form-item label="新草稿名">
      <el-input :model-value="importDraftName" :disabled="disabled" placeholder="例如 Books Spring Batch" @update:model-value="$emit('update:importDraftName', $event)" />
    </el-form-item>

    <el-form-item label="导入 SKU">
      <el-input
        :model-value="importSkuText"
        :disabled="disabled"
        type="textarea"
        :rows="4"
        placeholder="每行一个 SKU，或使用逗号分隔"
        @update:model-value="$emit('update:importSkuText', $event)"
      />
    </el-form-item>

    <el-button type="primary" style="width: 100%; margin-bottom: 12px" :disabled="disabled" :title="disabled ? disabledReason : ''" @click="$emit('import')">导入草稿</el-button>

    <el-scrollbar max-height="520px">
      <div
        v-for="item in drafts"
        :key="item.draftId"
        class="draft-item"
        :class="{ active: item.draftId === selectedDraftId }"
        @click="$emit('selectDraft', item.draftId)"
      >
        <div class="draft-name">{{ item.draftName || item.draftId }}</div>
        <div class="draft-meta">{{ item.descriptionCategoryName || '-' }} / {{ item.typeName || '-' }}</div>
        <div class="draft-meta">{{ item.status }} · {{ item.variantCount || 0 }} variants</div>
      </div>
      <el-empty v-if="!loading && drafts.length === 0" description="暂无草稿" :image-size="64" />
    </el-scrollbar>
  </el-card>
</template>

<script setup>
defineProps({
  authOptions: { type: Array, default: () => [] },
  authId: { type: String, default: '' },
  drafts: { type: Array, default: () => [] },
  importDraftName: { type: String, default: '' },
  importSkuText: { type: String, default: '' },
  selectedDraftId: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  disabledReason: { type: String, default: '' }
});

defineEmits([
  'update:authId',
  'update:importDraftName',
  'update:importSkuText',
  'refresh',
  'import',
  'selectDraft'
]);
</script>

<style scoped>
.section-card {
  height: 100%;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.draft-item {
  padding: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
}

.draft-item.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.draft-name {
  font-weight: 600;
  margin-bottom: 4px;
}

.draft-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
