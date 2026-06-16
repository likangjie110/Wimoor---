package com.wimoor.ozon.ops.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonOperationAuditRecordCommand {

    private String authId;
    private String shopId;
    private String operationType;
    private String objectType;
    private String objectId;
    private String objectCode;
    private String requestPayloadJson;
    private String resultStatus;
    private String resultMessage;
    private String operator;
}
