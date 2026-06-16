package com.wimoor.ozon.ads.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonAdsReportQuery {

    private String authId;

    private String accountId;

    private String campaignId;

    private String fromDate;

    private String toDate;
}
