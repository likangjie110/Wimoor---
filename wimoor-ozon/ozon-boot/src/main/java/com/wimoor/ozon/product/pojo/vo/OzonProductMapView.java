package com.wimoor.ozon.product.pojo.vo;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

@Data
public class OzonProductMapView {

    private String id;
    private String materialSku;
    private String materialName;
    private String ownerName;
    private String image;
    private BigDecimal materialPrice;
    private String ozonOfferId;
    private String ozonSku;
    private String ozonProductId;
    private String status;
    private String lastSyncStatus;
    private String lastSyncMessage;
    private Date lastSyncTime;
}
