package com.wimoor.ozon.finance.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.finance.pojo.entity.OzonFinTransaction;

@Mapper
public interface OzonFinTransactionMapper extends BaseMapper<OzonFinTransaction> {

    @Delete("delete from t_ozon_fin_transaction where auth_id = #{authId} and report_id = #{reportId}")
    int deleteByAuthIdAndReportId(@Param("authId") String authId, @Param("reportId") String reportId);
}
