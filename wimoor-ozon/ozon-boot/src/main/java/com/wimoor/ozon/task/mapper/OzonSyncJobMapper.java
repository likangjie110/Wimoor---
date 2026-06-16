package com.wimoor.ozon.task.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;

@Mapper
public interface OzonSyncJobMapper extends BaseMapper<OzonSyncJob> {
}
