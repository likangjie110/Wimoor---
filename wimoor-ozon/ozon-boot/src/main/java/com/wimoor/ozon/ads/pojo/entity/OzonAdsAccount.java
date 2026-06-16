package com.wimoor.ozon.ads.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_ads_account")
public class OzonAdsAccount {

    @TableId("id")
    private String id;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("account_id")
    private String accountId;

    @TableField("account_name")
    private String accountName;

    @TableField("status")
    private String status;

    @TableField("currency_code")
    private String currencyCode;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
