import postingApi from '@/api/ozon/posting/postingApi.js';

function pushTracking(data) {
  return postingApi.pushTracking(data);
}

function list(params) {
  return postingApi.listShipmentHistory(params.authId, params.postingId);
}

export default {
  pushTracking,
  list
};
