package com.wimoor.ozon.ops.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.ops.pojo.entity.OzonOperationAudit;

@Mapper
public interface OzonOperationAuditMapper extends BaseMapper<OzonOperationAudit> {

    /**
     * 按操作类型查询审计日志
     */
    @Select("SELECT * FROM t_ozon_operation_audit WHERE operation_type = #{operationType} " +
            "AND create_time >= #{startTime} AND create_time <= #{endTime} " +
            "ORDER BY create_time DESC LIMIT 100")
    List<OzonOperationAudit> listByOperationType(
            @Param("operationType") String operationType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 按授权查询审计日志
     */
    @Select("SELECT * FROM t_ozon_operation_audit WHERE auth_id = #{authId} " +
            "AND create_time >= #{startTime} AND create_time <= #{endTime} " +
            "ORDER BY create_time DESC LIMIT 100")
    List<OzonOperationAudit> listByAuth(
            @Param("authId") String authId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 按操作人查询审计日志
     */
    @Select("SELECT * FROM t_ozon_operation_audit WHERE shop_id = #{shopId} AND operator = #{operator} " +
            "AND create_time >= #{startTime} AND create_time <= #{endTime} " +
            "ORDER BY create_time DESC LIMIT 100")
    List<OzonOperationAudit> listByOperator(
            @Param("shopId") String shopId,
            @Param("operator") String operator,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计操作类型分布
     */
    @Select("SELECT operation_type as `key`, COUNT(*) as `value` FROM t_ozon_operation_audit " +
            "WHERE create_time >= #{startTime} AND create_time <= #{endTime} " +
            "GROUP BY operation_type ORDER BY `value` DESC")
    List<Map<String, Object>> countByOperationType(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计对象类型分布
     */
    @Select("SELECT object_type as `key`, COUNT(*) as `value` FROM t_ozon_operation_audit " +
            "WHERE create_time >= #{startTime} AND create_time <= #{endTime} " +
            "GROUP BY object_type ORDER BY `value` DESC")
    List<Map<String, Object>> countByObjectType(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计失败操作
     */
    @Select("SELECT * FROM t_ozon_operation_audit WHERE result_status = 'FAILED' " +
            "AND create_time >= #{startTime} AND create_time <= #{endTime} " +
            "ORDER BY create_time DESC LIMIT 50")
    List<OzonOperationAudit> listFailedOperations(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
