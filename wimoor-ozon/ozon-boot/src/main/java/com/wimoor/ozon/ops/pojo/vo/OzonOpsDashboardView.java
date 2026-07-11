package com.wimoor.ozon.ops.pojo.vo;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class OzonOpsDashboardView {

    private HealthStatus health = new HealthStatus();
    private Metrics metrics = new Metrics();
    private List<ModuleStat> moduleStats = new ArrayList<>();
    private List<AuditStat> auditStats = new ArrayList<>();
    private List<RecentError> recentErrors = new ArrayList<>();

    @Data
    public static class HealthStatus {
        private String application = "UP";
        private String database = "UP";
        private String ozonApi = "UNKNOWN";
    }

    @Data
    public static class Metrics {
        private long apiTotalCalls;
        private long apiFailedCalls;
        private double apiSuccessRate;
        private long avgResponseTime;
        private double errorRate;
        private long operationAuditTotal;
        private long operationAuditFailed;
    }

    @Data
    public static class ModuleStat {
        private String module;
        private long calls;
        private long failures;
        private double successRate;
        private long avgDuration;
    }

    @Data
    public static class AuditStat {
        private String operationType;
        private long count;
        private long failures;
        private double successRate;
    }

    @Data
    public static class RecentError {
        private String module;
        private String operation;
        private String errorMessage;
        private java.util.Date occurredAt;
    }
}
