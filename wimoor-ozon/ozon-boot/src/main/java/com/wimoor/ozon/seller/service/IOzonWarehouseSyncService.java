package com.wimoor.ozon.seller.service;

import com.wimoor.ozon.seller.pojo.vo.OzonWarehouseSyncResult;

/**
 * OZON 仓库同步服务接口
 *
 * @author Development Team
 */
public interface IOzonWarehouseSyncService {

    /**
     * 同步仓库信息
     *
     * @param authId 授权ID
     * @return 同步结果
     */
    OzonWarehouseSyncResult syncWarehouses(String authId);

    /**
     * 统计授权下的仓库数量
     *
     * @param authId 授权ID
     * @return 仓库数量
     */
    int countByAuth(String authId);

    /**
     * 获取默认仓库名称
     *
     * @param authId 授权ID
     * @return 默认仓库名称，如果没有则返回null
     */
    String getDefaultWarehouseName(String authId);
}
