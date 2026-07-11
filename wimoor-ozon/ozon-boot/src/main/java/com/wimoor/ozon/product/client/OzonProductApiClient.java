package com.wimoor.ozon.product.client;

import java.util.List;

import com.wimoor.ozon.ops.annotation.OzonApiLog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public interface OzonProductApiClient {

    default List<CategoryNode> listCategoryTree(String clientId, String apiKey) {
        return listCategoryTree(clientId, apiKey, null);
    }

    @OzonApiLog(apiGroup = "Product", actionName = "listCategoryTree", objectType = "Category")
    List<CategoryNode> listCategoryTree(String clientId, String apiKey, String language);

    default List<AttributeTemplateItem> listAttributes(String clientId, String apiKey, Long descriptionCategoryId, Long typeId) {
        return listAttributes(clientId, apiKey, descriptionCategoryId, typeId, null);
    }

    @OzonApiLog(apiGroup = "Product", actionName = "listAttributes", objectType = "Attribute")
    List<AttributeTemplateItem> listAttributes(String clientId, String apiKey, Long descriptionCategoryId, Long typeId, String language);

    @OzonApiLog(apiGroup = "Product", actionName = "submitProductImport", objectType = "Product")
    String submitProductImport(String clientId, String apiKey, String requestPayloadJson);

    @OzonApiLog(apiGroup = "Product", actionName = "getProductImportInfo", objectType = "Product")
    ProductImportInfo getProductImportInfo(String clientId, String apiKey, String remoteTaskId);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CategoryNode {
        private Long descriptionCategoryId;
        private String categoryName;
        private Long typeId;
        private String typeName;
        private List<CategoryNode> children;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class AttributeTemplateItem {
        private Long id;
        private String name;
        private String description;
        private String type;
        private Boolean isAspect;
        private Boolean isCollection;
        private Boolean isRequired;
        private Long dictionaryId;
        private Integer maxValueCount;
        private List<DictionaryValue> dictionaryValues;
        private List<String> sampleTexts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class DictionaryValue {
        private Long dictionaryValueId;
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ProductImportInfo {
        private List<ProductImportItem> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ProductImportItem {
        private String offerId;
        private String productId;
        private String status;
        private List<ProductImportError> errors;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ProductImportError {
        private String code;
        private String field;
        private Long attributeId;
        private String attributeName;
        private String message;
    }
}
