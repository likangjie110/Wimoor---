package com.wimoor.ozon.aftersale.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_package")
public class OzonPackageRecord {

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

    @TableField("package_number")
    private String packageNumber;

    @TableField("package_status")
    private String packageStatus;

    @TableField("tracking_number")
    private String trackingNumber;

    @TableField("raw_payload_json")
    private String rawPayloadJson;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
