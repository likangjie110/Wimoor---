package com.wimoor.ozon.stock.service;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.stock.pojo.vo.OzonStockTaskDetailView;

import java.util.List;
import java.util.Map;

/**
 * OZON 库存任务查询服务接口
 *
 * @author Development Team
 * @since 2026-06-25
 */
public interface IOzonStockTaskQueryService {

    /**
     * 获取任务详情
     *
     * @param user 用户信息
     * @param authId 授权ID
     * @param taskId 任务ID
     * @return 任务详情
     */
    OzonStockTaskDetailView getTaskDetail(UserInfo user, String authId, String taskId);

    /**
     * 根据SKU查询任务列表
     *
     * @param user 用户信息
     * @param authId 授权ID
     * @param sku SKU编码
     * @return 任务列表
     */
    List<OzonStockTaskDetailView> listTasksBySku(UserInfo user, String authId, String sku);

    /**
     * 获取错误摘要
     *
     * @param user 用户信息
     * @param authId 授权ID
     * @return 错误摘要统计 Map<错误类型, 次数>
     */
    Map<String, Integer> getErrorSummary(UserInfo user, String authId);

    /**
     * 查询任务历史
     *
     * @param user 用户信息
     * @param authId 授权ID
     * @param limit 限制数量
     * @return 任务历史列表
     */
    List<OzonStockTaskDetailView> listTaskHistory(UserInfo user, String authId, Integer limit);
}
