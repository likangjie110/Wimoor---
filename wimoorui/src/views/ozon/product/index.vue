<template>
  <div class="main-sty ozon-product-workbench">
    <OzonFeatureNotice
      :item="features.product"
      title="Ozon 商品模块当前已关闭"
      description="关闭状态下不会加载草稿、类目模板或商品映射，也无法保存草稿。"
    />
    <OzonFeatureNotice
      :item="features.productWrite"
      type="info"
      title="Ozon 商品发布写操作未开启"
      description="当前仍可导入草稿、编辑、预检与查看任务，但真实发布按钮会保持禁用。"
    />
    <el-card shadow="never" class="action-card">
      <div class="action-row">
        <div>
          <div class="action-title">商品下游操作</div>
          <div class="font-extraSmall action-desc">
            当前联动 SKU：{{ actionMaterialSku || '未识别' }}，可直接跳转到价格或库存工作台继续操作。
          </div>
        </div>
        <el-space>
          <el-button :disabled="!query.authId || !actionMaterialSku" data-testid="btn-goto-price" @click="goToPriceCenter">前往价格中心</el-button>
          <el-button :disabled="!query.authId || !actionMaterialSku" data-testid="btn-goto-stock" @click="goToStockCenter">前往库存中心</el-button>
        </el-space>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="7">
        <DraftSidebar
          data-testid="product-draft-list"
          :auth-options="authOptions"
          :auth-id="query.authId"
          :drafts="localizedDrafts"
          :import-draft-name="importForm.draftName"
          :import-sku-text="importForm.skuText"
          :selected-draft-id="selectedDraftId"
          :loading="draftLoading"
          :disabled="!isEnabled('product')"
          :disabled-reason="reason('product')"
          @update:authId="handleAuthChange"
          @update:importDraftName="importForm.draftName = $event"
          @update:importSkuText="importForm.skuText = $event"
          @refresh="loadDrafts"
          @import="importDraft"
          @select-draft="loadDraftDetail"
        />
      </el-col>

      <el-col :xs="24" :lg="17">
        <DraftBaseForm
          :draft-form="draftForm"
          :category-options="categoryOptions"
          :current-types="currentTypes"
          :metadata-language="metadataLanguage"
          :metadata-language-options="metadataLanguageOptions"
          :save-disabled="!isEnabled('product')"
          :save-disabled-reason="reason('product')"
          @save="saveDraft"
          @category-change="handleCategoryChange"
          @type-change="handleTypeChange"
          @metadata-language-change="handleMetadataLanguageChange"
        />
        <CommonAttributePanel :attributes="draftForm.commonAttributes" />
        <CommonImagePanel :images="draftForm.commonImages" />
        <VariantMatrix :variants="draftForm.variants" />
        <div ref="previewSectionRef">
          <PreviewPanel :preview="previewResult" />
        </div>
        <div ref="publishSectionRef" data-testid="publish-area">
          <PublishTaskPanel
            :draft-id="selectedDraftId"
            :task-id="draftForm.lastPublishTaskId"
            :task-result="taskResult"
            :task-history="taskHistory"
            :task-history-loaded="taskHistoryLoaded"
            :module-disabled="!isEnabled('product')"
            :module-disabled-reason="reason('product')"
            :publish-disabled="!isEnabled('productWrite')"
            :publish-disabled-reason="reason('productWrite')"
            @preview="runPreview"
            @publish="runPublish"
            @refresh-task="refreshTask"
            @select-task="selectTaskFromHistory"
          />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import authApi from '@/api/ozon/auth/authApi.js';
import productApi from '@/api/ozon/product/productApi.js';
import DraftSidebar from './components/DraftSidebar.vue';
import DraftBaseForm from './components/DraftBaseForm.vue';
import CommonAttributePanel from './components/CommonAttributePanel.vue';
import CommonImagePanel from './components/CommonImagePanel.vue';
import VariantMatrix from './components/VariantMatrix.vue';
import PreviewPanel from './components/PreviewPanel.vue';
import PublishTaskPanel from './components/PublishTaskPanel.vue';
import OzonFeatureNotice from '../components/OzonFeatureNotice.vue';
import { useOzonFeatures } from '../composables/useOzonFeatures.js';

const authOptions = ref([]);
const drafts = ref([]);
const draftLoading = ref(false);
const categoryTree = ref([]);
const currentTemplate = ref(null);
const selectedDraftId = ref('');
const previewResult = ref(null);
const taskResult = ref(null);
const metadataLanguage = ref('ZH_HANS');
const { features, loadFeatures, isEnabled, reason } = useOzonFeatures();
const taskHistory = ref([]);
const taskHistoryLoaded = ref(false);
const previewSectionRef = ref(null);
const publishSectionRef = ref(null);
const route = useRoute();
const router = useRouter();

