package com.wimoor.ozon.posting.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_posting_item")
public class OzonPostingItem {

    @TableId("id")
    private String id;

    @TableField("posting_id")
    private String postingId;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("posting_number")
    private String postingNumber;

    @TableField("material_sku")
    private String materialSku;

    @TableField("ozon_offer_id")
    private String ozonOfferId;

    @TableField("quantity")
    private Integer quantity;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
