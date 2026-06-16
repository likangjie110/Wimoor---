package com.wimoor.ozon.ops.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonApiLogRecordCommand {

    private String authId;
    private String shopId;
    private String apiGroup;
    private String actionName;
    private String endpoint;
    private String httpMethod;
    private String objectType;
    private String objectId;
    private String requestPayloadJson;
    private String responsePayloadJson;
    private String status;
    private String errorMessage;
    private Long durationMs;
    private String operator;
}
