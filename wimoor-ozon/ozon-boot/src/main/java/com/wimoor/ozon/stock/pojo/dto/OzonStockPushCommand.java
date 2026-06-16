package com.wimoor.ozon.stock.pojo.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonStockPushCommand {

    private String authId;
    private String warehouseId;
    private List<OzonStockPushItem> items;
}
