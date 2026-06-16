package com.wimoor.ozon.price.pojo.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonPricePushCommand {

    private String authId;

    private String currencyCode;

    private List<OzonPricePushItem> items;
}
