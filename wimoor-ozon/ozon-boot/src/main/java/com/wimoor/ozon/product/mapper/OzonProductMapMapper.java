package com.wimoor.ozon.product.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.product.pojo.entity.OzonProductMap;

@Mapper
public interface OzonProductMapMapper extends BaseMapper<OzonProductMap> {

    List<OzonProductMap> listByAuthId(@Param("authId") String authId, @Param("keyword") String keyword);

    OzonProductMap selectByAuthIdAndMaterialSku(@Param("authId") String authId, @Param("materialSku") String materialSku);

    List<OzonProductMap> listByMaterialSkus(@Param("authId") String authId, @Param("materialSkus") List<String> materialSkus);
}
