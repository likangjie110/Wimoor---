package com.wimoor.ozon.seller.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_shop_config")
public class OzonShopConfig {

    @TableId("id")
    private String id;

    @TableField("shop_id")
    private String shopId;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_name")
    private String shopName;

    @TableField("seller_code")
    private String sellerCode;

    @TableField("default_warehouse_id")
    private String defaultWarehouseId;

    @TableField("status")
    private String status;

    @TableField("last_warehouse_sync_time")
    private Date lastWarehouseSyncTime;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
