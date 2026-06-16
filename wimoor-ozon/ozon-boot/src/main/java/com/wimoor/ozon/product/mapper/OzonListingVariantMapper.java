package com.wimoor.ozon.product.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.product.pojo.entity.OzonListingVariant;

@Mapper
public interface OzonListingVariantMapper extends BaseMapper<OzonListingVariant> {

    List<OzonListingVariant> listByDraftId(@Param("draftId") String draftId);
}
