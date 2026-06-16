package com.wimoor.ozon.stock.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogRecordCommand;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozon.product.mapper.OzonProductMapMapper;
import com.wimoor.ozon.product.pojo.entity.OzonProductMap;
import com.wimoor.ozon.security.OzonCredentialService;
import com.wimoor.ozon.stock.mapper.OzonStockSnapshotMapper;
import com.wimoor.ozon.stock.mapper.OzonStockTaskMapper;
import com.wimoor.ozon.stock.pojo.dto.OzonStockPushCommand;
import com.wimoor.ozon.stock.pojo.dto.OzonStockPushItem;
import com.wimoor.ozon.stock.pojo.entity.OzonStockSnapshot;
import com.wimoor.ozon.stock.pojo.entity.OzonStockTask;
import com.wimoor.ozon.stock.pojo.vo.OzonStockPushResult;
import com.wimoor.ozon.stock.pojo.vo.OzonStockTaskView;
import com.wimoor.ozon.stock.service.IOzonStockService;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJobType;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class OzonStockServiceImpl implements IOzonStockService {

    private static final String RUNNING = "RUNNING";
    private static final String SUBMITTED = "SUBMITTED";
    private static final String FAILED = "FAILED";
    private static final String API_GROUP = "STOCK";
    private static final String STOCK_OBJECT_TYPE = "STOCK";
    private static final String STOCK_IMPORT_ENDPOINT = "/v2/products/stocks";

    private final OzonAuthAccessService authAccessService;
    private final OzonProductMapMapper productMapMapper;
    private final OzonStockTaskMapper stockTaskMapper;
    private final OzonStockSnapshotMapper stockSnapshotMapper;
    private final OzonSellerApiClient sellerApiClient;
    private final OzonSyncJobMapper syncJobMapper;
    private final OzonCredentialService credentialService;
    private final OzonFeatureGate featureGate;
    private IOzonOpsService opsService = new IOzonOpsService() {
    };

    @Autowired
    public OzonStockServiceImpl(
            OzonAuthAccessService authAccessService,
            OzonProductMapMapper productMapMapper,
            OzonStockTaskMapper stockTaskMapper,
            OzonStockSnapshotMapper stockSnapshotMapper,
            OzonSellerApiClient sellerApiClient,
            OzonSyncJobMapper syncJobMapper,
            OzonCredentialService credentialService,
            OzonFeatureGate featureGate
    ) {
        this.authAccessService = authAccessService;
        this.productMapMapper = productMapMapper;
        this.stockTaskMapper = stockTaskMapper;
        this.stockSnapshotMapper = stockSnapshotMapper;
        this.sellerApiClient = sellerApiClient;
        this.syncJobMapper = syncJobMapper;
        this.credentialService = credentialService;
        this.featureGate = featureGate;
    }

    public OzonStockServiceImpl(
            OzonAuthMapper authMapper,
            OzonProductMapMapper productMapMapper,
            OzonStockTaskMapper stockTaskMapper,
            OzonStockSnapshotMapper stockSnapshotMapper,
            OzonSellerApiClient sellerApiClient,
            OzonSyncJobMapper syncJobMapper,
            OzonCredentialService credentialService
    ) {
        this(new OzonAuthAccessService(authMapper), productMapMapper, stockTaskMapper, stockSnapshotMapper, sellerApiClient,
                syncJobMapper,
                credentialService,
                OzonFeatureGate.allEnabled());
    }

    public OzonStockServiceImpl(
            OzonAuthMapper authMapper,
            OzonProductMapMapper productMapMapper,
            OzonStockTaskMapper stockTaskMapper,
            OzonStockSnapshotMapper stockSnapshotMapper,
            OzonSellerApiClient sellerApiClient,
            OzonSyncJobMapper syncJobMapper,
            OzonCredentialService credentialService,
            OzonFeatureGate featureGate
    ) {
        this(new OzonAuthAccessService(authMapper), productMapMapper, stockTaskMapper, stockSnapshotMapper, sellerApiClient,
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
    public OzonStockPushResult push(UserInfo user, OzonStockPushCommand command) {
        featureGate.assertStockWriteEnabled();
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command.getAuthId());
        String auditPayload = JSON.toJSONString(command);
        List<OzonStockPushItem> items = cleanItems(command.getItems());
        Map<String, OzonProductMap> productMap = loadProductMap(auth.getId(), items);
        Date now = new Date();
        OzonStockTask task = buildTask(auth, user, command.getWarehouseId(), items.size(), now);
        OzonSyncJob syncJob = buildSyncJob(auth, user, OzonSyncJobType.STOCK_SYNC,
                buildJobPayload(items.size(), command.getWarehouseId()), now);
        stockTaskMapper.insert(task);
        syncJobMapper.insert(syncJob);
        String requestPayload = buildPayload(command.getWarehouseId(), items, productMap);
        long startedAt = System.currentTimeMillis();
        try {
            String response = sellerApiClient.updateStocks(
                    auth.getClientId(),
                    credentialService.decrypt(auth.getApiKeyCiphertext()),
                    requestPayload
            );
            recordApiLog(auth, user, task.getId(), requestPayload, response, SUBMITTED, null, startedAt);
            task.setTaskStatus(SUBMITTED);
            task.setSuccessCount(items.size());
            task.setUpdateTime(now);
            stockTaskMapper.updateById(task);
            finishSyncJob(syncJob, now);
            saveSnapshots(task, items, productMap, now, SUBMITTED, "submitted");
            recordOperationAudit(auth, user, task.getId(), trim(command.getWarehouseId()), auditPayload, SUBMITTED, "submitted");
            OzonStockPushResult result = new OzonStockPushResult();
            result.setTaskId(task.getId());
            result.setAccepted(items.size());
            result.setSubmittedAt(now);
            result.setMessage("submitted");
            return result;
        } catch (RuntimeException ex) {
            recordApiLog(auth, user, task.getId(), requestPayload, null, FAILED, ex.getMessage(), startedAt);
            task.setTaskStatus(FAILED);
            task.setErrorMessage(ex.getMessage());
            task.setUpdateTime(now);
            stockTaskMapper.updateById(task);
            failSyncJob(syncJob, now, ex);
            recordOperationAudit(auth, user, task.getId(), trim(command.getWarehouseId()), auditPayload, FAILED, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public List<OzonStockSnapshot> listSnapshots(UserInfo user, String authId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        return stockSnapshotMapper.listLatestByAuthId(auth.getId());
    }

    @Override
    public List<OzonStockTaskView> listTasks(UserInfo user, String authId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        List<OzonStockTask> tasks = stockTaskMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OzonStockTask>()
                .eq("auth_id", auth.getId())
                .orderByDesc("create_time")
                .last("limit 20"));
        if (tasks == null || tasks.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<OzonStockTaskView> result = new ArrayList<>(tasks.size());
        for (OzonStockTask task : tasks) {
            OzonStockTaskView view = new OzonStockTaskView();
            view.setTaskId(task.getId());
            view.setWarehouseId(task.getWarehouseId());
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

    private List<OzonStockPushItem> cleanItems(List<OzonStockPushItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("库存推送明细不能为空");
        }
        List<OzonStockPushItem> result = new ArrayList<>();
        for (OzonStockPushItem item : items) {
            if (item == null || StrUtil.isBlank(item.getMaterialSku()) || item.getQuantity() == null || item.getQuantity() < 0) {
                throw new IllegalArgumentException("库存推送明细格式不正确");
            }
            result.add(new OzonStockPushItem(item.getMaterialSku().trim(), item.getQuantity()));
        }
        return result;
    }

    private Map<String, OzonProductMap> loadProductMap(String authId, List<OzonStockPushItem> items) {
        List<String> skus = items.stream().map(OzonStockPushItem::getMaterialSku).collect(Collectors.toList());
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

    private OzonStockTask buildTask(OzonAuth auth, UserInfo user, String warehouseId, int requestedCount, Date now) {
        OzonStockTask task = new OzonStockTask();
        task.setId(nextId());
        task.setAuthId(auth.getId());
        task.setShopId(auth.getShopId());
        task.setWarehouseId(trim(warehouseId));
        task.setTaskStatus(RUNNING);
        task.setRequestedCount(requestedCount);
        task.setSuccessCount(0);
        task.setOperator(user != null ? user.getId() : null);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        return task;
    }

    private void saveSnapshots(
            OzonStockTask task,
            List<OzonStockPushItem> items,
            Map<String, OzonProductMap> productMap,
            Date now,
            String status,
            String message
    ) {
        for (OzonStockPushItem item : items) {
            OzonProductMap map = productMap.get(item.getMaterialSku());
            OzonStockSnapshot snapshot = new OzonStockSnapshot();
            snapshot.setId(nextId());
            snapshot.setTaskId(task.getId());
            snapshot.setAuthId(task.getAuthId());
            snapshot.setShopId(task.getShopId());
            snapshot.setWarehouseId(task.getWarehouseId());
            snapshot.setMaterialSku(item.getMaterialSku());
            snapshot.setOzonOfferId(map.getOzonOfferId());
            snapshot.setQuantity(item.getQuantity());
            snapshot.setSyncStatus(status);
            snapshot.setSyncMessage(message);
            snapshot.setSyncedAt(now);
            stockSnapshotMapper.insert(snapshot);
        }
    }

    private String buildPayload(String warehouseId, List<OzonStockPushItem> items, Map<String, OzonProductMap> productMap) {
        JSONArray stocks = new JSONArray();
        for (OzonStockPushItem item : items) {
            JSONObject stock = new JSONObject();
            stock.put("offer_id", productMap.get(item.getMaterialSku()).getOzonOfferId());
            stock.put("stock", item.getQuantity());
            if (StrUtil.isNotBlank(warehouseId)) {
                stock.put("warehouse_id", warehouseId.trim());
            }
            stocks.add(stock);
        }
        JSONObject payload = new JSONObject();
        payload.put("stocks", stocks);
        return payload.toJSONString();
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

    private String buildJobPayload(int count, String warehouseId) {
        JSONObject payload = new JSONObject();
        payload.put("count", count);
        payload.put("warehouseId", trim(warehouseId));
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

    private String trim(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
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
                "UPDATE_STOCKS",
                STOCK_IMPORT_ENDPOINT,
                "POST",
                STOCK_OBJECT_TYPE,
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
                "STOCK_PUSH",
                STOCK_OBJECT_TYPE,
                objectId,
                objectCode,
                requestPayload,
                resultStatus,
                resultMessage,
                user == null ? null : user.getId()
        ));
    }
}
