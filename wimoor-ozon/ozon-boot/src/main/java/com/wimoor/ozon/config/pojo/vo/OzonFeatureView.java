package com.wimoor.ozon.config.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OzonFeatureView {

    private FeatureItem auth;
    private FeatureItem product;
    private FeatureItem productWrite;
    private FeatureItem task;
    private FeatureItem error;
    private FeatureItem finance;
    private FeatureItem chat;
    private FeatureItem ads;
    private FeatureItem stockWrite;
    private FeatureItem priceWrite;
    private FeatureItem postingWrite;
    private FeatureItem chatSend;
    private FeatureItem adsSync;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureItem {
        private boolean enabled;
        private String reason;
    }
}
