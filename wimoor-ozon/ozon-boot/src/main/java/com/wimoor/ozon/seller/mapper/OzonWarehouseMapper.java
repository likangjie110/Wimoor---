package com.wimoor.ozon.seller.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.seller.pojo.entity.OzonWarehouse;

@Mapper
public interface OzonWarehouseMapper extends BaseMapper<OzonWarehouse> {

    @Delete("delete from t_ozon_warehouse where auth_id=#{authId}")
    int deleteByAuthId(@Param("authId") String authId);
}
