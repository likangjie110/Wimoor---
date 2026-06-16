package com.wimoor.ozon.seller.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.seller.pojo.entity.OzonDeliveryMethod;

@Mapper
public interface OzonDeliveryMethodMapper extends BaseMapper<OzonDeliveryMethod> {

    @Update("update t_ozon_delivery_method set is_default=b'0', update_time=now() where auth_id=#{authId}")
    int clearDefaultByAuthId(@Param("authId") String authId);
}
