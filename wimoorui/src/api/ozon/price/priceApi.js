import request from "@/utils/request.js";

function push(data) {
  return request.post("/ozon/api/v1/price/push", data);
}

function listSnapshots(authId) {
  return request.get("/ozon/api/v1/price/snapshot/list", { params: { authId } });
}

function listTasks(authId) {
  return request.get("/ozon/api/v1/price/task/list", { params: { authId } });
}

// ========== 任务历史查询 ==========

/**
 * 获取任务详情
 * @param {string} authId - 授权ID
 * @param {string} taskId - 任务ID
 */
function getTaskDetail(authId, taskId) {
  return request.get(`/ozon/api/v1/price/task/${taskId}/detail`, {
    params: { authId }
  });
}

/**
 * 查询任务历史
 * @param {string} authId - 授权ID
 * @param {number} limit - 限制数量
 */
function listTaskHistory(authId, limit) {
  return request.get("/ozon/api/v1/price/task/history", {
    params: { authId, limit }
  });
}

/**
 * 根据SKU查询任务列表
 * @param {string} authId - 授权ID
 * @param {string} sku - SKU编码
 */
function listTasksBySku(authId, sku) {
  return request.get("/ozon/api/v1/price/task/by-sku", {
    params: { authId, sku }
  });
}

/**
 * 获取错误摘要
 * @param {string} authId - 授权ID
 */
function getErrorSummary(authId) {
  return request.get("/ozon/api/v1/price/task/error-summary", {
    params: { authId }
  });
}

export default {
  push,
  listSnapshots,
  listTasks,
  // 任务历史查询
  getTaskDetail,
  listTaskHistory,
  listTasksBySku,
  getErrorSummary
}
