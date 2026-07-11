package com.wimoor.ozon.seller.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.seller.pojo.entity.OzonWarehouse;

/**
 * OZON 仓库 Mapper
 *
 * @author Development Team
 */
@Mapper
public interface OzonWarehouseMapper extends BaseMapper<OzonWarehouse> {

    @Delete("delete from t_ozon_warehouse where auth_id=#{authId}")
    int deleteByAuthId(@Param("authId") String authId);

    /**
     * 统计授权下的仓库数量
     *
     * @param authId 授权ID
     * @return 仓库数量
     */
    @Select("select count(*) from t_ozon_warehouse where auth_id=#{authId}")
    int countByAuthId(@Param("authId") String authId);

    /**
     * 查询授权下的默认仓库
     *
     * @param authId 授权ID
     * @return 默认仓库，如果没有则返回null
     */
    @Select("select * from t_ozon_warehouse where auth_id=#{authId} and is_default=1 limit 1")
    OzonWarehouse selectDefaultByAuthId(@Param("authId") String authId);
}
