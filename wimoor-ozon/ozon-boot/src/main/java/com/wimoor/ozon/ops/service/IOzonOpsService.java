package com.wimoor.ozon.ops.service;

import java.util.Collections;
import java.util.List;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogQuery;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogRecordCommand;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditQuery;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.pojo.entity.OzonApiLog;
import com.wimoor.ozon.ops.pojo.entity.OzonOperationAudit;
import com.wimoor.ozon.ops.pojo.vo.OzonOpsDashboardView;
import com.wimoor.ozon.ops.pojo.vo.OzonOpsSummaryView;

public interface IOzonOpsService {

    default void recordApiLog(OzonApiLogRecordCommand command) {
    }

    default void recordOperationAudit(OzonOperationAuditRecordCommand command) {
    }

    default List<OzonApiLog> listApiLogs(UserInfo user, OzonApiLogQuery query) {
        return Collections.emptyList();
    }

    default List<OzonOperationAudit> listOperationAudits(UserInfo user, OzonOperationAuditQuery query) {
        return Collections.emptyList();
    }

    default OzonOpsSummaryView summary(UserInfo user, String authId) {
        return new OzonOpsSummaryView();
    }

    default OzonOpsDashboardView dashboard(UserInfo user, String authId) {
        return new OzonOpsDashboardView();
    }
}
