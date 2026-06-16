package com.wimoor.ozon.auth.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonRotateKeyCommand {

    private String authId;
    private String apiKey;
}
