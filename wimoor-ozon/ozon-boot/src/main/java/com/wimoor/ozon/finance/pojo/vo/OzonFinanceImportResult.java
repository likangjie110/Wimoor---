package com.wimoor.ozon.finance.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonFinanceImportResult {

    private String taskId;

    private String reportId;

    private Integer importedCount;

    private Date importedAt;
}
