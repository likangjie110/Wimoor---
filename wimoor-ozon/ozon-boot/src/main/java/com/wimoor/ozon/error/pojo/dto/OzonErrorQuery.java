package com.wimoor.ozon.error.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonErrorQuery {

    private String authId;

    private String sourceType;

    private String status;

    private String keyword;
}
