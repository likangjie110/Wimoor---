package com.wimoor.ozon.product.pojo.vo;

import java.util.List;

import lombok.Data;

@Data
public class OzonProductDraftImportResult {

    private String draftId;
    private Integer importedCount;
    private Integer createdVariantCount;
    private Integer updatedVariantCount;
    private List<String> skippedSkus;
}
