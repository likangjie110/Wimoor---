package com.wimoor.ozon.posting.pojo.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class OzonPostingDetailView {

    private String id;
    private String authId;
    private String postingNumber;
    private String fulfillmentType;
    private String postingStatus;
    private String substatus;
    private String warehouseId;
    private String erpOrderId;
    private String bridgeStatus;
    private Integer syncVersion;
    private Date orderCreatedAt;
    private Date shipmentDeadlineAt;
    private String rawPayloadJson;
    private List<ItemView> items = new ArrayList<>();
    private List<ShipmentView> shipments = new ArrayList<>();

    @Data
    public static class ItemView {
        private String itemId;
        private String materialSku;
        private String ozonOfferId;
        private Integer quantity;
    }

    @Data
    public static class ShipmentView {
        private String shipmentId;
        private String trackingNumber;
        private String deliveryService;
        private String shipmentStatus;
        private Date createdAt;
    }
}
