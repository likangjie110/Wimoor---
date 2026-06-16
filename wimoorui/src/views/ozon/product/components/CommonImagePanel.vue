<template>
  <el-card shadow="never" class="section-card">
    <template #header>
      <div class="section-title">
        <span>公共图片</span>
        <el-button link type="primary" @click="addImage">新增</el-button>
      </div>
    </template>

    <el-table :data="images" border>
      <el-table-column label="图片地址" min-width="320">
        <template #default="scope">
          <el-input v-model="scope.row.imageUrl" placeholder="https://..." />
        </template>
      </el-table-column>
      <el-table-column label="来源" width="120">
        <template #default="scope">
          <el-input v-model="scope.row.source" placeholder="ERP" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="scope">
          <el-button link type="danger" @click="removeImage(scope.$index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
const props = defineProps({
  images: { type: Array, default: () => [] }
});

function addImage() {
  props.images.push({ source: 'MANUAL', imageUrl: '', sortOrder: props.images.length, primary: props.images.length === 0 });
}

function removeImage(index) {
  props.images.splice(index, 1);
}
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
