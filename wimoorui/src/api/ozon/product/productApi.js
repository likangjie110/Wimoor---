import request from "@/utils/request.js";

function list(params) {
  return request.get("/ozon/api/v1/product/list", { params });
}

function importDraft(data) {
  return request.post("/ozon/api/v1/product/importDraft", data);
}

function saveMapping(data) {
  return request.post("/ozon/api/v1/product/map/save", data);
}

function listDrafts(params) {
  return request.get("/ozon/api/v1/product/draft/list", { params });
}

function detail(params) {
  return request.get("/ozon/api/v1/product/draft/detail", { params });
}

function saveDraft(data) {
  return request.post("/ozon/api/v1/product/draft/save", data);
}

function categoryTree(params) {
  return request.get("/ozon/api/v1/product/category/tree", { params });
}

function categoryTemplate(params) {
  return request.get("/ozon/api/v1/product/category/template", { params });
}

function preview(data) {
  return request.post("/ozon/api/v1/product/preview", data);
}

function publish(data) {
  return request.post("/ozon/api/v1/product/publish", data);
}

function publishTaskDetail(params) {
  return request.get("/ozon/api/v1/product/publish/task/detail", { params });
}

function publishTaskList(params) {
  return request.get("/ozon/api/v1/product/publish/task/list", { params });
}

// ========== 草稿生命周期管理 ==========

/**
 * 克隆草稿
 * @param {Object} data - 克隆命令
 * @param {string} data.sourceDraftId - 源草稿ID
 * @param {string} data.newDraftName - 新草稿名称
 * @param {string} data.authId - 授权ID
 */
function cloneDraft(data) {
  return request.post("/ozon/api/v1/product/draft/clone", data);
}

/**
 * 归档草稿
 * @param {Object} data - 归档命令
 * @param {string} data.draftId - 草稿ID
 * @param {string} data.authId - 授权ID
 * @param {string} data.archiveReason - 归档原因
 */
function archiveDraft(data) {
  return request.post("/ozon/api/v1/product/draft/archive", data);
}

/**
 * 删除草稿
 * @param {string} authId - 授权ID
 * @param {string} draftId - 草稿ID
 */
function deleteDraft(authId, draftId) {
  return request.delete("/ozon/api/v1/product/draft/delete", {
    params: { authId, draftId }
  });
}

/**
 * 按状态查询草稿列表
 * @param {string} authId - 授权ID
 * @param {string} status - 状态（DRAFT, PUBLISHED, ARCHIVED）
 */
function listDraftsByStatus(authId, status) {
  return request.get("/ozon/api/v1/product/draft/listByStatus", {
    params: { authId, status }
  });
}

// ========== 任务历史查询 ==========

/**
 * 查询任务历史
 * @param {string} authId - 授权ID
 * @param {string} draftId - 草稿ID
 */
function getTaskHistory(authId, draftId) {
  return request.get("/ozon/api/v1/product/publish/task/history", {
    params: { authId, draftId }
  });
}

/**
 * 查询任务详情
 * @param {string} authId - 授权ID
 * @param {string} taskId - 任务ID
 */
function getTaskDetailNew(authId, taskId) {
  return request.get("/ozon/api/v1/product/publish/task/query/detail", {
    params: { authId, taskId }
  });
}

export default {
  list,
  importDraft,
  saveMapping,
  listDrafts,
  detail,
  saveDraft,
  categoryTree,
  categoryTemplate,
  preview,
  publish,
  publishTaskDetail,
  publishTaskList,
  // 草稿生命周期
  cloneDraft,
  archiveDraft,
  deleteDraft,
  listDraftsByStatus,
  // 任务历史
  getTaskHistory,
  getTaskDetailNew
}
