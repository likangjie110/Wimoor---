package com.wimoor.ozon.posting.pojo.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class OzonPostingSyncResult {

    private int imported;

    private List<String> erpOrderIds = new ArrayList<>();

    private Date syncedAt;

    private String syncSince;

    private String syncTo;

    private boolean cursorUsed;
}
