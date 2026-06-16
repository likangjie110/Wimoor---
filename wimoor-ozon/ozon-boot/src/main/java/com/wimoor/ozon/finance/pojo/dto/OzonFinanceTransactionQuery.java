package com.wimoor.ozon.finance.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonFinanceTransactionQuery {

    private String authId;

    private String reportId;

    private String fromDate;

    private String toDate;
}
