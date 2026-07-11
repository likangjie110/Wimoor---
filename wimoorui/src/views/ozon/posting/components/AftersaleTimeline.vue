<template>
  <div class="aftersale-timeline">
    <div class="timeline-header">
      <span class="timeline-title">售后时间线</span>
      <el-space>
        <el-button size="small" @click="$emit('refresh')">刷新</el-button>
        <el-button size="small" type="success" :loading="syncing.packages" @click="handleSyncPackages">同步包裹</el-button>
        <el-button size="small" type="success" :loading="syncing.returns" @click="handleSyncReturns">同步退货</el-button>
      </el-space>
    </div>

    <el-empty v-if="isEmpty" description="暂无售后记录" />

    <el-timeline v-else>
      <!-- 包裹跟踪记录 -->
      <el-timeline-item
        v-for="pkg in packages"
        :key="`pkg-${pkg.id}`"
        :timestamp="formatTime(pkg.updatedAt || pkg.createdAt)"
        placement="top"
        color="#67c23a"
      >
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-icon">📦</span>
              <span class="card-title">包裹跟踪</span>
              <el-tag size="small" :type="getPackageStatusType(pkg.packageStatus)">
                {{ pkg.packageStatus || '未知状态' }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="包裹号">
              {{ pkg.packageNumber || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="追踪号">
              {{ pkg.trackingNumber || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-timeline-item>

      <!-- 退货申请记录 -->
      <el-timeline-item
        v-for="ret in returns"
        :key="`ret-${ret.id}`"
        :timestamp="formatTime(ret.updatedAt || ret.createdAt)"
        placement="top"
        color="#e6a23c"
      >
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-icon">↩️</span>
              <span class="card-title">退货申请</span>
              <el-tag size="small" type="warning" :type="getReturnStatusType(ret.returnStatus)">
                {{ ret.returnStatus || '未知状态' }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="退货号">
              {{ ret.returnNumber || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="数量">
              {{ ret.quantity || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="退货原因" :span="2">
              {{ ret.reason || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-timeline-item>

      <!-- 取消记录 -->
      <el-timeline-item
        v-for="cancel in cancellations"
        :key="`cancel-${cancel.id}`"
        :timestamp="formatTime(cancel.updatedAt || cancel.createdAt)"
        placement="top"
        color="#f56c6c"
      >
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-icon">❌</span>
              <span class="card-title">订单取消</span>
              <el-tag size="small" type="danger" :type="getCancellationStatusType(cancel.cancellationStatus)">
                {{ cancel.cancellationStatus || '未知状态' }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="取消单号">
              {{ cancel.cancellationNumber || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              {{ cancel.cancellationStatus || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="取消原因" :span="2">
              {{ cancel.reason || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue';
import { dateFormat } from '@/utils/index.js';
import { ElMessage } from 'element-plus';
import {
  syncPackagesFromApi,
  syncReturnsFromApi
} from '@/api/ozon/aftersale/aftersaleApi';

const props = defineProps({
  authId: { type: String, required: true },
  postingId: { type: String, required: true },
  packages: { type: Array, default: () => [] },
  returns: { type: Array, default: () => [] },
  cancellations: { type: Array, default: () => [] }
});

const emit = defineEmits(['refresh']);

const syncing = reactive({
  packages: false,
  returns: false
});

// 计算是否为空
const isEmpty = computed(() => {
  return (
    (!props.packages || props.packages.length === 0) &&
    (!props.returns || props.returns.length === 0) &&
    (!props.cancellations || props.cancellations.length === 0)
  );
});

// 格式化时间
function formatTime(value) {
  return value ? dateFormat(value) : '-';
}

// 包裹状态类型
function getPackageStatusType(status) {
  const statusMap = {
    'delivered': 'success',
    'in_transit': 'primary',
    'pending': 'info',
    'failed': 'danger'
  };
  return statusMap[status?.toLowerCase()] || 'info';
}

// 退货状态类型
function getReturnStatusType(status) {
  const statusMap = {
    'approved': 'success',
    'pending': 'warning',
    'rejected': 'danger',
    'completed': 'success'
  };
  return statusMap[status?.toLowerCase()] || 'warning';
}

// 取消状态类型
function getCancellationStatusType(status) {
  const statusMap = {
    'completed': 'info',
    'pending': 'warning',
    'failed': 'danger'
  };
  return statusMap[status?.toLowerCase()] || 'danger';
}

// 同步包裹
async function handleSyncPackages() {
  syncing.packages = true;
  try {
    const res = await syncPackagesFromApi(props.authId, props.postingId);
    if (res.code === 200) {
      ElMessage.success('包裹同步成功');
      emit('refresh');
    } else {
      ElMessage.error(res.message || '包裹同步失败');
    }
  } catch (error) {
    ElMessage.error('包裹同步失败：' + error.message);
  } finally {
    syncing.packages = false;
  }
}

// 同步退货
async function handleSyncReturns() {
  syncing.returns = true;
  try {
    const res = await syncReturnsFromApi(props.authId, props.postingId);
    if (res.code === 200) {
      ElMessage.success('退货同步成功');
      emit('refresh');
    } else {
      ElMessage.error(res.message || '退货同步失败');
    }
  } catch (error) {
    ElMessage.error('退货同步失败：' + error.message);
  } finally {
    syncing.returns = false;
  }
}
</script>

<style scoped>
.aftersale-timeline {
  padding: 16px;
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.timeline-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-icon {
  font-size: 20px;
}

.card-title {
  font-weight: 600;
  flex: 1;
}

:deep(.el-timeline-item__wrapper) {
  padding-left: 24px;
}

:deep(.el-card) {
  margin-bottom: 0;
}

:deep(.el-card__header) {
  padding: 12px 16px;
  background-color: #fafafa;
}

:deep(.el-card__body) {
  padding: 16px;
}
</style>