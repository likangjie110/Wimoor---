package com.wimoor.ozon.finance.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_report_file")
public class OzonReportFile {

    @TableId("id")
    private String id;

    @TableField("task_id")
    private String taskId;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("report_id")
    private String reportId;

    @TableField("report_date")
    private Date reportDate;

    @TableField("content_type")
    private String contentType;

    @TableField("raw_content")
    private String rawContent;

    @TableField("create_time")
    private Date createTime;
}
