package com.wimoor.ozon.product.pojo.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonProductDraftImportCommand {

    private String authId;
    private String draftId;
    private String draftName;
    private List<String> skus;
}
