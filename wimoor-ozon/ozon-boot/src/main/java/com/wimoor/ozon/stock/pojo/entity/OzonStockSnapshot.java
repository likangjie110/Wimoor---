package com.wimoor.ozon.stock.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_stock_snapshot")
public class OzonStockSnapshot {

    @TableId("id")
    private String id;

    @TableField("task_id")
    private String taskId;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("warehouse_id")
    private String warehouseId;

    @TableField("material_sku")
    private String materialSku;

    @TableField("ozon_offer_id")
    private String ozonOfferId;

    @TableField("quantity")
    private Integer quantity;

    @TableField("sync_status")
    private String syncStatus;

    @TableField("sync_message")
    private String syncMessage;

    @TableField("synced_at")
    private Date syncedAt;
}
