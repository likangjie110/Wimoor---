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

export default {
  push,
  listSnapshots,
  listTasks
}
