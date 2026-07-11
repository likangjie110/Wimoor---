package com.wimoor.ozon.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.product.mapper.OzonListingAttributeMapper;
import com.wimoor.ozon.product.mapper.OzonListingDraftMapper;
import com.wimoor.ozon.product.mapper.OzonListingImageMapper;
import com.wimoor.ozon.product.mapper.OzonListingVariantMapper;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftArchiveCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftCloneCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftDetailQuery;
import com.wimoor.ozon.product.pojo.entity.OzonListingAttribute;
import com.wimoor.ozon.product.pojo.entity.OzonListingDraft;
import com.wimoor.ozon.product.pojo.entity.OzonListingImage;
import com.wimoor.ozon.product.pojo.entity.OzonListingVariant;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftDetailView;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OzonListingDraftLifecycleService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private final OzonAuthAccessService authAccessService;
    private final OzonListingDraftMapper draftMapper;
    private final OzonListingVariantMapper variantMapper;
    private final OzonListingAttributeMapper attributeMapper;
    private final OzonListingImageMapper imageMapper;

    @Transactional(rollbackFor = Exception.class)
    public OzonProductDraftDetailView cloneDraft(UserInfo user, OzonProductDraftCloneCommand command) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command.getAuthId());
        OzonListingDraft sourceDraft = draftMapper.selectById(command.getSourceDraftId());
        if (sourceDraft == null || !sourceDraft.getAuthId().equals(auth.getId())) {
            throw new IllegalArgumentException("源草稿不存在或无权限");
        }

        Date now = new Date();
        OzonListingDraft newDraft = new OzonListingDraft();
        newDraft.setId(IdUtil.simpleUUID());
        newDraft.setAuthId(auth.getId());
        newDraft.setShopId(auth.getShopId());
        newDraft.setDraftName(command.getNewDraftName());
        newDraft.setStatus(STATUS_DRAFT);
        newDraft.setDescriptionCategoryId(sourceDraft.getDescriptionCategoryId());
        newDraft.setDescriptionCategoryName(sourceDraft.getDescriptionCategoryName());
        newDraft.setTypeId(sourceDraft.getTypeId());
        newDraft.setTypeName(sourceDraft.getTypeName());
        newDraft.setTitleSourceValue(sourceDraft.getTitleSourceValue());
        newDraft.setTitleOverrideValue(sourceDraft.getTitleOverrideValue());
        newDraft.setBrandSourceValue(sourceDraft.getBrandSourceValue());
        newDraft.setBrandOverrideValue(sourceDraft.getBrandOverrideValue());
        newDraft.setDescriptionSourceValue(sourceDraft.getDescriptionSourceValue());
        newDraft.setDescriptionOverrideValue(sourceDraft.getDescriptionOverrideValue());
        String sourceSku = StrUtil.isNotBlank(sourceDraft.getMaterialSku()) ? sourceDraft.getMaterialSku() : sourceDraft.getDraftName();
        newDraft.setMaterialSku(StrUtil.blankToDefault(sourceSku, sourceDraft.getId()) + "-CLONE");
        newDraft.setCreateTime(now);
        newDraft.setUpdateTime(now);
        draftMapper.insert(newDraft);

        List<OzonListingAttribute> sourceAttributes = attributeMapper.selectList(
                new QueryWrapper<OzonListingAttribute>().eq("draft_id", sourceDraft.getId())
        );
        for (OzonListingAttribute sourceAttr : sourceAttributes) {
            OzonListingAttribute newAttr = new OzonListingAttribute();
            newAttr.setId(IdUtil.simpleUUID());
            newAttr.setDraftId(newDraft.getId());
            newAttr.setVariantId(sourceAttr.getVariantId());
            newAttr.setAuthId(auth.getId());
            newAttr.setShopId(auth.getShopId());
            newAttr.setAttributeId(sourceAttr.getAttributeId());
            newAttr.setAttributeName(sourceAttr.getAttributeName());
            newAttr.setAttributeValueJson(sourceAttr.getAttributeValueJson());
            newAttr.setRequiredFlag(sourceAttr.getRequiredFlag());
            newAttr.setScope(sourceAttr.getScope());
            newAttr.setCreateTime(now);
            newAttr.setUpdateTime(now);
            attributeMapper.insert(newAttr);
        }

        List<OzonListingImage> sourceImages = imageMapper.selectList(
                new QueryWrapper<OzonListingImage>().eq("draft_id", sourceDraft.getId())
        );
        for (OzonListingImage sourceImg : sourceImages) {
            OzonListingImage newImg = new OzonListingImage();
            newImg.setId(IdUtil.simpleUUID());
            newImg.setDraftId(newDraft.getId());
            newImg.setVariantId(sourceImg.getVariantId());
            newImg.setAuthId(auth.getId());
            newImg.setShopId(auth.getShopId());
            newImg.setImageUrl(sourceImg.getImageUrl());
            newImg.setImageType(sourceImg.getImageType());
            newImg.setSource(sourceImg.getSource());
            newImg.setSortOrder(sourceImg.getSortOrder());
            newImg.setPrimary(sourceImg.getPrimary());
            newImg.setScope(sourceImg.getScope());
            newImg.setCreateTime(now);
            newImg.setUpdateTime(now);
            imageMapper.insert(newImg);
        }

        List<OzonListingVariant> sourceVariants = variantMapper.selectList(
                new QueryWrapper<OzonListingVariant>().eq("draft_id", sourceDraft.getId())
        );
        for (OzonListingVariant sourceVar : sourceVariants) {
            OzonListingVariant newVar = new OzonListingVariant();
            newVar.setId(IdUtil.simpleUUID());
            newVar.setDraftId(newDraft.getId());
            newVar.setAuthId(auth.getId());
            newVar.setShopId(auth.getShopId());
            newVar.setVariantSku(sourceVar.getVariantSku() + "-CLONE");
            newVar.setMaterialName(sourceVar.getMaterialName());
            newVar.setOfferIdOverride(sourceVar.getOfferIdOverride());
            newVar.setBarcodeOverride(sourceVar.getBarcodeOverride());
            newVar.setPriceSourceValue(sourceVar.getPriceSourceValue());
            newVar.setPrice(sourceVar.getPrice());
            newVar.setOldPrice(sourceVar.getOldPrice());
            newVar.setVat(sourceVar.getVat());
            newVar.setWeightSourceValue(sourceVar.getWeightSourceValue());
            newVar.setWeightOverrideValue(sourceVar.getWeightOverrideValue());
            newVar.setLengthSourceValue(sourceVar.getLengthSourceValue());
            newVar.setLengthOverrideValue(sourceVar.getLengthOverrideValue());
            newVar.setWidthSourceValue(sourceVar.getWidthSourceValue());
            newVar.setWidthOverrideValue(sourceVar.getWidthOverrideValue());
            newVar.setHeightSourceValue(sourceVar.getHeightSourceValue());
            newVar.setHeightOverrideValue(sourceVar.getHeightOverrideValue());
            newVar.setVariantLabel(sourceVar.getVariantLabel());
            newVar.setStatus(sourceVar.getStatus());
            newVar.setLastSyncStatus(sourceVar.getLastSyncStatus());
            newVar.setLastSyncMessage(sourceVar.getLastSyncMessage());
            newVar.setCreateTime(now);
            newVar.setUpdateTime(now);
            variantMapper.insert(newVar);
        }

        OzonProductDraftDetailView detail = new OzonProductDraftDetailView();
        detail.setDraftId(newDraft.getId());
        detail.setDraftName(newDraft.getDraftName());
        detail.setDescriptionCategoryId(newDraft.getDescriptionCategoryId());
        detail.setDescriptionCategoryName(newDraft.getDescriptionCategoryName());
        detail.setTypeId(newDraft.getTypeId());
        detail.setTypeName(newDraft.getTypeName());
        detail.setTitleSourceValue(newDraft.getTitleSourceValue());
        detail.setTitleOverrideValue(newDraft.getTitleOverrideValue());
        detail.setBrandSourceValue(newDraft.getBrandSourceValue());
        detail.setBrandOverrideValue(newDraft.getBrandOverrideValue());
        detail.setDescriptionSourceValue(newDraft.getDescriptionSourceValue());
        detail.setDescriptionOverrideValue(newDraft.getDescriptionOverrideValue());
        detail.setStatus(newDraft.getStatus());
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public void archiveDraft(UserInfo user, OzonProductDraftArchiveCommand command) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command.getAuthId());
        OzonListingDraft draft = draftMapper.selectById(command.getDraftId());
        if (draft == null || !draft.getAuthId().equals(auth.getId())) {
            throw new IllegalArgumentException("草稿不存在或无权限");
        }
        draft.setStatus(STATUS_ARCHIVED);
        draft.setUpdateTime(new Date());
        draftMapper.updateById(draft);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(UserInfo user, String authId, String draftId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        OzonListingDraft draft = draftMapper.selectById(draftId);
        if (draft == null || !draft.getAuthId().equals(auth.getId())) {
            throw new IllegalArgumentException("草稿不存在或无权限");
        }
        attributeMapper.delete(new QueryWrapper<OzonListingAttribute>().eq("draft_id", draftId));
        imageMapper.delete(new QueryWrapper<OzonListingImage>().eq("draft_id", draftId));
        variantMapper.delete(new QueryWrapper<OzonListingVariant>().eq("draft_id", draftId));
        draftMapper.deleteById(draftId);
    }

    public List<OzonListingDraft> listByStatus(UserInfo user, String authId, String status) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        QueryWrapper<OzonListingDraft> wrapper = new QueryWrapper<>();
        wrapper.eq("auth_id", auth.getId());
        wrapper.eq("shop_id", auth.getShopId());
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("update_time");
        return draftMapper.selectList(wrapper);
    }
}
