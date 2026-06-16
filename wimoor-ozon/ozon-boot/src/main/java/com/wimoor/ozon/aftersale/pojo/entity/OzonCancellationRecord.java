package com.wimoor.ozon.aftersale.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_cancellation")
public class OzonCancellationRecord {

    @TableId("id")
    private String id;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("posting_id")
    private String postingId;

    @TableField("posting_number")
    private String postingNumber;

    @TableField("cancellation_number")
    private String cancellationNumber;

    @TableField("cancellation_status")
    private String cancellationStatus;

    @TableField("reason")
    private String reason;

    @TableField("raw_payload_json")
    private String rawPayloadJson;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