const metadataLanguageOptions = [
  { label: '中文', value: 'ZH_HANS' },
  { label: '英语', value: 'EN' },
  { label: '俄语', value: 'RU' }
];

const query = reactive({
  authId: ''
});

const importForm = reactive({
  draftName: '',
  skuText: ''
});

const draftForm = reactive(createEmptyDraft());

const categoryOptions = computed(() => categoryTree.value || []);
const localizedDrafts = computed(() => {
  return (drafts.value || []).map((item) => {
    const category = categoryOptions.value.find((row) => row.descriptionCategoryId === item.descriptionCategoryId);
    const type = category?.types?.find((row) => row.typeId === item.typeId);
    return {
      ...item,
      descriptionCategoryName: category?.descriptionCategoryName || item.descriptionCategoryName,
      typeName: type?.typeName || item.typeName
    };
  });
});
const currentTypes = computed(() => {
  const category = categoryOptions.value.find((item) => item.descriptionCategoryId === draftForm.descriptionCategoryId);
  return category?.types || [];
});
const actionMaterialSku = computed(() => normalizeQuery(route.query.materialSku) || draftForm.variants?.[0]?.materialSku || '');

onMounted(() => {
  loadFeatures().finally(loadAuths);
});

watch(
  () => route.query,
  () => {
    if (authOptions.value.length > 0) {
      applyRouteState();
    }
  },
  { deep: true }
);

function createEmptyDraft() {
  return {
    draftId: '',
    draftName: '',
    descriptionCategoryId: null,
    descriptionCategoryName: '',
    typeId: null,
    typeName: '',
    titleOverrideValue: '',
    brandOverrideValue: '',
    descriptionOverrideValue: '',
    lastPublishTaskId: '',
    commonAttributes: [],
    commonImages: [],
    variants: []
  };
}

function resetDraftForm() {
  Object.assign(draftForm, createEmptyDraft());
  previewResult.value = null;
  taskResult.value = null;
  taskHistory.value = [];
  taskHistoryLoaded.value = false;
}

function loadAuths() {
  authApi.list().then((res) => {
    authOptions.value = res.data || [];
    if (!query.authId && authOptions.value.length > 0) {
      query.authId = resolveInitialAuthId();
      loadDrafts();
      loadCategoryTree();
      applyRouteState();
    }
  });
}

function resolveInitialAuthId() {
  const routeAuthId = normalizeQuery(route.query.authId);
  if (routeAuthId && authOptions.value.some((item) => item.id === routeAuthId)) {
    return routeAuthId;
  }
  return authOptions.value[0]?.id || '';
}

function applyRouteState() {
  const routeAuthId = normalizeQuery(route.query.authId);
  const draftId = normalizeQuery(route.query.draftId);
  const focus = normalizeQuery(route.query.focus);
  if (routeAuthId && routeAuthId !== query.authId && authOptions.value.some((item) => item.id === routeAuthId)) {
    query.authId = routeAuthId;
    selectedDraftId.value = '';
    resetDraftForm();
    loadDrafts();
    loadCategoryTree();
  }
  if (draftId && query.authId && draftId !== selectedDraftId.value) {
    loadDraftDetail(draftId, focus, false);
  } else if (focus) {
    focusSection(focus);
  }
}

function handleAuthChange(authId) {
  query.authId = authId;
  selectedDraftId.value = '';
  resetDraftForm();
  syncRoute({ authId, draftId: null, focus: null });
  loadDrafts();
  loadCategoryTree();
}

function loadDrafts() {
  if (!query.authId || !isEnabled('product')) {
    drafts.value = [];
    return;
  }
  draftLoading.value = true;
  productApi.listDrafts({ authId: query.authId }).then((res) => {
    drafts.value = res.data || [];
  }).finally(() => {
    draftLoading.value = false;
  });
}

function loadDraftDetail(draftId, focus = null, updateRoute = true) {
  if (!query.authId || !draftId || !isEnabled('product')) {
    return;
  }
  selectedDraftId.value = draftId;
  if (updateRoute) {
    syncRoute({ authId: query.authId, draftId, focus });
  }
  productApi.detail({ authId: query.authId, draftId }).then((res) => {
    applyDraftDetail(res.data);
    loadCategoryTree();
    if (draftForm.descriptionCategoryId && draftForm.typeId) {
      loadCategoryTemplate();
    }
    if (draftForm.lastPublishTaskId) {
      refreshTask();
    } else {
      taskResult.value = null;
    }
    loadTaskHistory();
    focusSection(focus);
  });
}

