package com.wimoor.ozon.shipment.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_shipment")
public class OzonShipment {

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

    @TableField("tracking_number")
    private String trackingNumber;

    @TableField("delivery_service")
    private String deliveryService;

    @TableField("shipment_status")
    private String shipmentStatus;

    @TableField("request_payload_json")
    private String requestPayloadJson;

    @TableField("response_payload_json")
    private String responsePayloadJson;

    @TableField("operator")
    private String operator;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
