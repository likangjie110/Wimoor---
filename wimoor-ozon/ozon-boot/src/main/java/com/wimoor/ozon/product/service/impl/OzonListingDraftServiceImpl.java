package com.wimoor.ozon.product.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.result.Result;
import com.wimoor.erp.api.ErpClientOneFeign;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.product.mapper.OzonListingAttributeMapper;
import com.wimoor.ozon.product.mapper.OzonListingDraftMapper;
import com.wimoor.ozon.product.mapper.OzonListingImageMapper;
import com.wimoor.ozon.product.mapper.OzonListingPublishTaskMapper;
import com.wimoor.ozon.product.mapper.OzonListingVariantMapper;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftDetailQuery;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftImportCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftListQuery;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftSaveCommand;
import com.wimoor.ozon.product.pojo.entity.OzonListingAttribute;
import com.wimoor.ozon.product.pojo.entity.OzonListingDraft;
import com.wimoor.ozon.product.pojo.entity.OzonListingImage;
import com.wimoor.ozon.product.pojo.entity.OzonListingPublishTask;
import com.wimoor.ozon.product.pojo.entity.OzonListingVariant;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftDetailView;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftImportResult;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftListView;
import com.wimoor.ozon.product.service.IOzonListingDraftService;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class OzonListingDraftServiceImpl implements IOzonListingDraftService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_READY = "READY";
    private static final String SCOPE_COMMON = "COMMON";
    private static final String SCOPE_VARIANT = "VARIANT";

    private final OzonAuthAccessService authAccessService;
    private final OzonListingDraftMapper draftMapper;
    private final OzonListingVariantMapper variantMapper;
    private final OzonListingAttributeMapper attributeMapper;
    private final OzonListingImageMapper imageMapper;
    private final OzonListingPublishTaskMapper publishTaskMapper;
    private final ErpClientOneFeign erpClientOneFeign;

    @Autowired
    public OzonListingDraftServiceImpl(
            OzonAuthAccessService authAccessService,
            OzonListingDraftMapper draftMapper,
            OzonListingVariantMapper variantMapper,
            OzonListingAttributeMapper attributeMapper,
            OzonListingImageMapper imageMapper,
            OzonListingPublishTaskMapper publishTaskMapper,
            ErpClientOneFeign erpClientOneFeign
    ) {
        this.authAccessService = authAccessService;
        this.draftMapper = draftMapper;
        this.variantMapper = variantMapper;
        this.attributeMapper = attributeMapper;
        this.imageMapper = imageMapper;
        this.publishTaskMapper = publishTaskMapper;
        this.erpClientOneFeign = erpClientOneFeign;
    }

    public OzonListingDraftServiceImpl(
            OzonAuthMapper authMapper,
            OzonListingDraftMapper draftMapper,
            OzonListingVariantMapper variantMapper,
            OzonListingAttributeMapper attributeMapper,
            OzonListingImageMapper imageMapper,
            OzonListingPublishTaskMapper publishTaskMapper,
            ErpClientOneFeign erpClientOneFeign
    ) {
        this(new OzonAuthAccessService(authMapper), draftMapper, variantMapper, attributeMapper, imageMapper, publishTaskMapper, erpClientOneFeign);
    }

    @Override
    @Transactional
    public OzonProductDraftDetailView saveDraft(UserInfo user, OzonProductDraftSaveCommand command) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command == null ? null : command.getAuthId());
        validateDraft(command);
        Date now = new Date();

        OzonListingDraft draft = loadOrCreateDraft(auth, command, now);
        Map<String, OzonListingVariant> existingBySku = mapExistingVariants(draft.getId());

        replaceCommonAttributes(draft, command.getCommonAttributes(), now);
        replaceCommonImages(draft, command.getCommonImages(), now);
        syncVariants(draft, existingBySku, command.getVariants(), now);

        draft.setStatus(resolveDraftStatus(draft));
        draft.setUpdateTime(now);
        draftMapper.updateById(draft);
        return loadDetail(auth.getId(), draft.getId());
    }

    @Override
    @Transactional
    public OzonProductDraftImportResult importDraft(UserInfo user, OzonProductDraftImportCommand command) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command == null ? null : command.getAuthId());
        List<String> skus = cleanSkus(command == null ? null : command.getSkus());
        Map<String, Object> materials = loadMaterialMap(skus);
        Date now = new Date();
        OzonListingDraft draft = loadOrCreateDraftForImport(auth, command, now);
        Map<String, OzonListingVariant> existingBySku = mapExistingVariants(draft.getId());

        int created = 0;
        int updated = 0;
        int imported = 0;
        List<String> skipped = new ArrayList<>();
        for (String sku : skus) {
            Object materialData = materials.get(sku);
            if (!(materialData instanceof Map)) {
                skipped.add(sku);
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> material = (Map<String, Object>) materialData;
            applySourceSnapshot(draft, material, now);
            OzonListingVariant variant = existingBySku.get(sku);
            if (variant == null) {
                variant = new OzonListingVariant();
                variant.setId(nextId());
                variant.setDraftId(draft.getId());
                variant.setAuthId(draft.getAuthId());
                variant.setShopId(draft.getShopId());
                variant.setMaterialSku(sku);
                variant.setCreateTime(now);
                created++;
            } else {
                updated++;
            }
            variant.setMaterialName(asText(material.get("name")));
            variant.setPriceSourceValue(asDecimal(material.get("price")));
            variant.setStatus(STATUS_DRAFT);
            variant.setUpdateTime(now);
            if (existingBySku.containsKey(sku)) {
                variantMapper.updateById(variant);
            } else {
                variantMapper.insert(variant);
            }
            imported++;
        }
        draft.setStatus(resolveDraftStatus(draft));
        draft.setUpdateTime(now);
        draftMapper.updateById(draft);

        OzonProductDraftImportResult result = new OzonProductDraftImportResult();
        result.setDraftId(draft.getId());
        result.setImportedCount(imported);
        result.setCreatedVariantCount(created);
        result.setUpdatedVariantCount(updated);
        result.setSkippedSkus(skipped);
        return result;
    }

    @Override
    public List<OzonProductDraftListView> listDrafts(UserInfo user, OzonProductDraftListQuery query) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, query == null ? null : query.getAuthId());
        List<OzonListingDraft> drafts = draftMapper.selectList(new QueryWrapper<OzonListingDraft>()
                .eq("auth_id", auth.getId())
                .orderByDesc("update_time")
                .orderByDesc("create_time"));
        List<OzonProductDraftListView> result = new ArrayList<>();
        for (OzonListingDraft draft : safeList(drafts)) {
            OzonProductDraftListView item = new OzonProductDraftListView();
            item.setDraftId(draft.getId());
            item.setDraftName(draft.getDraftName());
            item.setDescriptionCategoryId(draft.getDescriptionCategoryId());
            item.setDescriptionCategoryName(draft.getDescriptionCategoryName());
            item.setTypeId(draft.getTypeId());
            item.setTypeName(draft.getTypeName());
            item.setStatus(draft.getStatus());
            item.setVariantCount(safeList(variantMapper.listByDraftId(draft.getId())).size());
            OzonListingPublishTask task = latestTask(draft.getId());
            item.setLastPublishAt(task == null ? null : task.getCreateTime());
            result.add(item);
        }
        return result;
    }

    @Override
    public OzonProductDraftDetailView getDraftDetail(UserInfo user, OzonProductDraftDetailQuery query) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, query == null ? null : query.getAuthId());
        return loadDetail(auth.getId(), query == null ? null : query.getDraftId());
    }

    private void validateDraft(OzonProductDraftSaveCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("draft command不能为空");
        }
        for (OzonProductDraftSaveCommand.AttributeItem item : safeList(command.getCommonAttributes())) {
            validateAttributeItem(item);
        }
        for (OzonProductDraftSaveCommand.ImageItem item : safeList(command.getCommonImages())) {
            validateImageItem(item);
        }
        List<OzonProductDraftSaveCommand.VariantItem> variants = safeList(command.getVariants());
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (OzonProductDraftSaveCommand.VariantItem variant : variants) {
            String sku = variant == null ? null : trim(variant.getMaterialSku());
            if (StrUtil.isBlank(sku)) {
                throw new IllegalArgumentException("variant materialSku不能为空");
            }
            if (seen.putIfAbsent(sku, Boolean.TRUE) != null) {
                throw new IllegalArgumentException("variant materialSku不能重复: " + sku);
            }
            for (OzonProductDraftSaveCommand.AttributeItem item : safeList(variant.getAttributes())) {
                validateAttributeItem(item);
            }
            for (OzonProductDraftSaveCommand.ImageItem item : safeList(variant.getImages())) {
                validateImageItem(item);
            }
        }
    }

    private OzonListingDraft loadOrCreateDraft(OzonAuth auth, OzonProductDraftSaveCommand command, Date now) {
        String draftId = trim(command.getDraftId());
        OzonListingDraft draft = draftId == null ? null : draftMapper.selectByAuthIdAndDraftId(auth.getId(), draftId);
        if (draft == null) {
            draft = new OzonListingDraft();
            draft.setId(draftId == null ? nextId() : draftId);
            draft.setAuthId(auth.getId());
            draft.setShopId(auth.getShopId());
            draft.setDraftName(trim(command.getDraftName()));
            draft.setDescriptionCategoryId(command.getDescriptionCategoryId());
            draft.setDescriptionCategoryName(trim(command.getDescriptionCategoryName()));
            draft.setTypeId(command.getTypeId());
            draft.setTypeName(trim(command.getTypeName()));
            draft.setStatus(STATUS_DRAFT);
            draft.setCreateTime(now);
            draftMapper.insert(draft);
        }
        draft.setDraftName(trim(command.getDraftName()));
        draft.setDescriptionCategoryId(command.getDescriptionCategoryId());
        draft.setDescriptionCategoryName(trim(command.getDescriptionCategoryName()));
        draft.setTypeId(command.getTypeId());
        draft.setTypeName(trim(command.getTypeName()));
        draft.setTitleOverrideValue(trim(command.getTitleOverrideValue()));
        draft.setBrandOverrideValue(trim(command.getBrandOverrideValue()));
        draft.setDescriptionOverrideValue(trim(command.getDescriptionOverrideValue()));
        draft.setUpdateTime(now);
        return draft;
    }

    private OzonListingDraft loadOrCreateDraftForImport(OzonAuth auth, OzonProductDraftImportCommand command, Date now) {
        String draftId = trim(command.getDraftId());
        OzonListingDraft draft = draftId == null ? null : draftMapper.selectByAuthIdAndDraftId(auth.getId(), draftId);
        if (draft == null) {
            draft = new OzonListingDraft();
            draft.setId(draftId == null ? nextId() : draftId);
            draft.setAuthId(auth.getId());
            draft.setShopId(auth.getShopId());
            draft.setCreateTime(now);
            draft.setDraftName(trim(command.getDraftName()));
            draft.setStatus(STATUS_DRAFT);
            draftMapper.insert(draft);
        }
        if (StrUtil.isBlank(draft.getDraftName())) {
            draft.setDraftName(trim(command.getDraftName()));
        }
        return draft;
    }

    private void applySourceSnapshot(OzonListingDraft draft, Map<String, Object> material, Date now) {
        draft.setTitleSourceValue(asText(material.get("name")));
        if (StrUtil.isBlank(draft.getDraftName())) {
            draft.setDraftName(asText(material.get("msku")));
        }
        draft.setUpdateTime(now);
    }

    private Map<String, OzonListingVariant> mapExistingVariants(String draftId) {
        Map<String, OzonListingVariant> result = new LinkedHashMap<>();
        for (OzonListingVariant item : safeList(variantMapper.listByDraftId(draftId))) {
            result.put(item.getMaterialSku(), item);
        }
        return result;
    }

    private void replaceCommonAttributes(
            OzonListingDraft draft,
            List<OzonProductDraftSaveCommand.AttributeItem> items,
            Date now
    ) {
        attributeMapper.deleteByDraftIdAndVariantId(draft.getId(), null);
        for (OzonProductDraftSaveCommand.AttributeItem item : safeList(items)) {
            attributeMapper.insert(toAttributeEntity(draft, null, SCOPE_COMMON, item, now));
        }
    }

    private void replaceCommonImages(
            OzonListingDraft draft,
            List<OzonProductDraftSaveCommand.ImageItem> items,
            Date now
    ) {
        imageMapper.deleteByDraftIdAndVariantId(draft.getId(), null);
        for (OzonProductDraftSaveCommand.ImageItem item : safeList(items)) {
            imageMapper.insert(toImageEntity(draft, null, "GROUP", item, now));
        }
    }

    private void syncVariants(
            OzonListingDraft draft,
            Map<String, OzonListingVariant> existingBySku,
            List<OzonProductDraftSaveCommand.VariantItem> incoming,
            Date now
    ) {
        Map<String, OzonProductDraftSaveCommand.VariantItem> incomingBySku = new LinkedHashMap<>();
        for (OzonProductDraftSaveCommand.VariantItem item : safeList(incoming)) {
            incomingBySku.put(trim(item.getMaterialSku()), item);
        }

        for (OzonListingVariant existing : existingBySku.values()) {
            if (!incomingBySku.containsKey(existing.getMaterialSku())) {
                attributeMapper.deleteByDraftIdAndVariantId(draft.getId(), existing.getId());
                imageMapper.deleteByDraftIdAndVariantId(draft.getId(), existing.getId());
                variantMapper.deleteById(existing.getId());
            }
        }

        for (OzonProductDraftSaveCommand.VariantItem item : incomingBySku.values()) {
            OzonListingVariant variant = existingBySku.get(trim(item.getMaterialSku()));
            if (variant == null) {
                variant = new OzonListingVariant();
                variant.setId(nextId());
                variant.setDraftId(draft.getId());
                variant.setAuthId(draft.getAuthId());
                variant.setShopId(draft.getShopId());
                variant.setCreateTime(now);
                fillVariant(variant, item, now);
                variantMapper.insert(variant);
            } else {
                fillVariant(variant, item, now);
                variantMapper.updateById(variant);
            }
            replaceVariantAttributes(draft, variant, item.getAttributes(), now);
            replaceVariantImages(draft, variant, item.getImages(), now);
        }
    }

    private void fillVariant(OzonListingVariant variant, OzonProductDraftSaveCommand.VariantItem item, Date now) {
        variant.setMaterialSku(trim(item.getMaterialSku()));
        variant.setMaterialName(trim(item.getMaterialName()));
        variant.setOfferIdOverride(trim(item.getOfferIdOverride()));
        variant.setBarcodeOverride(trim(item.getBarcodeOverride()));
        variant.setPriceOverride(item.getPriceOverride());
        variant.setWeightOverrideValue(item.getWeightOverrideValue());
        variant.setLengthOverrideValue(item.getLengthOverrideValue());
        variant.setWidthOverrideValue(item.getWidthOverrideValue());
        variant.setHeightOverrideValue(item.getHeightOverrideValue());
        variant.setVariantLabel(trim(item.getVariantLabel()));
        variant.setStatus(STATUS_DRAFT);
        variant.setUpdateTime(now);
    }

    private void replaceVariantAttributes(
            OzonListingDraft draft,
            OzonListingVariant variant,
            List<OzonProductDraftSaveCommand.AttributeItem> items,
            Date now
    ) {
        attributeMapper.deleteByDraftIdAndVariantId(draft.getId(), variant.getId());
        for (OzonProductDraftSaveCommand.AttributeItem item : safeList(items)) {
            attributeMapper.insert(toAttributeEntity(draft, variant, SCOPE_VARIANT, item, now));
        }
    }

    private void replaceVariantImages(
            OzonListingDraft draft,
            OzonListingVariant variant,
            List<OzonProductDraftSaveCommand.ImageItem> items,
            Date now
    ) {
        imageMapper.deleteByDraftIdAndVariantId(draft.getId(), variant.getId());
        for (OzonProductDraftSaveCommand.ImageItem item : safeList(items)) {
            imageMapper.insert(toImageEntity(draft, variant, SCOPE_VARIANT, item, now));
        }
    }

    private OzonListingAttribute toAttributeEntity(
            OzonListingDraft draft,
            OzonListingVariant variant,
            String scope,
            OzonProductDraftSaveCommand.AttributeItem item,
            Date now
    ) {
        OzonListingAttribute entity = new OzonListingAttribute();
        entity.setId(nextId());
        entity.setDraftId(draft.getId());
        entity.setVariantId(variant == null ? null : variant.getId());
        entity.setAuthId(draft.getAuthId());
        entity.setShopId(draft.getShopId());
        entity.setScope(scope);
        entity.setAttributeId(item == null ? null : item.getAttributeId());
        entity.setAttributeName(item == null ? null : trim(item.getAttributeName()));
        entity.setAttributeValueJson(item == null ? null : JSON.toJSONString(item.getValues()));
        entity.setRequiredFlag(Boolean.FALSE);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        return entity;
    }

    private OzonListingImage toImageEntity(
            OzonListingDraft draft,
            OzonListingVariant variant,
            String scope,
            OzonProductDraftSaveCommand.ImageItem item,
            Date now
    ) {
        OzonListingImage entity = new OzonListingImage();
        entity.setId(nextId());
        entity.setDraftId(draft.getId());
        entity.setVariantId(variant == null ? null : variant.getId());
        entity.setAuthId(draft.getAuthId());
        entity.setShopId(draft.getShopId());
        entity.setScope(scope);
        entity.setSource(item == null ? null : trim(item.getSource()));
        entity.setImageUrl(item == null ? null : trim(item.getImageUrl()));
        entity.setSortOrder(item == null ? null : item.getSortOrder());
        entity.setPrimary(item != null ? item.getPrimary() : null);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        return entity;
    }

    private String resolveDraftStatus(OzonListingDraft draft) {
        return draft.getDescriptionCategoryId() != null && draft.getTypeId() != null ? STATUS_READY : STATUS_DRAFT;
    }

    private OzonProductDraftDetailView loadDetail(String authId, String draftId) {
        OzonListingDraft draft = draftMapper.selectByAuthIdAndDraftId(authId, draftId);
        if (draft == null) {
            throw new IllegalArgumentException("Ozon刊登草稿不存在");
        }
        OzonProductDraftDetailView view = new OzonProductDraftDetailView();
        view.setDraftId(draft.getId());
        view.setDraftName(draft.getDraftName());
        view.setDescriptionCategoryId(draft.getDescriptionCategoryId());
        view.setDescriptionCategoryName(draft.getDescriptionCategoryName());
        view.setTypeId(draft.getTypeId());
        view.setTypeName(draft.getTypeName());
        view.setTitleSourceValue(draft.getTitleSourceValue());
        view.setTitleOverrideValue(draft.getTitleOverrideValue());
        view.setBrandSourceValue(draft.getBrandSourceValue());
        view.setBrandOverrideValue(draft.getBrandOverrideValue());
        view.setDescriptionSourceValue(draft.getDescriptionSourceValue());
        view.setDescriptionOverrideValue(draft.getDescriptionOverrideValue());
        view.setStatus(draft.getStatus());
        view.setLastPreviewStatus(draft.getLastPreviewStatus());
        view.setLastPreviewMessage(draft.getLastPreviewMessage());
        view.setLastPublishTaskId(draft.getLastPublishTaskId());
        view.setCommonAttributes(toAttributeViews(attributeMapper.listByDraftIdAndVariantId(draftId, null)));
        view.setCommonImages(toImageViews(imageMapper.listByDraftIdAndVariantId(draftId, null)));

        List<OzonProductDraftDetailView.VariantItem> variants = new ArrayList<>();
        for (OzonListingVariant variant : safeList(variantMapper.listByDraftId(draftId))) {
            OzonProductDraftDetailView.VariantItem item = new OzonProductDraftDetailView.VariantItem();
            item.setVariantId(variant.getId());
            item.setMaterialSku(variant.getMaterialSku());
            item.setMaterialName(variant.getMaterialName());
            item.setOfferIdOverride(variant.getOfferIdOverride());
            item.setBarcodeOverride(variant.getBarcodeOverride());
            item.setPriceSourceValue(variant.getPriceSourceValue());
            item.setPriceOverride(variant.getPriceOverride());
            item.setWeightSourceValue(variant.getWeightSourceValue());
            item.setWeightOverrideValue(variant.getWeightOverrideValue());
            item.setLengthSourceValue(variant.getLengthSourceValue());
            item.setLengthOverrideValue(variant.getLengthOverrideValue());
            item.setWidthSourceValue(variant.getWidthSourceValue());
            item.setWidthOverrideValue(variant.getWidthOverrideValue());
            item.setHeightSourceValue(variant.getHeightSourceValue());
            item.setHeightOverrideValue(variant.getHeightOverrideValue());
            item.setVariantLabel(variant.getVariantLabel());
            item.setStatus(variant.getStatus());
            item.setLastSyncStatus(variant.getLastSyncStatus());
            item.setLastSyncMessage(variant.getLastSyncMessage());
            item.setAttributes(toAttributeViews(attributeMapper.listByDraftIdAndVariantId(draftId, variant.getId())));
            item.setImages(toImageViews(imageMapper.listByDraftIdAndVariantId(draftId, variant.getId())));
            variants.add(item);
        }
        view.setVariants(variants);
        return view;
    }

    private OzonListingPublishTask latestTask(String draftId) {
        return publishTaskMapper.selectOne(new QueryWrapper<OzonListingPublishTask>()
                .eq("draft_id", draftId)
                .orderByDesc("create_time")
                .last("limit 1"));
    }

    private List<OzonProductDraftDetailView.AttributeItem> toAttributeViews(List<OzonListingAttribute> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        List<OzonProductDraftDetailView.AttributeItem> result = new ArrayList<>(entities.size());
        for (OzonListingAttribute entity : entities) {
            OzonProductDraftDetailView.AttributeItem item = new OzonProductDraftDetailView.AttributeItem();
            item.setAttributeId(entity.getAttributeId());
            item.setAttributeName(entity.getAttributeName());
            item.setAttributeValueJson(entity.getAttributeValueJson());
            item.setRequiredFlag(entity.getRequiredFlag());
            item.setScope(entity.getScope());
            result.add(item);
        }
        return result;
    }

    private List<OzonProductDraftDetailView.ImageItem> toImageViews(List<OzonListingImage> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        List<OzonProductDraftDetailView.ImageItem> result = new ArrayList<>(entities.size());
        for (OzonListingImage entity : entities) {
            OzonProductDraftDetailView.ImageItem item = new OzonProductDraftDetailView.ImageItem();
            item.setSource(entity.getSource());
            item.setImageUrl(entity.getImageUrl());
            item.setSortOrder(entity.getSortOrder());
            item.setPrimary(entity.getPrimary());
            item.setScope(entity.getScope());
            result.add(item);
        }
        return result;
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? Collections.emptyList() : items;
    }

    private String trim(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private void validateAttributeItem(OzonProductDraftSaveCommand.AttributeItem item) {
        if (item == null || item.getAttributeId() == null || item.getValues() == null || item.getValues().isEmpty()) {
            throw new IllegalArgumentException("attribute payload不合法");
        }
    }

    private void validateImageItem(OzonProductDraftSaveCommand.ImageItem item) {
        if (item == null || StrUtil.isBlank(item.getImageUrl()) || StrUtil.isBlank(item.getSource())) {
            throw new IllegalArgumentException("image payload不合法");
        }
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

    private Map<String, Object> loadMaterialMap(List<String> skus) {
        Result<Map<String, Object>> result = erpClientOneFeign.findMaterialMapBySku(skus);
        if (result == null || result.getData() == null) {
            return Collections.emptyMap();
        }
        return result.getData();
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private java.math.BigDecimal asDecimal(Object value) {
        if (value == null || StrUtil.isBlank(String.valueOf(value))) {
            return null;
        }
        return new java.math.BigDecimal(String.valueOf(value));
    }

    private String nextId() {
        return IdUtil.getSnowflakeNextIdStr();
    }
}