function applyDraftDetail(detail) {
  resetDraftForm();
  draftForm.draftId = detail?.draftId || '';
  draftForm.draftName = detail?.draftName || '';
  draftForm.descriptionCategoryId = detail?.descriptionCategoryId || null;
  draftForm.descriptionCategoryName = detail?.descriptionCategoryName || '';
  draftForm.typeId = detail?.typeId || null;
  draftForm.typeName = detail?.typeName || '';
  draftForm.titleOverrideValue = detail?.titleOverrideValue || '';
  draftForm.brandOverrideValue = detail?.brandOverrideValue || '';
  draftForm.descriptionOverrideValue = detail?.descriptionOverrideValue || '';
  draftForm.lastPublishTaskId = detail?.lastPublishTaskId || '';
  draftForm.commonAttributes = (detail?.commonAttributes || []).map(toAttributeRow);
  draftForm.commonImages = (detail?.commonImages || []).map(toImageRow);
  draftForm.variants = (detail?.variants || []).map((variant) => ({
    variantId: variant.variantId,
    materialSku: variant.materialSku,
    materialName: variant.materialName,
    offerIdOverride: variant.offerIdOverride || '',
    barcodeOverride: variant.barcodeOverride || '',
    priceOverride: variant.priceOverride || '',
    weightOverrideValue: variant.weightOverrideValue || '',
    lengthOverrideValue: variant.lengthOverrideValue || '',
    widthOverrideValue: variant.widthOverrideValue || '',
    heightOverrideValue: variant.heightOverrideValue || '',
    variantLabel: variant.variantLabel || '',
    attributes: (variant.attributes || []).map(toAttributeRow),
    images: (variant.images || []).map(toImageRow)
  }));
}

function toAttributeRow(item) {
  const values = parseJson(item.attributeValueJson);
  const first = values[0] || {};
  return {
    attributeId: item.attributeId,
    attributeName: item.attributeName,
    mode: item.mode || 'TEXT',
    required: item.requiredFlag ?? false,
    valueText: first.text || '',
    dictionaryValueId: first.dictionaryValueId || null,
    options: []
  };
}

function toImageRow(item) {
  return {
    source: item.source || 'MANUAL',
    imageUrl: item.imageUrl || '',
    sortOrder: item.sortOrder ?? 0,
    primary: item.primary ?? false
  };
}

function parseJson(text) {
  if (!text) {
    return [];
  }
  try {
    return JSON.parse(text);
  } catch (error) {
    return [];
  }
}

function loadCategoryTree() {
  if (!query.authId || !isEnabled('product')) {
    categoryTree.value = [];
    return;
  }
  productApi.categoryTree({
    authId: query.authId,
    language: metadataLanguage.value
  }).then((res) => {
    categoryTree.value = res.data?.categories || [];
    syncSelectedMetaNames();
  });
}

function handleCategoryChange(categoryId) {
  const category = categoryOptions.value.find((item) => item.descriptionCategoryId === categoryId);
  draftForm.descriptionCategoryName = category?.descriptionCategoryName || '';
  draftForm.typeId = null;
  draftForm.typeName = '';
  currentTemplate.value = null;
}

function handleTypeChange(typeId) {
  const type = currentTypes.value.find((item) => item.typeId === typeId);
  draftForm.typeName = type?.typeName || '';
  loadCategoryTemplate();
}

function handleMetadataLanguageChange(language) {
  metadataLanguage.value = language;
  currentTemplate.value = null;
  loadCategoryTree();
  if (draftForm.descriptionCategoryId && draftForm.typeId) {
    loadCategoryTemplate();
  }
}

function loadCategoryTemplate() {
  if (!query.authId || !draftForm.descriptionCategoryId || !draftForm.typeId || !isEnabled('product')) {
    return;
  }
  productApi.categoryTemplate({
    authId: query.authId,
    descriptionCategoryId: draftForm.descriptionCategoryId,
    typeId: draftForm.typeId,
    language: metadataLanguage.value
  }).then((res) => {
    currentTemplate.value = res.data || null;
    syncSelectedMetaNames();
    mergeTemplateIntoDraft();
  });
}

