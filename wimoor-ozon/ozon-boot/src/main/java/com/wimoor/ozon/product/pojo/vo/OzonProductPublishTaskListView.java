package com.wimoor.ozon.product.pojo.vo;

import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * OZON 商品发布任务列表视图
 *
 * @author Development Team
 * @since 2026-06-25
 */
@Data
public class OzonProductPublishTaskListView {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 草稿ID
     */
    private String draftId;

    /**
     * 草稿名称
     */
    private String draftName;

    /**
     * 任务状态 (RUNNING, SUCCESS, FAILED, PARTIAL)
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 完成时间
     */
    private Date completeTime;

    /**
     * 总变体数
     */
    private Integer totalVariants;

    /**
     * 成功数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failedCount;

    /**
     * 错误摘要
     */
    private String errorSummary;

    /**
     * 变体结果列表
     */
    private List<VariantResult> variantResults;

    /**
     * 变体发布结果
     */
    @Data
    public static class VariantResult {

        /**
         * 变体SKU
         */
        private String variantSku;

        /**
         * 状态
         */
        private String status;

        /**
         * OZON商品ID
         */
        private String ozonProductId;

        /**
         * 错误信息
         */
        private String errorMessage;
    }
}
