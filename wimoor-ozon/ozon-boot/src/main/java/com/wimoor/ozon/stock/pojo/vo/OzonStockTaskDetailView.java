package com.wimoor.ozon.stock.pojo.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * OZON 库存任务详情视图对象
 *
 * @author Development Team
 * @since 2026-06-25
 */
@Data
public class OzonStockTaskDetailView {

    private String taskId;
    private String authId;
    private String warehouseId;
    private String warehouseName;
    private String taskStatus;
    private Integer requestedCount;
    private Integer successCount;
    private Integer failedCount;
    private String errorMessage;
    private String errorSummary;
    private String operator;
    private Date createTime;
    private Date updateTime;
    private List<StockItemResult> itemResults;

    @Data
    public static class StockItemResult {
        private String sku;
        private String offerId;
        private Integer requestedStock;
        private Integer actualStock;
        private String status;
        private String errorMessage;
    }
}
