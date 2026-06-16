package com.wimoor.ozon.stock.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonStockPushItem {

    private String materialSku;
    private Integer quantity;
}
