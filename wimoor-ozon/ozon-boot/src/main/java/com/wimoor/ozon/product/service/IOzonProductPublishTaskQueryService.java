package com.wimoor.ozon.product.service;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishTaskListView;

import java.util.List;

/**
 * OZON 商品发布任务查询服务接口
 *
 * @author Development Team
 * @since 2026-06-25
 */
public interface IOzonProductPublishTaskQueryService {

    /**
     * 根据草稿ID查询发布任务历史
     *
     * @param user 用户信息
     * @param authId 授权ID
     * @param draftId 草稿ID
     * @return 任务历史列表
     */
    List<OzonProductPublishTaskListView> listByDraft(UserInfo user, String authId, String draftId);

    /**
     * 获取任务详情
     *
     * @param user 用户信息
     * @param authId 授权ID
     * @param taskId 任务ID
     * @return 任务详情（包含变体结果）
     */
    OzonProductPublishTaskListView getTaskDetail(UserInfo user, String authId, String taskId);
}
