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

    public OzonApiLogRecordCommand(
            String authId,
            String shopId,
            String apiGroup,
            String actionName,
            String endpoint,
            String requestPayloadJson,
            String responsePayloadJson,
            String status,
            String errorMessage,
            long durationMs
    ) {
        this.authId = authId;
        this.shopId = shopId;
        this.apiGroup = apiGroup;
        this.actionName = actionName;
        this.endpoint = endpoint;
        this.httpMethod = "POST";
        this.objectType = apiGroup;
        this.requestPayloadJson = requestPayloadJson;
        this.responsePayloadJson = responsePayloadJson;
        this.status = status;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
    }
}
