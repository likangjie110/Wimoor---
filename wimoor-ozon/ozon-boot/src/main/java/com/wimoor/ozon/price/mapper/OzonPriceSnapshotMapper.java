package com.wimoor.ozon.price.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.price.pojo.entity.OzonPriceSnapshot;

@Mapper
public interface OzonPriceSnapshotMapper extends BaseMapper<OzonPriceSnapshot> {

    @Select("select id, task_id, auth_id, shop_id, material_sku, ozon_offer_id, price, old_price, currency_code, "
            + "sync_status, sync_message, synced_at from t_ozon_price_snapshot "
            + "where auth_id = #{authId} order by synced_at desc limit 50")
    List<OzonPriceSnapshot> listLatestByAuthId(@Param("authId") String authId);
}
