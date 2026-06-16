package com.wimoor.ozon.task.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonTaskView {

    private String id;

    private String authId;

    private String jobType;

    private String status;

    private String payload;

    private String operator;

    private Date createdAt;

    private Date updatedAt;
}
