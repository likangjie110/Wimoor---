import request from "@/utils/request.js";

function summary(authId) {
  return request.get("/ozon/api/v1/ops/summary", { params: { authId } });
}

function dashboard(authId) {
  return request.get("/ozon/api/v1/ops/dashboard", { params: { authId } });
}

function listApiLogs(params) {
  return request.get("/ozon/api/v1/ops/api-log/list", { params });
}

function listOperationAudits(params) {
  return request.get("/ozon/api/v1/ops/operation-audit/list", { params });
}

export default {
  dashboard,
  summary,
  listApiLogs,
  listOperationAudits
}