function mergeTemplateIntoDraft() {
  if (!currentTemplate.value) {
    return;
  }
  mergeAttributeRows(draftForm.commonAttributes, currentTemplate.value.commonAttributes || []);
  for (const variant of draftForm.variants) {
    mergeAttributeRows(variant.attributes, currentTemplate.value.variantAttributes || []);
  }
}

function mergeAttributeRows(target, templateItems) {
  for (const item of templateItems) {
    const existing = target.find((row) => row.attributeId === item.attributeId);
    if (existing) {
      existing.attributeName = item.attributeName;
      existing.mode = item.mode;
      existing.required = item.required;
      existing.options = item.values || [];
      continue;
    }
    target.push({
      attributeId: item.attributeId,
      attributeName: item.attributeName,
      mode: item.mode || 'TEXT',
      required: item.required ?? false,
      valueText: '',
      dictionaryValueId: null,
      options: item.values || []
    });
  }
}

function syncSelectedMetaNames() {
  const category = categoryOptions.value.find((item) => item.descriptionCategoryId === draftForm.descriptionCategoryId);
  if (category) {
    draftForm.descriptionCategoryName = category.descriptionCategoryName || '';
  }
  const type = currentTypes.value.find((item) => item.typeId === draftForm.typeId);
  if (type) {
    draftForm.typeName = type.typeName || '';
  }
}

function importDraft() {
  if (!isEnabled('product')) {
    ElMessage.warning(reason('product'));
    return;
  }
  if (!query.authId) {
    ElMessage.error('请先选择 Ozon 授权');
    return;
  }
  const skus = parseSkuList(importForm.skuText);
  if (skus.length === 0) {
    ElMessage.error('请输入 SKU');
    return;
  }
  productApi.importDraft({
    authId: query.authId,
    draftId: selectedDraftId.value || undefined,
    draftName: importForm.draftName || undefined,
    skus
  }).then((res) => {
    const draftId = res.data?.draftId;
    ElMessage.success(`已导入 ${res.data?.importedCount || 0} 条草稿变体`);
    importForm.skuText = '';
    loadDrafts();
    if (draftId) {
      loadDraftDetail(draftId, 'publish');
    }
  });
}

function parseSkuList(text) {
  return [...new Set((text || '').split(/[\n,]+/).map((item) => item.trim()).filter(Boolean))];
}

function saveDraft() {
  if (!isEnabled('product')) {
    ElMessage.warning(reason('product'));
    return;
  }
  if (!query.authId) {
    ElMessage.error('请先选择 Ozon 授权');
    return;
  }
  productApi.saveDraft(buildSavePayload()).then((res) => {
    ElMessage.success('草稿已保存');
    selectedDraftId.value = res.data?.draftId || selectedDraftId.value;
    applyDraftDetail(res.data || {});
    loadDrafts();
    loadTaskHistory();
    syncRoute({ authId: query.authId, draftId: selectedDraftId.value, focus: normalizeQuery(route.query.focus) });
  });
}

function buildSavePayload() {
  return {
    authId: query.authId,
    draftId: selectedDraftId.value || undefined,
    draftName: draftForm.draftName,
    descriptionCategoryId: draftForm.descriptionCategoryId,
    descriptionCategoryName: draftForm.descriptionCategoryName,
    typeId: draftForm.typeId,
    typeName: draftForm.typeName,
    titleOverrideValue: draftForm.titleOverrideValue,
    brandOverrideValue: draftForm.brandOverrideValue,
    descriptionOverrideValue: draftForm.descriptionOverrideValue,
    commonAttributes: serializeAttributes(draftForm.commonAttributes),
    commonImages: serializeImages(draftForm.commonImages),
    variants: draftForm.variants.map((item) => ({
      materialSku: item.materialSku,
      materialName: item.materialName,
      offerIdOverride: item.offerIdOverride,
      barcodeOverride: item.barcodeOverride,
      priceOverride: item.priceOverride,
      weightOverrideValue: item.weightOverrideValue,
      lengthOverrideValue: item.lengthOverrideValue,
      widthOverrideValue: item.widthOverrideValue,
      heightOverrideValue: item.heightOverrideValue,
      variantLabel: item.variantLabel,
      attributes: serializeAttributes(item.attributes || []),
      images: serializeImages(item.images || [])
    }))
  };
}

function serializeAttributes(attributes) {
  return (attributes || [])
    .map((item) => {
      const values = buildAttributeValues(item);
      if (values.length === 0) {
        return null;
      }
      return {
        attributeId: item.attributeId,
        attributeName: item.attributeName,
        mode: item.mode,
        values
      };
    })
    .filter(Boolean);
}

