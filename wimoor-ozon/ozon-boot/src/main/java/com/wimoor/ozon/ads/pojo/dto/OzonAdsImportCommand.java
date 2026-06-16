package com.wimoor.ozon.ads.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonAdsImportCommand {

    private String authId;

    private String rawContent;
}
