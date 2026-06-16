package com.wimoor.ozon.ads.pojo.vo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class OzonAdsSummary {

    private Long impressions;

    private Long clicks;

    private BigDecimal spend;

    private Long orders;

    private BigDecimal sales;

    private BigDecimal acos;

    private BigDecimal roas;
}
