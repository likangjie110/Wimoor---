package com.wimoor.ozon.posting.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.erp.api.ErpClientOneFeign;
import com.wimoor.erp.order.pojo.dto.OzonErpOrderUpsertCommand;
import com.wimoor.erp.order.pojo.dto.OzonErpOrderUpsertResult;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.error.pojo.dto.OzonErrorRecordCommand;
import com.wimoor.ozon.error.pojo.entity.OzonErrorSourceType;
import com.wimoor.ozon.error.service.OzonErrorRecorder;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogRecordCommand;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozon.posting.mapper.OzonPostingItemMapper;
import com.wimoor.ozon.posting.mapper.OzonPostingMapper;
import com.wimoor.ozon.posting.pojo.dto.OzonPostingSyncCommand;
import com.wimoor.ozon.posting.pojo.entity.OzonPosting;
import com.wimoor.ozon.posting.pojo.entity.OzonPostingItem;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingDetailView;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingSyncResult;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingView;
import com.wimoor.ozon.posting.service.IOzonPostingService;
import com.wimoor.ozon.product.mapper.OzonProductMapMapper;
import com.wimoor.ozon.product.pojo.entity.OzonProductMap;
import com.wimoor.ozon.security.OzonCredentialService;
import com.wimoor.ozon.shipment.mapper.OzonShipmentMapper;
import com.wimoor.ozon.shipment.pojo.entity.OzonShipment;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.mapper.OzonSyncCursorMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;
import com.wimoor.ozon.task.pojo.entity.OzonSyncCursor;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJobType;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class OzonPostingServiceImpl implements IOzonPostingService {

    private static final String FBS = "FBS";
    private static final String BRIDGE_SYNCED = "SYNCED";
    private static final String BRIDGE_UNMAPPED = "UNMAPPED";
    private static final String BRIDGE_EMPTY = "EMPTY";
    private static final String CURSOR_TYPE_POSTING_SYNC = "POSTING_SYNC";
    private static final int DEFAULT_LIMIT = 50;
    private static final int DEFAULT_SINCE_DAYS = 7;
    private static final String POSTING_SYNC_OBJECT_TYPE = "POSTING_SYNC";
    private static final String POSTING_LIST_ENDPOINT = "/v3/posting/fbs/list";

    private final OzonAuthAccessService authAccessService;
    private final OzonPostingMapper postingMapper;
    private final OzonPostingItemMapper postingItemMapper;
    private final OzonProductMapMapper productMapMapper;
    private final OzonShipmentMapper shipmentMapper;
    private final OzonSyncJobMapper syncJobMapper;
    private final OzonSyncCursorMapper syncCursorMapper;
    private final OzonSellerApiClient sellerApiClient;
    private final ErpClientOneFeign erpClientOneFeign;
    private final OzonCredentialService credentialService;
    private final OzonErrorRecorder errorRecorder;
    private final OzonFeatureGate featureGate;
    private IOzonOpsService opsService = new IOzonOpsService() {
    };

    @Autowired
    public OzonPostingServiceImpl(
            OzonAuthAccessService authAccessService,
            OzonPostingMapper postingMapper,
            OzonPostingItemMapper postingItemMapper,
            OzonProductMapMapper productMapMapper,
            OzonShipmentMapper shipmentMapper,
            OzonSyncJobMapper syncJobMapper,
            OzonSyncCursorMapper syncCursorMapper,
            OzonSellerApiClient sellerApiClient,
            ErpClientOneFeign erpClientOneFeign,
            OzonCredentialService credentialService,
            OzonErrorRecorder errorRecorder,
            OzonFeatureGate featureGate
    ) {
        this.authAccessService = authAccessService;
        this.postingMapper = postingMapper;
        this.postingItemMapper = postingItemMapper;
        this.productMapMapper = productMapMapper;
        this.shipmentMapper = shipmentMapper;
        this.syncJobMapper = syncJobMapper;
        this.syncCursorMapper = syncCursorMapper;
        this.sellerApiClient = sellerApiClient;
        this.erpClientOneFeign = erpClientOneFeign;
        this.credentialService = credentialService;
        this.errorRecorder = errorRecorder;
        this.featureGate = featureGate;
    }

    public OzonPostingServiceImpl(
            OzonAuthMapper authMapper,
            OzonPostingMapper postingMapper,
            OzonPostingItemMapper postingItemMapper,
            OzonProductMapMapper productMapMapper,
            OzonShipmentMapper shipmentMapper,
            OzonSyncJobMapper syncJobMapper,
            OzonSyncCursorMapper syncCursorMapper,
            OzonSellerApiClient sellerApiClient,
            ErpClientOneFeign erpClientOneFeign,
            OzonCredentialService credentialService,
            OzonErrorRecorder errorRecorder
    ) {
        this(new OzonAuthAccessService(authMapper), postingMapper, postingItemMapper, productMapMapper, shipmentMapper, syncJobMapper, syncCursorMapper, sellerApiClient,
                erpClientOneFeign, credentialService, errorRecorder, OzonFeatureGate.allEnabled());
    }

    public OzonPostingServiceImpl(
            OzonAuthMapper authMapper,
            OzonPostingMapper postingMapper,
            OzonPostingItemMapper postingItemMapper,
            OzonProductMapMapper productMapMapper,
            OzonShipmentMapper shipmentMapper,
            OzonSyncJobMapper syncJobMapper,
            OzonSyncCursorMapper syncCursorMapper,
            OzonSellerApiClient sellerApiClient,
            ErpClientOneFeign erpClientOneFeign,
            OzonCredentialService credentialService,
            OzonErrorRecorder errorRecorder,
            OzonFeatureGate featureGate
    ) {
        this(new OzonAuthAccessService(authMapper), postingMapper, postingItemMapper, productMapMapper, shipmentMapper, syncJobMapper, syncCursorMapper, sellerApiClient,
                erpClientOneFeign, credentialService, errorRecorder, featureGate);
    }

    @Autowired(required = false)
    public void setOpsService(IOzonOpsService opsService) {
        if (opsService != null) {
            this.opsService = opsService;
        }
    }

    @Override
    public OzonPostingSyncResult syncIncremental(UserInfo user, OzonPostingSyncCommand command) {
        featureGate.assertPostingWriteEnabled();
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command.getAuthId());
        String auditPayload = JSON.toJSONString(command);
        Date now = new Date();
        SyncWindow syncWindow = resolveSyncWindow(auth, command, now);
        OzonSyncJob syncJob = buildSyncJob(auth, user, OzonSyncJobType.POSTING_SYNC,
                buildJobPayload(command, 0, syncWindow), now);
        syncJobMapper.insert(syncJob);
        List<String> erpOrderIds = new ArrayList<>();
        try {
            JSONArray postings = fetchPostings(auth, syncWindow, user);
            Map<String, OzonProductMap> productMap = loadProductMap(auth.getId(), postings);
            syncJob.setPayload(buildJobPayload(command, postings.size(), syncWindow));
            syncJobMapper.updateById(syncJob);
            for (int index = 0; index < postings.size(); index++) {
                JSONObject raw = postings.getJSONObject(index);
                OzonPosting posting = savePosting(auth, raw, now);
                List<OzonPostingItem> items = replaceItems(auth, posting, raw, productMap, now);
                List<String> bridgeIds = bridgeAndSave(auth, user, posting, items, productMap, now);
                erpOrderIds.addAll(bridgeIds);
            }
            saveCursor(auth, syncWindow, now);
            finishSyncJob(syncJob, now);
            recordOperationAudit(
                    auth,
                    user,
                    "POSTING_SYNC",
                    POSTING_SYNC_OBJECT_TYPE,
                    syncJob.getId(),
                    syncWindow.since + " -> " + syncWindow.to,
                    auditPayload,
                    "DONE",
                    "imported " + postings.size()
            );
            OzonPostingSyncResult result = new OzonPostingSyncResult();
            result.setImported(postings.size());
            result.setErpOrderIds(erpOrderIds);
            result.setSyncedAt(now);
            result.setSyncSince(syncWindow.since);
            result.setSyncTo(syncWindow.to);
            result.setCursorUsed(syncWindow.cursorUsed);
            return result;
        } catch (RuntimeException ex) {
            failSyncJob(syncJob, now, ex);
            recordOperationAudit(
                    auth,
                    user,
                    "POSTING_SYNC",
                    POSTING_SYNC_OBJECT_TYPE,
                    syncJob.getId(),
                    syncWindow.since + " -> " + syncWindow.to,
                    auditPayload,
                    "FAILED",
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    public OzonPostingSyncResult retryOne(UserInfo user, String authId, String postingId) {
        featureGate.assertPostingWriteEnabled();
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        OzonPosting posting = requireOwnedPosting(auth, postingId);
        JSONObject auditPayload = new JSONObject();
        auditPayload.put("authId", authId);
        auditPayload.put("postingId", postingId);
        try {
            List<OzonPostingItem> items = loadPostingItems(posting.getId());
            Date now = new Date();
            Map<String, OzonProductMap> productMap = loadProductMapByOfferIds(auth.getId(), collectOfferIds(items));
            refreshMappedItems(items, productMap, now);
            List<String> erpOrderIds = bridgeAndSave(auth, user, posting, items, productMap, now);
            recordOperationAudit(
                    auth,
                    user,
                    "POSTING_RETRY",
                    OzonErrorSourceType.POSTING,
                    posting.getId(),
                    posting.getPostingNumber(),
                    auditPayload.toJSONString(),
                    "DONE",
                    "bridged " + erpOrderIds.size()
            );
            OzonPostingSyncResult result = new OzonPostingSyncResult();
            result.setImported(1);
            result.setErpOrderIds(erpOrderIds);
            result.setSyncedAt(now);
            return result;
        } catch (RuntimeException ex) {
            recordOperationAudit(
                    auth,
                    user,
                    "POSTING_RETRY",
                    OzonErrorSourceType.POSTING,
                    posting.getId(),
                    posting.getPostingNumber(),
                    auditPayload.toJSONString(),
                    "FAILED",
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    public List<OzonPostingView> list(UserInfo user, String authId, String status, String fulfillmentType, String keyword) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        QueryWrapper<OzonPosting> query = new QueryWrapper<OzonPosting>().eq("auth_id", auth.getId());
        if (StrUtil.isNotBlank(status)) {
            query.eq("posting_status", status.trim());
        }
        if (StrUtil.isNotBlank(fulfillmentType)) {
            query.eq("fulfillment_type", fulfillmentType.trim());
        }
        if (StrUtil.isNotBlank(keyword)) {
            query.like("posting_number", keyword.trim());
        }
        query.orderByDesc("order_created_at").last("limit 50");
        List<OzonPosting> postings = postingMapper.selectList(query);
        if (postings == null || postings.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> ids = postings.stream().map(OzonPosting::getId).collect(Collectors.toList());
        List<OzonPostingItem> items = postingItemMapper.selectList(new QueryWrapper<OzonPostingItem>().in("posting_id", ids));
        List<OzonShipment> shipments = shipmentMapper.selectList(new QueryWrapper<OzonShipment>()
                .in("posting_id", ids)
                .orderByDesc("create_time"));
        Map<String, List<OzonPostingItem>> itemMap = new LinkedHashMap<>();
        if (items != null) {
            for (OzonPostingItem item : items) {
                itemMap.computeIfAbsent(item.getPostingId(), key -> new ArrayList<>()).add(item);
            }
        }
        Map<String, OzonShipment> shipmentMap = new LinkedHashMap<>();
        if (shipments != null) {
            for (OzonShipment shipment : shipments) {
                shipmentMap.putIfAbsent(shipment.getPostingId(), shipment);
            }
        }
        List<OzonPostingView> result = new ArrayList<>();
        for (OzonPosting posting : postings) {
            OzonPostingView view = new OzonPostingView();
            view.setId(posting.getId());
            view.setPostingNumber(posting.getPostingNumber());
            view.setFulfillmentType(posting.getFulfillmentType());
            view.setPostingStatus(posting.getPostingStatus());
            view.setSubstatus(posting.getSubstatus());
            view.setWarehouseId(posting.getWarehouseId());
            view.setErpOrderId(posting.getErpOrderId());
            view.setBridgeStatus(posting.getBridgeStatus());
            view.setRawPayloadJson(posting.getCustomerPayloadJson());
            view.setOrderCreatedAt(posting.getOrderCreatedAt());
            view.setShipmentDeadlineAt(posting.getShipmentDeadlineAt());
            view.setSyncVersion(posting.getSyncVersion());
            view.setItemSummary(buildItemSummary(itemMap.get(posting.getId())));
            OzonShipment shipment = shipmentMap.get(posting.getId());
            if (shipment != null) {
                view.setLatestTrackingNumber(shipment.getTrackingNumber());
                view.setLatestDeliveryService(shipment.getDeliveryService());
                view.setLatestShipmentStatus(shipment.getShipmentStatus());
            }
            result.add(view);
        }
        return result;
    }

    @Override
    public OzonPostingDetailView getDetail(UserInfo user, String authId, String postingId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        OzonPosting posting = requireOwnedPosting(auth, postingId);
        List<OzonPostingItem> items = loadPostingItems(posting.getId());
        List<OzonShipment> shipments = shipmentMapper.selectList(new QueryWrapper<OzonShipment>()
                .eq("posting_id", posting.getId())
                .orderByDesc("create_time"));
        OzonPostingDetailView detail = new OzonPostingDetailView();
        detail.setId(posting.getId());
        detail.setAuthId(posting.getAuthId());
        detail.setPostingNumber(posting.getPostingNumber());
        detail.setFulfillmentType(posting.getFulfillmentType());
        detail.setPostingStatus(posting.getPostingStatus());
        detail.setSubstatus(posting.getSubstatus());
        detail.setWarehouseId(posting.getWarehouseId());
        detail.setErpOrderId(posting.getErpOrderId());
        detail.setBridgeStatus(posting.getBridgeStatus());
        detail.setSyncVersion(posting.getSyncVersion());
        detail.setOrderCreatedAt(posting.getOrderCreatedAt());
        detail.setShipmentDeadlineAt(posting.getShipmentDeadlineAt());
        detail.setRawPayloadJson(posting.getCustomerPayloadJson());
        for (OzonPostingItem item : items) {
            OzonPostingDetailView.ItemView view = new OzonPostingDetailView.ItemView();
            view.setItemId(item.getId());
            view.setMaterialSku(item.getMaterialSku());
            view.setOzonOfferId(item.getOzonOfferId());
            view.setQuantity(item.getQuantity());
            detail.getItems().add(view);
        }
        for (OzonShipment shipment : shipments == null ? Collections.<OzonShipment>emptyList() : shipments) {
            OzonPostingDetailView.ShipmentView view = new OzonPostingDetailView.ShipmentView();
            view.setShipmentId(shipment.getId());
            view.setTrackingNumber(shipment.getTrackingNumber());
            view.setDeliveryService(shipment.getDeliveryService());
            view.setShipmentStatus(shipment.getShipmentStatus());
            view.setCreatedAt(shipment.getCreateTime());
            detail.getShipments().add(view);
        }
        return detail;
    }

    private OzonPosting requireOwnedPosting(OzonAuth auth, String postingId) {
        if (StrUtil.isBlank(postingId)) {
            throw new IllegalArgumentException("postingId不能为空");
        }
        OzonPosting posting = postingMapper.selectById(postingId.trim());
        if (posting == null || !auth.getId().equals(posting.getAuthId())) {
            throw new IllegalArgumentException("Ozon posting不存在");
        }
        return posting;
    }

    private List<OzonPostingItem> loadPostingItems(String postingId) {
        List<OzonPostingItem> items = postingItemMapper.selectList(new QueryWrapper<OzonPostingItem>().eq("posting_id", postingId));
        return items == null ? Collections.emptyList() : items;
    }

    private JSONArray fetchPostings(OzonAuth auth, SyncWindow syncWindow, UserInfo user) {
        String apiKey = credentialService.decrypt(auth.getApiKeyCiphertext());
        JSONArray postings = new JSONArray();
        int offset = 0;
        while (true) {
            String requestPayload = buildPayload(syncWindow.since, syncWindow.to, offset);
            long startedAt = System.currentTimeMillis();
            String responsePayload = null;
            JSONObject result;
            try {
                responsePayload = sellerApiClient.listFbsPostings(auth.getClientId(), apiKey, requestPayload);
                recordApiLog(auth, user, requestPayload, responsePayload, "DONE", null, startedAt);
                result = parsePostingResult(responsePayload);
            } catch (RuntimeException ex) {
                recordApiLog(auth, user, requestPayload, responsePayload, "FAILED", ex.getMessage(), startedAt);
                throw ex;
            }
            JSONArray page = result == null ? null : result.getJSONArray("postings");
            if (page != null && !page.isEmpty()) {
                postings.addAll(page);
            }
            Boolean hasNext = result == null ? null : result.getBoolean("has_next");
            if (page == null || page.isEmpty() || !Boolean.TRUE.equals(hasNext)) {
                return postings;
            }
            offset += DEFAULT_LIMIT;
        }
    }

    private JSONObject parsePostingResult(String response) {
        JSONObject payload = StrUtil.isBlank(response) ? null : JSONObject.parseObject(response);
        return payload == null ? null : payload.getJSONObject("result");
    }

    private String buildPayload(String since, String to, int offset) {
        JSONObject filter = new JSONObject();
        filter.put("since", since);
        filter.put("to", to);
        JSONObject with = new JSONObject();
        with.put("analytics_data", false);
        with.put("financial_data", false);
        JSONObject payload = new JSONObject();
        payload.put("dir", "ASC");
        payload.put("filter", filter);
        payload.put("limit", DEFAULT_LIMIT);
        payload.put("offset", offset);
        payload.put("with", with);
        return payload.toJSONString();
    }

    private OzonSyncJob buildSyncJob(OzonAuth auth, UserInfo user, OzonSyncJobType jobType, String payload, Date now) {
        OzonSyncJob job = new OzonSyncJob();
        job.setId(nextId());
        job.setAuthId(auth.getId());
        job.setShopId(auth.getShopId());
        job.setJobType(jobType.name());
        job.setStatus("RUNNING");
        job.setPayload(payload);
        job.setOperator(user != null ? user.getId() : null);
        job.setCreateTime(now);
        job.setUpdateTime(now);
        return job;
    }

    private String buildJobPayload(OzonPostingSyncCommand command, int imported, SyncWindow syncWindow) {
        JSONObject payload = new JSONObject();
        payload.put("sinceDays", command == null ? DEFAULT_SINCE_DAYS : normalizeSinceDays(command.getSinceDays()));
        payload.put("useCursor", command != null && Boolean.TRUE.equals(command.getUseCursor()));
        payload.put("syncSince", syncWindow.since);
        payload.put("syncTo", syncWindow.to);
        payload.put("imported", imported);
        return payload.toJSONString();
    }

    private void finishSyncJob(OzonSyncJob syncJob, Date now) {
        syncJob.setStatus("DONE");
        syncJob.setUpdateTime(now);
        syncJobMapper.updateById(syncJob);
    }

    private void failSyncJob(OzonSyncJob syncJob, Date now, RuntimeException ex) {
        syncJob.setStatus("FAILED");
        syncJob.setPayload(ex.getMessage());
        syncJob.setUpdateTime(now);
        syncJobMapper.updateById(syncJob);
    }

    private int normalizeSinceDays(Integer sinceDays) {
        return sinceDays == null || sinceDays <= 0 ? DEFAULT_SINCE_DAYS : sinceDays;
    }

    private SyncWindow resolveSyncWindow(OzonAuth auth, OzonPostingSyncCommand command, Date now) {
        OffsetDateTime to = OffsetDateTime.ofInstant(now.toInstant(), ZoneOffset.UTC);
        if (command != null && Boolean.TRUE.equals(command.getUseCursor())) {
            OzonSyncCursor cursor = syncCursorMapper.selectOne(new QueryWrapper<OzonSyncCursor>()
                    .eq("auth_id", auth.getId())
                    .eq("cursor_type", CURSOR_TYPE_POSTING_SYNC)
                    .last("limit 1"));
            if (cursor != null && StrUtil.isNotBlank(cursor.getCursorValue())) {
                return new SyncWindow(cursor.getCursorValue(), to.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), true);
            }
        }
        int sinceDays = command == null ? DEFAULT_SINCE_DAYS : normalizeSinceDays(command.getSinceDays());
        return new SyncWindow(
                to.minusDays(sinceDays).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                to.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                false
        );
    }

    private void saveCursor(OzonAuth auth, SyncWindow syncWindow, Date now) {
        OzonSyncCursor cursor = syncCursorMapper.selectOne(new QueryWrapper<OzonSyncCursor>()
                .eq("auth_id", auth.getId())
                .eq("cursor_type", CURSOR_TYPE_POSTING_SYNC)
                .last("limit 1"));
        if (cursor == null) {
            cursor = new OzonSyncCursor();
            cursor.setId(nextId());
            cursor.setAuthId(auth.getId());
            cursor.setShopId(auth.getShopId());
            cursor.setCursorType(CURSOR_TYPE_POSTING_SYNC);
            cursor.setCreateTime(now);
        }
        cursor.setCursorValue(syncWindow.to);
        cursor.setLastSyncedAt(now);
        cursor.setUpdateTime(now);
        if (syncCursorMapper.selectById(cursor.getId()) == null) {
            syncCursorMapper.insert(cursor);
        } else {
            syncCursorMapper.updateById(cursor);
        }
    }

    private OzonPosting savePosting(OzonAuth auth, JSONObject raw, Date now) {
        String postingNumber = trim(raw.getString("posting_number"));
        if (postingNumber == null) {
            throw new IllegalArgumentException("Ozon posting_number不能为空");
        }
        OzonPosting posting = postingMapper.selectOne(new QueryWrapper<OzonPosting>()
                .eq("auth_id", auth.getId())
                .eq("posting_number", postingNumber));
        boolean isNew = posting == null;
        if (isNew) {
            posting = new OzonPosting();
            posting.setId(nextId());
            posting.setAuthId(auth.getId());
            posting.setShopId(auth.getShopId());
            posting.setCreateTime(now);
            posting.setSyncVersion(1);
        } else {
            posting.setSyncVersion(posting.getSyncVersion() == null ? 1 : posting.getSyncVersion() + 1);
        }
        posting.setPostingNumber(postingNumber);
        posting.setFulfillmentType(FBS);
        posting.setPostingStatus(trim(raw.getString("status")));
        posting.setSubstatus(trim(raw.getString("substatus")));
        posting.setWarehouseId(trim(raw.getString("warehouse_id")));
        posting.setOrderCreatedAt(parseDate(raw.getString("order_created_at"), raw.getString("in_process_at")));
        posting.setShipmentDeadlineAt(parseDate(raw.getString("shipment_date"), raw.getString("shipment_deadline_at")));
        posting.setCustomerPayloadJson(raw.toJSONString());
        posting.setUpdateTime(now);
        if (isNew) {
            postingMapper.insert(posting);
        } else {
            postingMapper.updateById(posting);
        }
        return posting;
    }

    private List<OzonPostingItem> replaceItems(
            OzonAuth auth,
            OzonPosting posting,
            JSONObject raw,
            Map<String, OzonProductMap> productMap,
            Date now
    ) {
        postingItemMapper.delete(new QueryWrapper<OzonPostingItem>().eq("posting_id", posting.getId()));
        JSONArray products = raw.getJSONArray("products");
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        List<OzonPostingItem> result = new ArrayList<>();
        for (int index = 0; index < products.size(); index++) {
            JSONObject product = products.getJSONObject(index);
            Integer quantity = product.getInteger("quantity");
            if (quantity == null || quantity <= 0) {
                continue;
            }
            String offerId = trim(product.getString("offer_id"));
            OzonProductMap mapping = offerId == null ? null : productMap.get(offerId);
            OzonPostingItem item = new OzonPostingItem();
            item.setId(nextId());
            item.setPostingId(posting.getId());
            item.setAuthId(auth.getId());
            item.setShopId(auth.getShopId());
            item.setPostingNumber(posting.getPostingNumber());
            item.setMaterialSku(mapping == null ? null : mapping.getMaterialSku());
            item.setOzonOfferId(offerId);
            item.setQuantity(quantity);
            item.setCreateTime(now);
            item.setUpdateTime(now);
            postingItemMapper.insert(item);
            result.add(item);
        }
        return result;
    }

    private List<String> bridgeToErp(
            OzonAuth auth,
            OzonPosting posting,
            List<OzonPostingItem> items,
            Map<String, OzonProductMap> productMap
    ) {
        List<String> result = new ArrayList<>();
        for (OzonPostingItem item : items) {
            if (StrUtil.isBlank(item.getMaterialSku())) {
                continue;
            }
            OzonProductMap mapping = productMap.get(item.getOzonOfferId());
            Result<OzonErpOrderUpsertResult> bridgeResult = erpClientOneFeign.upsertOzonOrder(new OzonErpOrderUpsertCommand(
                    auth.getShopId(),
                    posting.getPostingNumber(),
                    item.getMaterialSku(),
                    posting.getWarehouseId(),
                    posting.getWarehouseId(),
                    "RU",
                    "RUB",
                    item.getQuantity(),
                    mapping == null ? null : mapping.getMaterialPrice(),
                    posting.getOrderCreatedAt()
            ));
            if (Result.isSuccess(bridgeResult) && bridgeResult.getData() != null && StrUtil.isNotBlank(bridgeResult.getData().getErpOrderId())) {
                result.add(bridgeResult.getData().getErpOrderId());
            }
        }
        return result;
    }

    private List<String> bridgeAndSave(
            OzonAuth auth,
            UserInfo user,
            OzonPosting posting,
            List<OzonPostingItem> items,
            Map<String, OzonProductMap> productMap,
            Date now
    ) {
        try {
            List<String> bridgeIds = bridgeToErp(auth, posting, items, productMap);
            String bridgeStatus = resolveBridgeStatus(items, bridgeIds);
            posting.setBridgeStatus(bridgeStatus);
            posting.setErpOrderId(bridgeIds.isEmpty() ? null : String.join(",", bridgeIds));
            posting.setUpdateTime(now);
            postingMapper.updateById(posting);
            syncPostingError(auth, user, posting, items, bridgeIds, bridgeStatus, null);
            return bridgeIds;
        } catch (RuntimeException ex) {
            syncPostingError(auth, user, posting, items, Collections.emptyList(), BRIDGE_UNMAPPED, ex.getMessage());
            throw ex;
        }
    }

    private void syncPostingError(
            OzonAuth auth,
            UserInfo user,
            OzonPosting posting,
            List<OzonPostingItem> items,
            List<String> bridgeIds,
            String bridgeStatus,
            String errorMessage
    ) {
        if (!BRIDGE_UNMAPPED.equals(bridgeStatus)) {
            errorRecorder.markResolved(auth.getId(), OzonErrorSourceType.POSTING, posting.getId());
            return;
        }
        errorRecorder.recordOpen(new OzonErrorRecordCommand(
                auth.getId(),
                auth.getShopId(),
                OzonErrorSourceType.POSTING,
                posting.getId(),
                posting.getPostingNumber(),
                StrUtil.blankToDefault(trim(errorMessage), "Ozon订单桥接ERP失败，存在未映射商品或ERP未返回订单号"),
                posting.getCustomerPayloadJson(),
                buildBridgeErrorPayload(items, bridgeIds, bridgeStatus, errorMessage),
                user == null ? null : user.getId()
        ));
    }

    private String buildBridgeErrorPayload(List<OzonPostingItem> items, List<String> bridgeIds, String bridgeStatus, String errorMessage) {
        JSONObject payload = new JSONObject();
        payload.put("bridgeStatus", bridgeStatus);
        payload.put("erpOrderIds", bridgeIds);
        payload.put("itemSummary", buildItemSummary(items));
        if (StrUtil.isNotBlank(errorMessage)) {
            payload.put("errorMessage", errorMessage);
        }
        return payload.toJSONString();
    }

    private Map<String, OzonProductMap> loadProductMap(String authId, JSONArray postings) {
        return loadProductMapByOfferIds(authId, collectOfferIds(postings));
    }

    private Map<String, OzonProductMap> loadProductMapByOfferIds(String authId, List<String> offerIds) {
        if (offerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OzonProductMap> mappings = productMapMapper.selectList(new QueryWrapper<OzonProductMap>()
                .eq("auth_id", authId)
                .in("ozon_offer_id", offerIds));
        Map<String, OzonProductMap> result = new LinkedHashMap<>();
        if (mappings != null) {
            for (OzonProductMap item : mappings) {
                if (StrUtil.isNotBlank(item.getOzonOfferId())) {
                    result.put(item.getOzonOfferId(), item);
                }
            }
        }
        return result;
    }

    private List<String> collectOfferIds(JSONArray postings) {
        List<String> offerIds = new ArrayList<>();
        for (int index = 0; index < postings.size(); index++) {
            JSONArray products = postings.getJSONObject(index).getJSONArray("products");
            if (products == null) {
                continue;
            }
            for (int itemIndex = 0; itemIndex < products.size(); itemIndex++) {
                String offerId = trim(products.getJSONObject(itemIndex).getString("offer_id"));
                if (offerId != null) {
                    offerIds.add(offerId);
                }
            }
        }
        return offerIds;
    }

    private List<String> collectOfferIds(List<OzonPostingItem> items) {
        List<String> offerIds = new ArrayList<>();
        if (items == null) {
            return offerIds;
        }
        for (OzonPostingItem item : items) {
            String offerId = trim(item.getOzonOfferId());
            if (offerId != null) {
                offerIds.add(offerId);
            }
        }
        return offerIds;
    }

    private void refreshMappedItems(List<OzonPostingItem> items, Map<String, OzonProductMap> productMap, Date now) {
        for (OzonPostingItem item : items) {
            OzonProductMap mapping = productMap.get(item.getOzonOfferId());
            String materialSku = mapping == null ? null : trim(mapping.getMaterialSku());
            if (!StrUtil.equals(trim(item.getMaterialSku()), materialSku)) {
                item.setMaterialSku(materialSku);
                item.setUpdateTime(now);
                postingItemMapper.updateById(item);
            }
        }
    }

    private String resolveBridgeStatus(List<OzonPostingItem> items, List<String> erpOrderIds) {
        if (items == null || items.isEmpty()) {
            return BRIDGE_EMPTY;
        }
        return erpOrderIds.isEmpty() ? BRIDGE_UNMAPPED : BRIDGE_SYNCED;
    }

    private String buildItemSummary(List<OzonPostingItem> items) {
        if (items == null || items.isEmpty()) {
            return "-";
        }
        return items.stream()
                .map(item -> (StrUtil.isBlank(item.getMaterialSku()) ? item.getOzonOfferId() : item.getMaterialSku()) + " x" + item.getQuantity())
                .collect(Collectors.joining(", "));
    }

    private Date parseDate(String... values) {
        for (String value : values) {
            if (StrUtil.isBlank(value)) {
                continue;
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
        return null;
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
            String requestPayload,
            String responsePayload,
            String status,
            String errorMessage,
            long startedAt
    ) {
        opsService.recordApiLog(new OzonApiLogRecordCommand(
                auth.getId(),
                auth.getShopId(),
                "POSTING",
                "LIST_FBS_POSTINGS",
                POSTING_LIST_ENDPOINT,
                "POST",
                POSTING_SYNC_OBJECT_TYPE,
                auth.getId(),
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
            String operationType,
            String objectType,
            String objectId,
            String objectCode,
            String requestPayload,
            String resultStatus,
            String resultMessage
    ) {
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                auth.getId(),
                auth.getShopId(),
                operationType,
                objectType,
                objectId,
                objectCode,
                requestPayload,
                resultStatus,
                resultMessage,
                user == null ? null : user.getId()
        ));
    }

    private static class SyncWindow {
        private final String since;
        private final String to;
        private final boolean cursorUsed;

        private SyncWindow(String since, String to, boolean cursorUsed) {
            this.since = since;
            this.to = to;
            this.cursorUsed = cursorUsed;
        }
    }
}
