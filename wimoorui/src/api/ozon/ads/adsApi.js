import request from "@/utils/request.js";

function importAds(data) {
  return request.post("/ozon/api/v1/ads/import", data);
}

function listAccounts(authId) {
  return request.get("/ozon/api/v1/ads/account/list", {
    params: { authId }
  });
}

function listCampaigns(params) {
  return request.get("/ozon/api/v1/ads/campaign/list", { params });
}

function listReports(params) {
  return request.get("/ozon/api/v1/ads/report/list", { params });
}

function getSummary(params) {
  return request.get("/ozon/api/v1/ads/summary", { params });
}

function createSyncIntent(data) {
  return request.post("/ozon/api/v1/ads/sync/intent", data);
}

export default {
  importAds,
  listAccounts,
  listCampaigns,
  listReports,
  getSummary,
  createSyncIntent
}
