package com.wimoor.ozon.seller.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozon.posting.mapper.OzonPostingMapper;
import com.wimoor.ozon.posting.pojo.entity.OzonPosting;
import com.wimoor.ozon.seller.mapper.OzonDeliveryMethodMapper;
import com.wimoor.ozon.seller.mapper.OzonShopConfigMapper;
import com.wimoor.ozon.seller.mapper.OzonWarehouseMapper;
import com.wimoor.ozon.seller.pojo.dto.OzonDeliveryMethodSaveCommand;
import com.wimoor.ozon.seller.pojo.entity.OzonDeliveryMethod;
import com.wimoor.ozon.seller.pojo.entity.OzonShopConfig;
import com.wimoor.ozon.seller.pojo.entity.OzonWarehouse;
import com.wimoor.ozon.seller.pojo.vo.OzonWarehouseView;
import com.wimoor.ozon.seller.service.IOzonSellerSettingsService;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class OzonSellerSettingsServiceImpl implements IOzonSellerSettingsService {

    private static final String DELIVERY_METHOD_OBJECT_TYPE = "DELIVERY_METHOD";

    private final OzonAuthAccessService authAccessService;
    private final OzonWarehouseMapper warehouseMapper;
    private final OzonShopConfigMapper shopConfigMapper;
    private final OzonDeliveryMethodMapper deliveryMethodMapper;
    private OzonPostingMapper postingMapper;
    private IOzonOpsService opsService = new IOzonOpsService() {
    };

    @Autowired
    public OzonSellerSettingsServiceImpl(
            OzonAuthAccessService authAccessService,
            OzonWarehouseMapper warehouseMapper,
            OzonShopConfigMapper shopConfigMapper,
            OzonDeliveryMethodMapper deliveryMethodMapper
    ) {
        this.authAccessService = authAccessService;
        this.warehouseMapper = warehouseMapper;
        this.shopConfigMapper = shopConfigMapper;
        this.deliveryMethodMapper = deliveryMethodMapper;
    }

    public OzonSellerSettingsServiceImpl(
            OzonAuthMapper authMapper,
            OzonWarehouseMapper warehouseMapper,
            OzonShopConfigMapper shopConfigMapper,
            OzonDeliveryMethodMapper deliveryMethodMapper
    ) {
        this(new OzonAuthAccessService(authMapper), warehouseMapper, shopConfigMapper, deliveryMethodMapper);
    }

    @Autowired(required = false)
    public void setOpsService(IOzonOpsService opsService) {
        if (opsService != null) {
            this.opsService = opsService;
        }
    }

    @Autowired(required = false)
    public void setPostingMapper(OzonPostingMapper postingMapper) {
        this.postingMapper = postingMapper;
    }

    @Override
    public List<OzonWarehouseView> listWarehouses(UserInfo user, String authId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        OzonShopConfig shopConfig = loadShopConfig(auth.getId());
        List<OzonWarehouse> warehouses = warehouseMapper.selectList(new QueryWrapper<OzonWarehouse>()
                .eq("auth_id", auth.getId())
                .orderByDesc("active")
                .orderByAsc("warehouse_id"));
        if (warehouses == null || warehouses.isEmpty()) {
            return Collections.emptyList();
        }
        List<OzonWarehouseView> result = new ArrayList<>(warehouses.size());
        for (OzonWarehouse warehouse : warehouses) {
            OzonWarehouseView view = new OzonWarehouseView();
            view.setId(warehouse.getId());
            view.setAuthId(warehouse.getAuthId());
            view.setWarehouseId(warehouse.getWarehouseId());
            view.setName(warehouse.getName());
            view.setStatus(warehouse.getStatus());
            view.setWarehouseType(warehouse.getWarehouseType());
            view.setActive(warehouse.getActive());
            view.setDefaultWarehouse(shopConfig != null && StrUtil.equals(shopConfig.getDefaultWarehouseId(), warehouse.getWarehouseId()));
            view.setSyncedAt(warehouse.getSyncedAt());
            view.setLastWarehouseSyncTime(shopConfig == null ? null : shopConfig.getLastWarehouseSyncTime());
            result.add(view);
        }
        return result;
    }

    @Override
    public List<OzonDeliveryMethod> listDeliveryMethods(UserInfo user, String authId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        List<OzonDeliveryMethod> methods = deliveryMethodMapper.selectList(new QueryWrapper<OzonDeliveryMethod>()
                .eq("auth_id", auth.getId())
                .orderByDesc("is_default")
                .orderByDesc("enabled")
                .orderByAsc("method_code"));
        return methods == null ? Collections.emptyList() : methods;
    }

    @Override
    public OzonDeliveryMethod saveDeliveryMethod(UserInfo user, OzonDeliveryMethodSaveCommand command) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command == null ? null : command.getAuthId());
        String payload = JSON.toJSONString(command);
        OzonDeliveryMethod method = null;
        String methodCode = requireText(command == null ? null : command.getMethodCode(), "methodCode不能为空");
        String methodName = requireText(command == null ? null : command.getMethodName(), "methodName不能为空");
        try {
            method = loadOrCreateMethod(auth, command, methodCode);
            Date now = new Date();
            boolean makeDefault = Boolean.TRUE.equals(command.getDefaultMethod());
            if (makeDefault) {
                deliveryMethodMapper.clearDefaultByAuthId(auth.getId());
            }
            if (method.getCreateTime() == null) {
                method.setCreateTime(now);
            }
            method.setAuthId(auth.getId());
            method.setShopId(auth.getShopId());
            method.setMethodCode(methodCode);
            method.setMethodName(methodName);
            method.setDescription(trim(command.getDescription()));
            method.setEnabled(command.getEnabled() == null ? Boolean.TRUE : command.getEnabled());
            method.setDefaultMethod(makeDefault);
            method.setUpdateTime(now);
            if (deliveryMethodMapper.selectById(method.getId()) == null) {
                deliveryMethodMapper.insert(method);
            } else {
                deliveryMethodMapper.updateById(method);
            }
            recordOperationAudit(auth, user, method, payload, "SUCCESS", "saved");
            return method;
        } catch (RuntimeException ex) {
            recordOperationAudit(auth, user, method, payload, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    @Override
    public OzonDeliveryMethod setDefaultDeliveryMethod(UserInfo user, String authId, String methodId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        OzonDeliveryMethod method = requireOwnedMethod(auth, methodId);
        deliveryMethodMapper.clearDefaultByAuthId(auth.getId());
        method.setDefaultMethod(Boolean.TRUE);
        method.setEnabled(Boolean.TRUE);
        method.setUpdateTime(new Date());
        deliveryMethodMapper.updateById(method);
        recordOperationAudit(auth, user, method, JSON.toJSONString(method), "SUCCESS", "default updated");
        return method;
    }

    @Override
    public void deleteDeliveryMethod(UserInfo user, String authId, String methodId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        OzonDeliveryMethod method = requireOwnedMethod(auth, methodId);
        if (Boolean.TRUE.equals(method.getDefaultMethod())) {
            throw new IllegalArgumentException("默认配送方式不能删除");
        }
        if (postingMapper == null) {
            throw new IllegalStateException("Posting关联校验组件未就绪");
        }
        Long referenceCount = postingMapper.selectCount(new QueryWrapper<OzonPosting>()
                .eq("auth_id", auth.getId())
                .eq("delivery_method_id", method.getId()));
        if (referenceCount != null && referenceCount > 0) {
            throw new IllegalArgumentException("配送方式已被Posting引用，不能删除");
        }
        deliveryMethodMapper.deleteById(method.getId());
        recordOperationAudit(auth, user, method, JSON.toJSONString(method), "SUCCESS", "deleted");
    }

    private OzonDeliveryMethod requireOwnedMethod(OzonAuth auth, String methodId) {
        OzonDeliveryMethod method = deliveryMethodMapper.selectById(requireText(methodId, "methodId不能为空"));
        if (method == null || !StrUtil.equals(method.getAuthId(), auth.getId())) {
            throw new IllegalArgumentException("配送方式不存在或无权操作");
        }
        return method;
    }

    private OzonDeliveryMethod loadOrCreateMethod(OzonAuth auth, OzonDeliveryMethodSaveCommand command, String methodCode) {
        OzonDeliveryMethod existing = null;
        String requestedId = trim(command == null ? null : command.getId());
        if (requestedId != null) {
            existing = deliveryMethodMapper.selectById(requestedId);
            if (existing != null && !StrUtil.equals(existing.getAuthId(), auth.getId())) {
                throw new IllegalArgumentException("无权操作该配送方式");
            }
        }
        if (existing != null) {
            return existing;
        }
        OzonDeliveryMethod duplicate = deliveryMethodMapper.selectOne(new QueryWrapper<OzonDeliveryMethod>()
                .eq("auth_id", auth.getId())
                .eq("method_code", methodCode)
                .last("limit 1"));
        if (duplicate != null) {
            return duplicate;
        }
        OzonDeliveryMethod method = new OzonDeliveryMethod();
        method.setId(nextId());
        return method;
    }

    private OzonShopConfig loadShopConfig(String authId) {
        return shopConfigMapper.selectOne(new QueryWrapper<OzonShopConfig>()
                .eq("auth_id", authId)
                .last("limit 1"));
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

    private String nextId() {
        return IdUtil.getSnowflakeNextIdStr();
    }

    private void recordOperationAudit(
            OzonAuth auth,
            UserInfo user,
            OzonDeliveryMethod method,
            String payload,
            String resultStatus,
            String resultMessage
    ) {
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                auth.getId(),
                auth.getShopId(),
                "DELIVERY_METHOD_SAVE",
                DELIVERY_METHOD_OBJECT_TYPE,
                method == null ? null : method.getId(),
                method == null ? null : method.getMethodCode(),
                payload,
                resultStatus,
                resultMessage,
                user == null ? null : user.getId()
        ));
    }
}
