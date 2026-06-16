package com.wimoor.ozon.ops.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_operation_audit")
public class OzonOperationAudit {

    @TableId("id")
    private String id;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("operation_type")
    private String operationType;

    @TableField("object_type")
    private String objectType;

    @TableField("object_id")
    private String objectId;

    @TableField("object_code")
    private String objectCode;

    @TableField("request_payload_json")
    private String requestPayloadJson;

    @TableField("result_status")
    private String resultStatus;

    @TableField("result_message")
    private String resultMessage;

    @TableField("operator")
    private String operator;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
