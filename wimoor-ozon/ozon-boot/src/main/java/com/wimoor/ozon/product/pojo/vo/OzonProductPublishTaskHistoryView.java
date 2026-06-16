package com.wimoor.ozon.product.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonProductPublishTaskHistoryView {

    private String taskId;
    private String draftId;
    private String taskStatus;
    private String remoteTaskId;
    private String errorSummary;
    private String operator;
    private Date createdAt;
    private Date updatedAt;
}