function buildAttributeValues(item) {
  if (item.mode?.includes('DICT')) {
    if (!item.dictionaryValueId) {
      return [];
    }
    const option = (item.options || []).find((row) => row.dictionaryValueId === item.dictionaryValueId);
    return [{ dictionaryValueId: item.dictionaryValueId, text: option?.text || '' }];
  }
  return (item.valueText || '')
    .split(item.mode?.includes('MULTI') ? /[\n,]+/ : /\n/)
    .map((row) => row.trim())
    .filter(Boolean)
    .map((text) => ({ text }));
}

function serializeImages(images) {
  return (images || [])
    .filter((item) => item.imageUrl)
    .map((item, index) => ({
      source: item.source || 'MANUAL',
      imageUrl: item.imageUrl,
      sortOrder: index,
      primary: index === 0
    }));
}

function runPreview() {
  if (!isEnabled('product')) {
    ElMessage.warning(reason('product'));
    return;
  }
  if (!selectedDraftId.value) {
    ElMessage.error('请先保存或选择草稿');
    return;
  }
  productApi.preview({ authId: query.authId, draftId: selectedDraftId.value }).then((res) => {
    previewResult.value = res.data || null;
    syncRoute({ authId: query.authId, draftId: selectedDraftId.value, focus: 'preview' });
    focusSection('preview');
  });
}

function runPublish() {
  if (!isEnabled('productWrite')) {
    ElMessage.warning(reason('productWrite'));
    return;
  }
  if (!selectedDraftId.value) {
    ElMessage.error('请先保存或选择草稿');
    return;
  }
  productApi.publish({ authId: query.authId, draftId: selectedDraftId.value }).then((res) => {
    taskResult.value = res.data?.resultSummary || null;
    draftForm.lastPublishTaskId = res.data?.localTaskId || '';
    loadTaskHistory();
    syncRoute({ authId: query.authId, draftId: selectedDraftId.value, focus: 'publish' });
    focusSection('publish');
    ElMessage.success(`发布任务已创建：${res.data?.taskStatus || 'RUNNING'}`);
  });
}

function refreshTask() {
  if (!draftForm.lastPublishTaskId || !isEnabled('product')) {
    return;
  }
  productApi.publishTaskDetail({
    authId: query.authId,
    taskId: draftForm.lastPublishTaskId
  }).then((res) => {
    taskResult.value = res.data || null;
    loadTaskHistory();
  });
}

function loadTaskHistory() {
  if (!selectedDraftId.value || !isEnabled('product')) {
    taskHistory.value = [];
    taskHistoryLoaded.value = false;
    return;
  }
  productApi.publishTaskList({
    authId: query.authId,
    draftId: selectedDraftId.value
  }).then((res) => {
    taskHistory.value = res.data || [];
    taskHistoryLoaded.value = true;
  });
}

function selectTaskFromHistory(taskId) {
  if (!taskId) {
    return;
  }
  draftForm.lastPublishTaskId = taskId;
  syncRoute({ authId: query.authId, draftId: selectedDraftId.value, focus: 'publish' });
  refreshTask();
  focusSection('publish');
}

function focusSection(focus) {
  nextTick(() => {
    if (focus === 'preview' && previewSectionRef.value?.scrollIntoView) {
      previewSectionRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' });
      return;
    }
    if (focus === 'publish' && publishSectionRef.value?.scrollIntoView) {
      publishSectionRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  });
}

function syncRoute({ authId, draftId, focus }) {
  const nextQuery = { ...route.query };
  nextQuery.authId = authId || undefined;
  nextQuery.draftId = draftId || undefined;
  nextQuery.focus = focus || undefined;
  router.replace({ path: route.path, query: nextQuery });
}

function normalizeQuery(value) {
  if (Array.isArray(value)) {
    return value[0] || '';
  }
  return value || '';
}

function goToPriceCenter() {
  if (!query.authId || !actionMaterialSku.value) {
    return;
  }
  router.push({
    path: '/ozon/price',
    query: {
      authId: query.authId,
      materialSku: actionMaterialSku.value
    }
  });
}

function goToStockCenter() {
  if (!query.authId || !actionMaterialSku.value) {
    return;
  }
  router.push({
    path: '/ozon/stock',
    query: {
      authId: query.authId,
      materialSku: actionMaterialSku.value
    }
  });
}
</script>

<style scoped>
.ozon-product-workbench {
  min-height: calc(100vh - 140px);
}

.action-card {
  margin-bottom: 16px;
}

.action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.action-title {
  font-weight: 600;
}

.action-desc {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
}
</style>
