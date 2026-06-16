package com.wimoor.ozon.stock.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonStockTaskView {

    private String taskId;
    private String warehouseId;
    private String taskStatus;
    private Integer requestedCount;
    private Integer successCount;
    private String errorMessage;
    private String operator;
    private Date createdAt;
    private Date updatedAt;
}
