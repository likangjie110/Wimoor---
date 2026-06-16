package com.wimoor.ozon.client;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OzonConnectionStatus {

    private boolean success;
    private String message;

    public static OzonConnectionStatus success(String message) {
        return new OzonConnectionStatus(true, message);
    }

    public static OzonConnectionStatus failure(String message) {
        return new OzonConnectionStatus(false, message);
    }
}
