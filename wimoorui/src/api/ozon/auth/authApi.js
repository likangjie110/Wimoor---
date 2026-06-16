import request from "@/utils/request.js";

function bind(data) {
  return request.post("/ozon/api/v1/auth/bind", data);
}

function list() {
  return request.get("/ozon/api/v1/auth/list");
}

function ping(authId) {
  return request.get("/ozon/api/v1/auth/ping", { params: { authId } });
}

function disable(authId) {
  return request.post("/ozon/api/v1/auth/disable", null, { params: { authId } });
}

function rotateKey(data) {
  return request.post("/ozon/api/v1/auth/rotateKey", data);
}

function listWarehouses(authId) {
  return request.get("/ozon/api/v1/seller/warehouse/list", { params: { authId } });
}

function listDeliveryMethods(authId) {
  return request.get("/ozon/api/v1/seller/deliveryMethod/list", { params: { authId } });
}

function saveDeliveryMethod(data) {
  return request.post("/ozon/api/v1/seller/deliveryMethod/save", data);
}

export default {
  bind,
  list,
  ping,
  disable,
  rotateKey,
  listWarehouses,
  listDeliveryMethods,
  saveDeliveryMethod
}
