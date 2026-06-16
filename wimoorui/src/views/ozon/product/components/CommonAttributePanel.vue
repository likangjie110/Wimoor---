<template>
  <el-card shadow="never" class="section-card">
    <template #header>
      <span>公共属性</span>
    </template>

    <el-table :data="attributes" border>
      <el-table-column prop="attributeName" label="属性" min-width="180" />
      <el-table-column label="值" min-width="220">
        <template #default="scope">
          <el-input
            v-if="!isDict(scope.row)"
            v-model="scope.row.valueText"
            :type="isMulti(scope.row) ? 'textarea' : 'text'"
            :rows="isMulti(scope.row) ? 2 : undefined"
            placeholder="请输入值"
          />
          <el-select v-else v-model="scope.row.dictionaryValueId" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="option in scope.row.options || []"
              :key="option.dictionaryValueId"
              :label="option.text"
              :value="option.dictionaryValueId"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column prop="required" label="必填" width="80">
        <template #default="scope">
          {{ scope.row.required ? '是' : '否' }}
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
defineProps({
  attributes: { type: Array, default: () => [] }
});

function isDict(row) {
  return row?.mode && row.mode.includes('DICT');
}

function isMulti(row) {
  return row?.mode && row.mode.includes('MULTI');
}
</script>

<style scoped>
.section-card {
  margin-bottom: 16px;
}
</style>
