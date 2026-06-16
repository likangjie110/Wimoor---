package com.wimoor.ozon.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.product.pojo.entity.OzonListingDraft;

@Mapper
public interface OzonListingDraftMapper extends BaseMapper<OzonListingDraft> {

    OzonListingDraft selectByAuthIdAndDraftId(@Param("authId") String authId, @Param("draftId") String draftId);
}
