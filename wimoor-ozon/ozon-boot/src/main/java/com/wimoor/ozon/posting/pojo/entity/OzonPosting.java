package com.wimoor.ozon.posting.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_posting")
public class OzonPosting {

    @TableId("id")
    private String id;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("posting_number")
    private String postingNumber;

    @TableField("fulfillment_type")
    private String fulfillmentType;

    @TableField("posting_status")
    private String postingStatus;

    @TableField("substatus")
    private String substatus;

    @TableField("warehouse_id")
    private String warehouseId;

    @TableField("order_created_at")
    private Date orderCreatedAt;

    @TableField("shipment_deadline_at")
    private Date shipmentDeadlineAt;

    @TableField("customer_payload_json")
    private String customerPayloadJson;

    @TableField("erp_order_id")
    private String erpOrderId;

    @TableField("delivery_method_id")
    private String deliveryMethodId;

    @TableField("bridge_status")
    private String bridgeStatus;

    @TableField("sync_version")
    private Integer syncVersion;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
