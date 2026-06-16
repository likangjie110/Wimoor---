package com.wimoor.ozon.auth.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonAuthBindCommand {

    private String name;
    private String clientId;
    private String apiKey;
}
