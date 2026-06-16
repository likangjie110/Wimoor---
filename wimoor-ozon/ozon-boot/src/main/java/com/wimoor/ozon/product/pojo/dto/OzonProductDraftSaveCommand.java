package com.wimoor.ozon.product.pojo.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class OzonProductDraftSaveCommand {

    private String authId;
    private String draftId;
    private String draftName;
    private Long descriptionCategoryId;
    private String descriptionCategoryName;
    private Long typeId;
    private String typeName;
    private String titleOverrideValue;
    private String brandOverrideValue;
    private String descriptionOverrideValue;
    private List<AttributeItem> commonAttributes;
    private List<ImageItem> commonImages;
    private List<VariantItem> variants;

    @Data
    public static class VariantItem {
        private String materialSku;
        private String materialName;
        private String offerIdOverride;
        private String barcodeOverride;
        private BigDecimal priceOverride;
        private BigDecimal weightOverrideValue;
        private BigDecimal lengthOverrideValue;
        private BigDecimal widthOverrideValue;
        private BigDecimal heightOverrideValue;
        private String variantLabel;
        private List<AttributeItem> attributes;
        private List<ImageItem> images;
    }

    @Data
    public static class AttributeItem {
        private Long attributeId;
        private String attributeName;
        private String mode;
        private List<AttributeValue> values;
    }

    @Data
    public static class AttributeValue {
        private Long dictionaryValueId;
        private String text;
    }

    @Data
    public static class ImageItem {
        private String source;
        private String imageUrl;
        private Integer sortOrder;
        private Boolean primary;
    }
}
