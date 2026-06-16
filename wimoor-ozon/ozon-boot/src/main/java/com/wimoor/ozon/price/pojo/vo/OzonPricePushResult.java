package com.wimoor.ozon.price.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonPricePushResult {

    private String taskId;

    private Integer accepted;

    private Date submittedAt;

    private String message;
}
