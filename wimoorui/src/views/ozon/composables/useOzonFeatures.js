import { computed, reactive } from 'vue';
import metaApi from '@/api/ozon/meta/metaApi.js';

const FEATURE_KEYS = [
  'auth',
  'product',
  'productWrite',
  'task',
  'error',
  'finance',
  'financeSync',
  'chat',
  'chatSync',
  'ads',
  'stockWrite',
  'priceWrite',
  'postingWrite',
  'chatSend',
  'adsSync'
];

const FEATURE_LABELS = {
  auth: '授权',
  product: '商品',
  productWrite: '商品发布',
  task: '任务',
  error: '错误中心',
  finance: '财务',
  financeSync: '财务同步',
  chat: '聊天',
  chatSync: '聊天同步',
  ads: '广告',
  stockWrite: '库存写入',
  priceWrite: '价格写入',
  postingWrite: '履约写入',
  chatSend: '聊天发送',
  adsSync: '广告同步'
};

function emptyFeatures() {
  return FEATURE_KEYS.reduce((result, key) => {
    result[key] = {
      enabled: true,
      reason: null,
      name: FEATURE_LABELS[key] || key,
      description: '',
      category: 'read'
    };
    return result;
  }, {});
}

const state = reactive({
  loaded: false,
  loading: false,
  promise: null,
  data: emptyFeatures()
});

function normalizeFeatureData(payload) {
  const result = emptyFeatures();
  for (const key of FEATURE_KEYS) {
    const item = payload?.[key];
    if (item && typeof item.enabled === 'boolean') {
      result[key] = {
        enabled: item.enabled,
        reason: item.reason || null,
        name: item.name || FEATURE_LABELS[key] || key,
        description: item.description || '',
        category: item.category || 'read'
      };
    }
  }
  return result;
}

export function useOzonFeatures() {
  const features = computed(() => state.data);
  const hasLoaded = computed(() => state.loaded);
  const isLoading = computed(() => state.loading);
  const featureItems = computed(() => FEATURE_KEYS.map((key) => ({
    key,
    label: FEATURE_LABELS[key] || key,
    enabled: !!state.data[key]?.enabled,
    reason: state.data[key]?.reason || null
  })));

  function loadFeatures(force = false) {
    if (state.promise && !force) {
      return state.promise;
    }
    if (state.loaded && !force) {
      return Promise.resolve(state.data);
    }
    state.loading = true;
    state.promise = metaApi.features()
      .then((res) => {
        state.data = normalizeFeatureData(res.data || {});
        state.loaded = true;
        return state.data;
      })
      .finally(() => {
        state.loading = false;
        state.promise = null;
      });
    return state.promise;
  }

  function isEnabled(key) {
    return !!state.data[key]?.enabled;
  }

  function reason(key, fallback = '功能未开启') {
    return state.data[key]?.reason || fallback;
  }

  return {
    features,
    hasLoaded,
    isLoading,
    featureItems,
    loadFeatures,
    isEnabled,
    reason
  };
}
