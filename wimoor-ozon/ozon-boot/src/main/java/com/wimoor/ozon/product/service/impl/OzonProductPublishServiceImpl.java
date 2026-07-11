package com.wimoor.ozon.product.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.error.pojo.dto.OzonErrorRecordCommand;
import com.wimoor.ozon.error.pojo.entity.OzonErrorSourceType;
import com.wimoor.ozon.error.service.OzonErrorRecorder;
import com.wimoor.ozon.ops.annotation.OzonAudit;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogRecordCommand;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozon.product.client.OzonProductApiClient;
import com.wimoor.ozon.product.mapper.OzonListingDraftMapper;
import com.wimoor.ozon.product.mapper.OzonListingPublishTaskMapper;
import com.wimoor.ozon.product.mapper.OzonListingVariantMapper;
import com.wimoor.ozon.product.mapper.OzonProductMapMapper;
import com.wimoor.ozon.product.pojo.dto.OzonProductPreviewCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductPublishCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductPublishTaskQuery;
import com.wimoor.ozon.product.pojo.entity.OzonListingDraft;
import com.wimoor.ozon.product.pojo.entity.OzonListingPublishTask;
import com.wimoor.ozon.product.pojo.entity.OzonProductMap;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishTaskHistoryView;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishTaskView;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishView;
import com.wimoor.ozon.product.service.IOzonProductPreviewService;
import com.wimoor.ozon.product.service.IOzonProductPublishService;
import com.wimoor.ozon.security.OzonCredentialService;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJobType;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class OzonProductPublishServiceImpl implements IOzonProductPublishService {

    private static final String TASK_RUNNING = "RUNNING";
    private static final String TASK_SUCCESS = "SUCCESS";
    private static final String TASK_FAILED = "FAILED";
    private static final String TASK_PARTIAL = "PARTIAL";
    private static final String PRODUCT_IMPORT_ENDPOINT = "/v3/product/import";
    private static final String PRODUCT_IMPORT_INFO_ENDPOINT = "/v1/product/import/info";

    private final OzonProductDraftResolver draftResolver;
    private final OzonListingDraftMapper draftMapper;
    private final OzonListingVariantMapper variantMapper;
    private final OzonProductMapMapper productMapMapper;
    private final OzonListingPublishTaskMapper publishTaskMapper;
    private final OzonSyncJobMapper syncJobMapper;
    private final OzonProductApiClient productApiClient;
    private final OzonCredentialService credentialService;
    private final OzonErrorRecorder errorRecorder;
    private final IOzonProductPreviewService previewService;
    private final OzonFeatureGate featureGate;
    private IOzonOpsService opsService = new IOzonOpsService() {
    };

    @Autowired
    public OzonProductPublishServiceImpl(
            OzonProductDraftResolver draftResolver,
            OzonListingDraftMapper draftMapper,
            OzonListingVariantMapper variantMapper,
            OzonProductMapMapper productMapMapper,
            OzonListingPublishTaskMapper publishTaskMapper,
            OzonSyncJobMapper syncJobMapper,
            OzonProductApiClient productApiClient,
            OzonCredentialService credentialService,
            OzonErrorRecorder errorRecorder,
            IOzonProductPreviewService previewService,
            OzonFeatureGate featureGate
    ) {
        this.draftResolver = draftResolver;
        this.draftMapper = draftMapper;
        this.variantMapper = variantMapper;
        this.productMapMapper = productMapMapper;
        this.publishTaskMapper = publishTaskMapper;
        this.syncJobMapper = syncJobMapper;
        this.productApiClient = productApiClient;
        this.credentialService = credentialService;
        this.errorRecorder = errorRecorder;
        this.previewService = previewService;
        this.featureGate = featureGate;
    }

    public OzonProductPublishServiceImpl(
            OzonAuthMapper authMapper,
            OzonListingDraftMapper draftMapper,
            OzonListingVariantMapper variantMapper,
            OzonProductMapMapper productMapMapper,
            OzonListingPublishTaskMapper publishTaskMapper,
            OzonSyncJobMapper syncJobMapper,
            OzonProductApiClient productApiClient,
            OzonCredentialService credentialService,
            OzonErrorRecorder errorRecorder,
            IOzonProductPreviewService previewService
    ) {
        this(
                new OzonProductDraftResolver(
                        new OzonAuthAccessService(authMapper),
                        draftMapper,
                        variantMapper,
                        null,
                        null,
                        productMapMapper
                ),
                draftMapper,
                variantMapper,
                productMapMapper,
                publishTaskMapper,
                syncJobMapper,
                productApiClient,
                credentialService,
                errorRecorder,
                previewService,
                OzonFeatureGate.allEnabled()
        );
    }

    @Autowired(required = false)
    public void setOpsService(IOzonOpsService opsService) {
        if (opsService != null) {
            this.opsService = opsService;
        }
    }

    @Override
    @OzonAudit(operationType = "PUBLISH", objectType = "PRODUCT", description = "发布商品到 OZON")
    public OzonProductPublishView publish(UserInfo user, OzonProductPublishCommand command) {
        featureGate.assertProductWriteEnabled();
        String auditPayload = JSON.toJSONString(command);
        OzonProductDraftResolver.ResolvedDraftContext context = draftResolver.resolve(user, command.getAuthId(), command.getDraftId());
        OzonListingPublishTask active = publishTaskMapper.selectOne(new QueryWrapper<OzonListingPublishTask>()
                .eq("draft_id", context.draft().getId())
                .eq("task_status", TASK_RUNNING)
                .last("limit 1"));
        if (active != null) {
            throw new IllegalStateException("当前草稿已有运行中的发布任务");
        }
        if (!previewService.preview(user, new OzonProductPreviewCommand(command.getAuthId(), command.getDraftId())).isCanPublish()) {
            throw new IllegalStateException("草稿预检未通过，不能发布");
        }
        Date now = new Date();
        OzonListingPublishTask task = buildTask(context, user, now);
        OzonSyncJob syncJob = buildSyncJob(context, user, now);
        publishTaskMapper.insert(task);
        syncJobMapper.insert(syncJob);
        updateDraftStatus(context.draft(), "PUBLISHING", task.getId(), now);

        String requestPayload = buildRequestPayload(context);
        task.setRequestPayloadJson(requestPayload);
        boolean submitSucceeded = false;
        long submitStartedAt = System.currentTimeMillis();
        try {
            String remoteTaskId = productApiClient.submitProductImport(
                    context.auth().getClientId(),
                    credentialService.decrypt(context.auth().getApiKeyCiphertext()),
                    requestPayload
            );
            recordApiLog(
                    context,
                    user,
                    "SUBMIT_PRODUCT_IMPORT",
                    PRODUCT_IMPORT_ENDPOINT,
                    requestPayload,
                    "{\"taskId\":\"" + remoteTaskId + "\"}",
                    TASK_SUCCESS,
                    null,
                    submitStartedAt
            );
            submitSucceeded = true;
            task.setRemoteTaskId(remoteTaskId);
            publishTaskMapper.updateById(task);

            OzonProductPublishTaskView detail = pollTask(context, task, 15, user);
            finishSyncJob(syncJob, detail, now);
            recordOperationAudit(
                    context,
                    user,
                    auditPayload,
                    detail.getTaskStatus(),
                    StrUtil.blankToDefault(detail.getErrorSummary(), "published")
            );
            OzonProductPublishView result = new OzonProductPublishView();
            result.setDraftId(context.draft().getId());
            result.setLocalTaskId(task.getId());
            result.setRemoteTaskId(task.getRemoteTaskId());
            result.setTaskStatus(detail.getTaskStatus());
            result.setResultSummary(detail);
            return result;
        } catch (RuntimeException ex) {
            if (!submitSucceeded) {
                recordApiLog(
                        context,
                        user,
                        "SUBMIT_PRODUCT_IMPORT",
                        PRODUCT_IMPORT_ENDPOINT,
                        requestPayload,
                        null,
                        TASK_FAILED,
                        ex.getMessage(),
                        submitStartedAt
                );
            }
            failPublish(context, task, syncJob, ex);
            recordOperationAudit(context, user, auditPayload, TASK_FAILED, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public OzonProductPublishTaskView getTaskDetail(UserInfo user, OzonProductPublishTaskQuery query) {
        OzonListingPublishTask task = publishTaskMapper.selectById(query.getTaskId());
        if (task == null) {
            throw new IllegalArgumentException("Ozon发布任务不存在");
        }
        OzonProductDraftResolver.ResolvedDraftContext context = draftResolver.resolve(user, query.getAuthId(), task.getDraftId());
        if (!context.auth().getId().equals(task.getAuthId())) {
            throw new IllegalArgumentException("Ozon发布任务不存在");
        }
        if (!TASK_RUNNING.equals(task.getTaskStatus()) || StrUtil.isBlank(task.getRemoteTaskId())) {
            return toTaskView(task, Collections.emptyList());
        }
        return pollTask(context, task, 1, user);
    }

    @Override
    public List<OzonProductPublishTaskHistoryView> listTaskHistory(UserInfo user, String authId, String draftId) {
        OzonProductDraftResolver.ResolvedDraftContext context = draftResolver.resolve(user, authId, draftId);
        List<OzonListingPublishTask> tasks = publishTaskMapper.selectList(new QueryWrapper<OzonListingPublishTask>()
                .eq("auth_id", context.auth().getId())
                .eq("draft_id", context.draft().getId())
                .orderByDesc("create_time")
                .last("limit 20"));
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<OzonProductPublishTaskHistoryView> result = new ArrayList<>(tasks.size());
        for (OzonListingPublishTask task : tasks) {
            result.add(toTaskHistoryItem(task));
        }
        return result;
    }

    private OzonProductPublishTaskView pollTask(
            OzonProductDraftResolver.ResolvedDraftContext context,
            OzonListingPublishTask task,
            int maxAttempts,
            UserInfo user
    ) {
        OzonProductPublishTaskView detail = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            JSONObject requestPayload = new JSONObject();
            requestPayload.put("taskId", task.getRemoteTaskId());
            long startedAt = System.currentTimeMillis();
            OzonProductApiClient.ProductImportInfo info;
            try {
                info = productApiClient.getProductImportInfo(
                        context.auth().getClientId(),
                        credentialService.decrypt(context.auth().getApiKeyCiphertext()),
                        task.getRemoteTaskId()
                );
                recordApiLog(
                        context,
                        user,
                        "GET_PRODUCT_IMPORT_INFO",
                        PRODUCT_IMPORT_INFO_ENDPOINT,
                        requestPayload.toJSONString(),
                        JSON.toJSONString(info),
                        TASK_SUCCESS,
                        null,
                        startedAt
                );
            } catch (RuntimeException ex) {
                recordApiLog(
                        context,
                        user,
                        "GET_PRODUCT_IMPORT_INFO",
                        PRODUCT_IMPORT_INFO_ENDPOINT,
                        requestPayload.toJSONString(),
                        null,
                        TASK_FAILED,
                        ex.getMessage(),
                        startedAt
                );
                throw ex;
            }
            detail = normalizeTask(task, info);
            if (!TASK_RUNNING.equals(detail.getTaskStatus())) {
                persistTerminalResult(context, task, detail);
                return detail;
            }
            if (attempt + 1 < maxAttempts) {
                sleepQuietly(2000L);
            }
        }
        task.setTaskStatus(TASK_RUNNING);
        task.setErrorMessage("remote task still running");
        task.setUpdateTime(new Date());
        publishTaskMapper.updateById(task);
        return detail == null ? toTaskView(task, Collections.emptyList()) : detail;
    }

    private void persistTerminalResult(
            OzonProductDraftResolver.ResolvedDraftContext context,
            OzonListingPublishTask task,
            OzonProductPublishTaskView detail
    ) {
        Date now = new Date();
        task.setTaskStatus(detail.getTaskStatus());
        task.setResponsePayloadJson(detail.getResponsePayloadJson());
        task.setErrorMessage(detail.getErrorSummary());
        task.setUpdateTime(now);
        publishTaskMapper.updateById(task);

        Map<String, OzonProductDraftResolver.ResolvedVariant> variantByOfferId = new LinkedHashMap<>();
        for (OzonProductDraftResolver.ResolvedVariant variant : context.variants()) {
            variantByOfferId.put(variant.effectiveOfferId(), variant);
            variant.variant().setStatus(TASK_FAILED.equals(detail.getTaskStatus()) ? "FAILED" : variant.variant().getStatus());
        }
        for (OzonProductPublishTaskView.NormalizedItem item : safeList(detail.getNormalizedItems())) {
            OzonProductDraftResolver.ResolvedVariant variant = variantByOfferId.get(item.getOfferId());
            if (variant == null) {
                continue;
            }
            boolean success = "imported".equalsIgnoreCase(item.getRemoteStatus()) && StrUtil.isNotBlank(item.getProductId());
            variant.variant().setStatus(success ? "PUBLISHED" : "FAILED");
            variant.variant().setLastSyncStatus(success ? TASK_SUCCESS : TASK_FAILED);
            variant.variant().setLastSyncMessage(success ? "Published" : detail.getErrorSummary());
            variant.variant().setUpdateTime(now);
            variantMapper.updateById(variant.variant());
            if (success) {
                OzonProductMap map = context.productMapBySku().get(variant.variant().getMaterialSku());
                if (map == null) {
                    map = new OzonProductMap();
                    map.setId(nextId());
                    map.setAuthId(context.auth().getId());
                    map.setShopId(context.auth().getShopId());
                    map.setMaterialSku(variant.variant().getMaterialSku());
                    map.setCreateTime(now);
                }
                map.setOzonOfferId(item.getOfferId());
                map.setOzonProductId(item.getProductId());
                map.setStatus("MAPPED");
                map.setLastSyncStatus(TASK_SUCCESS);
                map.setLastSyncMessage("Published");
                map.setLastSyncTime(now);
                map.setUpdateTime(now);
                productMapMapper.updateById(map);
            }
        }
        updateDraftStatus(context.draft(), resolveDraftStatus(detail.getTaskStatus()), task.getId(), now);
        if (TASK_SUCCESS.equals(detail.getTaskStatus())) {
            errorRecorder.markResolved(context.auth().getId(), OzonErrorSourceType.PRODUCT, context.draft().getId());
            return;
        }
        errorRecorder.recordOpen(new OzonErrorRecordCommand(
                context.auth().getId(),
                context.auth().getShopId(),
                OzonErrorSourceType.PRODUCT,
                context.draft().getId(),
                context.draft().getDraftName(),
                detail.getErrorSummary(),
                task.getRequestPayloadJson(),
                task.getResponsePayloadJson(),
                task.getOperator()
        ));
    }

    private String buildRequestPayload(OzonProductDraftResolver.ResolvedDraftContext context) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (OzonProductDraftResolver.ResolvedVariant variant : context.variants()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("offer_id", variant.effectiveOfferId());
            item.put("name", variant.effectiveName());
            item.put("description_category_id", context.draft().getDescriptionCategoryId());
            item.put("type_id", context.draft().getTypeId());
            if (StrUtil.isNotBlank(variant.effectiveBarcode())) {
                item.put("barcode", variant.effectiveBarcode());
            }
            if (variant.effectivePrice() != null) {
                item.put("price", variant.effectivePrice());
            }
            if (variant.effectiveWeight() != null) {
                item.put("weight", variant.effectiveWeight());
            }
            if (variant.effectiveLength() != null) {
                item.put("depth", variant.effectiveLength());
            }
            if (variant.effectiveWidth() != null) {
                item.put("width", variant.effectiveWidth());
            }
            if (variant.effectiveHeight() != null) {
                item.put("height", variant.effectiveHeight());
            }
            if (!variant.effectiveImages().isEmpty()) {
                item.put("images", variant.effectiveImages());
            }
            List<Map<String, Object>> attributes = new ArrayList<>();
            appendAttributes(attributes, variant.commonAttributes());
            appendAttributes(attributes, variant.variantAttributes());
            if (!attributes.isEmpty()) {
                item.put("attributes", attributes);
            }
            items.add(item);
        }
        JSONObject payload = new JSONObject();
        payload.put("items", items);
        return payload.toJSONString();
    }

    private void appendAttributes(List<Map<String, Object>> target, List<com.wimoor.ozon.product.pojo.entity.OzonListingAttribute> attributes) {
        for (com.wimoor.ozon.product.pojo.entity.OzonListingAttribute attribute : safeList(attributes)) {
            List<Map<String, Object>> values = OzonProductDraftResolver.parseAttributeValues(attribute);
            if (values.isEmpty()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", attribute.getAttributeId());
            row.put("values", values);
            target.add(row);
        }
    }

    private OzonProductPublishTaskView normalizeTask(
            OzonListingPublishTask task,
            OzonProductApiClient.ProductImportInfo info
    ) {
        List<OzonProductPublishTaskView.NormalizedItem> items = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;
        for (OzonProductApiClient.ProductImportItem remote : safeList(info == null ? null : info.getItems())) {
            OzonProductPublishTaskView.NormalizedItem item = new OzonProductPublishTaskView.NormalizedItem();
            item.setOfferId(remote.getOfferId());
            item.setProductId(remote.getProductId());
            item.setRemoteStatus(remote.getStatus());
            item.setHasErrors(!safeList(remote.getErrors()).isEmpty());
            item.setErrors(toErrors(remote.getErrors()));
            items.add(item);
            boolean success = "imported".equalsIgnoreCase(remote.getStatus()) && StrUtil.isNotBlank(remote.getProductId());
            if (success) {
                successCount++;
            }
            if (item.isHasErrors() || !success) {
                errorCount++;
            }
        }
        String taskStatus = TASK_RUNNING;
        if (!items.isEmpty()) {
            if (errorCount == 0) {
                taskStatus = TASK_SUCCESS;
            } else if (successCount == 0) {
                taskStatus = TASK_FAILED;
            } else {
                taskStatus = TASK_PARTIAL;
            }
        }
        OzonProductPublishTaskView detail = new OzonProductPublishTaskView();
        detail.setTaskStatus(taskStatus);
        detail.setRemoteTaskId(task.getRemoteTaskId());
        detail.setRequestPayloadJson(task.getRequestPayloadJson());
        detail.setResponsePayloadJson(JSON.toJSONString(info));
        detail.setNormalizedItems(items);
        detail.setErrorSummary(buildErrorSummary(items));
        return detail;
    }

    private List<OzonProductPublishTaskView.ErrorItem> toErrors(List<OzonProductApiClient.ProductImportError> errors) {
        List<OzonProductPublishTaskView.ErrorItem> result = new ArrayList<>();
        for (OzonProductApiClient.ProductImportError remote : safeList(errors)) {
            OzonProductPublishTaskView.ErrorItem item = new OzonProductPublishTaskView.ErrorItem();
            item.setCode(remote.getCode());
            item.setField(remote.getField());
            item.setAttributeId(remote.getAttributeId());
            item.setAttributeName(remote.getAttributeName());
            item.setMessage(remote.getMessage());
            result.add(item);
        }
        return result;
    }

    private String buildErrorSummary(List<OzonProductPublishTaskView.NormalizedItem> items) {
        int withErrors = 0;
        for (OzonProductPublishTaskView.NormalizedItem item : safeList(items)) {
            if (item.isHasErrors() || !"imported".equalsIgnoreCase(item.getRemoteStatus())) {
                withErrors++;
            }
        }
        return withErrors == 0 ? null : withErrors + " variant has remote validation errors";
    }

    private void updateDraftStatus(OzonListingDraft draft, String status, String taskId, Date now) {
        draft.setStatus(status);
        draft.setLastPublishTaskId(taskId);
        draft.setUpdateTime(now);
        draftMapper.updateById(draft);
    }

    private String resolveDraftStatus(String taskStatus) {
        if (TASK_SUCCESS.equals(taskStatus)) {
            return "PUBLISHED";
        }
        if (TASK_PARTIAL.equals(taskStatus)) {
            return "PARTIAL";
        }
        if (TASK_FAILED.equals(taskStatus)) {
            return "FAILED";
        }
        return "PUBLISHING";
    }

    private OzonListingPublishTask buildTask(OzonProductDraftResolver.ResolvedDraftContext context, UserInfo user, Date now) {
        OzonListingPublishTask task = new OzonListingPublishTask();
        task.setId(nextId());
        task.setDraftId(context.draft().getId());
        task.setAuthId(context.auth().getId());
        task.setShopId(context.auth().getShopId());
        task.setTaskStatus(TASK_RUNNING);
        task.setOperator(user == null ? null : user.getId());
        task.setCreateTime(now);
        task.setUpdateTime(now);
        return task;
    }

    private OzonSyncJob buildSyncJob(OzonProductDraftResolver.ResolvedDraftContext context, UserInfo user, Date now) {
        OzonSyncJob job = new OzonSyncJob();
        job.setId(nextId());
        job.setAuthId(context.auth().getId());
        job.setShopId(context.auth().getShopId());
        job.setJobType(OzonSyncJobType.PRODUCT_PUBLISH.name());
        job.setStatus(TASK_RUNNING);
        job.setPayload("{\"draftId\":\"" + context.draft().getId() + "\"}");
        job.setOperator(user == null ? null : user.getId());
        job.setCreateTime(now);
        job.setUpdateTime(now);
        return job;
    }

    private void finishSyncJob(OzonSyncJob job, OzonProductPublishTaskView detail, Date now) {
        job.setStatus(detail.getTaskStatus());
        job.setPayload(detail.getErrorSummary());
        job.setUpdateTime(now);
        syncJobMapper.updateById(job);
    }

    private void failSyncJob(OzonSyncJob job, RuntimeException ex) {
        job.setStatus(TASK_FAILED);
        job.setPayload(ex.getMessage());
        job.setUpdateTime(new Date());
        syncJobMapper.updateById(job);
    }

    private void failPublish(
            OzonProductDraftResolver.ResolvedDraftContext context,
            OzonListingPublishTask task,
            OzonSyncJob syncJob,
            RuntimeException ex
    ) {
        Date now = new Date();
        task.setTaskStatus(TASK_FAILED);
        task.setErrorMessage(ex.getMessage());
        task.setUpdateTime(now);
        publishTaskMapper.updateById(task);
        updateDraftStatus(context.draft(), "FAILED", task.getId(), now);
        failSyncJob(syncJob, ex);
        errorRecorder.recordOpen(new OzonErrorRecordCommand(
                context.auth().getId(),
                context.auth().getShopId(),
                OzonErrorSourceType.PRODUCT,
                context.draft().getId(),
                context.draft().getDraftName(),
                ex.getMessage(),
                task.getRequestPayloadJson(),
                task.getResponsePayloadJson(),
                task.getOperator()
        ));
    }

    private OzonProductPublishTaskView toTaskView(OzonListingPublishTask task, List<OzonProductPublishTaskView.NormalizedItem> items) {
        OzonProductPublishTaskView detail = new OzonProductPublishTaskView();
        detail.setTaskStatus(task.getTaskStatus());
        detail.setRemoteTaskId(task.getRemoteTaskId());
        detail.setRequestPayloadJson(task.getRequestPayloadJson());
        detail.setResponsePayloadJson(task.getResponsePayloadJson());
        detail.setNormalizedItems(items);
        detail.setErrorSummary(task.getErrorMessage());
        return detail;
    }

    private OzonProductPublishTaskHistoryView toTaskHistoryItem(OzonListingPublishTask task) {
        OzonProductPublishTaskHistoryView item = new OzonProductPublishTaskHistoryView();
        item.setTaskId(task.getId());
        item.setDraftId(task.getDraftId());
        item.setTaskStatus(task.getTaskStatus());
        item.setRemoteTaskId(task.getRemoteTaskId());
        item.setErrorSummary(task.getErrorMessage());
        item.setOperator(task.getOperator());
        item.setCreatedAt(task.getCreateTime());
        item.setUpdatedAt(task.getUpdateTime());
        return item;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("publish polling interrupted", ex);
        }
    }

    private void recordApiLog(
            OzonProductDraftResolver.ResolvedDraftContext context,
            UserInfo user,
            String actionName,
            String endpoint,
            String requestPayload,
            String responsePayload,
            String status,
            String errorMessage,
            long startedAt
    ) {
        opsService.recordApiLog(new OzonApiLogRecordCommand(
                context.auth().getId(),
                context.auth().getShopId(),
                "PRODUCT",
                actionName,
                endpoint,
                "POST",
                OzonErrorSourceType.PRODUCT,
                context.draft().getId(),
                requestPayload,
                responsePayload,
                status,
                errorMessage,
                Math.max(System.currentTimeMillis() - startedAt, 0L),
                user == null ? null : user.getId()
        ));
    }

    private void recordOperationAudit(
            OzonProductDraftResolver.ResolvedDraftContext context,
            UserInfo user,
            String requestPayload,
            String resultStatus,
            String resultMessage
    ) {
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                context.auth().getId(),
                context.auth().getShopId(),
                "PRODUCT_PUBLISH",
                OzonErrorSourceType.PRODUCT,
                context.draft().getId(),
                context.draft().getDraftName(),
                requestPayload,
                resultStatus,
                resultMessage,
                user == null ? null : user.getId()
        ));
    }

    private String nextId() {
        return IdUtil.getSnowflakeNextIdStr();
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? Collections.emptyList() : items;
    }
}
