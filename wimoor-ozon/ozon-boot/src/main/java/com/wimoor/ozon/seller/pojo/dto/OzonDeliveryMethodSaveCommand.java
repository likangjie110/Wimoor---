package com.wimoor.ozon.seller.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonDeliveryMethodSaveCommand {

    private String authId;
    private String id;
    private String methodCode;
    private String methodName;
    private String description;
    private Boolean enabled;
    private Boolean defaultMethod;
}
