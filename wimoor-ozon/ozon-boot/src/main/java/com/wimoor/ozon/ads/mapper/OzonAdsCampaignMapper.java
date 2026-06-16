package com.wimoor.ozon.ads.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.ads.pojo.entity.OzonAdsCampaign;

@Mapper
public interface OzonAdsCampaignMapper extends BaseMapper<OzonAdsCampaign> {

    @Insert("insert into t_ozon_ads_campaign (id, auth_id, shop_id, account_id, campaign_id, campaign_name, campaign_type, campaign_status, budget, create_time, update_time) "
            + "values (#{id}, #{authId}, #{shopId}, #{accountId}, #{campaignId}, #{campaignName}, #{campaignType}, #{campaignStatus}, #{budget}, #{createTime}, #{updateTime}) "
            + "on duplicate key update account_id = values(account_id), campaign_name = values(campaign_name), campaign_type = values(campaign_type), "
            + "campaign_status = values(campaign_status), budget = values(budget), update_time = values(update_time)")
    int upsert(OzonAdsCampaign campaign);
}
