package com.wimoor.ozon.finance.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_report_task")
public class OzonReportTask {

    @TableId("id")
    private String id;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("report_id")
    private String reportId;

    @TableField("report_date")
    private Date reportDate;

    @TableField("task_status")
    private String taskStatus;

    @TableField("imported_count")
    private Integer importedCount;

    @TableField("error_message")
    private String errorMessage;

    @TableField("operator")
    private String operator;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
