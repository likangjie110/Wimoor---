package com.wimoor.ozon.seller.mapper;

import java.util.Date;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.seller.pojo.entity.OzonShopConfig;

@Mapper
public interface OzonShopConfigMapper extends BaseMapper<OzonShopConfig> {

    @Update("update t_ozon_shop_config set status='DISABLED', update_time=now() where auth_id=#{authId}")
    int disableByAuthId(@Param("authId") String authId);

    @Update("update t_ozon_shop_config set default_warehouse_id=#{defaultWarehouseId}, last_warehouse_sync_time=#{syncTime}, update_time=#{syncTime} where auth_id=#{authId}")
    int updateWarehouseSyncInfo(@Param("authId") String authId,
                                @Param("defaultWarehouseId") String defaultWarehouseId,
                                @Param("syncTime") Date syncTime);
}
