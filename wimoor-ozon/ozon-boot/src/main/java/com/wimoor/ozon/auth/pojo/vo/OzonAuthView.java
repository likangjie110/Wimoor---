package com.wimoor.ozon.auth.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonAuthView {

    private String id;
    private String name;
    private String clientId;
    private String apiKeyMasked;
    private String status;
    private String lastSyncStatus;
    private String lastSyncMessage;
    private Date lastSyncTime;
}
