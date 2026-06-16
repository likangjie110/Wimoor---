package com.wimoor.ozon.product.pojo.vo;

import java.util.List;

import lombok.Data;

@Data
public class OzonProductPublishTaskView {

    private String taskStatus;
    private String remoteTaskId;
    private String requestPayloadJson;
    private String responsePayloadJson;
    private List<NormalizedItem> normalizedItems;
    private String errorSummary;

    @Data
    public static class NormalizedItem {
        private String offerId;
        private String productId;
        private String remoteStatus;
        private boolean hasErrors;
        private List<ErrorItem> errors;
    }

    @Data
    public static class ErrorItem {
        private String code;
        private String field;
        private Long attributeId;
        private String attributeName;
        private String message;
    }
}
