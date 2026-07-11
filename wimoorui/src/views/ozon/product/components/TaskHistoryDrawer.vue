<template>
  <el-drawer
    v-model="visible"
    title="发布任务历史"
    size="60%"
    :before-close="handleClose"
  >
    <div class="task-history-drawer">
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
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ formatStatus(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变体统计" width="140" align="center">
          <template #default="scope">
            <div>
              <el-tag type="success" size="small">✓ {{ scope.row.successCount || 0 }}</el-tag>
              <el-tag type="danger" size="small" style="margin-left: 4px">✗ {{ scope.row.failedCount || 0 }}</el-tag>
            </div>
            <div style="font-size: 12px; color: #909399; margin-top: 2px">
              总数: {{ scope.row.totalVariants || 0 }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="scope">
            {{ formatTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="完成时间" min-width="160">
          <template #default="scope">
            {{ formatTime(scope.row.completeTime) }}
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100" align="center">
          <template #default="scope">
            {{ calculateDuration(scope.row.createTime, scope.row.completeTime) }}
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
          <el-descriptions-item label="草稿ID">{{ currentTask.draftId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentTask.status)" size="small">
              {{ formatStatus(currentTask.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总变体数">{{ currentTask.totalVariants || 0 }}</el-descriptions-item>
          <el-descriptions-item label="成功数">
            <el-tag type="success" size="small">{{ currentTask.successCount || 0 }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="失败数">
            <el-tag type="danger" size="small">{{ currentTask.failedCount || 0 }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(currentTask.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatTime(currentTask.completeTime) }}</el-descriptions-item>
        </el-descriptions>

        <!-- 错误摘要 -->
        <div v-if="currentTask.errorSummary" class="error-summary">
          <el-divider content-position="left">错误摘要</el-divider>
          <el-alert type="error" :closable="false">
            {{ currentTask.errorSummary }}
          </el-alert>
        </div>

        <!-- 变体结果列表 -->
        <div v-if="currentTask.variantResults && currentTask.variantResults.length > 0" class="variant-results">
          <el-divider content-position="left">变体结果</el-divider>
          <el-table :data="currentTask.variantResults" border size="small" max-height="400">
            <el-table-column prop="variantSku" label="变体SKU" min-width="140" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
                  {{ scope.row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ozonProductId" label="OZON商品ID" width="140" />
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
import productApi from '@/api/ozon/product/productApi.js';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  authId: { type: String, required: true },
  draftId: { type: String, required: true }
});

const emit = defineEmits(['update:modelValue']);

const visible = ref(false);
const loading = ref(false);
const taskList = ref([]);
const detailDialogVisible = ref(false);
const currentTask = ref(null);

const statusMap = {
  'RUNNING': '执行中',
  'SUCCESS': '执行成功',
  'FAILED': '执行失败',
  'PARTIAL': '部分成功'
};

watch(() => props.modelValue, (val) => {
  visible.value = val;
  if (val) {
    loadTaskHistory();
  }
});

watch(visible, (val) => {
  emit('update:modelValue', val);
});

function handleClose() {
  visible.value = false;
}

async function loadTaskHistory() {
  if (!props.authId || !props.draftId) {
    return;
  }

  loading.value = true;
  try {
    const res = await productApi.getTaskHistory(props.authId, props.draftId);
    taskList.value = res.data || [];
  } catch (error) {
    ElMessage.error('加载任务历史失败: ' + (error.message || '未知错误'));
  } finally {
    loading.value = false;
  }
}

async function openDetail(task) {
  try {
    const res = await productApi.getTaskDetailNew(props.authId, task.taskId);
    currentTask.value = res.data;
    detailDialogVisible.value = true;
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
    'PARTIAL': 'warning'
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

function calculateDuration(startTime, endTime) {
  if (!startTime || !endTime) return '-';

  const start = new Date(startTime).getTime();
  const end = new Date(endTime).getTime();
  const duration = end - start;

  if (duration < 0) return '-';

  const seconds = Math.floor(duration / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);

  if (hours > 0) {
    return `${hours}h ${minutes % 60}m`;
  } else if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`;
  } else {
    return `${seconds}s`;
  }
}
</script>

<style scoped>
.task-history-drawer {
  padding: 0 20px;
}

.task-detail {
  padding: 0 20px;
}

.error-summary {
  margin-top: 20px;
}

.variant-results {
  margin-top: 20px;
}
</style>
