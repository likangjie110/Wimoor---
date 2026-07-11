package com.wimoor.ozon.product.pojo.dto;

import lombok.Data;

/**
 * 草稿克隆命令
 *
 * @author Development Team
 * @since 2026-06-25
 */
@Data
public class OzonProductDraftCloneCommand {

    /**
     * 源草稿ID
     */
    private String sourceDraftId;

    /**
     * 新草稿名称
     */
    private String newDraftName;

    /**
     * 授权ID
     */
    private String authId;
}
