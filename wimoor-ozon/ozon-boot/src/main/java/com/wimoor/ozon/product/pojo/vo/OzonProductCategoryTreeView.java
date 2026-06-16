package com.wimoor.ozon.product.pojo.vo;

import java.util.List;

import lombok.Data;

@Data
public class OzonProductCategoryTreeView {

    private List<CategoryItem> categories;

    @Data
    public static class CategoryItem {
        private Long descriptionCategoryId;
        private String descriptionCategoryName;
        private List<TypeItem> types;
    }

    @Data
    public static class TypeItem {
        private Long typeId;
        private String typeName;
    }
}
