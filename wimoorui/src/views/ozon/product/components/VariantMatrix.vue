<template>
  <el-card shadow="never" class="section-card">
    <template #header>
      <span>变体矩阵</span>
    </template>

    <div v-for="variant in variants" :key="variant.variantId || variant.materialSku" class="variant-card">
      <div class="variant-title">{{ variant.materialSku }} / {{ variant.materialName || '-' }}</div>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="Offer ID">
            <el-input v-model="variant.offerIdOverride" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="Barcode">
            <el-input v-model="variant.barcodeOverride" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="标签">
            <el-input v-model="variant.variantLabel" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="12">
        <el-col :span="6"><el-form-item label="价格"><el-input v-model="variant.priceOverride" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="重量"><el-input v-model="variant.weightOverrideValue" /></el-form-item></el-col>
        <el-col :span="4"><el-form-item label="长"><el-input v-model="variant.lengthOverrideValue" /></el-form-item></el-col>
        <el-col :span="4"><el-form-item label="宽"><el-input v-model="variant.widthOverrideValue" /></el-form-item></el-col>
        <el-col :span="4"><el-form-item label="高"><el-input v-model="variant.heightOverrideValue" /></el-form-item></el-col>
      </el-row>

      <el-table :data="variant.attributes || []" border size="small" style="margin-bottom: 12px">
        <el-table-column prop="attributeName" label="变体属性" min-width="160" />
        <el-table-column label="值" min-width="220">
          <template #default="scope">
            <el-input
              v-if="!isDict(scope.row)"
              v-model="scope.row.valueText"
              :type="isMulti(scope.row) ? 'textarea' : 'text'"
              :rows="isMulti(scope.row) ? 2 : undefined"
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
      </el-table>

      <el-table :data="variant.images || []" border size="small">
        <el-table-column label="图片地址" min-width="300">
          <template #default="scope">
            <el-input v-model="scope.row.imageUrl" />
          </template>
        </el-table-column>
        <el-table-column label="来源" width="120">
          <template #default="scope">
            <el-input v-model="scope.row.source" />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-card>
</template>

<script setup>
defineProps({
  variants: { type: Array, default: () => [] }
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

.variant-card {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.variant-title {
  font-weight: 600;
  margin-bottom: 12px;
}
</style>
