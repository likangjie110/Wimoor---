package com.wimoor.ozon.price.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_price_task")
public class OzonPriceTask {

    @TableId("id")
    private String id;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("task_status")
    private String taskStatus;

    @TableField("requested_count")
    private Integer requestedCount;

    @TableField("success_count")
    private Integer successCount;

    @TableField("error_message")
    private String errorMessage;

    @TableField("operator")
    private String operator;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
