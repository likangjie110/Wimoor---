package com.wimoor.ozon.ads.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.ads.pojo.entity.OzonAdsReport;

@Mapper
public interface OzonAdsReportMapper extends BaseMapper<OzonAdsReport> {

    @Insert("insert into t_ozon_ads_report (id, auth_id, shop_id, account_id, campaign_id, report_date, impressions, clicks, spend, orders, sales, ctr, cpc, acos, roas, raw_line_json, create_time) "
            + "values (#{id}, #{authId}, #{shopId}, #{accountId}, #{campaignId}, #{reportDate}, #{impressions}, #{clicks}, #{spend}, #{orders}, #{sales}, #{ctr}, #{cpc}, #{acos}, #{roas}, #{rawLineJson}, #{createTime}) "
            + "on duplicate key update account_id = values(account_id), impressions = values(impressions), clicks = values(clicks), spend = values(spend), "
            + "orders = values(orders), sales = values(sales), ctr = values(ctr), cpc = values(cpc), acos = values(acos), roas = values(roas), raw_line_json = values(raw_line_json)")
    int upsert(OzonAdsReport report);
}
