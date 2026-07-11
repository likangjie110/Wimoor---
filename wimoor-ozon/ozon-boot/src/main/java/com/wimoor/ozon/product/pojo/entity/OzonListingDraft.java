package com.wimoor.ozon.product.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_listing_draft")
public class OzonListingDraft {

    @TableId("id")
    private String id;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("draft_name")
    private String draftName;

    @TableField("description_category_id")
    private Long descriptionCategoryId;

    @TableField("description_category_name")
    private String descriptionCategoryName;

    @TableField("type_id")
    private Long typeId;

    @TableField("type_name")
    private String typeName;

    @TableField("title_source_value")
    private String titleSourceValue;

    @TableField("title_override_value")
    private String titleOverrideValue;

    @TableField("brand_source_value")
    private String brandSourceValue;

    @TableField("brand_override_value")
    private String brandOverrideValue;

    @TableField("description_source_value")
    private String descriptionSourceValue;

    @TableField("description_override_value")
    private String descriptionOverrideValue;

    @TableField("status")
    private String status;

    @TableField("last_preview_status")
    private String lastPreviewStatus;

    @TableField("last_preview_message")
    private String lastPreviewMessage;

    @TableField("last_publish_task_id")
    private String lastPublishTaskId;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField(exist = false)
    private String materialSku;

    public String getName() {
        return draftName;
    }

    public void setName(String name) {
        this.draftName = name;
    }

    public Long getCategoryId() {
        return descriptionCategoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.descriptionCategoryId = categoryId;
    }

    public String getDescription() {
        return descriptionOverrideValue;
    }

    public void setDescription(String description) {
        this.descriptionOverrideValue = description;
    }
}
