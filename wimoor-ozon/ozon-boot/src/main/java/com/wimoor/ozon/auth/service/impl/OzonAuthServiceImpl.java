package com.wimoor.ozon.auth.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.dto.OzonAuthBindCommand;
import com.wimoor.ozon.auth.pojo.dto.OzonRotateKeyCommand;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.pojo.vo.OzonAuthView;
import com.wimoor.ozon.auth.service.IOzonAuthService;
import com.wimoor.ozon.client.OzonConnectionStatus;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogRecordCommand;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozon.security.OzonCredentialService;
import com.wimoor.ozon.seller.mapper.OzonShopConfigMapper;
import com.wimoor.ozon.seller.pojo.entity.OzonShopConfig;
import com.wimoor.ozon.seller.pojo.vo.OzonWarehouseSyncResult;
import com.wimoor.ozon.seller.service.IOzonWarehouseSyncService;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJobType;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class OzonAuthServiceImpl implements IOzonAuthService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DISABLED = "DISABLED";
    private static final String PENDING = "PENDING";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";
    private static final String AUTH_OBJECT_TYPE = "AUTH";
    private static final String SELLER_API_GROUP = "SELLER";
    private static final String WAREHOUSE_LIST_ENDPOINT = "/v1/warehouse/list";

    private final OzonAuthMapper authMapper;
    private final OzonShopConfigMapper shopConfigMapper;
    private final OzonSyncJobMapper syncJobMapper;
    private final OzonSellerApiClient sellerApiClient;
    private final OzonCredentialService credentialService;
    private final IOzonWarehouseSyncService warehouseSyncService;
    private IOzonOpsService opsService = new IOzonOpsService() {
    };

    @Autowired
    public OzonAuthServiceImpl(
            OzonAuthMapper authMapper,
            OzonShopConfigMapper shopConfigMapper,
            OzonSyncJobMapper syncJobMapper,
            OzonSellerApiClient sellerApiClient,
            OzonCredentialService credentialService,
            IOzonWarehouseSyncService warehouseSyncService
    ) {
        this.authMapper = authMapper;
        this.shopConfigMapper = shopConfigMapper;
        this.syncJobMapper = syncJobMapper;
        this.sellerApiClient = sellerApiClient;
        this.credentialService = credentialService;
        this.warehouseSyncService = warehouseSyncService;
    }

    public OzonAuthServiceImpl(
            OzonAuthMapper authMapper,
            OzonShopConfigMapper shopConfigMapper,
            OzonSyncJobMapper syncJobMapper,
            OzonSellerApiClient sellerApiClient,
            OzonCredentialService credentialService
    ) {
        this(authMapper, shopConfigMapper, syncJobMapper, sellerApiClient, credentialService, null);
    }

    @Autowired(required = false)
    public void setOpsService(IOzonOpsService opsService) {
        if (opsService != null) {
            this.opsService = opsService;
        }
    }

    @Override
    public OzonAuth bindAuth(UserInfo user, OzonAuthBindCommand command) {
        String shopId = requireShopId(user);
        String clientId = requireText(command.getClientId(), "Client ID不能为空");
        String apiKey = requireText(command.getApiKey(), "API Key不能为空");
        String requestPayload = buildAuthRequestPayload(command.getName(), clientId);
        long startedAt = System.currentTimeMillis();
        OzonConnectionStatus connectionStatus = sellerApiClient.ping(clientId, apiKey);
        recordApiLog(
                null,
                shopId,
                "PING_AUTH",
                null,
                requestPayload,
                toConnectionPayload(connectionStatus),
                connectionStatus.isSuccess() ? SUCCESS : FAILED,
                connectionStatus.isSuccess() ? null : connectionStatus.getMessage(),
                startedAt,
                user == null ? null : user.getId()
        );
        if (!connectionStatus.isSuccess()) {
            recordOperationAudit(
                    null,
                    shopId,
                    "AUTH_BIND",
                    null,
                    resolveAuthName(command.getName(), clientId),
                    requestPayload,
                    FAILED,
                    connectionStatus.getMessage(),
                    user
            );
            throw new IllegalStateException(connectionStatus.getMessage());
        }
        Date now = new Date();
        OzonAuth auth = new OzonAuth();
        auth.setId(nextId());
        auth.setShopId(shopId);
        auth.setName(resolveAuthName(command.getName(), clientId));
        auth.setClientId(clientId);
        auth.setApiKeyCiphertext(credentialService.encrypt(apiKey));
        auth.setApiKeyFingerprint(credentialService.fingerprint(apiKey));
        auth.setStatus(ACTIVE);
        auth.setDisabled(Boolean.FALSE);
        auth.setLastSyncStatus(SUCCESS);
        auth.setLastSyncMessage(connectionStatus.getMessage());
        auth.setCreatedBy(user != null ? user.getId() : null);
        auth.setUpdatedBy(user != null ? user.getId() : null);
        auth.setCreateTime(now);
        auth.setUpdateTime(now);
        authMapper.insert(auth);

        OzonShopConfig shopConfig = new OzonShopConfig();
        shopConfig.setId(nextId());
        shopConfig.setShopId(shopId);
        shopConfig.setAuthId(auth.getId());
        shopConfig.setShopName(auth.getName());
        shopConfig.setSellerCode(clientId);
        shopConfig.setStatus(ACTIVE);
        shopConfig.setCreateTime(now);
        shopConfig.setUpdateTime(now);
        shopConfigMapper.insert(shopConfig);

        queueInitJob(auth, user, OzonSyncJobType.INIT_SELLER, now);
        queueInitJob(auth, user, OzonSyncJobType.INIT_WAREHOUSE, now);
        auth.setApiKeyPlaintext(null);
        recordOperationAudit(
                auth.getId(),
                shopId,
                "AUTH_BIND",
                auth.getId(),
                auth.getName(),
                requestPayload,
                SUCCESS,
                connectionStatus.getMessage(),
                user
        );
        return auth;
    }

    @Override
    public List<OzonAuthView> listAuth(UserInfo user) {
        String shopId = requireShopId(user);
        List<OzonAuth> authList = authMapper.listByShopId(shopId);
        if (authList == null || authList.isEmpty()) {
            return Collections.emptyList();
        }
        return authList.stream().map(this::toView).collect(Collectors.toList());
    }

    @Override
    public OzonWarehouseSyncResult ping(UserInfo user, String authId) {
        OzonAuth auth = validateOwnedAuth(user, authId);
        if (warehouseSyncService == null) {
            throw new IllegalStateException("Warehouse sync service is unavailable");
        }
        JSONObject payload = new JSONObject();
        payload.put("authId", auth.getId());
        try {
            OzonWarehouseSyncResult result = warehouseSyncService.syncWarehouses(authId);
            recordOperationAudit(
                    auth.getId(),
                    auth.getShopId(),
                    "AUTH_PING",
                    auth.getId(),
                    auth.getName(),
                    payload.toJSONString(),
                    SUCCESS,
                    result == null ? "warehouse synced" : result.getMessage(),
                    user
            );
            return result;
        } catch (RuntimeException ex) {
            recordOperationAudit(
                    auth.getId(),
                    auth.getShopId(),
                    "AUTH_PING",
                    auth.getId(),
                    auth.getName(),
                    payload.toJSONString(),
                    FAILED,
                    ex.getMessage(),
                    user
            );
            throw ex;
        }
    }

    @Override
    public void disableAuth(UserInfo user, String authId) {
        OzonAuth auth = validateOwnedAuth(user, authId);
        JSONObject payload = new JSONObject();
        payload.put("authId", auth.getId());
        try {
            auth.setDisabled(Boolean.TRUE);
            auth.setStatus(DISABLED);
            auth.setUpdatedBy(user != null ? user.getId() : null);
            auth.setUpdateTime(new Date());
            authMapper.updateById(auth);
            shopConfigMapper.disableByAuthId(authId);
            recordOperationAudit(
                    auth.getId(),
                    auth.getShopId(),
                    "AUTH_DISABLE",
                    auth.getId(),
                    auth.getName(),
                    payload.toJSONString(),
                    SUCCESS,
                    "disabled",
                    user
            );
        } catch (RuntimeException ex) {
            recordOperationAudit(
                    auth.getId(),
                    auth.getShopId(),
                    "AUTH_DISABLE",
                    auth.getId(),
                    auth.getName(),
                    payload.toJSONString(),
                    FAILED,
                    ex.getMessage(),
                    user
            );
            throw ex;
        }
    }

    @Override
    public OzonAuth rotateKey(UserInfo user, OzonRotateKeyCommand command) {
        String apiKey = requireText(command.getApiKey(), "API Key不能为空");
        OzonAuth auth = validateOwnedAuth(user, command.getAuthId());
        String requestPayload = buildRotateKeyPayload(auth.getId(), auth.getClientId());
        long startedAt = System.currentTimeMillis();
        OzonConnectionStatus connectionStatus = sellerApiClient.ping(auth.getClientId(), apiKey);
        recordApiLog(
                auth.getId(),
                auth.getShopId(),
                "PING_AUTH",
                auth.getId(),
                requestPayload,
                toConnectionPayload(connectionStatus),
                connectionStatus.isSuccess() ? SUCCESS : FAILED,
                connectionStatus.isSuccess() ? null : connectionStatus.getMessage(),
                startedAt,
                user == null ? null : user.getId()
        );
        if (!connectionStatus.isSuccess()) {
            recordOperationAudit(
                    auth.getId(),
                    auth.getShopId(),
                    "AUTH_ROTATE_KEY",
                    auth.getId(),
                    auth.getName(),
                    requestPayload,
                    FAILED,
                    connectionStatus.getMessage(),
                    user
            );
            throw new IllegalStateException(connectionStatus.getMessage());
        }
        auth.setApiKeyCiphertext(credentialService.encrypt(apiKey));
        auth.setApiKeyFingerprint(credentialService.fingerprint(apiKey));
        auth.setLastSyncStatus(SUCCESS);
        auth.setLastSyncMessage(connectionStatus.getMessage());
        auth.setUpdatedBy(user != null ? user.getId() : null);
        auth.setUpdateTime(new Date());
        auth.setApiKeyPlaintext(null);
        authMapper.updateById(auth);
        recordOperationAudit(
                auth.getId(),
                auth.getShopId(),
                "AUTH_ROTATE_KEY",
                auth.getId(),
                auth.getName(),
                requestPayload,
                SUCCESS,
                connectionStatus.getMessage(),
                user
        );
        return auth;
    }

    private void queueInitJob(OzonAuth auth, UserInfo user, OzonSyncJobType jobType, Date now) {
        OzonSyncJob job = new OzonSyncJob();
        job.setId(nextId());
        job.setAuthId(auth.getId());
        job.setShopId(auth.getShopId());
        job.setJobType(jobType.name());
        job.setStatus(PENDING);
        job.setOperator(user != null ? user.getId() : null);
        job.setCreateTime(now);
        job.setUpdateTime(now);
        syncJobMapper.insert(job);
    }

    private OzonAuth validateOwnedAuth(UserInfo user, String authId) {
        String shopId = requireShopId(user);
        String cleanAuthId = requireText(authId, "authId不能为空");
        OzonAuth auth = authMapper.selectById(cleanAuthId);
        if (auth == null) {
            throw new IllegalArgumentException("Ozon授权不存在");
        }
        if (!shopId.equals(auth.getShopId())) {
            throw new IllegalArgumentException("无权操作该Ozon授权");
        }
        return auth;
    }

    private OzonAuthView toView(OzonAuth auth) {
        OzonAuthView view = new OzonAuthView();
        view.setId(auth.getId());
        view.setName(auth.getName());
        view.setClientId(auth.getClientId());
        view.setApiKeyMasked(maskFingerprint(auth.getApiKeyFingerprint()));
        view.setStatus(Boolean.TRUE.equals(auth.getDisabled()) ? DISABLED : auth.getStatus());
        view.setLastSyncStatus(auth.getLastSyncStatus());
        view.setLastSyncMessage(auth.getLastSyncMessage());
        view.setLastSyncTime(auth.getLastSyncTime());
        return view;
    }

    private String requireShopId(UserInfo user) {
        if (user == null || StrUtil.isBlank(user.getCompanyid())) {
            throw new IllegalArgumentException("当前用户缺少店铺上下文");
        }
        return user.getCompanyid();
    }

    private String requireText(String value, String message) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String resolveAuthName(String name, String clientId) {
        if (StrUtil.isNotBlank(name)) {
            return name.trim();
        }
        return "Ozon-" + clientId;
    }

    private String buildAuthRequestPayload(String name, String clientId) {
        JSONObject payload = new JSONObject();
        payload.put("name", resolveAuthName(name, clientId));
        payload.put("clientId", clientId);
        payload.put("hasApiKey", Boolean.TRUE);
        return payload.toJSONString();
    }

    private String buildRotateKeyPayload(String authId, String clientId) {
        JSONObject payload = new JSONObject();
        payload.put("authId", authId);
        payload.put("clientId", clientId);
        payload.put("hasApiKey", Boolean.TRUE);
        return payload.toJSONString();
    }

    private String toConnectionPayload(OzonConnectionStatus status) {
        JSONObject payload = new JSONObject();
        payload.put("success", status != null && status.isSuccess());
        payload.put("message", status == null ? null : status.getMessage());
        return payload.toJSONString();
    }

    private void recordApiLog(
            String authId,
            String shopId,
            String actionName,
            String objectId,
            String requestPayload,
            String responsePayload,
            String status,
            String errorMessage,
            long startedAt,
            String operator
    ) {
        opsService.recordApiLog(new OzonApiLogRecordCommand(
                authId,
                shopId,
                SELLER_API_GROUP,
                actionName,
                WAREHOUSE_LIST_ENDPOINT,
                "POST",
                AUTH_OBJECT_TYPE,
                objectId,
                requestPayload,
                responsePayload,
                status,
                errorMessage,
                Math.max(System.currentTimeMillis() - startedAt, 0L),
                operator
        ));
    }

    private void recordOperationAudit(
            String authId,
            String shopId,
            String operationType,
            String objectId,
            String objectCode,
            String requestPayload,
            String resultStatus,
            String resultMessage,
            UserInfo user
    ) {
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                authId,
                shopId,
                operationType,
                AUTH_OBJECT_TYPE,
                objectId,
                objectCode,
                requestPayload,
                resultStatus,
                resultMessage,
                user == null ? null : user.getId()
        ));
    }

    private String maskFingerprint(String fingerprint) {
        if (StrUtil.isBlank(fingerprint) || fingerprint.length() < 8) {
            return "****";
        }
        return fingerprint.substring(0, 4) + "****" + fingerprint.substring(fingerprint.length() - 4);
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
