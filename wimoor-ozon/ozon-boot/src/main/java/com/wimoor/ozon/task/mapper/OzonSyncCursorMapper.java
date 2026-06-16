package com.wimoor.ozon.task.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncCursor;

@Mapper
public interface OzonSyncCursorMapper extends BaseMapper<OzonSyncCursor> {
}
