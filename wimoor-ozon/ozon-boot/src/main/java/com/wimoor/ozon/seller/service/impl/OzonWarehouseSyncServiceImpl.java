package com.wimoor.ozon.seller.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.client.OzonRemoteWarehouse;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozon.security.OzonCredentialService;
import com.wimoor.ozon.seller.mapper.OzonShopConfigMapper;
import com.wimoor.ozon.seller.mapper.OzonWarehouseMapper;
import com.wimoor.ozon.seller.pojo.entity.OzonWarehouse;
import com.wimoor.ozon.seller.pojo.vo.OzonWarehouseSyncResult;
import com.wimoor.ozon.seller.service.IOzonWarehouseSyncService;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OzonWarehouseSyncServiceImpl implements IOzonWarehouseSyncService {

    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";
    private static final String API_GROUP = "SELLER";
    private static final String WAREHOUSE_OBJECT_TYPE = "WAREHOUSE";
    private static final String WAREHOUSE_LIST_ENDPOINT = "/v1/warehouse/list";

    private final OzonAuthMapper authMapper;
    private final OzonWarehouseMapper warehouseMapper;
    private final OzonShopConfigMapper shopConfigMapper;
    private final OzonSellerApiClient sellerApiClient;
    private final OzonCredentialService credentialService;
    private IOzonOpsService opsService = new IOzonOpsService() {
    };

    @Autowired(required = false)
    public void setOpsService(IOzonOpsService opsService) {
        if (opsService != null) {
            this.opsService = opsService;
        }
    }

    @Override
    public OzonWarehouseSyncResult syncWarehouses(String authId) {
        if (StrUtil.isBlank(authId)) {
            throw new IllegalArgumentException("authId不能为空");
        }
        OzonAuth auth = authMapper.selectById(authId);
        if (auth == null) {
            throw new IllegalArgumentException("Ozon授权不存在");
        }
        String apiKey = credentialService.decrypt(auth.getApiKeyCiphertext());
        Date syncedAt = new Date();
        long startedAt = System.currentTimeMillis();
        try {
            List<OzonRemoteWarehouse> remoteWarehouses = sellerApiClient.listWarehouses(auth.getClientId(), apiKey);
            recordApiLog(auth, "{}", JSON.toJSONString(remoteWarehouses), SUCCESS, null, startedAt);
            warehouseMapper.deleteByAuthId(authId);
            for (OzonRemoteWarehouse remoteWarehouse : remoteWarehouses) {
                warehouseMapper.insert(toWarehouse(auth, remoteWarehouse, syncedAt));
            }
            String defaultWarehouseId = remoteWarehouses.isEmpty() ? null : String.valueOf(remoteWarehouses.get(0).getWarehouseId());
            shopConfigMapper.updateWarehouseSyncInfo(authId, defaultWarehouseId, syncedAt);
            auth.setLastSyncStatus(SUCCESS);
            auth.setLastSyncMessage("warehouse synced");
            auth.setLastSyncTime(syncedAt);
            authMapper.updateById(auth);
            OzonWarehouseSyncResult result = new OzonWarehouseSyncResult();
            result.setWarehouseCount(remoteWarehouses.size());
            result.setSyncedAt(syncedAt);
            result.setMessage("warehouse synced");
            return result;
        } catch (RuntimeException ex) {
            recordApiLog(auth, "{}", null, FAILED, ex.getMessage(), startedAt);
            auth.setLastSyncStatus(FAILED);
            auth.setLastSyncMessage(ex.getMessage());
            auth.setLastSyncTime(syncedAt);
            authMapper.updateById(auth);
            throw ex;
        }
    }

    private OzonWarehouse toWarehouse(OzonAuth auth, OzonRemoteWarehouse remoteWarehouse, Date syncedAt) {
        OzonWarehouse warehouse = new OzonWarehouse();
        warehouse.setId(IdUtil.getSnowflakeNextIdStr());
        warehouse.setAuthId(auth.getId());
        warehouse.setShopId(auth.getShopId());
        warehouse.setWarehouseId(String.valueOf(remoteWarehouse.getWarehouseId()));
        warehouse.setName(remoteWarehouse.getName());
        warehouse.setStatus(remoteWarehouse.getStatus());
        warehouse.setWarehouseType(remoteWarehouse.getType());
        warehouse.setActive("ACTIVE".equalsIgnoreCase(remoteWarehouse.getStatus()));
        warehouse.setRawData(JSONObject.toJSONString(remoteWarehouse));
        warehouse.setSyncedAt(syncedAt);
        return warehouse;
    }

    @Override
    public int countByAuth(String authId) {
        if (StrUtil.isBlank(authId)) {
            return 0;
        }
        return warehouseMapper.countByAuthId(authId);
    }

    @Override
    public String getDefaultWarehouseName(String authId) {
        if (StrUtil.isBlank(authId)) {
            return null;
        }
        OzonWarehouse defaultWarehouse = warehouseMapper.selectDefaultByAuthId(authId);
        return defaultWarehouse != null ? defaultWarehouse.getName() : null;
    }

    private void recordApiLog(
            OzonAuth auth,
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
                "LIST_WAREHOUSES",
                WAREHOUSE_LIST_ENDPOINT,
                "POST",
                WAREHOUSE_OBJECT_TYPE,
                auth.getId(),
                requestPayload,
                responsePayload,
                status,
                errorMessage,
                Math.max(System.currentTimeMillis() - startedAt, 0L),
                null
        ));
    }
}
