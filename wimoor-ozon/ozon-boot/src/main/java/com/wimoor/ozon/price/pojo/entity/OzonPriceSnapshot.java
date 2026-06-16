package com.wimoor.ozon.price.pojo.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_price_snapshot")
public class OzonPriceSnapshot {

    @TableId("id")
    private String id;

    @TableField("task_id")
    private String taskId;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("material_sku")
    private String materialSku;

    @TableField("ozon_offer_id")
    private String ozonOfferId;

    @TableField("price")
    private BigDecimal price;

    @TableField("old_price")
    private BigDecimal oldPrice;

    @TableField("currency_code")
    private String currencyCode;

    @TableField("sync_status")
    private String syncStatus;

    @TableField("sync_message")
    private String syncMessage;

    @TableField("synced_at")
    private Date syncedAt;
}
