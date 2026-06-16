import request from "@/utils/request.js";

function list(params) {
  return request.get("/ozon/api/v1/task/list", { params });
}

export default {
  list
}
