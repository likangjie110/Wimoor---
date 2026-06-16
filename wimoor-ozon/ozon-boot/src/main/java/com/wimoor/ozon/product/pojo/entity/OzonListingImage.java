package com.wimoor.ozon.product.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_listing_image")
public class OzonListingImage {

    @TableId("id")
    private String id;

    @TableField("draft_id")
    private String draftId;

    @TableField("variant_id")
    private String variantId;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("scope")
    private String scope;

    @TableField("source")
    private String source;

    @TableField("image_url")
    private String imageUrl;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("is_primary")
    private Boolean primary;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
