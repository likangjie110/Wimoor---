package com.wimoor.ozon.error.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonErrorView {

    private String id;

    private String authId;

    private String sourceType;

    private String objectId;

    private String objectCode;

    private String status;

    private String errorMessage;

    private String requestPayloadJson;

    private String responsePayloadJson;

    private Integer retryCount;

    private Date lastRetryAt;

    private String operator;

    private Date createdAt;

    private Date updatedAt;
}
