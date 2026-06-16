package com.wimoor.ozon.ops.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.ops.pojo.entity.OzonOperationAudit;

@Mapper
public interface OzonOperationAuditMapper extends BaseMapper<OzonOperationAudit> {
}
