package com.wimoor.ozon.aftersale.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonPostingCancelCommand {

    private String authId;
    private String postingId;
    private Long cancelReasonId;
    private String message;
}
