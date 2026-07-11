package com.wimoor.ozon.ops.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.ops.mapper.OzonApiLogMapper;
import com.wimoor.ozon.ops.mapper.OzonOperationAuditMapper;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogQuery;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogRecordCommand;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditQuery;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.pojo.entity.OzonApiLog;
import com.wimoor.ozon.ops.pojo.entity.OzonOperationAudit;
import com.wimoor.ozon.ops.pojo.vo.OzonOpsDashboardView;
import com.wimoor.ozon.ops.pojo.vo.OzonOpsSummaryView;
import com.wimoor.ozon.ops.service.IOzonOpsService;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OzonOpsServiceImpl implements IOzonOpsService {

    private static final String FAILED = "FAILED";

    private final OzonAuthAccessService authAccessService;
    private final OzonApiLogMapper apiLogMapper;
    private final OzonOperationAuditMapper operationAuditMapper;

    @Override
    public void recordApiLog(OzonApiLogRecordCommand command) {
        if (command == null || StrUtil.isBlank(command.getApiGroup()) || StrUtil.isBlank(command.getActionName())) {
            return;
        }
        Date now = new Date();
        OzonApiLog log = new OzonApiLog();
        log.setId(nextId());
        log.setAuthId(trim(command.getAuthId()));
        log.setShopId(trim(command.getShopId()));
        log.setApiGroup(trim(command.getApiGroup()));
        log.setActionName(trim(command.getActionName()));
        log.setEndpoint(trim(command.getEndpoint()));
        log.setHttpMethod(StrUtil.blankToDefault(trim(command.getHttpMethod()), "POST"));
        log.setObjectType(trim(command.getObjectType()));
        log.setObjectId(trim(command.getObjectId()));
        log.setRequestPayloadJson(trim(command.getRequestPayloadJson()));
        log.setResponsePayloadJson(trim(command.getResponsePayloadJson()));
        log.setStatus(StrUtil.blankToDefault(trim(command.getStatus()), "SUCCESS"));
        log.setErrorMessage(trim(command.getErrorMessage()));
        log.setDurationMs(command.getDurationMs());
        log.setOperator(trim(command.getOperator()));
        log.setCreateTime(now);
        log.setUpdateTime(now);
        apiLogMapper.insert(log);
    }

    @Override
    public void recordOperationAudit(OzonOperationAuditRecordCommand command) {
        if (command == null || StrUtil.isBlank(command.getOperationType()) || StrUtil.isBlank(command.getObjectType())) {
            return;
        }
        Date now = new Date();
        OzonOperationAudit audit = new OzonOperationAudit();
        audit.setId(nextId());
        audit.setAuthId(trim(command.getAuthId()));
        audit.setShopId(trim(command.getShopId()));
        audit.setOperationType(trim(command.getOperationType()));
        audit.setObjectType(trim(command.getObjectType()));
        audit.setObjectId(trim(command.getObjectId()));
        audit.setObjectCode(trim(command.getObjectCode()));
        audit.setRequestPayloadJson(trim(command.getRequestPayloadJson()));
        audit.setResultStatus(StrUtil.blankToDefault(trim(command.getResultStatus()), "SUCCESS"));
        audit.setResultMessage(trim(command.getResultMessage()));
        audit.setOperator(trim(command.getOperator()));
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        operationAuditMapper.insert(audit);
    }

    @Override
    public List<OzonApiLog> listApiLogs(UserInfo user, OzonApiLogQuery query) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, query == null ? null : query.getAuthId());
        QueryWrapper<OzonApiLog> wrapper = new QueryWrapper<OzonApiLog>().eq("auth_id", auth.getId());
        if (query != null && StrUtil.isNotBlank(query.getApiGroup())) {
            wrapper.eq("api_group", query.getApiGroup().trim());
        }
        if (query != null && StrUtil.isNotBlank(query.getStatus())) {
            wrapper.eq("status", query.getStatus().trim());
        }
        if (query != null && StrUtil.isNotBlank(query.getObjectType())) {
            wrapper.eq("object_type", query.getObjectType().trim());
        }
        if (query != null && StrUtil.isNotBlank(query.getObjectId())) {
            wrapper.eq("object_id", query.getObjectId().trim());
        }
        wrapper.orderByDesc("create_time").last("limit 50");
        List<OzonApiLog> rows = apiLogMapper.selectList(wrapper);
        return rows == null ? Collections.emptyList() : rows;
    }

    @Override
    public List<OzonOperationAudit> listOperationAudits(UserInfo user, OzonOperationAuditQuery query) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, query == null ? null : query.getAuthId());
        QueryWrapper<OzonOperationAudit> wrapper = new QueryWrapper<OzonOperationAudit>().eq("auth_id", auth.getId());
        if (query != null && StrUtil.isNotBlank(query.getOperationType())) {
            wrapper.eq("operation_type", query.getOperationType().trim());
        }
        if (query != null && StrUtil.isNotBlank(query.getResultStatus())) {
            wrapper.eq("result_status", query.getResultStatus().trim());
        }
        if (query != null && StrUtil.isNotBlank(query.getObjectType())) {
            wrapper.eq("object_type", query.getObjectType().trim());
        }
        if (query != null && StrUtil.isNotBlank(query.getObjectId())) {
            wrapper.eq("object_id", query.getObjectId().trim());
        }
        wrapper.orderByDesc("create_time").last("limit 50");
        List<OzonOperationAudit> rows = operationAuditMapper.selectList(wrapper);
        return rows == null ? Collections.emptyList() : rows;
    }

    @Override
    public OzonOpsSummaryView summary(UserInfo user, String authId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        OzonOpsSummaryView view = new OzonOpsSummaryView();
        long apiTotal = apiLogMapper.selectCount(new QueryWrapper<OzonApiLog>().eq("auth_id", auth.getId()));
        long apiFailed = apiLogMapper.selectCount(new QueryWrapper<OzonApiLog>().eq("auth_id", auth.getId()).eq("status", FAILED));
        long auditTotal = operationAuditMapper.selectCount(new QueryWrapper<OzonOperationAudit>().eq("auth_id", auth.getId()));
        long auditFailed = operationAuditMapper.selectCount(new QueryWrapper<OzonOperationAudit>().eq("auth_id", auth.getId()).eq("result_status", FAILED));
        view.setApiLogTotal(apiTotal);
        view.setApiLogFailed(apiFailed);
        view.setOperationAuditTotal(auditTotal);
        view.setOperationAuditFailed(auditFailed);
        return view;
    }

    @Override
    public OzonOpsDashboardView dashboard(UserInfo user, String authId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        OzonOpsDashboardView view = new OzonOpsDashboardView();
        List<OzonApiLog> apiLogs = safeApiLogs(apiLogMapper.selectList(new QueryWrapper<OzonApiLog>()
                .eq("auth_id", auth.getId())
                .orderByDesc("create_time")
                .last("limit 200")));
        List<OzonOperationAudit> audits = safeAudits(operationAuditMapper.selectList(new QueryWrapper<OzonOperationAudit>()
                .eq("auth_id", auth.getId())
                .orderByDesc("create_time")
                .last("limit 200")));
        long apiTotal = apiLogs.size();
        long apiFailed = apiLogs.stream().filter(item -> FAILED.equals(item.getStatus())).count();
        long auditFailed = audits.stream().filter(item -> FAILED.equals(item.getResultStatus())).count();
        OzonOpsDashboardView.Metrics metrics = view.getMetrics();
        metrics.setApiTotalCalls(apiTotal);
        metrics.setApiFailedCalls(apiFailed);
        metrics.setApiSuccessRate(rate(apiTotal - apiFailed, apiTotal));
        metrics.setAvgResponseTime(avgDuration(apiLogs));
        metrics.setErrorRate(rate(apiFailed, apiTotal));
        metrics.setOperationAuditTotal(audits.size());
        metrics.setOperationAuditFailed(auditFailed);
        view.getHealth().setApplication("UP");
        view.getHealth().setDatabase("UP");
        view.getHealth().setOzonApi(apiFailed > 0 ? "DEGRADED" : "UP");
        view.setModuleStats(moduleStats(apiLogs));
        view.setAuditStats(auditStats(audits));
        view.setRecentErrors(recentErrors(apiLogs));
        return view;
    }

    private List<OzonOpsDashboardView.ModuleStat> moduleStats(List<OzonApiLog> rows) {
        Map<String, List<OzonApiLog>> grouped = rows.stream()
                .collect(Collectors.groupingBy(item -> StrUtil.blankToDefault(item.getApiGroup(), "UNKNOWN"), LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream().map(entry -> {
            long calls = entry.getValue().size();
            long failures = entry.getValue().stream().filter(item -> FAILED.equals(item.getStatus())).count();
            OzonOpsDashboardView.ModuleStat stat = new OzonOpsDashboardView.ModuleStat();
            stat.setModule(entry.getKey());
            stat.setCalls(calls);
            stat.setFailures(failures);
            stat.setSuccessRate(rate(calls - failures, calls));
            stat.setAvgDuration(avgDuration(entry.getValue()));
            return stat;
        }).collect(Collectors.toList());
    }

    private List<OzonOpsDashboardView.AuditStat> auditStats(List<OzonOperationAudit> rows) {
        Map<String, List<OzonOperationAudit>> grouped = rows.stream()
                .collect(Collectors.groupingBy(item -> StrUtil.blankToDefault(item.getOperationType(), "UNKNOWN"), LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream().map(entry -> {
            long count = entry.getValue().size();
            long failures = entry.getValue().stream().filter(item -> FAILED.equals(item.getResultStatus())).count();
            OzonOpsDashboardView.AuditStat stat = new OzonOpsDashboardView.AuditStat();
            stat.setOperationType(entry.getKey());
            stat.setCount(count);
            stat.setFailures(failures);
            stat.setSuccessRate(rate(count - failures, count));
            return stat;
        }).collect(Collectors.toList());
    }

    private List<OzonOpsDashboardView.RecentError> recentErrors(List<OzonApiLog> rows) {
        return rows.stream()
                .filter(item -> FAILED.equals(item.getStatus()))
                .limit(20)
                .map(item -> {
                    OzonOpsDashboardView.RecentError error = new OzonOpsDashboardView.RecentError();
                    error.setModule(item.getApiGroup());
                    error.setOperation(item.getActionName());
                    error.setErrorMessage(item.getErrorMessage());
                    error.setOccurredAt(item.getCreateTime());
                    return error;
                })
                .collect(Collectors.toList());
    }

    private long avgDuration(List<OzonApiLog> rows) {
        return Math.round(rows.stream()
                .filter(item -> item.getDurationMs() != null)
                .mapToLong(OzonApiLog::getDurationMs)
                .average()
                .orElse(0D));
    }

    private double rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return Math.round((numerator * 10000D / denominator)) / 100D;
    }

    private List<OzonApiLog> safeApiLogs(List<OzonApiLog> rows) {
        return rows == null ? Collections.emptyList() : rows;
    }

    private List<OzonOperationAudit> safeAudits(List<OzonOperationAudit> rows) {
        return rows == null ? Collections.emptyList() : rows;
    }

    private String trim(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private String nextId() {
        try {
            return IdUtil.getSnowflakeNextIdStr();
        } catch (IllegalStateException ex) {
            long fallback = System.currentTimeMillis() * 1000L + ThreadLocalRandom.current().nextInt(1000);
            return String.valueOf(fallback);
        }
    }
}
