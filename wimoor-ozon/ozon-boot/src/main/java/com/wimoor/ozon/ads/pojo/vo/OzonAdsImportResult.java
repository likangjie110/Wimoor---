package com.wimoor.ozon.ads.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonAdsImportResult {

    private Integer campaignCount;

    private Integer reportCount;

    private Date importedAt;
}
