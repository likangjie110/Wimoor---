import request from "@/utils/request.js";

function importReport(data) {
  return request.post("/ozon/api/v1/finance/import", data);
}

function listTasks(authId) {
  return request.get("/ozon/api/v1/finance/task/list", {
    params: { authId }
  });
}

function listTransactions(params) {
  return request.get("/ozon/api/v1/finance/transaction/list", { params });
}

function getRawContent(authId, taskId) {
  return request.get("/ozon/api/v1/finance/task/raw", {
    params: { authId, taskId }
  });
}

function syncTransactions(data) {
  return request.post("/ozon/api/v1/finance/sync/transactions", null, {
    params: data
  });
}

function syncRealizations(data) {
  return request.post("/ozon/api/v1/finance/sync/realizations", null, {
    params: data
  });
}

function fetchReport(data) {
  return request.post("/ozon/api/v1/finance/fetch/report", null, {
    params: data
  });
}

export default {
  importReport,
  listTasks,
  listTransactions,
  getRawContent,
  syncTransactions,
  syncRealizations,
  fetchReport
}
