package com.wimoor.ozon.aftersale.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonCancellationSaveCommand {

    private String authId;
    private String postingId;
    private String id;
    private String cancellationNumber;
    private String cancellationStatus;
    private String reason;
    private String rawPayloadJson;
}
