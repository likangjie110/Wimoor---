package com.wimoor.ozon.error.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonErrorRecordCommand {

    private String authId;

    private String shopId;

    private String sourceType;

    private String objectId;

    private String objectCode;

    private String errorMessage;

    private String requestPayloadJson;

    private String responsePayloadJson;

    private String operator;
}
