package com.wimoor.ozon.shipment.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonShipmentPushResult {

    private String shipmentId;

    private String postingNumber;

    private String trackingNumber;

    private String shipmentStatus;

    private Date pushedAt;
}
