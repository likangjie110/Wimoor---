package com.wimoor.ozon.aftersale.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonReturnSaveCommand {

    private String authId;
    private String postingId;
    private String id;
    private String returnNumber;
    private String returnStatus;
    private String reason;
    private Integer quantity;
    private String rawPayloadJson;
}
