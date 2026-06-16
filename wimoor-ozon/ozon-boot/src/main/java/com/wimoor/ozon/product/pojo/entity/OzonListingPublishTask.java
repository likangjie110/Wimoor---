package com.wimoor.ozon.product.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_listing_publish_task")
public class OzonListingPublishTask {

    @TableId("id")
    private String id;

    @TableField("draft_id")
    private String draftId;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("task_status")
    private String taskStatus;

    @TableField("remote_task_id")
    private String remoteTaskId;

    @TableField("request_payload_json")
    private String requestPayloadJson;

    @TableField("response_payload_json")
    private String responsePayloadJson;

    @TableField("error_message")
    private String errorMessage;

    @TableField("operator")
    private String operator;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
