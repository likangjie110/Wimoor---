package com.wimoor.ozon.shipment.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonShipmentPushCommand {

    private String authId;

    private String postingId;

    private String trackingNumber;

    private String deliveryService;
}
