package com.wimoor.ozon.finance.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.finance.mapper.OzonFinTransactionMapper;
import com.wimoor.ozon.finance.mapper.OzonReportFileMapper;
import com.wimoor.ozon.finance.mapper.OzonReportTaskMapper;
import com.wimoor.ozon.finance.pojo.dto.OzonFinanceImportCommand;
import com.wimoor.ozon.finance.pojo.dto.OzonFinanceTransactionQuery;
import com.wimoor.ozon.finance.pojo.entity.OzonFinTransaction;
import com.wimoor.ozon.finance.pojo.entity.OzonReportFile;
import com.wimoor.ozon.finance.pojo.entity.OzonReportTask;
import com.wimoor.ozon.finance.pojo.vo.OzonFinanceImportResult;
import com.wimoor.ozon.finance.pojo.vo.OzonFinanceTaskView;
import com.wimoor.ozon.finance.service.IOzonFinanceService;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJobType;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class OzonFinanceServiceImpl implements IOzonFinanceService {

    private static final String RUNNING = "RUNNING";
    private static final String DONE = "DONE";
    private static final String FAILED = "FAILED";
    private static final String JSON_CONTENT = "JSON";
    private static final String DEFAULT_CURRENCY = "RUB";
    private static final String SOURCE_MODE_LOCAL_IMPORT = "LOCAL_IMPORT";

    private final OzonAuthAccessService authAccessService;
    private final OzonReportTaskMapper reportTaskMapper;
    private final OzonReportFileMapper reportFileMapper;
    private final OzonFinTransactionMapper finTransactionMapper;
    private final OzonSyncJobMapper syncJobMapper;
    private IOzonOpsService opsService = new IOzonOpsService() {
    };

    @Autowired
    public OzonFinanceServiceImpl(
            OzonAuthAccessService authAccessService,
            OzonReportTaskMapper reportTaskMapper,
            OzonReportFileMapper reportFileMapper,
            OzonFinTransactionMapper finTransactionMapper,
            OzonSyncJobMapper syncJobMapper
    ) {
        this.authAccessService = authAccessService;
        this.reportTaskMapper = reportTaskMapper;
        this.reportFileMapper = reportFileMapper;
        this.finTransactionMapper = finTransactionMapper;
        this.syncJobMapper = syncJobMapper;
    }

    @Autowired(required = false)
    public void setOpsService(IOzonOpsService opsService) {
        if (opsService != null) {
            this.opsService = opsService;
        }
    }

    @Override
    public OzonFinanceImportResult importReport(UserInfo user, OzonFinanceImportCommand command) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command == null ? null : command.getAuthId());
        String reportId = requireText(command == null ? null : command.getReportId(), "reportId不能为空");
        Date reportDate = requireDay(command == null ? null : command.getReportDate(), "reportDate不能为空");
        String rawContent = requireText(command == null ? null : command.getRawContent(), "rawContent不能为空");
        String auditPayload = JSON.toJSONString(command);
        Date now = new Date();
        OzonReportTask task = buildTask(auth, user, reportId, reportDate, now);
        OzonReportFile reportFile = buildReportFile(task, auth, reportId, reportDate, rawContent, now);
        OzonSyncJob syncJob = buildSyncJob(auth, user, reportId, command.getReportDate(), now);
        reportTaskMapper.insert(task);
        reportFileMapper.insert(reportFile);
        syncJobMapper.insert(syncJob);
        try {
            List<OzonFinTransaction> transactions = parseTransactions(task, auth, reportId, reportDate, rawContent, now);
            finTransactionMapper.deleteByAuthIdAndReportId(auth.getId(), reportId);
            for (OzonFinTransaction item : transactions) {
                finTransactionMapper.insert(item);
            }
            finishTask(task, transactions.size(), now);
            finishSyncJob(syncJob, now);
            OzonFinanceImportResult result = new OzonFinanceImportResult();
            result.setTaskId(task.getId());
            result.setReportId(reportId);
            result.setImportedCount(transactions.size());
            result.setImportedAt(now);
            recordOperationAudit(auth, user, task.getId(), reportId, auditPayload, DONE, "imported " + transactions.size());
            return result;
        } catch (RuntimeException ex) {
            failTask(task, now, ex);
            failSyncJob(syncJob, now, ex);
            recordOperationAudit(auth, user, task.getId(), reportId, auditPayload, FAILED, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public List<OzonFinanceTaskView> listTasks(UserInfo user, String authId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        List<OzonReportTask> tasks = reportTaskMapper.selectList(new QueryWrapper<OzonReportTask>()
                .eq("auth_id", auth.getId())
                .orderByDesc("create_time")
                .last("limit 50"));
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<OzonFinanceTaskView> result = new ArrayList<>(tasks.size());
        for (OzonReportTask task : tasks) {
            OzonFinanceTaskView view = new OzonFinanceTaskView();
            view.setId(task.getId());
            view.setAuthId(task.getAuthId());
            view.setReportId(task.getReportId());
            view.setReportDate(task.getReportDate());
            view.setTaskStatus(task.getTaskStatus());
            view.setImportedCount(task.getImportedCount());
            view.setSourceMode(SOURCE_MODE_LOCAL_IMPORT);
            view.setRawContentReady(Boolean.TRUE);
            view.setErrorMessage(task.getErrorMessage());
            view.setOperator(task.getOperator());
            view.setCreatedAt(task.getCreateTime());
            view.setUpdatedAt(task.getUpdateTime());
            result.add(view);
        }
        return result;
    }

    @Override
    public List<OzonFinTransaction> listTransactions(UserInfo user, OzonFinanceTransactionQuery query) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, query == null ? null : query.getAuthId());
        QueryWrapper<OzonFinTransaction> wrapper = new QueryWrapper<OzonFinTransaction>().eq("auth_id", auth.getId());
        if (query != null && StrUtil.isNotBlank(query.getReportId())) {
            wrapper.eq("report_id", query.getReportId().trim());
        }
        if (query != null && StrUtil.isNotBlank(query.getFromDate())) {
            wrapper.ge("report_date", requireDay(query.getFromDate(), "fromDate格式不正确"));
        }
        if (query != null && StrUtil.isNotBlank(query.getToDate())) {
            wrapper.le("report_date", requireDay(query.getToDate(), "toDate格式不正确"));
        }
        wrapper.orderByDesc("transaction_time").orderByDesc("create_time").last("limit 200");
        List<OzonFinTransaction> rows = finTransactionMapper.selectList(wrapper);
        return rows == null ? Collections.emptyList() : rows;
    }

    @Override
    public String getRawContent(UserInfo user, String authId, String taskId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        OzonReportTask task = reportTaskMapper.selectById(requireText(taskId, "taskId不能为空"));
        if (task == null || !auth.getId().equals(task.getAuthId())) {
            throw new IllegalArgumentException("Ozon财务任务不存在");
        }
        OzonReportFile reportFile = reportFileMapper.selectOne(new QueryWrapper<OzonReportFile>()
                .eq("task_id", task.getId())
                .orderByDesc("create_time")
                .last("limit 1"));
        if (reportFile == null || StrUtil.isBlank(reportFile.getRawContent())) {
            throw new IllegalArgumentException("未找到报表原文");
        }
        return reportFile.getRawContent();
    }

    private OzonReportTask buildTask(OzonAuth auth, UserInfo user, String reportId, Date reportDate, Date now) {
        OzonReportTask task = new OzonReportTask();
        task.setId(nextId());
        task.setAuthId(auth.getId());
        task.setShopId(auth.getShopId());
        task.setReportId(reportId);
        task.setReportDate(reportDate);
        task.setTaskStatus(RUNNING);
        task.setImportedCount(0);
        task.setOperator(user == null ? null : user.getId());
        task.setCreateTime(now);
        task.setUpdateTime(now);
        return task;
    }

    private OzonReportFile buildReportFile(
            OzonReportTask task,
            OzonAuth auth,
            String reportId,
            Date reportDate,
            String rawContent,
            Date now
    ) {
        OzonReportFile reportFile = new OzonReportFile();
        reportFile.setId(nextId());
        reportFile.setTaskId(task.getId());
        reportFile.setAuthId(auth.getId());
        reportFile.setShopId(auth.getShopId());
        reportFile.setReportId(reportId);
        reportFile.setReportDate(reportDate);
        reportFile.setContentType(JSON_CONTENT);
        reportFile.setRawContent(rawContent);
        reportFile.setCreateTime(now);
        return reportFile;
    }

    private OzonSyncJob buildSyncJob(OzonAuth auth, UserInfo user, String reportId, String reportDate, Date now) {
        JSONObject payload = new JSONObject();
        payload.put("reportId", reportId);
        payload.put("reportDate", reportDate);
        OzonSyncJob job = new OzonSyncJob();
        job.setId(nextId());
        job.setAuthId(auth.getId());
        job.setShopId(auth.getShopId());
        job.setJobType(OzonSyncJobType.FINANCE_IMPORT.name());
        job.setStatus(RUNNING);
        job.setPayload(payload.toJSONString());
        job.setOperator(user == null ? null : user.getId());
        job.setCreateTime(now);
        job.setUpdateTime(now);
        return job;
    }

    private List<OzonFinTransaction> parseTransactions(
            OzonReportTask task,
            OzonAuth auth,
            String reportId,
            Date reportDate,
            String rawContent,
            Date now
    ) {
        Object payload = JSON.parse(rawContent);
        JSONArray transactions = resolveTransactions(payload);
        if (transactions == null || transactions.isEmpty()) {
            throw new IllegalArgumentException("未找到可导入的交易明细");
        }
        List<OzonFinTransaction> result = new ArrayList<>(transactions.size());
        for (int index = 0; index < transactions.size(); index++) {
            JSONObject item = transactions.getJSONObject(index);
            String transactionId = requireText(firstText(item, "transactionId", "transaction_id"), "transactionId不能为空");
            BigDecimal amount = requireAmount(item);
            OzonFinTransaction transaction = new OzonFinTransaction();
            transaction.setId(nextId());
            transaction.setTaskId(task.getId());
            transaction.setAuthId(auth.getId());
            transaction.setShopId(auth.getShopId());
            transaction.setReportId(reportId);
            transaction.setReportDate(reportDate);
            transaction.setTransactionId(transactionId);
            transaction.setOperationType(firstText(item, "operationType", "operation_type"));
            transaction.setPostingNumber(firstText(item, "postingNumber", "posting_number"));
            transaction.setAmount(amount);
            transaction.setCurrencyCode(StrUtil.blankToDefault(firstText(item, "currencyCode", "currency_code"), DEFAULT_CURRENCY));
            transaction.setTransactionTime(parseDate(firstText(item, "transactionTime", "transaction_time")));
            transaction.setRawLineJson(item.toJSONString());
            transaction.setCreateTime(now);
            result.add(transaction);
        }
        return result;
    }

    private JSONArray resolveTransactions(Object payload) {
        if (payload instanceof JSONArray) {
            return (JSONArray) payload;
        }
        if (payload instanceof JSONObject) {
            JSONObject json = (JSONObject) payload;
            if (json.getJSONArray("transactions") != null) {
                return json.getJSONArray("transactions");
            }
            JSONObject result = json.getJSONObject("result");
            if (result != null && result.getJSONArray("transactions") != null) {
                return result.getJSONArray("transactions");
            }
        }
        return null;
    }

    private BigDecimal requireAmount(JSONObject item) {
        BigDecimal amount = item.getBigDecimal("amount");
        if (amount != null) {
            return amount;
        }
        String rawAmount = firstText(item, "amount");
        if (StrUtil.isBlank(rawAmount)) {
            throw new IllegalArgumentException("amount不能为空");
        }
        return new BigDecimal(rawAmount.trim());
    }

    private void finishTask(OzonReportTask task, int importedCount, Date now) {
        task.setTaskStatus(DONE);
        task.setImportedCount(importedCount);
        task.setUpdateTime(now);
        reportTaskMapper.updateById(task);
    }

    private void failTask(OzonReportTask task, Date now, RuntimeException ex) {
        task.setTaskStatus(FAILED);
        task.setErrorMessage(ex.getMessage());
        task.setUpdateTime(now);
        reportTaskMapper.updateById(task);
    }

    private void finishSyncJob(OzonSyncJob syncJob, Date now) {
        syncJob.setStatus(DONE);
        syncJob.setUpdateTime(now);
        syncJobMapper.updateById(syncJob);
    }

    private void failSyncJob(OzonSyncJob syncJob, Date now, RuntimeException ex) {
        syncJob.setStatus(FAILED);
        syncJob.setPayload(ex.getMessage());
        syncJob.setUpdateTime(now);
        syncJobMapper.updateById(syncJob);
    }

    private String requireText(String value, String message) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private Date requireDay(String value, String message) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return Date.from(LocalDate.parse(value.trim()).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private String firstText(JSONObject item, String... keys) {
        for (String key : keys) {
            String value = item.getString(key);
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private Date parseDate(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Date.from(OffsetDateTime.parse(value).toInstant());
        } catch (DateTimeParseException ex) {
            try {
                return Date.from(Instant.parse(value));
            } catch (DateTimeParseException ignored) {
                try {
                    return Date.from(LocalDateTime.parse(value).toInstant(ZoneOffset.UTC));
                } catch (DateTimeParseException ignoredAgain) {
                    return null;
                }
            }
        }
    }

    private String nextId() {
        try {
            return IdUtil.getSnowflakeNextIdStr();
        } catch (IllegalStateException ex) {
            long fallback = System.currentTimeMillis() * 1000L + ThreadLocalRandom.current().nextInt(1000);
            return String.valueOf(fallback);
        }
    }

    private void recordOperationAudit(
            OzonAuth auth,
            UserInfo user,
            String objectId,
            String objectCode,
            String requestPayload,
            String resultStatus,
            String resultMessage
    ) {
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                auth.getId(),
                auth.getShopId(),
                "FINANCE_IMPORT",
                "FINANCE",
                objectId,
                objectCode,
                requestPayload,
                resultStatus,
                resultMessage,
                user == null ? null : user.getId()
        ));
    }
}
