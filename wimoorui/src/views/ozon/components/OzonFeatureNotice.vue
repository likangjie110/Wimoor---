<template>
  <el-alert
    v-if="item && !item.enabled"
    :title="title || item.reason || '功能未开启'"
    :description="description"
    :type="type"
    :closable="false"
    class="feature-notice"
  />
</template>

<script setup>
defineProps({
  item: { type: Object, default: null },
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  type: { type: String, default: 'warning' }
});
</script>

<style scoped>
.feature-notice {
  margin-bottom: 16px;
}
</style>
