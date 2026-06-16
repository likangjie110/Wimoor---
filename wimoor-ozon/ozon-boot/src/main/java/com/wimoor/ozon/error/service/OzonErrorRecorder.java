package com.wimoor.ozon.error.service;

import com.wimoor.ozon.error.pojo.dto.OzonErrorRecordCommand;

public interface OzonErrorRecorder {

    void recordOpen(OzonErrorRecordCommand command);

    void markResolved(String authId, String sourceType, String objectId);
}
