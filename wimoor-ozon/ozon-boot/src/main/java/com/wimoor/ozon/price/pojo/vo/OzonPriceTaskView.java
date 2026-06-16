package com.wimoor.ozon.price.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonPriceTaskView {

    private String taskId;
    private String taskStatus;
    private Integer requestedCount;
    private Integer successCount;
    private String errorMessage;
    private String operator;
    private Date createdAt;
    private Date updatedAt;
}
