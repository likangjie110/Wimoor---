package com.wimoor.ozon.seller.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonWarehouseView {

    private String id;
    private String authId;
    private String warehouseId;
    private String name;
    private String status;
    private String warehouseType;
    private Boolean active;
    private Boolean defaultWarehouse;
    private Date syncedAt;
    private Date lastWarehouseSyncTime;
}
