package com.wimoor.ozon.product.service;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftArchiveCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftCloneCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftDetailQuery;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftImportCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftListQuery;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftSaveCommand;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftDetailView;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftImportResult;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftListView;

import java.util.List;

/**
 * OZON 商品草稿服务接口
 *
 * @author Development Team
 */
public interface IOzonListingDraftService {

    /**
     * 保存草稿
     */
    OzonProductDraftDetailView saveDraft(UserInfo user, OzonProductDraftSaveCommand command);

    /**
     * 导入草稿
     */
    OzonProductDraftImportResult importDraft(UserInfo user, OzonProductDraftImportCommand command);

    /**
     * 查询草稿列表
     */
    List<OzonProductDraftListView> listDrafts(UserInfo user, OzonProductDraftListQuery query);

    /**
     * 获取草稿详情
     */
    OzonProductDraftDetailView getDraftDetail(UserInfo user, OzonProductDraftDetailQuery query);

    /**
     * 克隆草稿
     * 复制草稿及所有关联数据（属性、图片、变体）
     *
     * @param user 用户信息
     * @param command 克隆命令
     * @return 新草稿详情
     */
    OzonProductDraftDetailView cloneDraft(UserInfo user, OzonProductDraftCloneCommand command);

    /**
     * 归档草稿
     * 将草稿状态更新为 ARCHIVED
     *
     * @param user 用户信息
     * @param command 归档命令
     */
    void archiveDraft(UserInfo user, OzonProductDraftArchiveCommand command);

    /**
     * 删除草稿
     * 删除草稿及所有关联数据
     *
     * @param user 用户信息
     * @param authId 授权ID
     * @param draftId 草稿ID
     */
    void deleteDraft(UserInfo user, String authId, String draftId);

    /**
     * 按状态查询草稿列表
     *
     * @param user 用户信息
     * @param authId 授权ID
     * @param status 状态（DRAFT, PUBLISHED, ARCHIVED）
     * @return 草稿列表
     */
    List<OzonProductDraftListView> listByStatus(UserInfo user, String authId, String status);
}
