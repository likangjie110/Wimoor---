<template>
  <div class="main-sty monitoring-dashboard">
    <el-row :gutter="16">
      <!-- 系统健康状态 -->
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <div class="card-title">
              <span>系统健康状态</span>
              <el-button @click="handleRefresh">刷新</el-button>
            </div>
          </template>

          <el-row :gutter="16">
            <el-col :span="8">
              <div class="health-item">
                <div class="health-label">系统状态</div>
                <div class="health-value">
                  <el-tag :type="healthStatus.system === 'UP' ? 'success' : 'danger'" size="large">
                    {{ healthStatus.system === 'UP' ? '正常' : '异常' }}
                  </el-tag>
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="health-item">
                <div class="health-label">数据库</div>
                <div class="health-value">
                  <el-tag :type="healthStatus.database === 'UP' ? 'success' : 'danger'" size="large">
                    {{ healthStatus.database === 'UP' ? '正常' : '异常' }}
                  </el-tag>
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="health-item">
                <div class="health-label">OZON API</div>
                <div class="health-value">
                  <el-tag :type="healthStatus.ozonApi === 'UP' ? 'success' : 'warning'" size="large">
                    {{ healthStatus.ozonApi === 'UP' ? '正常' : '降级' }}
                  </el-tag>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- 关键指标 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="API 调用总数" :value="metrics.apiTotalCalls">
            <template #suffix>
              <span style="font-size: 14px; color: #909399">次</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="API 成功率" :value="metrics.apiSuccessRate">
            <template #suffix>
              <span style="font-size: 14px; color: #67c23a">%</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="平均响应时间" :value="metrics.avgResponseTime">
            <template #suffix>
              <span style="font-size: 14px; color: #409eff">ms</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="系统错误率" :value="metrics.errorRate">
            <template #suffix>
              <span style="font-size: 14px" :style="{ color: metrics.errorRate > 1 ? '#f56c6c' : '#67c23a' }">%</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- 模块调用统计 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span>模块调用统计（最近24小时）</span>
          </template>
          <el-table :data="moduleStats" border>
            <el-table-column prop="module" label="模块" width="120" />
            <el-table-column prop="calls" label="调用次数" width="100" />
            <el-table-column prop="successRate" label="成功率" width="100">
              <template #default="{ row }">
                <span :style="{ color: row.successRate >= 95 ? '#67c23a' : '#f56c6c' }">
                  {{ row.successRate }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="avgDuration" label="平均耗时" width="100">
              <template #default="{ row }">
                {{ row.avgDuration }}ms
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span>操作审计统计（最近24小时）</span>
          </template>
          <el-table :data="auditStats" border>
            <el-table-column prop="operationType" label="操作类型" width="120">
              <template #default="{ row }">
                <el-tag size="small">{{ row.operationType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="count" label="操作次数" width="100" />
            <el-table-column prop="successRate" label="成功率" width="100">
              <template #default="{ row }">
                <span :style="{ color: row.successRate >= 95 ? '#67c23a' : '#f56c6c' }">
                  {{ row.successRate }}%
                </span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近错误 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <div class="card-title">
              <span>最近错误（最近1小时）</span>
              <el-button link type="primary" @click="goToApiLog">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentErrors" border>
            <el-table-column prop="module" label="模块" width="100" />
            <el-table-column prop="operation" label="操作" width="150" />
            <el-table-column prop="errorMessage" label="错误信息" min-width="300" show-overflow-tooltip />
            <el-table-column prop="occurredAt" label="发生时间" width="170">
              <template #default="{ row }">
                {{ formatTime(row.occurredAt) }}
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="recentErrors.length === 0" description="暂无错误" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import authApi from '@/api/ozon/auth/authApi.js';
import opsApi from '@/api/ozon/ops/opsApi.js';

const router = useRouter();
const authId = ref('');

const healthStatus = reactive({
  system: 'UP',
  database: 'UP',
  ozonApi: 'UP'
});

const metrics = reactive({
  apiTotalCalls: 0,
  apiSuccessRate: 0,
  avgResponseTime: 0,
  errorRate: 0
});

const moduleStats = ref([]);
const auditStats = ref([]);
const recentErrors = ref([]);

let refreshTimer = null;

onMounted(() => {
  loadAuths();

  // 每30秒自动刷新
  refreshTimer = setInterval(() => {
    loadDashboard();
  }, 30000);
});

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer);
  }
});

async function loadAuths() {
  try {
    const res = await authApi.list();
    const auths = res.data || [];
    authId.value = auths[0]?.id || '';
    await loadDashboard();
  } catch (error) {
    console.error('加载授权失败', error);
  }
}

async function loadDashboard() {
  if (!authId.value) {
    return;
  }
  try {
    const res = await opsApi.dashboard(authId.value);
    const data = res.data || {};
    healthStatus.system = data.health?.application || 'UP';
    healthStatus.database = data.health?.database || 'UP';
    healthStatus.ozonApi = data.health?.ozonApi || 'UNKNOWN';
    metrics.apiTotalCalls = data.metrics?.apiTotalCalls || 0;
    metrics.apiSuccessRate = data.metrics?.apiSuccessRate || 0;
    metrics.avgResponseTime = data.metrics?.avgResponseTime || 0;
    metrics.errorRate = data.metrics?.errorRate || 0;
    moduleStats.value = data.moduleStats || [];
    auditStats.value = data.auditStats || [];
    recentErrors.value = data.recentErrors || [];
  } catch (error) {
    console.error('加载监控数据失败', error);
  }
}

function handleRefresh() {
  loadDashboard();
  ElMessage.success('刷新成功');
}

function goToApiLog() {
  router.push('/ozon/error');
}

function formatTime(time) {
  if (!time) return '-';
  return new Date(time).toLocaleString('zh-CN');
}
</script>

<style scoped>
.monitoring-dashboard {
  min-height: calc(100vh - 140px);
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.health-item {
  text-align: center;
  padding: 20px 0;
}

.health-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
}

.health-value {
  font-size: 18px;
  font-weight: 500;
}
</style>
