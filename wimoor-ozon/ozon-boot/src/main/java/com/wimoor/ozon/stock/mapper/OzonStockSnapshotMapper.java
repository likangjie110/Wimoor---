package com.wimoor.ozon.stock.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.stock.pojo.entity.OzonStockSnapshot;

@Mapper
public interface OzonStockSnapshotMapper extends BaseMapper<OzonStockSnapshot> {

    @Select("select id, task_id, auth_id, shop_id, warehouse_id, material_sku, ozon_offer_id, quantity, sync_status, sync_message, synced_at "
            + "from t_ozon_stock_snapshot where auth_id = #{authId} order by synced_at desc limit 50")
    List<OzonStockSnapshot> listLatestByAuthId(@Param("authId") String authId);
}
