package com.wimoor.ozon.product.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_listing_attribute")
public class OzonListingAttribute {

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

    @TableField("attribute_id")
    private Long attributeId;

    @TableField("attribute_name")
    private String attributeName;

    @TableField("attribute_value_json")
    private String attributeValueJson;

    @TableField("required_flag")
    private Boolean requiredFlag;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
