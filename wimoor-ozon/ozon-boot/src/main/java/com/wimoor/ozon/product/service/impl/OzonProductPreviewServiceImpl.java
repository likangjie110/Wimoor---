package com.wimoor.ozon.product.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.product.mapper.OzonListingAttributeMapper;
import com.wimoor.ozon.product.mapper.OzonListingDraftMapper;
import com.wimoor.ozon.product.mapper.OzonListingImageMapper;
import com.wimoor.ozon.product.mapper.OzonListingVariantMapper;
import com.wimoor.ozon.product.mapper.OzonProductMapMapper;
import com.wimoor.ozon.product.pojo.dto.OzonProductPreviewCommand;
import com.wimoor.ozon.product.pojo.entity.OzonListingAttribute;
import com.wimoor.ozon.product.pojo.entity.OzonListingDraft;
import com.wimoor.ozon.product.pojo.vo.OzonProductCategoryTemplateView;
import com.wimoor.ozon.product.pojo.vo.OzonProductPreviewView;
import com.wimoor.ozon.product.service.IOzonProductMetadataService;
import com.wimoor.ozon.product.service.IOzonProductPreviewService;

import cn.hutool.core.util.StrUtil;

@Service
public class OzonProductPreviewServiceImpl implements IOzonProductPreviewService {

    private final OzonProductDraftResolver draftResolver;
    private final OzonListingDraftMapper draftMapper;
    private final IOzonProductMetadataService metadataService;

    @Autowired
    public OzonProductPreviewServiceImpl(
            OzonProductDraftResolver draftResolver,
            OzonListingDraftMapper draftMapper,
            IOzonProductMetadataService metadataService
    ) {
        this.draftResolver = draftResolver;
        this.draftMapper = draftMapper;
        this.metadataService = metadataService;
    }

    public OzonProductPreviewServiceImpl(
            OzonAuthMapper authMapper,
            OzonListingDraftMapper draftMapper,
            OzonListingVariantMapper variantMapper,
            OzonListingAttributeMapper attributeMapper,
            OzonListingImageMapper imageMapper,
            OzonProductMapMapper productMapMapper,
            IOzonProductMetadataService metadataService
    ) {
        this(new OzonProductDraftResolver(
                new OzonAuthAccessService(authMapper),
                draftMapper,
                variantMapper,
                attributeMapper,
                imageMapper,
                productMapMapper
        ), draftMapper, metadataService);
    }

    @Override
    public OzonProductPreviewView preview(UserInfo user, OzonProductPreviewCommand command) {
        OzonProductDraftResolver.ResolvedDraftContext context = draftResolver.resolve(user, command.getAuthId(), command.getDraftId());
        OzonProductCategoryTemplateView template = metadataService.getTemplate(
                user,
                context.auth().getId(),
                context.draft().getDescriptionCategoryId(),
                context.draft().getTypeId()
        );
        List<String> validationErrors = new ArrayList<>();
        List<OzonProductPreviewView.VariantIssue> variantIssues = new ArrayList<>();

        validateDraftHeader(context.draft(), template, validationErrors);
        for (OzonProductCategoryTemplateView.AttributeItem attribute : safeList(template.getCommonAttributes())) {
            if (isRequired(attribute) && !containsAttribute(context.commonAttributes(), attribute.getAttributeId())) {
                validationErrors.add("缺少必填公共属性: " + attribute.getAttributeName());
            }
        }

        if (context.variants().isEmpty()) {
            validationErrors.add("至少需要一个刊登变体");
        }
        OzonProductPreviewView.EffectivePayloadSummary summary = new OzonProductPreviewView.EffectivePayloadSummary();
        summary.setDraftId(context.draft().getId());
        List<OzonProductPreviewView.EffectiveVariantSummary> variants = new ArrayList<>();
        for (OzonProductDraftResolver.ResolvedVariant variant : context.variants()) {
            variants.add(toVariantSummary(variant));
            List<String> variantMessages = validateVariant(template, variant);
            if (!variantMessages.isEmpty()) {
                OzonProductPreviewView.VariantIssue issue = new OzonProductPreviewView.VariantIssue();
                issue.setVariantId(variant.variant().getId());
                issue.setMaterialSku(variant.variant().getMaterialSku());
                issue.setMessages(variantMessages);
                variantIssues.add(issue);
                validationErrors.addAll(variantMessages);
            }
        }
        summary.setVariants(variants);
        summary.setTopLevelFields(buildTopLevelFields(variants));

        boolean canPublish = validationErrors.isEmpty() && variantIssues.isEmpty();
        updateDraftPreview(context.draft(), canPublish, validationErrors);

        OzonProductPreviewView view = new OzonProductPreviewView();
        view.setEffectivePayloadSummary(summary);
        view.setValidationErrors(validationErrors);
        view.setVariantIssues(variantIssues);
        view.setCanPublish(canPublish);
        return view;
    }

    private void validateDraftHeader(
            OzonListingDraft draft,
            OzonProductCategoryTemplateView template,
            List<String> validationErrors
    ) {
        if (draft.getDescriptionCategoryId() == null) {
            validationErrors.add("缺少 descriptionCategoryId");
        }
        if (draft.getTypeId() == null) {
            validationErrors.add("缺少 typeId");
        }
        if (template == null) {
            validationErrors.add("类目模板不可用");
        }
    }

