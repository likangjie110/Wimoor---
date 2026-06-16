package com.wimoor.ozon.seller.service;

import com.wimoor.ozon.seller.pojo.vo.OzonWarehouseSyncResult;

public interface IOzonWarehouseSyncService {

    OzonWarehouseSyncResult syncWarehouses(String authId);
}
