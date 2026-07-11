package com.wimoor.ozon.product.pojo.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_listing_variant")
public class OzonListingVariant {

    @TableId("id")
    private String id;

    @TableField("draft_id")
    private String draftId;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("material_sku")
    private String materialSku;

    @TableField("material_name")
    private String materialName;

    @TableField("offer_id_override")
    private String offerIdOverride;

    @TableField("barcode_override")
    private String barcodeOverride;

    @TableField("price_source_value")
    private BigDecimal priceSourceValue;

    @TableField("price_override")
    private BigDecimal priceOverride;

    @TableField("weight_source_value")
    private BigDecimal weightSourceValue;

    @TableField("weight_override_value")
    private BigDecimal weightOverrideValue;

    @TableField("length_source_value")
    private BigDecimal lengthSourceValue;

    @TableField("length_override_value")
    private BigDecimal lengthOverrideValue;

    @TableField("width_source_value")
    private BigDecimal widthSourceValue;

    @TableField("width_override_value")
    private BigDecimal widthOverrideValue;

    @TableField("height_source_value")
    private BigDecimal heightSourceValue;

    @TableField("height_override_value")
    private BigDecimal heightOverrideValue;

    @TableField("variant_label")
    private String variantLabel;

    @TableField("status")
    private String status;

    @TableField("last_sync_status")
    private String lastSyncStatus;

    @TableField("last_sync_message")
    private String lastSyncMessage;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField(exist = false)
    private BigDecimal oldPrice;

    @TableField(exist = false)
    private String vat;

    public String getVariantSku() {
        return materialSku;
    }

    public void setVariantSku(String variantSku) {
        this.materialSku = variantSku;
    }

    public BigDecimal getPrice() {
        return priceOverride;
    }

    public void setPrice(BigDecimal price) {
        this.priceOverride = price;
    }
}
