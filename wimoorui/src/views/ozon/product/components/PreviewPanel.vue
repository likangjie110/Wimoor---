<template>
  <el-card shadow="never" class="section-card">
    <template #header>
      <span>预览结果</span>
    </template>

    <el-alert
      :title="preview?.canPublish ? '预检通过，可发版' : '预检未通过'"
      :type="preview?.canPublish ? 'success' : 'warning'"
      :closable="false"
      show-icon
      style="margin-bottom: 12px"
    />

    <el-table v-if="preview?.effectivePayloadSummary?.variants?.length" :data="preview.effectivePayloadSummary.variants" border size="small">
      <el-table-column prop="materialSku" label="SKU" min-width="140" />
      <el-table-column prop="effectiveOfferId" label="Offer ID" min-width="140" />
      <el-table-column prop="effectivePrice" label="价格" width="120" />
      <el-table-column prop="effectiveWeight" label="重量" width="120" />
      <el-table-column prop="effectiveImageCount" label="图片数" width="100" />
    </el-table>

    <el-alert
      v-for="item in preview?.validationErrors || []"
      :key="item"
      :title="item"
      type="error"
      :closable="false"
      style="margin-top: 8px"
    />
  </el-card>
</template>

<script setup>
defineProps({
  preview: { type: Object, default: null }
});
</script>

<style scoped>
.section-card {
  margin-bottom: 16px;
}
</style>
