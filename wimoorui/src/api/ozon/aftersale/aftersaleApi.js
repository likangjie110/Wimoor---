import request from '@/utils/request';

/**
 * 获取售后详情
 */
export function getAfterSaleDetail(authId, postingId) {
  return request({
    url: '/api/v1/posting/aftersale/detail',
    method: 'get',
    params: { authId, postingId }
  });
}

/**
 * 保存包裹信息
 */
export function savePackage(data) {
  return request({
    url: '/api/v1/posting/aftersale/package/save',
    method: 'post',
    data
  });
}

/**
 * 保存退货信息
 */
export function saveReturn(data) {
  return request({
    url: '/api/v1/posting/aftersale/return/save',
    method: 'post',
    data
  });
}

/**
 * 保存取消信息
 */
export function saveCancellation(data) {
  return request({
    url: '/api/v1/posting/aftersale/cancellation/save',
    method: 'post',
    data
  });
}

/**
 * 通过 API 取消订单
 */
export function cancelPostingWithApi(authId, postingId, reason) {
  return request({
    url: '/api/v1/posting/aftersale/posting/cancel',
    method: 'post',
    params: { authId, postingId, reason }
  });
}

/**
 * 从 API 同步包裹信息
 */
export function syncPackagesFromApi(authId, postingId) {
  return request({
    url: '/api/v1/posting/aftersale/package/sync',
    method: 'post',
    params: { authId, postingId }
  });
}

/**
 * 从 API 同步退货信息
 */
export function syncReturnsFromApi(authId, postingId) {
  return request({
    url: '/api/v1/posting/aftersale/return/sync',
    method: 'post',
    params: { authId, postingId }
  });
}
