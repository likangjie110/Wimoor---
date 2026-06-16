package com.wimoor.ozon.price.pojo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonPricePushItem {

    private String materialSku;

    private BigDecimal price;

    private BigDecimal oldPrice;
}
