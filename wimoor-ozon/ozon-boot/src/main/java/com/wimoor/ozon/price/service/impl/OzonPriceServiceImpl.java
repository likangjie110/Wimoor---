package com.wimoor.ozon.price.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.ops.annotation.OzonAudit;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogRecordCommand;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozon.price.mapper.OzonPriceSnapshotMapper;
import com.wimoor.ozon.price.mapper.OzonPriceTaskMapper;
import com.wimoor.ozon.price.pojo.dto.OzonPricePushCommand;
import com.wimoor.ozon.price.pojo.dto.OzonPricePushItem;
import com.wimoor.ozon.price.pojo.entity.OzonPriceSnapshot;
import com.wimoor.ozon.price.pojo.entity.OzonPriceTask;
import com.wimoor.ozon.price.pojo.vo.OzonPricePushResult;
import com.wimoor.ozon.price.pojo.vo.OzonPriceTaskView;
import com.wimoor.ozon.price.service.IOzonPriceService;
import com.wimoor.ozon.product.mapper.OzonProductMapMapper;
import com.wimoor.ozon.product.pojo.entity.OzonProductMap;
import com.wimoor.ozon.security.OzonCredentialService;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJobType;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class OzonPriceServiceImpl implements IOzonPriceService {

    private static final String RUNNING = "RUNNING";
    private static final String SUBMITTED = "SUBMITTED";
    private static final String FAILED = "FAILED";
    private static final String API_GROUP = "PRICE";
    private static final String PRICE_OBJECT_TYPE = "PRICE";
    private static final String PRICE_IMPORT_ENDPOINT = "/v1/product/import/prices";

    private final OzonAuthAccessService authAccessService;
    private final OzonProductMapMapper productMapMapper;
    private final OzonPriceTaskMapper priceTaskMapper;
    private final OzonPriceSnapshotMapper priceSnapshotMapper;
    private final OzonSellerApiClient sellerApiClient;
    private final OzonSyncJobMapper syncJobMapper;
    private final OzonCredentialService credentialService;
    private final OzonFeatureGate featureGate;
    private IOzonOpsService opsService = new IOzonOpsService() {
    };

    @Autowired
    public OzonPriceServiceImpl(
            OzonAuthAccessService authAccessService,
            OzonProductMapMapper productMapMapper,
            OzonPriceTaskMapper priceTaskMapper,
            OzonPriceSnapshotMapper priceSnapshotMapper,
            OzonSellerApiClient sellerApiClient,
            OzonSyncJobMapper syncJobMapper,
            OzonCredentialService credentialService,
            OzonFeatureGate featureGate
    ) {
        this.authAccessService = authAccessService;
        this.productMapMapper = productMapMapper;
        this.priceTaskMapper = priceTaskMapper;
        this.priceSnapshotMapper = priceSnapshotMapper;
        this.sellerApiClient = sellerApiClient;
        this.syncJobMapper = syncJobMapper;
        this.credentialService = credentialService;
        this.featureGate = featureGate;
    }

    public OzonPriceServiceImpl(
            OzonAuthMapper authMapper,
            OzonProductMapMapper productMapMapper,
            OzonPriceTaskMapper priceTaskMapper,
            OzonPriceSnapshotMapper priceSnapshotMapper,
            OzonSellerApiClient sellerApiClient,
            OzonSyncJobMapper syncJobMapper,
            OzonCredentialService credentialService
    ) {
        this(new OzonAuthAccessService(authMapper), productMapMapper, priceTaskMapper, priceSnapshotMapper, sellerApiClient,
                syncJobMapper,
                credentialService,
                OzonFeatureGate.allEnabled());
    }

    public OzonPriceServiceImpl(
            OzonAuthMapper authMapper,
            OzonProductMapMapper productMapMapper,
            OzonPriceTaskMapper priceTaskMapper,
            OzonPriceSnapshotMapper priceSnapshotMapper,
            OzonSellerApiClient sellerApiClient,
            OzonSyncJobMapper syncJobMapper,
            OzonCredentialService credentialService,
            OzonFeatureGate featureGate
    ) {
        this(new OzonAuthAccessService(authMapper), productMapMapper, priceTaskMapper, priceSnapshotMapper, sellerApiClient,
                syncJobMapper,
                credentialService,
                featureGate);
    }

    @Autowired(required = false)
    public void setOpsService(IOzonOpsService opsService) {
        if (opsService != null) {
            this.opsService = opsService;
        }
    }

    @Override
    @OzonAudit(operationType = "PUSH", objectType = "PRICE", description = "推送价格到 OZON")
    public OzonPricePushResult push(UserInfo user, OzonPricePushCommand command) {
        featureGate.assertPriceWriteEnabled();
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command.getAuthId());
        String auditPayload = JSON.toJSONString(command);
        String currencyCode = requireCurrency(command.getCurrencyCode());
        List<OzonPricePushItem> items = cleanItems(command.getItems());
        Map<String, OzonProductMap> productMap = loadProductMap(auth.getId(), items);
        Date now = new Date();
        OzonPriceTask task = buildTask(auth, user, items.size(), now);
        OzonSyncJob syncJob = buildSyncJob(auth, user, OzonSyncJobType.PRICE_SYNC, buildJobPayload(items.size(), currencyCode), now);
        priceTaskMapper.insert(task);
        syncJobMapper.insert(syncJob);
        String requestPayload = buildPayload(currencyCode, items, productMap);
        long startedAt = System.currentTimeMillis();
        try {
            String response = sellerApiClient.importPrices(
                    auth.getClientId(),
                    credentialService.decrypt(auth.getApiKeyCiphertext()),
                    requestPayload
            );
            recordApiLog(auth, user, task.getId(), requestPayload, response, SUBMITTED, null, startedAt);
            finishTask(task, items.size(), now);
            finishSyncJob(syncJob, now);
            saveSnapshots(task, currencyCode, items, productMap, now);
            recordOperationAudit(auth, user, task.getId(), currencyCode, auditPayload, SUBMITTED, "submitted");
            return buildResult(task.getId(), items.size(), now);
        } catch (RuntimeException ex) {
            recordApiLog(auth, user, task.getId(), requestPayload, null, FAILED, ex.getMessage(), startedAt);
            failTask(task, now, ex);
            failSyncJob(syncJob, now, ex);
            recordOperationAudit(auth, user, task.getId(), currencyCode, auditPayload, FAILED, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public List<OzonPriceSnapshot> listSnapshots(UserInfo user, String authId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        return priceSnapshotMapper.listLatestByAuthId(auth.getId());
    }

    @Override
    public List<OzonPriceTaskView> listTasks(UserInfo user, String authId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        List<OzonPriceTask> tasks = priceTaskMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OzonPriceTask>()
                .eq("auth_id", auth.getId())
                .orderByDesc("create_time")
                .last("limit 20"));
        if (tasks == null || tasks.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<OzonPriceTaskView> result = new ArrayList<>(tasks.size());
        for (OzonPriceTask task : tasks) {
            OzonPriceTaskView view = new OzonPriceTaskView();
            view.setTaskId(task.getId());
            view.setTaskStatus(task.getTaskStatus());
            view.setRequestedCount(task.getRequestedCount());
            view.setSuccessCount(task.getSuccessCount());
            view.setErrorMessage(task.getErrorMessage());
            view.setOperator(task.getOperator());
            view.setCreatedAt(task.getCreateTime());
            view.setUpdatedAt(task.getUpdateTime());
            result.add(view);
        }
        return result;
    }

    private List<OzonPricePushItem> cleanItems(List<OzonPricePushItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("价格推送明细不能为空");
        }
        List<OzonPricePushItem> result = new ArrayList<>();
        for (OzonPricePushItem item : items) {
            if (item == null || StrUtil.isBlank(item.getMaterialSku())) {
                throw new IllegalArgumentException("价格推送明细格式不正确");
            }
            result.add(new OzonPricePushItem(
                    item.getMaterialSku().trim(),
                    requirePositive(item.getPrice(), "价格必须大于0"),
                    normalizeOldPrice(item.getOldPrice())
            ));
        }
        return result;
    }

    private Map<String, OzonProductMap> loadProductMap(String authId, List<OzonPricePushItem> items) {
        List<String> skus = items.stream().map(OzonPricePushItem::getMaterialSku).collect(Collectors.toList());
        List<OzonProductMap> mappings = productMapMapper.listByMaterialSkus(authId, skus);
        Map<String, OzonProductMap> result = new LinkedHashMap<>();
        if (mappings != null) {
            for (OzonProductMap mapping : mappings) {
                result.put(mapping.getMaterialSku(), mapping);
            }
        }
        for (String sku : skus) {
            OzonProductMap map = result.get(sku);
            if (map == null || StrUtil.isBlank(map.getOzonOfferId())) {
                throw new IllegalArgumentException("SKU未完成Ozon映射: " + sku);
            }
        }
        return result;
    }

    private OzonPriceTask buildTask(OzonAuth auth, UserInfo user, int requestedCount, Date now) {
        OzonPriceTask task = new OzonPriceTask();
        task.setId(nextId());
        task.setAuthId(auth.getId());
        task.setShopId(auth.getShopId());
        task.setTaskStatus(RUNNING);
        task.setRequestedCount(requestedCount);
        task.setSuccessCount(0);
        task.setOperator(user != null ? user.getId() : null);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        return task;
    }

    private void finishTask(OzonPriceTask task, int successCount, Date now) {
        task.setTaskStatus(SUBMITTED);
        task.setSuccessCount(successCount);
        task.setUpdateTime(now);
        priceTaskMapper.updateById(task);
    }

    private void failTask(OzonPriceTask task, Date now, RuntimeException ex) {
        task.setTaskStatus(FAILED);
        task.setErrorMessage(ex.getMessage());
        task.setUpdateTime(now);
        priceTaskMapper.updateById(task);
    }

    private void saveSnapshots(
            OzonPriceTask task,
            String currencyCode,
            List<OzonPricePushItem> items,
            Map<String, OzonProductMap> productMap,
            Date now
    ) {
        for (OzonPricePushItem item : items) {
            OzonProductMap map = productMap.get(item.getMaterialSku());
            OzonPriceSnapshot snapshot = new OzonPriceSnapshot();
            snapshot.setId(nextId());
            snapshot.setTaskId(task.getId());
            snapshot.setAuthId(task.getAuthId());
            snapshot.setShopId(task.getShopId());
            snapshot.setMaterialSku(item.getMaterialSku());
            snapshot.setOzonOfferId(map.getOzonOfferId());
            snapshot.setPrice(item.getPrice());
            snapshot.setOldPrice(item.getOldPrice());
            snapshot.setCurrencyCode(currencyCode);
            snapshot.setSyncStatus(SUBMITTED);
            snapshot.setSyncMessage("submitted");
            snapshot.setSyncedAt(now);
            priceSnapshotMapper.insert(snapshot);
        }
    }

    private OzonPricePushResult buildResult(String taskId, int accepted, Date now) {
        OzonPricePushResult result = new OzonPricePushResult();
        result.setTaskId(taskId);
        result.setAccepted(accepted);
        result.setSubmittedAt(now);
        result.setMessage("submitted");
        return result;
    }

    private OzonSyncJob buildSyncJob(OzonAuth auth, UserInfo user, OzonSyncJobType jobType, String payload, Date now) {
        OzonSyncJob job = new OzonSyncJob();
        job.setId(nextId());
        job.setAuthId(auth.getId());
        job.setShopId(auth.getShopId());
        job.setJobType(jobType.name());
        job.setStatus(RUNNING);
        job.setPayload(payload);
        job.setOperator(user != null ? user.getId() : null);
        job.setCreateTime(now);
        job.setUpdateTime(now);
        return job;
    }

    private String buildJobPayload(int count, String currencyCode) {
        JSONObject payload = new JSONObject();
        payload.put("count", count);
        payload.put("currencyCode", currencyCode);
        return payload.toJSONString();
    }

    private void finishSyncJob(OzonSyncJob syncJob, Date now) {
        syncJob.setStatus("DONE");
        syncJob.setUpdateTime(now);
        syncJobMapper.updateById(syncJob);
    }

    private void failSyncJob(OzonSyncJob syncJob, Date now, RuntimeException ex) {
        syncJob.setStatus(FAILED);
        syncJob.setPayload(ex.getMessage());
        syncJob.setUpdateTime(now);
        syncJobMapper.updateById(syncJob);
    }

    private String buildPayload(String currencyCode, List<OzonPricePushItem> items, Map<String, OzonProductMap> productMap) {
        JSONArray prices = new JSONArray();
        for (OzonPricePushItem item : items) {
            JSONObject price = new JSONObject();
            price.put("offer_id", productMap.get(item.getMaterialSku()).getOzonOfferId());
            price.put("price", item.getPrice().toPlainString());
            price.put("currency_code", currencyCode);
            if (item.getOldPrice() != null) {
                price.put("old_price", item.getOldPrice().toPlainString());
            }
            prices.add(price);
        }
        JSONObject payload = new JSONObject();
        payload.put("prices", prices);
        return payload.toJSONString();
    }

    private String requireCurrency(String currencyCode) {
        if (StrUtil.isBlank(currencyCode)) {
            throw new IllegalArgumentException("币种不能为空");
        }
        return currencyCode.trim().toUpperCase();
    }

    private BigDecimal requirePositive(BigDecimal amount, String message) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
        return amount.stripTrailingZeros();
    }

    private BigDecimal normalizeOldPrice(BigDecimal oldPrice) {
        if (oldPrice == null) {
            return null;
        }
        if (oldPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("原价不能小于0");
        }
        return oldPrice.stripTrailingZeros();
    }

    private String nextId() {
        return IdUtil.getSnowflakeNextIdStr();
    }

    private void recordApiLog(
            OzonAuth auth,
            UserInfo user,
            String objectId,
            String requestPayload,
            String responsePayload,
            String status,
            String errorMessage,
            long startedAt
    ) {
        opsService.recordApiLog(new OzonApiLogRecordCommand(
                auth.getId(),
                auth.getShopId(),
                API_GROUP,
                "IMPORT_PRICES",
                PRICE_IMPORT_ENDPOINT,
                "POST",
                PRICE_OBJECT_TYPE,
                objectId,
                requestPayload,
                responsePayload,
                status,
                errorMessage,
                Math.max(System.currentTimeMillis() - startedAt, 0L),
                user == null ? null : user.getId()
        ));
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
                "PRICE_PUSH",
                PRICE_OBJECT_TYPE,
                objectId,
                objectCode,
                requestPayload,
                resultStatus,
                resultMessage,
                user == null ? null : user.getId()
        ));
    }
}
