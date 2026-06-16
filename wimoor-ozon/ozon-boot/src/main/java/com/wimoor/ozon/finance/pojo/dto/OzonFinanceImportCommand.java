package com.wimoor.ozon.finance.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonFinanceImportCommand {

    private String authId;

    private String reportId;

    private String reportDate;

    private String rawContent;
}
