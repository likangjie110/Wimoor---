package com.wimoor.ozon.price.service;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.price.pojo.vo.OzonPriceTaskDetailView;

import java.util.List;
import java.util.Map;

/**
 * OZON 价格任务查询服务接口
 *
 * @author Development Team
 * @since 2026-06-25
 */
public interface IOzonPriceTaskQueryService {

    /**
     * 获取任务详情
     *
     * @param user 用户信息
     * @param authId 授权ID
     * @param taskId 任务ID
     * @return 任务详情
     */
    OzonPriceTaskDetailView getTaskDetail(UserInfo user, String authId, String taskId);

    /**
     * 根据SKU查询任务列表
     *
     * @param user 用户信息
     * @param authId 授权ID
     * @param sku SKU编码
     * @return 任务列表
     */
    List<OzonPriceTaskDetailView> listTasksBySku(UserInfo user, String authId, String sku);

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
    List<OzonPriceTaskDetailView> listTaskHistory(UserInfo user, String authId, Integer limit);
}
