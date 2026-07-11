package com.wimoor.ozon.ops.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogQuery;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditQuery;
import com.wimoor.ozon.ops.pojo.entity.OzonApiLog;
import com.wimoor.ozon.ops.pojo.entity.OzonOperationAudit;
import com.wimoor.ozon.ops.pojo.vo.OzonOpsDashboardView;
import com.wimoor.ozon.ops.pojo.vo.OzonOpsSummaryView;
import com.wimoor.ozon.ops.service.IOzonOpsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/ops")
@RequiredArgsConstructor
public class OzonOpsController {

    private final IOzonOpsService opsService;
    private final OzonFeatureGate featureGate;

    @GetMapping("/summary")
    public Result<OzonOpsSummaryView> summary(String authId) {
        return execute(() -> {
            featureGate.assertAuthEnabled();
            return opsService.summary(currentUser(), authId);
        });
    }

    @GetMapping("/dashboard")
    public Result<OzonOpsDashboardView> dashboard(String authId) {
        return execute(() -> {
            featureGate.assertAuthEnabled();
            return opsService.dashboard(currentUser(), authId);
        });
    }

    @GetMapping("/api-log/list")
    public Result<List<OzonApiLog>> apiLogList(OzonApiLogQuery query) {
        return execute(() -> {
            featureGate.assertAuthEnabled();
            return opsService.listApiLogs(currentUser(), query);
        });
    }

    @GetMapping("/operation-audit/list")
    public Result<List<OzonOperationAudit>> operationAuditList(OzonOperationAuditQuery query) {
        return execute(() -> {
            featureGate.assertAuthEnabled();
            return opsService.listOperationAudits(currentUser(), query);
        });
    }

    private UserInfo currentUser() {
        return UserInfoContext.get();
    }

    private <T> Result<T> execute(OpsCall<T> call) {
        try {
            return Result.success(call.run());
        } catch (RuntimeException ex) {
            return Result.failed(ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface OpsCall<T> {
        T run();
    }
}
