package com.wimoor.ozon.seller.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonWarehouseSyncResult {

    private int warehouseCount;
    private Date syncedAt;
    private String message;
}
