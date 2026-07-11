<template>
  <el-card shadow="never" class="section-card">
    <template #header>
      <div class="section-title">
        <span>草稿列表</span>
        <el-button link type="primary" :disabled="disabled" :title="disabled ? disabledReason : ''" @click="$emit('refresh')">刷新</el-button>
      </div>
    </template>

    <el-form-item label="授权店铺">
      <el-select :model-value="authId" placeholder="请选择 Ozon 授权" style="width: 100%" :disabled="disabled" @change="$emit('update:authId', $event)">
        <el-option v-for="item in authOptions" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
    </el-form-item>

    <el-form-item label="新草稿名">
      <el-input :model-value="importDraftName" :disabled="disabled" placeholder="例如 Books Spring Batch" @update:model-value="$emit('update:importDraftName', $event)" />
    </el-form-item>

    <el-form-item label="导入 SKU">
      <el-input
        :model-value="importSkuText"
        :disabled="disabled"
        type="textarea"
        :rows="4"
        placeholder="每行一个 SKU，或使用逗号分隔"
        @update:model-value="$emit('update:importSkuText', $event)"
      />
    </el-form-item>

    <el-button type="primary" style="width: 100%; margin-bottom: 12px" :disabled="disabled" :title="disabled ? disabledReason : ''" @click="$emit('import')">导入草稿</el-button>

    <!-- 搜索和筛选 -->
    <el-divider content-position="left">草稿筛选</el-divider>

    <el-form-item label="搜索">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索草稿名称或ID"
        clearable
        :prefix-icon="Search"
      />
    </el-form-item>

    <el-form-item label="状态筛选">
      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 100%">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="已归档" value="ARCHIVED" />
      </el-select>
    </el-form-item>

    <el-scrollbar max-height="520px">
      <div
        v-for="item in filteredDrafts"
        :key="item.draftId"
        class="draft-item"
        :class="{ active: item.draftId === selectedDraftId }"
        @click="$emit('selectDraft', item.draftId)"
      >
        <div class="draft-header">
          <div class="draft-name">{{ item.draftName || item.draftId }}</div>
          <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, item)" @click.stop>
            <el-icon class="draft-menu-icon"><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="clone" :icon="CopyDocument">克隆</el-dropdown-item>
                <el-dropdown-item command="archive" :icon="FolderOpened" :disabled="item.status === 'ARCHIVED'">归档</el-dropdown-item>
                <el-dropdown-item command="delete" :icon="Delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="draft-meta">{{ item.descriptionCategoryName || '-' }} / {{ item.typeName || '-' }}</div>
        <div class="draft-meta">
          <el-tag :type="getStatusType(item.status)" size="small">{{ item.status }}</el-tag>
          <span style="margin-left: 8px">{{ item.variantCount || 0 }} variants</span>
        </div>
      </div>
      <el-empty v-if="!loading && filteredDrafts.length === 0" description="暂无草稿" :image-size="64" />
    </el-scrollbar>

    <!-- 克隆对话框 -->
    <el-dialog v-model="cloneDialogVisible" title="克隆草稿" width="400px">
      <el-form label-width="100px">
        <el-form-item label="源草稿">
          <el-input :model-value="currentDraft?.draftName" disabled />
        </el-form-item>
        <el-form-item label="新草稿名称" required>
          <el-input v-model="cloneForm.newDraftName" placeholder="请输入新草稿名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cloneDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmClone" :loading="actionLoading">确认克隆</el-button>
      </template>
    </el-dialog>

    <!-- 归档对话框 -->
    <el-dialog v-model="archiveDialogVisible" title="归档草稿" width="400px">
      <el-form label-width="100px">
        <el-form-item label="草稿名称">
          <el-input :model-value="currentDraft?.draftName" disabled />
        </el-form-item>
        <el-form-item label="归档原因">
          <el-input v-model="archiveForm.archiveReason" type="textarea" :rows="3" placeholder="请输入归档原因（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="archiveDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmArchive" :loading="actionLoading">确认归档</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search, MoreFilled, CopyDocument, FolderOpened, Delete } from '@element-plus/icons-vue';
import productApi from '@/api/ozon/product/productApi.js';

