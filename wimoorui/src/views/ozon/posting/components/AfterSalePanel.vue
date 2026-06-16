<template>
  <div class="after-sale-panel">
    <div class="section-head">
      <span>售后对象</span>
      <el-space>
        <el-button size="small" @click="$emit('refresh')">刷新</el-button>
        <el-button size="small" :disabled="disabled" @click="openPackageDialog()">新增包裹</el-button>
        <el-button size="small" :disabled="disabled" @click="openReturnDialog()">新增退货</el-button>
        <el-button size="small" :disabled="disabled" @click="openCancellationDialog()">新增取消</el-button>
      </el-space>
    </div>

    <el-divider content-position="left">包裹</el-divider>
    <el-table :data="detail?.packages || []" border size="small">
      <el-table-column prop="packageNumber" label="包裹号" min-width="160" />
      <el-table-column prop="packageStatus" label="状态" width="120" />
      <el-table-column prop="trackingNumber" label="追踪号" min-width="160" />
      <el-table-column label="更新时间" min-width="180">
        <template #default="scope">
          {{ formatTime(scope.row.updatedAt || scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button link type="primary" @click="openPackageDialog(scope.row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-divider content-position="left">退货</el-divider>
    <el-table :data="detail?.returns || []" border size="small">
      <el-table-column prop="returnNumber" label="退货号" min-width="160" />
      <el-table-column prop="returnStatus" label="状态" width="120" />
      <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
      <el-table-column prop="quantity" label="数量" width="90" />
      <el-table-column label="更新时间" min-width="180">
        <template #default="scope">
          {{ formatTime(scope.row.updatedAt || scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button link type="primary" @click="openReturnDialog(scope.row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-divider content-position="left">取消</el-divider>
    <el-table :data="detail?.cancellations || []" border size="small">
      <el-table-column prop="cancellationNumber" label="取消单号" min-width="160" />
      <el-table-column prop="cancellationStatus" label="状态" width="120" />
      <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
      <el-table-column label="更新时间" min-width="180">
        <template #default="scope">
          {{ formatTime(scope.row.updatedAt || scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button link type="primary" @click="openCancellationDialog(scope.row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="packageDialog.visible" title="包裹记录" width="520px">
      <el-form label-width="110px">
        <el-form-item label="包裹号">
          <el-input v-model="packageDialog.form.packageNumber" />
        </el-form-item>
        <el-form-item label="状态">
          <el-input v-model="packageDialog.form.packageStatus" />
        </el-form-item>
        <el-form-item label="追踪号">
          <el-input v-model="packageDialog.form.trackingNumber" />
        </el-form-item>
        <el-form-item label="原始载荷">
          <el-input v-model="packageDialog.form.rawPayloadJson" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="packageDialog.visible = false">取消</el-button>
        <el-button type="primary" :disabled="disabled" @click="emitPackageSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="returnDialog.visible" title="退货记录" width="520px">
      <el-form label-width="110px">
        <el-form-item label="退货号">
          <el-input v-model="returnDialog.form.returnNumber" />
        </el-form-item>
        <el-form-item label="状态">
          <el-input v-model="returnDialog.form.returnStatus" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="returnDialog.form.reason" />
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="returnDialog.form.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原始载荷">
          <el-input v-model="returnDialog.form.rawPayloadJson" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnDialog.visible = false">取消</el-button>
        <el-button type="primary" :disabled="disabled" @click="emitReturnSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="cancellationDialog.visible" title="取消记录" width="520px">
      <el-form label-width="110px">
        <el-form-item label="取消单号">
          <el-input v-model="cancellationDialog.form.cancellationNumber" />
        </el-form-item>
        <el-form-item label="状态">
          <el-input v-model="cancellationDialog.form.cancellationStatus" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="cancellationDialog.form.reason" />
        </el-form-item>
        <el-form-item label="原始载荷">
          <el-input v-model="cancellationDialog.form.rawPayloadJson" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancellationDialog.visible = false">取消</el-button>
        <el-button type="primary" :disabled="disabled" @click="emitCancellationSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive } from 'vue';
import { dateFormat } from '@/utils/index.js';

const props = defineProps({
  detail: { type: Object, default: null },
  disabled: { type: Boolean, default: false }
});

const emit = defineEmits(['refresh', 'save-package', 'save-return', 'save-cancellation']);

const packageDialog = reactive({
  visible: false,
  form: createPackageForm()
});

const returnDialog = reactive({
  visible: false,
  form: createReturnForm()
});

const cancellationDialog = reactive({
  visible: false,
  form: createCancellationForm()
});

function createPackageForm() {
  return {
    id: '',
    packageNumber: '',
    packageStatus: '',
    trackingNumber: '',
    rawPayloadJson: ''
  };
}

function createReturnForm() {
  return {
    id: '',
    returnNumber: '',
    returnStatus: '',
    reason: '',
    quantity: 1,
    rawPayloadJson: ''
  };
}

function createCancellationForm() {
  return {
    id: '',
    cancellationNumber: '',
    cancellationStatus: '',
    reason: '',
    rawPayloadJson: ''
  };
}

function openPackageDialog(row = null) {
  packageDialog.form = row ? {
    id: row.id || '',
    packageNumber: row.packageNumber || '',
    packageStatus: row.packageStatus || '',
    trackingNumber: row.trackingNumber || '',
    rawPayloadJson: row.rawPayloadJson || ''
  } : createPackageForm();
  packageDialog.visible = true;
}

function openReturnDialog(row = null) {
  returnDialog.form = row ? {
    id: row.id || '',
    returnNumber: row.returnNumber || '',
    returnStatus: row.returnStatus || '',
    reason: row.reason || '',
    quantity: row.quantity || 1,
    rawPayloadJson: row.rawPayloadJson || ''
  } : createReturnForm();
  returnDialog.visible = true;
}

function openCancellationDialog(row = null) {
  cancellationDialog.form = row ? {
    id: row.id || '',
    cancellationNumber: row.cancellationNumber || '',
    cancellationStatus: row.cancellationStatus || '',
    reason: row.reason || '',
    rawPayloadJson: row.rawPayloadJson || ''
  } : createCancellationForm();
  cancellationDialog.visible = true;
}

function emitPackageSave() {
  emit('save-package', { ...packageDialog.form });
  packageDialog.visible = false;
}

function emitReturnSave() {
  emit('save-return', { ...returnDialog.form });
  returnDialog.visible = false;
}

function emitCancellationSave() {
  emit('save-cancellation', { ...cancellationDialog.form });
  cancellationDialog.visible = false;
}

function formatTime(value) {
  return value ? dateFormat(value) : '-';
}
</script>

<style scoped>
.after-sale-panel {
  margin-top: 16px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}
</style>
