package com.wimoor.ozon.ops.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonApiLogQuery {

    private String authId;
    private String apiGroup;
    private String status;
    private String objectType;
    private String objectId;
}
