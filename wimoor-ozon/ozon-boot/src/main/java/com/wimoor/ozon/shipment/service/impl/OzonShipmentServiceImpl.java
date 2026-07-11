package com.wimoor.ozon.shipment.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
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
import com.wimoor.ozon.posting.mapper.OzonPostingMapper;
import com.wimoor.ozon.posting.pojo.entity.OzonPosting;
import com.wimoor.ozon.security.OzonCredentialService;
import com.wimoor.ozon.shipment.mapper.OzonShipmentMapper;
import com.wimoor.ozon.shipment.pojo.dto.OzonShipmentPushCommand;
import com.wimoor.ozon.shipment.pojo.entity.OzonShipment;
import com.wimoor.ozon.shipment.pojo.vo.OzonShipmentPushResult;
import com.wimoor.ozon.shipment.service.IOzonShipmentService;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJobType;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class OzonShipmentServiceImpl implements IOzonShipmentService {

    private static final String FBS = "FBS";
    private static final String TRACKING_SET = "TRACKING_SET";
    private static final String FAILED = "FAILED";
    private static final String API_GROUP = "SHIPMENT";
    private static final String SHIPMENT_ENDPOINT = "/v2/fbs/posting/tracking-number/set";

    private final OzonAuthAccessService authAccessService;
    private final OzonPostingMapper postingMapper;
    private final OzonShipmentMapper shipmentMapper;
    private final OzonSellerApiClient sellerApiClient;
    private final OzonSyncJobMapper syncJobMapper;
    private final OzonCredentialService credentialService;
    private final OzonErrorRecorder errorRecorder;
    private final OzonFeatureGate featureGate;
    private IOzonOpsService opsService = new IOzonOpsService() {
    };

    @Autowired
    public OzonShipmentServiceImpl(
            OzonAuthAccessService authAccessService,
            OzonPostingMapper postingMapper,
            OzonShipmentMapper shipmentMapper,
            OzonSellerApiClient sellerApiClient,
            OzonSyncJobMapper syncJobMapper,
            OzonCredentialService credentialService,
            OzonErrorRecorder errorRecorder,
            OzonFeatureGate featureGate
    ) {
        this.authAccessService = authAccessService;
        this.postingMapper = postingMapper;
        this.shipmentMapper = shipmentMapper;
        this.sellerApiClient = sellerApiClient;
        this.syncJobMapper = syncJobMapper;
        this.credentialService = credentialService;
        this.errorRecorder = errorRecorder;
        this.featureGate = featureGate;
    }

    public OzonShipmentServiceImpl(
            OzonAuthMapper authMapper,
            OzonPostingMapper postingMapper,
            OzonShipmentMapper shipmentMapper,
            OzonSellerApiClient sellerApiClient,
            OzonSyncJobMapper syncJobMapper,
            OzonCredentialService credentialService,
            OzonErrorRecorder errorRecorder
    ) {
        this(new OzonAuthAccessService(authMapper), postingMapper, shipmentMapper, sellerApiClient, syncJobMapper, credentialService, errorRecorder,
                OzonFeatureGate.allEnabled());
    }

    public OzonShipmentServiceImpl(
            OzonAuthMapper authMapper,
            OzonPostingMapper postingMapper,
            OzonShipmentMapper shipmentMapper,
            OzonSellerApiClient sellerApiClient,
            OzonSyncJobMapper syncJobMapper,
            OzonCredentialService credentialService,
            OzonErrorRecorder errorRecorder,
            OzonFeatureGate featureGate
    ) {
        this(new OzonAuthAccessService(authMapper), postingMapper, shipmentMapper, sellerApiClient, syncJobMapper, credentialService, errorRecorder,
                featureGate);
    }

    @Autowired(required = false)
    public void setOpsService(IOzonOpsService opsService) {
        if (opsService != null) {
            this.opsService = opsService;
        }
    }

    @Override
    public OzonShipmentPushResult pushTracking(UserInfo user, OzonShipmentPushCommand command) {
        featureGate.assertPostingWriteEnabled();
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command.getAuthId());
        OzonPosting posting = requireOwnedPosting(auth, command.getPostingId());
        String auditPayload = JSON.toJSONString(command);
        String trackingNumber = requireText(command.getTrackingNumber(), "trackingNumber不能为空");
        String deliveryService = trim(command.getDeliveryService());
        if (!FBS.equalsIgnoreCase(trim(posting.getFulfillmentType()))) {
            throw new IllegalArgumentException("仅支持FBS订单推送追踪号");
        }
        Date now = new Date();
        OzonSyncJob syncJob = buildSyncJob(auth, user, posting.getId(), trackingNumber, now);
        syncJobMapper.insert(syncJob);
        // Keep the request narrow: posting number + tracking + carrier only.
        JSONObject payload = new JSONObject();
        payload.put("posting_number", posting.getPostingNumber());
        payload.put("tracking_number", trackingNumber);
        if (StrUtil.isNotBlank(deliveryService)) {
            payload.put("delivery_service", deliveryService);
        }
        String retryPayload = buildRetryPayload(command, trackingNumber, deliveryService);
        long startedAt = System.currentTimeMillis();
        try {
            String response = sellerApiClient.setTrackingNumber(
                    auth.getClientId(),
                    credentialService.decrypt(auth.getApiKeyCiphertext()),
                    payload.toJSONString()
            );
            recordApiLog(auth, user, posting, payload.toJSONString(), response, TRACKING_SET, null, startedAt);
            OzonShipment shipment = new OzonShipment();
            shipment.setId(nextId());
            shipment.setAuthId(auth.getId());
            shipment.setShopId(auth.getShopId());
            shipment.setPostingId(posting.getId());
            shipment.setPostingNumber(posting.getPostingNumber());
            shipment.setTrackingNumber(trackingNumber);
            shipment.setDeliveryService(deliveryService);
            shipment.setShipmentStatus(TRACKING_SET);
            shipment.setRequestPayloadJson(payload.toJSONString());
            shipment.setResponsePayloadJson(response);
            shipment.setOperator(user == null ? null : user.getId());
            shipment.setCreateTime(now);
            shipment.setUpdateTime(now);
            shipmentMapper.insert(shipment);
            posting.setUpdateTime(now);
            postingMapper.updateById(posting);
            errorRecorder.markResolved(auth.getId(), OzonErrorSourceType.SHIPMENT, posting.getId());
            finishSyncJob(syncJob, now);
            recordOperationAudit(auth, user, posting, auditPayload, TRACKING_SET, "tracking pushed");
            OzonShipmentPushResult result = new OzonShipmentPushResult();
            result.setShipmentId(shipment.getId());
            result.setPostingNumber(posting.getPostingNumber());
            result.setTrackingNumber(trackingNumber);
            result.setShipmentStatus(TRACKING_SET);
            result.setPushedAt(now);
            return result;
        } catch (RuntimeException ex) {
            recordApiLog(auth, user, posting, payload.toJSONString(), null, FAILED, ex.getMessage(), startedAt);
            failSyncJob(syncJob, now, ex);
            errorRecorder.recordOpen(new OzonErrorRecordCommand(
                    auth.getId(),
                    auth.getShopId(),
                    OzonErrorSourceType.SHIPMENT,
                    posting.getId(),
                    posting.getPostingNumber(),
                    ex.getMessage(),
                    retryPayload,
                    payload.toJSONString(),
                    user == null ? null : user.getId()
            ));
            recordOperationAudit(auth, user, posting, auditPayload, FAILED, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public List<OzonShipment> listByPosting(UserInfo user, String authId, String postingId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        OzonPosting posting = requireOwnedPosting(auth, postingId);
        List<OzonShipment> shipments = shipmentMapper.selectList(new QueryWrapper<OzonShipment>()
                .eq("posting_id", posting.getId())
                .orderByDesc("create_time"));
        return shipments == null ? Collections.emptyList() : shipments;
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

    private String requireText(String value, String message) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String trim(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private String buildRetryPayload(OzonShipmentPushCommand command, String trackingNumber, String deliveryService) {
        JSONObject payload = new JSONObject();
        payload.put("authId", command.getAuthId());
        payload.put("postingId", command.getPostingId());
        payload.put("trackingNumber", trackingNumber);
        if (StrUtil.isNotBlank(deliveryService)) {
            payload.put("deliveryService", deliveryService);
        }
        return payload.toJSONString();
    }

    private OzonSyncJob buildSyncJob(OzonAuth auth, UserInfo user, String postingId, String trackingNumber, Date now) {
        JSONObject payload = new JSONObject();
        payload.put("postingId", postingId);
        payload.put("trackingNumber", trackingNumber);
        OzonSyncJob job = new OzonSyncJob();
        job.setId(nextId());
        job.setAuthId(auth.getId());
        job.setShopId(auth.getShopId());
        job.setJobType(OzonSyncJobType.TRACKING_PUSH.name());
        job.setStatus("RUNNING");
        job.setPayload(payload.toJSONString());
        job.setOperator(user == null ? null : user.getId());
        job.setCreateTime(now);
        job.setUpdateTime(now);
        return job;
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

    private String nextId() {
        return IdUtil.getSnowflakeNextIdStr();
    }

    private void recordApiLog(
            OzonAuth auth,
            UserInfo user,
            OzonPosting posting,
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
                "SET_TRACKING_NUMBER",
                SHIPMENT_ENDPOINT,
                "POST",
                OzonErrorSourceType.SHIPMENT,
                posting.getId(),
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
            OzonPosting posting,
            String requestPayload,
            String resultStatus,
            String resultMessage
    ) {
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                auth.getId(),
                auth.getShopId(),
                "SHIPMENT_PUSH_TRACKING",
                OzonErrorSourceType.SHIPMENT,
                posting.getId(),
                posting.getPostingNumber(),
                requestPayload,
                resultStatus,
                resultMessage,
                user == null ? null : user.getId()
        ));
    }
}
