package com.wimoor.ozon.aftersale.pojo.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class OzonAfterSaleDetailView {

    private List<PackageItem> packages = new ArrayList<>();
    private List<ReturnItem> returns = new ArrayList<>();
    private List<CancellationItem> cancellations = new ArrayList<>();

    @Data
    public static class PackageItem {
        private String id;
        private String packageNumber;
        private String packageStatus;
        private String trackingNumber;
        private String rawPayloadJson;
        private Date createdAt;
        private Date updatedAt;
    }

    @Data
    public static class ReturnItem {
        private String id;
        private String returnNumber;
        private String returnStatus;
        private String reason;
        private Integer quantity;
        private String rawPayloadJson;
        private Date createdAt;
        private Date updatedAt;
    }

    @Data
    public static class CancellationItem {
        private String id;
        private String cancellationNumber;
        private String cancellationStatus;
        private String reason;
        private String rawPayloadJson;
        private Date createdAt;
        private Date updatedAt;
    }
}
