<template>
  <el-card shadow="never" class="section-card">
    <template #header>
      <div class="section-title">
        <span>配送方式</span>
        <el-space>
          <el-button :disabled="disabled || !authId" @click="$emit('refresh')">刷新列表</el-button>
          <el-button @click="$emit('reset-form')">重置表单</el-button>
          <el-button type="primary" :disabled="disabled || !authId" @click="$emit('save')">保存配送方式</el-button>
        </el-space>
      </div>
    </template>

    <el-row :gutter="16">
      <el-col :span="8">
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
      </el-col>
      <el-col :span="8">
        <el-form-item label="方式编码">
          <el-input v-model="form.methodCode" :disabled="disabled" placeholder="例如 fbs-main" />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="方式名称">
          <el-input v-model="form.methodName" :disabled="disabled" placeholder="例如 FBS 主配送" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-form-item label="说明">
          <el-input v-model="form.description" :disabled="disabled" placeholder="可选说明" />
        </el-form-item>
      </el-col>
      <el-col :span="6">
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :disabled="disabled" />
        </el-form-item>
      </el-col>
      <el-col :span="6">
        <el-form-item label="默认">
          <el-switch v-model="form.defaultMethod" :disabled="disabled" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="methods" border @row-click="$emit('select-method', $event)">
      <el-table-column prop="methodCode" label="方式编码" min-width="140" />
      <el-table-column prop="methodName" label="方式名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
      <el-table-column label="启用" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="默认" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.defaultMethod" type="success">默认</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
defineProps({
  authOptions: { type: Array, default: () => [] },
  authId: { type: String, default: '' },
  methods: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  form: { type: Object, required: true }
});

defineEmits(['update:authId', 'refresh', 'save', 'select-method', 'reset-form']);
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
