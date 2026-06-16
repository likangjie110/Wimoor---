package com.wimoor.ozon.ads.pojo.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_ads_campaign")
public class OzonAdsCampaign {

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

    @TableField("campaign_name")
    private String campaignName;

    @TableField("campaign_type")
    private String campaignType;

    @TableField("campaign_status")
    private String campaignStatus;

    @TableField("budget")
    private BigDecimal budget;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
