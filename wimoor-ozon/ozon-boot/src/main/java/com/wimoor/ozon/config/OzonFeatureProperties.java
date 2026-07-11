package com.wimoor.ozon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "ozon.feature")
public class OzonFeatureProperties {

    private boolean auth = true;
    private boolean product = true;
    private boolean productWrite = false;
    private boolean task = true;
    private boolean error = true;
    private boolean finance = true;
    private boolean financeSync = false;
    private boolean chat = true;
    private boolean chatSync = false;
    private boolean ads = true;
    private boolean stockWrite = false;
    private boolean priceWrite = false;
    private boolean postingWrite = false;
    private boolean chatSend = false;
    private boolean adsSync = false;
}
