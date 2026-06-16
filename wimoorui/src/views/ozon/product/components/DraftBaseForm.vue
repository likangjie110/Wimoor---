<template>
  <el-card shadow="never" class="section-card">
    <template #header>
      <div class="section-title">
        <span>基础信息</span>
        <el-button link type="primary" :disabled="saveDisabled" :title="saveDisabled ? saveDisabledReason : ''" @click="$emit('save')">保存草稿</el-button>
      </div>
    </template>

    <el-form label-width="120px">
      <el-form-item label="展示语言">
        <el-select
          :model-value="metadataLanguage"
          :disabled="saveDisabled"
          placeholder="请选择展示语言"
          style="width: 100%"
          @change="$emit('metadata-language-change', $event)"
        >
          <el-option
            v-for="item in metadataLanguageOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="草稿名称">
        <el-input v-model="draftForm.draftName" :disabled="saveDisabled" placeholder="请输入草稿名称" />
      </el-form-item>
      <el-form-item label="类目">
        <el-select v-model="draftForm.descriptionCategoryId" :disabled="saveDisabled" placeholder="请选择类目" style="width: 100%" @change="$emit('category-change', $event)">
          <el-option
            v-for="item in categoryOptions"
            :key="item.descriptionCategoryId"
            :label="item.descriptionCategoryName"
            :value="item.descriptionCategoryId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="draftForm.typeId" :disabled="saveDisabled" placeholder="请选择类型" style="width: 100%" @change="$emit('type-change', $event)">
          <el-option v-for="item in currentTypes" :key="item.typeId" :label="item.typeName" :value="item.typeId" />
        </el-select>
      </el-form-item>
      <el-form-item label="标题覆盖">
        <el-input v-model="draftForm.titleOverrideValue" :disabled="saveDisabled" placeholder="留空则走 ERP 快照" />
      </el-form-item>
      <el-form-item label="品牌覆盖">
        <el-input v-model="draftForm.brandOverrideValue" :disabled="saveDisabled" placeholder="可选" />
      </el-form-item>
      <el-form-item label="描述覆盖">
        <el-input v-model="draftForm.descriptionOverrideValue" :disabled="saveDisabled" type="textarea" :rows="4" placeholder="可选" />
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
defineProps({
  draftForm: { type: Object, required: true },
  categoryOptions: { type: Array, default: () => [] },
  currentTypes: { type: Array, default: () => [] },
  metadataLanguage: { type: String, default: 'ZH_HANS' },
  metadataLanguageOptions: { type: Array, default: () => [] },
  saveDisabled: { type: Boolean, default: false },
  saveDisabledReason: { type: String, default: '' }
});

defineEmits(['save', 'category-change', 'type-change', 'metadata-language-change']);
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
