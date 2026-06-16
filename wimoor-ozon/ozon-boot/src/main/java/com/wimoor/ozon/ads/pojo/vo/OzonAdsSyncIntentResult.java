package com.wimoor.ozon.ads.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonAdsSyncIntentResult {

    private String requestId;

    private String accountId;

    private String campaignId;

    private String requestStatus;

    private String message;

    private Date requestedAt;
}
