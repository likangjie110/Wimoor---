package com.wimoor.ozon.ads.pojo.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_ads_report")
public class OzonAdsReport {

    @TableId("id")
    private String id;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("account_id")
    private String accountId;

    @TableField("campaign_id")
    private String campaignId;

    @TableField("report_date")
    private Date reportDate;

    @TableField("impressions")
    private Long impressions;

    @TableField("clicks")
    private Long clicks;

    @TableField("spend")
    private BigDecimal spend;

    @TableField("orders")
    private Long orders;

    @TableField("sales")
    private BigDecimal sales;

    @TableField("ctr")
    private BigDecimal ctr;

    @TableField("cpc")
    private BigDecimal cpc;

    @TableField("acos")
    private BigDecimal acos;

    @TableField("roas")
    private BigDecimal roas;

    @TableField("raw_line_json")
    private String rawLineJson;

    @TableField("create_time")
    private Date createTime;
}