    private List<String> validateVariant(
            OzonProductCategoryTemplateView template,
            OzonProductDraftResolver.ResolvedVariant variant
    ) {
        List<String> errors = new ArrayList<>();
        if (StrUtil.isBlank(variant.variant().getMaterialSku())) {
            errors.add("variant 缺少 ERP SKU 绑定");
        }
        if (StrUtil.isBlank(variant.effectiveOfferId())) {
            errors.add(variant.variant().getMaterialSku() + " 缺少 effective offer id");
        }
        if (variant.effectivePrice() == null) {
            errors.add(variant.variant().getMaterialSku() + " 缺少 price");
        }
        if (variant.effectiveWeight() == null) {
            errors.add(variant.variant().getMaterialSku() + " 缺少 weight");
        }
        if (variant.effectiveLength() == null || variant.effectiveWidth() == null || variant.effectiveHeight() == null) {
            errors.add(variant.variant().getMaterialSku() + " 缺少 dimensions");
        }
        if (safeList(variant.effectiveImages()).size() < template.getRequiredImageCount()) {
            errors.add(variant.variant().getMaterialSku() + " 缺少有效图片");
        }
        if (template.isRequiresBarcode() && StrUtil.isBlank(variant.effectiveBarcode())) {
            errors.add(variant.variant().getMaterialSku() + " 缺少 barcode");
        }
        for (OzonProductCategoryTemplateView.AttributeItem attribute : safeList(template.getVariantAttributes())) {
            if (isRequired(attribute) && !containsAttribute(variant.variantAttributes(), attribute.getAttributeId())) {
                errors.add(variant.variant().getMaterialSku() + " 缺少必填变体属性: " + attribute.getAttributeName());
            }
        }
        return errors;
    }

    private OzonProductPreviewView.EffectiveVariantSummary toVariantSummary(OzonProductDraftResolver.ResolvedVariant variant) {
        OzonProductPreviewView.EffectiveVariantSummary item = new OzonProductPreviewView.EffectiveVariantSummary();
        item.setVariantId(variant.variant().getId());
        item.setMaterialSku(variant.variant().getMaterialSku());
        item.setEffectiveOfferId(variant.effectiveOfferId());
        item.setEffectiveBarcode(variant.effectiveBarcode());
        item.setEffectivePrice(toText(variant.effectivePrice()));
        item.setEffectiveWeight(toText(variant.effectiveWeight()));
        OzonProductPreviewView.EffectiveDimensionSummary dimensions = new OzonProductPreviewView.EffectiveDimensionSummary();
        dimensions.setDepth(toText(variant.effectiveLength()));
        dimensions.setWidth(toText(variant.effectiveWidth()));
        dimensions.setHeight(toText(variant.effectiveHeight()));
        item.setEffectiveDimensions(dimensions);
        item.setEffectiveImageCount(safeList(variant.effectiveImages()).size());
        return item;
    }

    private OzonProductPreviewView.TopLevelFields buildTopLevelFields(List<OzonProductPreviewView.EffectiveVariantSummary> variants) {
        OzonProductPreviewView.TopLevelFields fields = new OzonProductPreviewView.TopLevelFields();
        int offerCount = 0;
        boolean hasImages = false;
        boolean hasDimensions = true;
        for (OzonProductPreviewView.EffectiveVariantSummary item : safeList(variants)) {
            if (StrUtil.isNotBlank(item.getEffectiveOfferId())) {
                offerCount++;
            }
            hasImages = hasImages || (item.getEffectiveImageCount() != null && item.getEffectiveImageCount() > 0);
            hasDimensions = hasDimensions
                    && item.getEffectiveDimensions() != null
                    && StrUtil.isNotBlank(item.getEffectiveDimensions().getDepth())
                    && StrUtil.isNotBlank(item.getEffectiveDimensions().getWidth())
                    && StrUtil.isNotBlank(item.getEffectiveDimensions().getHeight());
        }
        fields.setOfferIdCount(offerCount);
        fields.setHasImages(hasImages);
        fields.setHasDimensions(hasDimensions);
        return fields;
    }

    private void updateDraftPreview(OzonListingDraft draft, boolean canPublish, List<String> errors) {
        draft.setLastPreviewStatus(canPublish ? "READY" : "FAILED");
        draft.setLastPreviewMessage(canPublish ? "Preview ready" : firstError(errors));
        draft.setUpdateTime(new Date());
        draftMapper.updateById(draft);
    }

    private boolean containsAttribute(List<OzonListingAttribute> attributes, Long attributeId) {
        for (OzonListingAttribute attribute : safeList(attributes)) {
            if (attributeId != null && attributeId.equals(attribute.getAttributeId())
                    && !OzonProductDraftResolver.parseAttributeValues(attribute).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean isRequired(OzonProductCategoryTemplateView.AttributeItem attribute) {
        return attribute != null && !Boolean.FALSE.equals(attribute.getRequired());
    }

    private String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstError(List<String> errors) {
        return errors == null || errors.isEmpty() ? null : errors.get(0);
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? Collections.emptyList() : items;
    }
}
