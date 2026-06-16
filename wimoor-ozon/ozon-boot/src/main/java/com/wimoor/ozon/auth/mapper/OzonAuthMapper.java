package com.wimoor.ozon.auth.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;

@Mapper
public interface OzonAuthMapper extends BaseMapper<OzonAuth> {

    List<OzonAuth> listByShopId(@Param("shopId") String shopId);
}
