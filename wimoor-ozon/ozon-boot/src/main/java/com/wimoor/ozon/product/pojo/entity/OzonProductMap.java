package com.wimoor.ozon.product.pojo.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_product_map")
public class OzonProductMap {

    @TableId("id")
    private String id;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("material_sku")
    private String materialSku;

    @TableField("material_name")
    private String materialName;

    @TableField("owner_name")
    private String ownerName;

    @TableField("image")
    private String image;

    @TableField("material_price")
    private BigDecimal materialPrice;

    @TableField("ozon_offer_id")
    private String ozonOfferId;

    @TableField("ozon_sku")
    private String ozonSku;

    @TableField("ozon_product_id")
    private String ozonProductId;

    @TableField("status")
    private String status;

    @TableField("last_sync_status")
    private String lastSyncStatus;

    @TableField("last_sync_message")
    private String lastSyncMessage;

    @TableField("last_sync_time")
    private Date lastSyncTime;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
