import request from "@/utils/request.js";

function list(params) {
  return request.get("/ozon/api/v1/product/list", { params });
}

function importDraft(data) {
  return request.post("/ozon/api/v1/product/importDraft", data);
}

function saveMapping(data) {
  return request.post("/ozon/api/v1/product/map/save", data);
}

function listDrafts(params) {
  return request.get("/ozon/api/v1/product/draft/list", { params });
}

function detail(params) {
  return request.get("/ozon/api/v1/product/draft/detail", { params });
}

function saveDraft(data) {
  return request.post("/ozon/api/v1/product/draft/save", data);
}

function categoryTree(params) {
  return request.get("/ozon/api/v1/product/category/tree", { params });
}

function categoryTemplate(params) {
  return request.get("/ozon/api/v1/product/category/template", { params });
}

function preview(data) {
  return request.post("/ozon/api/v1/product/preview", data);
}

function publish(data) {
  return request.post("/ozon/api/v1/product/publish", data);
}

function publishTaskDetail(params) {
  return request.get("/ozon/api/v1/product/publish/task/detail", { params });
}

function publishTaskList(params) {
  return request.get("/ozon/api/v1/product/publish/task/list", { params });
}

export default {
  list,
  importDraft,
  saveMapping,
  listDrafts,
  detail,
  saveDraft,
  categoryTree,
  categoryTemplate,
  preview,
  publish,
  publishTaskDetail,
  publishTaskList
}
