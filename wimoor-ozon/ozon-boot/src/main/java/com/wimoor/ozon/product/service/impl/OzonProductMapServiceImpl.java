package com.wimoor.ozon.product.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.erp.api.ErpClientOneFeign;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.product.mapper.OzonProductMapMapper;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftImportCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductMapSaveCommand;
import com.wimoor.ozon.product.pojo.entity.OzonProductMap;
import com.wimoor.ozon.product.pojo.vo.OzonProductMapView;
import com.wimoor.ozon.product.service.IOzonProductMapService;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class OzonProductMapServiceImpl implements IOzonProductMapService {

    private static final String DRAFT = "DRAFT";
    private static final String MAPPED = "MAPPED";

    private final OzonAuthAccessService authAccessService;
    private final OzonProductMapMapper productMapMapper;
    private final ErpClientOneFeign erpClientOneFeign;

    @Autowired
    public OzonProductMapServiceImpl(
            OzonAuthAccessService authAccessService,
            OzonProductMapMapper productMapMapper,
            ErpClientOneFeign erpClientOneFeign
    ) {
        this.authAccessService = authAccessService;
        this.productMapMapper = productMapMapper;
        this.erpClientOneFeign = erpClientOneFeign;
    }

    public OzonProductMapServiceImpl(
            OzonAuthMapper authMapper,
            OzonProductMapMapper productMapMapper,
            ErpClientOneFeign erpClientOneFeign
    ) {
        this(new OzonAuthAccessService(authMapper), productMapMapper, erpClientOneFeign);
    }

    @Override
    public List<OzonProductMapView> list(UserInfo user, String authId, String keyword) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        List<OzonProductMap> items = productMapMapper.listByAuthId(auth.getId(), trim(keyword));
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream().map(this::toView).collect(Collectors.toList());
    }

    @Override
    public OzonProductMap saveMapping(UserInfo user, OzonProductMapSaveCommand command) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command.getAuthId());
        String materialSku = requireText(command.getMaterialSku(), "ERP SKU不能为空");
        Map<String, Object> material = loadMaterial(materialSku);
        OzonProductMap map = productMapMapper.selectByAuthIdAndMaterialSku(auth.getId(), materialSku);
        Date now = new Date();
        if (map == null) {
            map = new OzonProductMap();
            map.setId(nextId());
            map.setAuthId(auth.getId());
            map.setShopId(auth.getShopId());
            map.setCreateTime(now);
        }
        fillMaterial(map, material);
        map.setOzonOfferId(requireText(command.getOzonOfferId(), "Ozon Offer ID不能为空"));
        map.setOzonSku(trim(command.getOzonSku()));
        map.setOzonProductId(trim(command.getOzonProductId()));
        map.setStatus(MAPPED);
        map.setUpdateTime(now);
        persist(map);
        return map;
    }

    @Override
    public int importDraft(UserInfo user, OzonProductDraftImportCommand command) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command.getAuthId());
        List<String> skus = cleanSkus(command.getSkus());
        Map<String, Object> materialMap = loadMaterialMap(skus);
        int imported = 0;
        Date now = new Date();
        for (String sku : skus) {
            Object materialData = materialMap.get(sku);
            if (!(materialData instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> material = (Map<String, Object>) materialData;
            OzonProductMap map = productMapMapper.selectByAuthIdAndMaterialSku(auth.getId(), sku);
            if (map == null) {
                map = new OzonProductMap();
                map.setId(nextId());
                map.setAuthId(auth.getId());
                map.setShopId(auth.getShopId());
                map.setCreateTime(now);
            }
            fillMaterial(map, material);
            if (StrUtil.isBlank(map.getStatus())) {
                map.setStatus(DRAFT);
            }
            map.setUpdateTime(now);
            persist(map);
            imported++;
        }
        return imported;
    }

    private Map<String, Object> loadMaterial(String sku) {
        Map<String, Object> materialMap = loadMaterialMap(Collections.singletonList(sku));
        Object material = materialMap.get(sku);
        if (!(material instanceof Map)) {
            throw new IllegalArgumentException("ERP物料不存在: " + sku);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) material;
        return result;
    }

    private Map<String, Object> loadMaterialMap(List<String> skus) {
        Result<Map<String, Object>> result = erpClientOneFeign.findMaterialMapBySku(cleanSkus(skus));
        if (result == null || result.getData() == null) {
            return Collections.emptyMap();
        }
        return result.getData();
    }

    private List<String> cleanSkus(List<String> skus) {
        if (skus == null || skus.isEmpty()) {
            throw new IllegalArgumentException("SKU列表不能为空");
        }
        List<String> result = new ArrayList<>();
        for (String sku : skus) {
            if (StrUtil.isNotBlank(sku)) {
                result.add(sku.trim());
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("SKU列表不能为空");
        }
        return result;
    }

    private void fillMaterial(OzonProductMap map, Map<String, Object> material) {
        map.setMaterialSku(asText(material.get("msku")));
        map.setMaterialName(asText(material.get("name")));
        map.setOwnerName(asText(material.get("ownername")));
        map.setImage(asText(material.get("image")));
        map.setMaterialPrice(asDecimal(material.get("price")));
    }

    private void persist(OzonProductMap map) {
        if (productMapMapper.selectById(map.getId()) == null) {
            productMapMapper.insert(map);
        } else {
            productMapMapper.updateById(map);
        }
    }

    private OzonProductMapView toView(OzonProductMap item) {
        OzonProductMapView view = new OzonProductMapView();
        view.setId(item.getId());
        view.setMaterialSku(item.getMaterialSku());
        view.setMaterialName(item.getMaterialName());
        view.setOwnerName(item.getOwnerName());
        view.setImage(item.getImage());
        view.setMaterialPrice(item.getMaterialPrice());
        view.setOzonOfferId(item.getOzonOfferId());
        view.setOzonSku(item.getOzonSku());
        view.setOzonProductId(item.getOzonProductId());
        view.setStatus(item.getStatus());
        view.setLastSyncStatus(item.getLastSyncStatus());
        view.setLastSyncMessage(item.getLastSyncMessage());
        view.setLastSyncTime(item.getLastSyncTime());
        return view;
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

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal asDecimal(Object value) {
        if (value == null || StrUtil.isBlank(String.valueOf(value))) {
            return null;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private String nextId() {
        return IdUtil.getSnowflakeNextIdStr();
    }
}