const props = defineProps({
  authOptions: { type: Array, default: () => [] },
  authId: { type: String, default: '' },
  drafts: { type: Array, default: () => [] },
  importDraftName: { type: String, default: '' },
  importSkuText: { type: String, default: '' },
  selectedDraftId: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  disabledReason: { type: String, default: '' }
});

const emit = defineEmits([
  'update:authId',
  'update:importDraftName',
  'update:importSkuText',
  'refresh',
  'import',
  'selectDraft'
]);

// 搜索和筛选
const searchKeyword = ref('');
const statusFilter = ref('');

// 对话框状态
const cloneDialogVisible = ref(false);
const archiveDialogVisible = ref(false);
const currentDraft = ref(null);
const actionLoading = ref(false);

// 表单数据
const cloneForm = ref({
  newDraftName: ''
});

const archiveForm = ref({
  archiveReason: ''
});

// 筛选后的草稿列表
const filteredDrafts = computed(() => {
  let result = props.drafts;

  // 搜索过滤
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase();
    result = result.filter(draft =>
      (draft.draftName || '').toLowerCase().includes(keyword) ||
      (draft.draftId || '').toLowerCase().includes(keyword)
    );
  }

  // 状态过滤
  if (statusFilter.value) {
    result = result.filter(draft => draft.status === statusFilter.value);
  }

  return result;
});

// 获取状态标签类型
function getStatusType(status) {
  const typeMap = {
    'DRAFT': 'info',
    'PUBLISHED': 'success',
    'ARCHIVED': 'warning'
  };
  return typeMap[status] || 'info';
}

// 处理菜单命令
function handleCommand(command, draft) {
  currentDraft.value = draft;

  switch (command) {
    case 'clone':
      openCloneDialog(draft);
      break;
    case 'archive':
      openArchiveDialog(draft);
      break;
    case 'delete':
      confirmDelete(draft);
      break;
  }
}

// 打开克隆对话框
function openCloneDialog(draft) {
  cloneForm.value.newDraftName = `${draft.draftName || draft.draftId} - 副本`;
  cloneDialogVisible.value = true;
}

// 确认克隆
async function confirmClone() {
  if (!cloneForm.value.newDraftName) {
    ElMessage.warning('请输入新草稿名称');
    return;
  }

  actionLoading.value = true;
  try {
    await productApi.cloneDraft({
      sourceDraftId: currentDraft.value.draftId,
      newDraftName: cloneForm.value.newDraftName,
      authId: props.authId
    });

    ElMessage.success('克隆成功');
    cloneDialogVisible.value = false;
    emit('refresh');
  } catch (error) {
    ElMessage.error('克隆失败: ' + (error.message || '未知错误'));
  } finally {
    actionLoading.value = false;
  }
}

// 打开归档对话框
function openArchiveDialog(draft) {
  archiveForm.value.archiveReason = '';
  archiveDialogVisible.value = true;
}

// 确认归档
async function confirmArchive() {
  actionLoading.value = true;
  try {
    await productApi.archiveDraft({
      draftId: currentDraft.value.draftId,
      authId: props.authId,
      archiveReason: archiveForm.value.archiveReason
    });

    ElMessage.success('归档成功');
    archiveDialogVisible.value = false;
    emit('refresh');
  } catch (error) {
    ElMessage.error('归档失败: ' + (error.message || '未知错误'));
  } finally {
    actionLoading.value = false;
  }
}

// 确认删除
async function confirmDelete(draft) {
  try {
    await ElMessageBox.confirm(
      `确定要删除草稿 "${draft.draftName || draft.draftId}" 吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );

    await productApi.deleteDraft(props.authId, draft.draftId);
    ElMessage.success('删除成功');
    emit('refresh');
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + (error.message || '未知错误'));
    }
  }
}
</script>

<style scoped>
.section-card {
  height: 100%;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.draft-item {
  padding: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.draft-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.draft-item.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.draft-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.draft-name {
  font-weight: 600;
  flex: 1;
}

.draft-menu-icon {
  font-size: 18px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}

.draft-menu-icon:hover {
  background: var(--el-fill-color-light);
  color: var(--el-color-primary);
}

.draft-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
</style>
