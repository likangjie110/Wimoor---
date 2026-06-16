package com.wimoor.ozon.ads.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.ads.pojo.entity.OzonAdsAccount;

@Mapper
public interface OzonAdsAccountMapper extends BaseMapper<OzonAdsAccount> {

    @Insert("insert into t_ozon_ads_account (id, auth_id, shop_id, account_id, account_name, status, currency_code, create_time, update_time) "
            + "values (#{id}, #{authId}, #{shopId}, #{accountId}, #{accountName}, #{status}, #{currencyCode}, #{createTime}, #{updateTime}) "
            + "on duplicate key update account_name = values(account_name), status = values(status), currency_code = values(currency_code), update_time = values(update_time)")
    int upsert(OzonAdsAccount account);
}
