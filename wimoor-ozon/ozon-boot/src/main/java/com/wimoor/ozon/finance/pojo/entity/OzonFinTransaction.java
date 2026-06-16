package com.wimoor.ozon.finance.pojo.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_fin_transaction")
public class OzonFinTransaction {

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

    @TableField("transaction_id")
    private String transactionId;

    @TableField("operation_type")
    private String operationType;

    @TableField("posting_number")
    private String postingNumber;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("currency_code")
    private String currencyCode;

    @TableField("transaction_time")
    private Date transactionTime;

    @TableField("raw_line_json")
    private String rawLineJson;

    @TableField("create_time")
    private Date createTime;
}
