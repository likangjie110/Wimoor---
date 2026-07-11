package com.wimoor.ozon.product.pojo.dto;

import lombok.Data;

/**
 * 草稿归档命令
 *
 * @author Development Team
 * @since 2026-06-25
 */
@Data
public class OzonProductDraftArchiveCommand {

    /**
     * 草稿ID
     */
    private String draftId;

    /**
     * 授权ID
     */
    private String authId;

    /**
     * 归档原因
     */
    private String archiveReason;
}
