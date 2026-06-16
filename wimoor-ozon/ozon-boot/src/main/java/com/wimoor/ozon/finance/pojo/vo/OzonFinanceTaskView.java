package com.wimoor.ozon.finance.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonFinanceTaskView {

    private String id;

    private String authId;

    private String reportId;

    private Date reportDate;

    private String taskStatus;

    private Integer importedCount;

    private String sourceMode;

    private Boolean rawContentReady;

    private String errorMessage;

    private String operator;

    private Date createdAt;

    private Date updatedAt;
}
