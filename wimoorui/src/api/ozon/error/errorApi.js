import request from "@/utils/request.js";

function list(params) {
  return request.get("/ozon/api/v1/error/list", { params });
}

function retryOne(errorId) {
  return request.post("/ozon/api/v1/error/retryOne", null, {
    params: { errorId }
  });
}

function ignore(errorId) {
  return request.post("/ozon/api/v1/error/ignore", null, {
    params: { errorId }
  });
}

export default {
  list,
  retryOne,
  ignore
}
