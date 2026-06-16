<template>
  <div class="main-sty">
    <OzonFeatureNotice
      :item="features.chat"
      title="Ozon 聊天模块当前已关闭"
      description="关闭状态下不会加载会话和消息，也无法导入聊天 JSON。"
    />
    <OzonFeatureNotice
      :item="features.chatSend"
      type="info"
      title="Ozon 聊天发送未开启"
      description="当前页面仅支持本地导入和本地回复审计，不会把回复真正发送到 Ozon。"
    />
    <OzonFeatureSummaryBar :items="summaryFeatureItems" />
    <ModeSwitchBanner
      title="聊天双模工作台"
      description="当前以本地导入和回复审计为主，未来官方发送能力会直接融合到当前会话工作台。"
      local-title="本地导入与回复审计"
      local-description="导入会话 JSON、查看时间线、沉淀回复审计，是当前稳定工作模式。"
      remote-title="官方发送模式"
      remote-description="当聊天发送开关放开后，本页会直接承接发送动作和审计，不再新增独立回复页。"
      :remote-enabled="isEnabled('chatSend')"
      :remote-reason="reason('chatSend')"
    />

    <el-card shadow="never" class="toolbar-card">
      <template #header>
        <div class="card-title">
          <div>
            <h3>Ozon 聊天导入</h3>
            <p class="font-extraSmall">导入本地 JSON 会话消息，查看未读会话、消息时间线，并记录本地回复审计。</p>
          </div>
          <el-button type="primary" :loading="importing" :disabled="!isEnabled('chat')" @click="submitImport">导入会话</el-button>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="授权店铺">
            <el-select v-model="form.authId" placeholder="请选择 Ozon 授权" style="width: 100%" @change="reloadSessions">
              <el-option v-for="item in authOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="关键字">
            <el-input v-model="filters.keyword" placeholder="客户名/消息/会话ID" @keyup.enter="loadSessions" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="只看未读">
            <el-switch v-model="filters.unreadOnly" @change="loadSessions" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="会话状态">
            <el-select v-model="filters.sessionStatus" clearable placeholder="全部" style="width: 100%" @change="loadSessions">
              <el-option label="OPEN" value="OPEN" />
              <el-option label="CLOSED" value="CLOSED" />
              <el-option label="ARCHIVED" value="ARCHIVED" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="原始 JSON">
        <el-input
          v-model="form.rawContent"
          type="textarea"
          :rows="8"
          placeholder='{"sessions":[{"sessionId":"session-1","customerName":"Buyer A","sessionStatus":"OPEN","messages":[{"messageId":"msg-1","senderType":"BUYER","messageText":"hello","messageTime":"2026-03-26T10:00:00Z","read":false}]}]}'
        />
      </el-form-item>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="9">
        <el-card shadow="never" class="session-card">
          <template #header>
            <div class="card-title">
              <div>会话列表</div>
              <el-button @click="loadSessions">刷新</el-button>
            </div>
          </template>
          <el-table
            v-loading="sessionLoading"
            :data="sessionData"
            border
            highlight-current-row
            @current-change="handleSessionChange"
          >
            <el-table-column prop="customerName" label="客户" min-width="140" show-overflow-tooltip />
            <el-table-column prop="unreadCount" label="未读" width="80" />
            <el-table-column prop="sessionStatus" label="状态" width="100" />
            <el-table-column prop="lastMessageText" label="最后消息" min-width="220" show-overflow-tooltip />
            <el-table-column label="最后时间" min-width="160">
              <template #default="scope">
                {{ scope.row.lastMessageAt ? dateFormat(scope.row.lastMessageAt) : '-' }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="15">
        <el-card shadow="never" v-loading="messageLoading">
          <template #header>
            <div class="card-title">
              <div>消息时间线</div>
              <div>{{ selectedSession?.customerName || '未选择会话' }}</div>
            </div>
          </template>

          <el-empty v-if="!selectedSession" description="请选择左侧会话" />

          <template v-else>
            <el-scrollbar max-height="420px" class="message-board">
              <div v-for="item in messageData" :key="item.messageId" class="message-item" :class="item.senderType === 'SELLER' ? 'seller' : 'buyer'">
                <div class="message-meta">
                  <span>{{ item.senderType }}</span>
                  <span>{{ item.messageTime ? dateFormat(item.messageTime) : '-' }}</span>
                </div>
                <div class="message-text">{{ item.messageText || '-' }}</div>
              </div>
            </el-scrollbar>

            <ReplyComposer
              v-model="replyForm.replyText"
              :send-enabled="isEnabled('chatSend')"
              :send-reason="reason('chatSend')"
              :loading="replying"
              :audits="replyAuditData"
              @submit="submitReply"
            />
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import authApi from '@/api/ozon/auth/authApi.js';
import chatApi from '@/api/ozon/chat/chatApi.js';
import { dateFormat } from '@/utils/index.js';
import OzonFeatureNotice from '../components/OzonFeatureNotice.vue';
import OzonFeatureSummaryBar from '../components/OzonFeatureSummaryBar.vue';
import { useOzonFeatures } from '../composables/useOzonFeatures.js';
import ModeSwitchBanner from '../shared/components/ModeSwitchBanner.vue';
import ReplyComposer from './components/ReplyComposer.vue';

const importing = ref(false);
const sessionLoading = ref(false);
const messageLoading = ref(false);
const replying = ref(false);
const authOptions = ref([]);
const sessionData = ref([]);
const messageData = ref([]);
const replyAuditData = ref([]);
const selectedSession = ref(null);

const form = reactive({
  authId: '',
  rawContent: ''
});

const filters = reactive({
  keyword: '',
  unreadOnly: false,
  sessionStatus: ''
});

const replyForm = reactive({
  replyText: ''
});
const { features, featureItems, loadFeatures, isEnabled, reason } = useOzonFeatures();
const summaryFeatureItems = computed(() => featureItems.value.filter((item) => ['chat', 'chatSend'].includes(item.key)));

onMounted(() => {
  loadFeatures().finally(loadAuths);
});

function loadAuths() {
  authApi.list().then(res => {
    authOptions.value = res.data || [];
    if (!form.authId && authOptions.value.length > 0) {
      form.authId = authOptions.value[0].id;
      loadSessions();
    }
  });
}

function submitImport() {
  if (!isEnabled('chat')) {
    ElMessage.warning(reason('chat'));
    return;
  }
  if (!form.authId || !form.rawContent) {
    ElMessage.error('请先选择授权并输入原始 JSON');
    return;
  }
  importing.value = true;
  chatApi.importMessages({
    authId: form.authId,
    rawContent: form.rawContent
  }).then(res => {
    ElMessage.success(`已导入 ${res.data?.sessionCount || 0} 个会话，${res.data?.messageCount || 0} 条消息`);
    loadSessions();
  }).finally(() => {
    importing.value = false;
  });
}

function reloadSessions() {
  selectedSession.value = null;
  messageData.value = [];
  replyAuditData.value = [];
  loadSessions();
}

function loadSessions() {
  if (!form.authId || !isEnabled('chat')) {
    sessionData.value = [];
    return;
  }
  sessionLoading.value = true;
  chatApi.listSessions({
    authId: form.authId,
    unreadOnly: filters.unreadOnly || undefined,
    keyword: filters.keyword || undefined,
    sessionStatus: filters.sessionStatus || undefined
  }).then(res => {
    sessionData.value = res.data || [];
  }).finally(() => {
    sessionLoading.value = false;
  });
}

function handleSessionChange(row) {
  selectedSession.value = row || null;
  replyForm.replyText = '';
  replyAuditData.value = [];
  if (row?.sessionId) {
    loadMessages(row.sessionId);
    loadReplyAudits(row.sessionId);
  } else {
    messageData.value = [];
  }
}

function loadMessages(sessionId) {
  if (!isEnabled('chat')) {
    messageData.value = [];
    return;
  }
  messageLoading.value = true;
  chatApi.listMessages({
    authId: form.authId,
    sessionId
  }).then(res => {
    messageData.value = res.data || [];
  }).finally(() => {
    messageLoading.value = false;
  });
}

function loadReplyAudits(sessionId) {
  if (!form.authId || !sessionId || !isEnabled('chat')) {
    replyAuditData.value = [];
    return;
  }
  chatApi.listReplyAudits({
    authId: form.authId,
    sessionId
  }).then(res => {
    replyAuditData.value = res.data || [];
  });
}

function submitReply() {
  if (!isEnabled('chat')) {
    ElMessage.warning(reason('chat'));
    return;
  }
  if (!selectedSession.value?.sessionId) {
    ElMessage.error('请先选择会话');
    return;
  }
  if (!replyForm.replyText.trim()) {
    ElMessage.error('请输入回复内容');
    return;
  }
  replying.value = true;
  const request = {
    authId: form.authId,
    sessionId: selectedSession.value.sessionId,
    replyText: replyForm.replyText.trim()
  };
  const action = isEnabled('chatSend') ? chatApi.sendReply(request) : chatApi.recordReply(request);
  action.then(res => {
    replyForm.replyText = '';
    replyAuditData.value = res.data ? [res.data, ...replyAuditData.value.filter((item) => item.id !== res.data.id)] : replyAuditData.value;
    if (res.data && selectedSession.value) {
      selectedSession.value.lastMessageText = request.replyText;
      selectedSession.value.lastMessageAt = new Date().toISOString();
      loadMessages(selectedSession.value.sessionId);
    }
    ElMessage.success(isEnabled('chatSend') ? '已发送到 Ozon 并记录审计' : '已记录本地回复');
  }).finally(() => {
    replying.value = false;
  });
}
</script>

<style scoped>
.toolbar-card {
  margin-bottom: 16px;
}

.session-card {
  height: 100%;
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.message-board {
  margin-bottom: 16px;
}

.message-item {
  padding: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  margin-bottom: 12px;
}

.message-item.buyer {
  background: var(--el-color-primary-light-9);
}

.message-item.seller {
  background: var(--el-color-success-light-9);
}

.message-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.reply-box {
  margin-top: 12px;
}

.reply-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.reply-alert {
  margin-top: 16px;
}
</style>
