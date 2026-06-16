package com.wimoor.ozon.product.pojo.vo;

import java.util.List;

import lombok.Data;

@Data
public class OzonProductCategoryTemplateView {

    private Long descriptionCategoryId;
    private String descriptionCategoryName;
    private Long typeId;
    private String typeName;
    private List<AttributeItem> commonAttributes;
    private List<AttributeItem> variantAttributes;
    private int requiredImageCount;
    private boolean requiresBarcode;

    @Data
    public static class AttributeItem {
        private Long attributeId;
        private String attributeName;
        private String mode;
        private Boolean required;
        private List<AttributeValue> values;
    }

    @Data
    public static class AttributeValue {
        private Long dictionaryValueId;
        private String text;
    }
}
