package com.wimoor.ozon.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonRemoteWarehouse {

    private Long warehouseId;
    private String name;
    private String status;
    private String type;
}
