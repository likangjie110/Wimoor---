package com.wimoor.ozon.product.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonProductPreviewCommand {

    private String authId;
    private String draftId;
}
