package com.wimoor.ozon.product.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonProductMapSaveCommand {

    private String authId;
    private String materialSku;
    private String ozonOfferId;
    private String ozonSku;
    private String ozonProductId;
}
