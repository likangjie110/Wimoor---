<template>
  <el-drawer
    v-model="visible"
    title="价格任务历史"
    size="60%"
    :before-close="handleClose"
  >
    <div class="task-history-panel">
      <!-- 错误摘要卡片 -->
      <el-card v-if="errorSummary && Object.keys(errorSummary).length > 0" shadow="never" style="margin-bottom: 16px">
        <template #header>
          <span>错误摘要</span>
        </template>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item v-for="(count, errorType) in errorSummary" :key="errorType" :label="errorType">
            <el-tag type="danger" size="small">{{ count }} 次</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 任务历史列表 -->
      <el-table
        v-loading="loading"
        :data="taskList"
        border
        size="default"
        style="width: 100%"
      >
        <el-table-column prop="taskId" label="任务ID" width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.taskStatus)" size="small">
              {{ formatStatus(scope.row.taskStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="SKU统计" width="140" align="center">
          <template #default="scope">
            <div>
              <el-tag type="success" size="small">✓ {{ scope.row.successCount || 0 }}</el-tag>
              <el-tag type="danger" size="small" style="margin-left: 4px">✗ {{ scope.row.failedCount || 0 }}</el-tag>
            </div>
            <div style="font-size: 12px; color: #909399; margin-top: 2px">
              总数: {{ scope.row.requestedCount || 0 }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="scope">
            {{ formatTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="160">
          <template #default="scope">
            {{ formatTime(scope.row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作员" width="100">
          <template #default="scope">
            {{ scope.row.operator || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openDetail(scope.row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && taskList.length === 0" description="暂无任务历史记录" />
    </div>

    <!-- 任务详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="任务详情"
      width="70%"
      :append-to-body="true"
    >
      <div v-if="currentTask" class="task-detail">
        <!-- 任务基本信息 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务ID">{{ currentTask.taskId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentTask.taskStatus)" size="small">
              {{ formatStatus(currentTask.taskStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总SKU数">{{ currentTask.requestedCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="成功数">
            <el-tag type="success" size="small">{{ currentTask.successCount || 0 }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="失败数">
            <el-tag type="danger" size="small">{{ currentTask.failedCount || 0 }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(currentTask.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(currentTask.updateTime) }}</el-descriptions-item>
          <el-descriptions-item label="操作员">{{ currentTask.operator || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 错误信息 -->
        <div v-if="currentTask.errorMessage" class="error-message">
          <el-divider content-position="left">错误信息</el-divider>
          <el-alert type="error" :closable="false">
            {{ currentTask.errorMessage }}
          </el-alert>
        </div>

        <!-- SKU结果列表 -->
        <div v-if="currentTask.itemResults && currentTask.itemResults.length > 0" class="item-results">
          <el-divider content-position="left">SKU结果</el-divider>
          <el-table :data="currentTask.itemResults" border size="small" max-height="400">
            <el-table-column prop="sku" label="SKU" min-width="140" />
            <el-table-column prop="offerId" label="Offer ID" width="120" />
            <el-table-column prop="price" label="价格" width="100" align="right" />
            <el-table-column prop="oldPrice" label="原价" width="100" align="right" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
                  {{ scope.row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="errorMessage" label="错误信息" min-width="200" show-overflow-tooltip />
          </el-table>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup>
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import priceApi from '@/api/ozon/price/priceApi.js';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  authId: { type: String, required: true },
  limit: { type: Number, default: 50 }
});

const emit = defineEmits(['update:modelValue']);

const visible = ref(false);
const loading = ref(false);
const taskList = ref([]);
const errorSummary = ref({});
const detailDialogVisible = ref(false);
const currentTask = ref(null);

const statusMap = {
  'RUNNING': '执行中',
  'SUCCESS': '执行成功',
  'FAILED': '执行失败',
  'PARTIAL': '部分成功',
  'PENDING': '待执行'
};

watch(() => props.modelValue, (val) => {
  visible.value = val;
  if (val) {
    loadTaskHistory();
    loadErrorSummary();
  }
});

watch(visible, (val) => {
  emit('update:modelValue', val);
});

function handleClose() {
  visible.value = false;
}

async function loadTaskHistory() {
  if (!props.authId) {
    return;
  }

  loading.value = true;
  try {
    const res = await priceApi.listTaskHistory(props.authId, props.limit);
    if (res.code === 200) {
      taskList.value = res.data || [];
    } else {
      ElMessage.error(res.msg || '加载任务历史失败');
    }
  } catch (error) {
    ElMessage.error('加载任务历史失败: ' + (error.message || '未知错误'));
  } finally {
    loading.value = false;
  }
}

async function loadErrorSummary() {
  if (!props.authId) {
    return;
  }

  try {
    const res = await priceApi.getErrorSummary(props.authId);
    if (res.code === 200) {
      errorSummary.value = res.data || {};
    }
  } catch (error) {
    console.warn('加载错误摘要失败:', error);
  }
}

async function openDetail(task) {
  try {
    const res = await priceApi.getTaskDetail(props.authId, task.taskId);
    if (res.code === 200) {
      currentTask.value = res.data;
      detailDialogVisible.value = true;
    } else {
      ElMessage.error(res.msg || '加载任务详情失败');
    }
  } catch (error) {
    ElMessage.error('加载任务详情失败: ' + (error.message || '未知错误'));
  }
}

function formatStatus(status) {
  return statusMap[status] || status;
}

function getStatusType(status) {
  const typeMap = {
    'SUCCESS': 'success',
    'RUNNING': 'primary',
    'FAILED': 'danger',
    'PARTIAL': 'warning',
    'PENDING': 'info'
  };
  return typeMap[status] || 'info';
}

function formatTime(time) {
  if (!time) return '-';
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}
</script>

<style scoped>
.task-history-panel {
  padding: 0 20px;
}

.task-detail {
  padding: 0 20px;
}

.error-message {
  margin-top: 20px;
}

.item-results {
  margin-top: 20px;
}
</style>
