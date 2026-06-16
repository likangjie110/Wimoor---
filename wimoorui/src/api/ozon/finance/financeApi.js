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

export default {
  importReport,
  listTasks,
  listTransactions,
  getRawContent
}
