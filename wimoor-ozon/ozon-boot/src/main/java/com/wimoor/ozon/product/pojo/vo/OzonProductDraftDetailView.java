package com.wimoor.ozon.product.pojo.vo;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class OzonProductDraftDetailView {

    private String draftId;
    private String draftName;
    private Long descriptionCategoryId;
    private String descriptionCategoryName;
    private Long typeId;
    private String typeName;
    private String titleSourceValue;
    private String titleOverrideValue;
    private String brandSourceValue;
    private String brandOverrideValue;
    private String descriptionSourceValue;
    private String descriptionOverrideValue;
    private String status;
    private String lastPreviewStatus;
    private String lastPreviewMessage;
    private String lastPublishTaskId;
    private List<AttributeItem> commonAttributes;
    private List<ImageItem> commonImages;
    private List<VariantItem> variants;

    @Data
    public static class VariantItem {
        private String variantId;
        private String materialSku;
        private String materialName;
        private String offerIdOverride;
        private String barcodeOverride;
        private BigDecimal priceSourceValue;
        private BigDecimal priceOverride;
        private BigDecimal weightSourceValue;
        private BigDecimal weightOverrideValue;
        private BigDecimal lengthSourceValue;
        private BigDecimal lengthOverrideValue;
        private BigDecimal widthSourceValue;
        private BigDecimal widthOverrideValue;
        private BigDecimal heightSourceValue;
        private BigDecimal heightOverrideValue;
        private String variantLabel;
        private String status;
        private String lastSyncStatus;
        private String lastSyncMessage;
        private List<AttributeItem> attributes;
        private List<ImageItem> images;
    }

    @Data
    public static class AttributeItem {
        private Long attributeId;
        private String attributeName;
        private String attributeValueJson;
        private Boolean requiredFlag;
        private String scope;
    }

    @Data
    public static class ImageItem {
        private String source;
        private String imageUrl;
        private Integer sortOrder;
        private Boolean primary;
        private String scope;
    }
}
