import request from "@/utils/request.js";

function push(data) {
  return request.post("/ozon/api/v1/stock/push", data);
}

function listSnapshots(authId) {
  return request.get("/ozon/api/v1/stock/snapshot/list", { params: { authId } });
}

function listTasks(authId) {
  return request.get("/ozon/api/v1/stock/task/list", { params: { authId } });
}

export default {
  push,
  listSnapshots,
  listTasks
}
