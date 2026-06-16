package com.wimoor.ozon.product.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonProductDraftListView {

    private String draftId;
    private String draftName;
    private Long descriptionCategoryId;
    private String descriptionCategoryName;
    private Long typeId;
    private String typeName;
    private String status;
    private Integer variantCount;
    private Date lastPublishAt;
}
