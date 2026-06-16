import request from "@/utils/request.js";

function sync(data) {
  return request.post("/ozon/api/v1/posting/sync", data);
}

function list(params) {
  return request.get("/ozon/api/v1/posting/list", { params });
}

function detail(authId, postingId) {
  return request.get("/ozon/api/v1/posting/detail", {
    params: { authId, postingId }
  });
}

function afterSaleDetail(authId, postingId) {
  return request.get("/ozon/api/v1/posting/aftersale/detail", {
    params: { authId, postingId }
  });
}

function retryOne(authId, postingId) {
  return request.post("/ozon/api/v1/posting/retryOne", null, {
    params: { authId, postingId }
  });
}

function pushTracking(data) {
  return request.post("/ozon/api/v1/shipment/pushTracking", data);
}

function listShipmentHistory(authId, postingId) {
  return request.get("/ozon/api/v1/shipment/list", {
    params: { authId, postingId }
  });
}

function savePackage(data) {
  return request.post("/ozon/api/v1/posting/aftersale/package/save", data);
}

function saveReturn(data) {
  return request.post("/ozon/api/v1/posting/aftersale/return/save", data);
}

function saveCancellation(data) {
  return request.post("/ozon/api/v1/posting/aftersale/cancellation/save", data);
}

export default {
  sync,
  list,
  detail,
  afterSaleDetail,
  retryOne,
  pushTracking,
  listShipmentHistory,
  savePackage,
  saveReturn,
  saveCancellation
}
