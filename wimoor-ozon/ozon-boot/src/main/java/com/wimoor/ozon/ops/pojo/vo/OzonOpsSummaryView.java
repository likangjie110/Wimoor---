package com.wimoor.ozon.ops.pojo.vo;

import lombok.Data;

@Data
public class OzonOpsSummaryView {

    private long apiLogTotal;
    private long apiLogFailed;
    private long operationAuditTotal;
    private long operationAuditFailed;
}
