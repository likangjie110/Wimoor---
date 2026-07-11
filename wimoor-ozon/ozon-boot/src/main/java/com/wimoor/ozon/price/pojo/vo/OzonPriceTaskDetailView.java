package com.wimoor.ozon.price.pojo.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * OZON 价格任务详情视图对象
 *
 * @author Development Team
 * @since 2026-06-25
 */
@Data
public class OzonPriceTaskDetailView {

    private String taskId;
    private String authId;
    private String taskStatus;
    private Integer requestedCount;
    private Integer successCount;
    private Integer failedCount;
    private String errorMessage;
    private String errorSummary;
    private String operator;
    private Date createTime;
    private Date updateTime;
    private List<PriceItemResult> itemResults;

    @Data
    public static class PriceItemResult {
        private String sku;
        private String offerId;
        private String price;
        private String oldPrice;
        private String status;
        private String errorMessage;
    }
}
