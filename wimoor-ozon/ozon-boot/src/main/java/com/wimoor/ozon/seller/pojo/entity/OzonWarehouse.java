package com.wimoor.ozon.seller.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_warehouse")
public class OzonWarehouse {

    @TableId("id")
    private String id;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("warehouse_id")
    private String warehouseId;

    @TableField("name")
    private String name;

    @TableField("status")
    private String status;

    @TableField("warehouse_type")
    private String warehouseType;

    @TableField("active")
    private Boolean active;

    @TableField("raw_data")
    private String rawData;

    @TableField("synced_at")
    private Date syncedAt;
}
