package com.wimoor.ozon.product.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.product.mapper.OzonListingAttributeMapper;
import com.wimoor.ozon.product.mapper.OzonListingDraftMapper;
import com.wimoor.ozon.product.mapper.OzonListingImageMapper;
import com.wimoor.ozon.product.mapper.OzonListingVariantMapper;
import com.wimoor.ozon.product.mapper.OzonProductMapMapper;
import com.wimoor.ozon.product.pojo.entity.OzonListingAttribute;
import com.wimoor.ozon.product.pojo.entity.OzonListingDraft;
import com.wimoor.ozon.product.pojo.entity.OzonListingImage;
import com.wimoor.ozon.product.pojo.entity.OzonListingVariant;
import com.wimoor.ozon.product.pojo.entity.OzonProductMap;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class OzonProductDraftResolver {

    private final OzonAuthAccessService authAccessService;
    private final OzonListingDraftMapper draftMapper;
    private final OzonListingVariantMapper variantMapper;
    private final OzonListingAttributeMapper attributeMapper;
    private final OzonListingImageMapper imageMapper;
    private final OzonProductMapMapper productMapMapper;

    ResolvedDraftContext resolve(UserInfo user, String authId, String draftId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        OzonListingDraft draft = draftMapper.selectByAuthIdAndDraftId(auth.getId(), draftId);
        if (draft == null) {
            throw new IllegalArgumentException("Ozon刊登草稿不存在");
        }
        List<OzonListingVariant> variants = safeList(variantMapper.listByDraftId(draft.getId()));
        List<String> skus = new ArrayList<>();
        for (OzonListingVariant variant : variants) {
            if (StrUtil.isNotBlank(variant.getMaterialSku())) {
                skus.add(variant.getMaterialSku());
            }
        }
        Map<String, OzonProductMap> mapBySku = new LinkedHashMap<>();
        if (!skus.isEmpty()) {
            for (OzonProductMap map : safeList(productMapMapper.listByMaterialSkus(auth.getId(), skus))) {
                mapBySku.put(map.getMaterialSku(), map);
            }
        }
        List<OzonListingAttribute> commonAttributes = attributeMapper == null
                ? Collections.emptyList()
                : safeList(attributeMapper.listByDraftIdAndVariantId(draft.getId(), null));
        List<OzonListingImage> commonImages = imageMapper == null
                ? Collections.emptyList()
                : sortImages(imageMapper.listByDraftIdAndVariantId(draft.getId(), null));
        List<ResolvedVariant> resolved = new ArrayList<>();
        for (OzonListingVariant variant : variants) {
            List<OzonListingAttribute> variantAttributes = attributeMapper == null
                    ? Collections.emptyList()
                    : safeList(attributeMapper.listByDraftIdAndVariantId(draft.getId(), variant.getId()));
            List<OzonListingImage> variantImages = imageMapper == null
                    ? Collections.emptyList()
                    : sortImages(imageMapper.listByDraftIdAndVariantId(draft.getId(), variant.getId()));
            resolved.add(buildVariant(draft, variant, mapBySku.get(variant.getMaterialSku()), commonAttributes, commonImages, variantAttributes, variantImages));
        }
        return new ResolvedDraftContext(auth, draft, commonAttributes, commonImages, mapBySku, resolved);
    }

    private ResolvedVariant buildVariant(
            OzonListingDraft draft,
            OzonListingVariant variant,
            OzonProductMap productMap,
            List<OzonListingAttribute> commonAttributes,
            List<OzonListingImage> commonImages,
            List<OzonListingAttribute> variantAttributes,
            List<OzonListingImage> variantImages
    ) {
        List<String> effectiveImages = new ArrayList<>();
        if (!variantImages.isEmpty()) {
            for (OzonListingImage image : variantImages) {
                if (StrUtil.isNotBlank(image.getImageUrl())) {
                    effectiveImages.add(image.getImageUrl().trim());
                }
            }
        } else if (!commonImages.isEmpty()) {
            for (OzonListingImage image : commonImages) {
                if (StrUtil.isNotBlank(image.getImageUrl())) {
                    effectiveImages.add(image.getImageUrl().trim());
                }
            }
        } else if (productMap != null && StrUtil.isNotBlank(productMap.getImage())) {
            effectiveImages.add(productMap.getImage().trim());
        }
        return new ResolvedVariant(
                variant,
                firstText(variant.getOfferIdOverride(), productMap == null ? null : productMap.getOzonOfferId(), variant.getMaterialSku()),
                trim(variant.getBarcodeOverride()),
                firstDecimal(variant.getPriceOverride(), variant.getPriceSourceValue(), productMap == null ? null : productMap.getMaterialPrice()),
                firstDecimal(variant.getWeightOverrideValue(), variant.getWeightSourceValue()),
                firstDecimal(variant.getLengthOverrideValue(), variant.getLengthSourceValue()),
                firstDecimal(variant.getWidthOverrideValue(), variant.getWidthSourceValue()),
                firstDecimal(variant.getHeightOverrideValue(), variant.getHeightSourceValue()),
                firstText(draft.getTitleOverrideValue(), draft.getTitleSourceValue(), variant.getMaterialName(), variant.getMaterialSku()),
                commonAttributes,
                variantAttributes,
                effectiveImages
        );
    }

    private List<OzonListingImage> sortImages(List<OzonListingImage> images) {
        List<OzonListingImage> result = new ArrayList<>(safeList(images));
        result.sort(Comparator.comparing(OzonListingImage::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(OzonListingImage::getId, Comparator.nullsLast(String::compareTo)));
        return result;
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? Collections.emptyList() : items;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String trim(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private BigDecimal firstDecimal(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    static List<Map<String, Object>> parseAttributeValues(OzonListingAttribute attribute) {
        if (attribute == null || StrUtil.isBlank(attribute.getAttributeValueJson())) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> values = JSON.parseObject(attribute.getAttributeValueJson(), new TypeReference<List<Map<String, Object>>>() { });
        return values == null ? Collections.emptyList() : values;
    }

    static final class ResolvedDraftContext {

        private final OzonAuth auth;
        private final OzonListingDraft draft;
        private final List<OzonListingAttribute> commonAttributes;
        private final List<OzonListingImage> commonImages;
        private final Map<String, OzonProductMap> productMapBySku;
        private final List<ResolvedVariant> variants;

        private ResolvedDraftContext(
                OzonAuth auth,
                OzonListingDraft draft,
                List<OzonListingAttribute> commonAttributes,
                List<OzonListingImage> commonImages,
                Map<String, OzonProductMap> productMapBySku,
                List<ResolvedVariant> variants
        ) {
            this.auth = auth;
            this.draft = draft;
            this.commonAttributes = commonAttributes;
            this.commonImages = commonImages;
            this.productMapBySku = productMapBySku;
            this.variants = variants;
        }

        OzonAuth auth() {
            return auth;
        }

        OzonListingDraft draft() {
            return draft;
        }

        List<OzonListingAttribute> commonAttributes() {
            return commonAttributes;
        }

        List<OzonListingImage> commonImages() {
            return commonImages;
        }

        Map<String, OzonProductMap> productMapBySku() {
            return productMapBySku;
        }

        List<ResolvedVariant> variants() {
            return variants;
        }
    }

    static final class ResolvedVariant {

        private final OzonListingVariant variant;
        private final String effectiveOfferId;
        private final String effectiveBarcode;
        private final BigDecimal effectivePrice;
        private final BigDecimal effectiveWeight;
        private final BigDecimal effectiveLength;
        private final BigDecimal effectiveWidth;
        private final BigDecimal effectiveHeight;
        private final String effectiveName;
        private final List<OzonListingAttribute> commonAttributes;
        private final List<OzonListingAttribute> variantAttributes;
        private final List<String> effectiveImages;

        private ResolvedVariant(
                OzonListingVariant variant,
                String effectiveOfferId,
                String effectiveBarcode,
                BigDecimal effectivePrice,
                BigDecimal effectiveWeight,
                BigDecimal effectiveLength,
                BigDecimal effectiveWidth,
                BigDecimal effectiveHeight,
                String effectiveName,
                List<OzonListingAttribute> commonAttributes,
                List<OzonListingAttribute> variantAttributes,
                List<String> effectiveImages
        ) {
            this.variant = variant;
            this.effectiveOfferId = effectiveOfferId;
            this.effectiveBarcode = effectiveBarcode;
            this.effectivePrice = effectivePrice;
            this.effectiveWeight = effectiveWeight;
            this.effectiveLength = effectiveLength;
            this.effectiveWidth = effectiveWidth;
            this.effectiveHeight = effectiveHeight;
            this.effectiveName = effectiveName;
            this.commonAttributes = commonAttributes;
            this.variantAttributes = variantAttributes;
            this.effectiveImages = effectiveImages;
        }

        OzonListingVariant variant() {
            return variant;
        }

        String effectiveOfferId() {
            return effectiveOfferId;
        }

        String effectiveBarcode() {
            return effectiveBarcode;
        }

        BigDecimal effectivePrice() {
            return effectivePrice;
        }

        BigDecimal effectiveWeight() {
            return effectiveWeight;
        }

        BigDecimal effectiveLength() {
            return effectiveLength;
        }

        BigDecimal effectiveWidth() {
            return effectiveWidth;
        }

        BigDecimal effectiveHeight() {
            return effectiveHeight;
        }

        String effectiveName() {
            return effectiveName;
        }

        List<OzonListingAttribute> commonAttributes() {
            return commonAttributes;
        }

        List<OzonListingAttribute> variantAttributes() {
            return variantAttributes;
        }

        List<String> effectiveImages() {
            return effectiveImages;
        }
    }
}
