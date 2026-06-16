package com.wimoor.erp.order.pojo.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonErpOrderUpsertCommand {

    private String shopId;

    private String postingNumber;

    private String materialSku;

    private String warehouseId;

    private String thirdpartyWarehouseId;

    private String country;

    private String currency;

    private Integer quantity;

    private BigDecimal price;

    private Date purchaseDate;
}
