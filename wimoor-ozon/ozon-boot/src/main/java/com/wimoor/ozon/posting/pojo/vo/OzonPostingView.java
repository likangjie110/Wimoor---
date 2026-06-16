package com.wimoor.ozon.posting.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonPostingView {

    private String id;

    private String postingNumber;

    private String fulfillmentType;

    private String postingStatus;

    private String substatus;

    private String warehouseId;

    private String erpOrderId;

    private String bridgeStatus;

    private String itemSummary;

    private String rawPayloadJson;

    private Date orderCreatedAt;

    private Date shipmentDeadlineAt;

    private Integer syncVersion;

    private String latestTrackingNumber;

    private String latestDeliveryService;

    private String latestShipmentStatus;
}
