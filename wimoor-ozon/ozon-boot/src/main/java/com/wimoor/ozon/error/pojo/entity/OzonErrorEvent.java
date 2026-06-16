package com.wimoor.ozon.error.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_error_event")
public class OzonErrorEvent {

    @TableId("id")
    private String id;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("source_type")
    private String sourceType;

    @TableField("object_id")
    private String objectId;

    @TableField("object_code")
    private String objectCode;

    @TableField("status")
    private String status;

    @TableField("error_message")
    private String errorMessage;

    @TableField("request_payload_json")
    private String requestPayloadJson;

    @TableField("response_payload_json")
    private String responsePayloadJson;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("last_retry_at")
    private Date lastRetryAt;

    @TableField("operator")
    private String operator;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
