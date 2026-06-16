package com.wimoor.ozon.product.pojo.vo;

import lombok.Data;

@Data
public class OzonProductPublishView {

    private String draftId;
    private String localTaskId;
    private String remoteTaskId;
    private String taskStatus;
    private OzonProductPublishTaskView resultSummary;
}
