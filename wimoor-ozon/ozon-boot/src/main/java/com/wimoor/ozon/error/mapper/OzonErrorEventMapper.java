package com.wimoor.ozon.error.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.error.pojo.entity.OzonErrorEvent;

@Mapper
public interface OzonErrorEventMapper extends BaseMapper<OzonErrorEvent> {
}
