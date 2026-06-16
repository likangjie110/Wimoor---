package com.wimoor.ozon.ops.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonOperationAuditQuery {

    private String authId;
    private String operationType;
    private String resultStatus;
    private String objectType;
    private String objectId;
}
