package com.wimoor.ozon.stock.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonStockPushResult {

    private String taskId;
    private int accepted;
    private Date submittedAt;
    private String message;
}
