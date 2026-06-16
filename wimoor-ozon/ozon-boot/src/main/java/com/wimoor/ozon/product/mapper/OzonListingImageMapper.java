package com.wimoor.ozon.product.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.product.pojo.entity.OzonListingImage;

@Mapper
public interface OzonListingImageMapper extends BaseMapper<OzonListingImage> {

    List<OzonListingImage> listByDraftIdAndVariantId(@Param("draftId") String draftId, @Param("variantId") String variantId);

    int deleteByDraftIdAndVariantId(@Param("draftId") String draftId, @Param("variantId") String variantId);
}
