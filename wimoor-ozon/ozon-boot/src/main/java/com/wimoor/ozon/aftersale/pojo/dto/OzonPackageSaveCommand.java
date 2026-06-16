package com.wimoor.ozon.aftersale.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonPackageSaveCommand {

    private String authId;
    private String postingId;
    private String id;
    private String packageNumber;
    private String packageStatus;
    private String trackingNumber;
    private String rawPayloadJson;
}
