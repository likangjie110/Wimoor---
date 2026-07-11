package com.wimoor.ozon.product.service.impl;

import java.time.Clock;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozon.product.client.OzonProductApiClient;
import com.wimoor.ozon.product.client.OzonProductApiClient.AttributeTemplateItem;
import com.wimoor.ozon.product.client.OzonProductApiClient.CategoryNode;
import com.wimoor.ozon.product.client.OzonProductApiClient.DictionaryValue;
import com.wimoor.ozon.product.pojo.vo.OzonProductCategoryTemplateView;
import com.wimoor.ozon.product.pojo.vo.OzonProductCategoryTreeView;
import com.wimoor.ozon.product.service.IOzonProductMetadataService;
import com.wimoor.ozon.security.OzonCredentialService;

import cn.hutool.core.util.StrUtil;

@Service
public class OzonProductMetadataServiceImpl implements IOzonProductMetadataService {

    private static final long TTL_MILLIS = 6L * 60L * 60L * 1000L;
    private static final String MODE_TEXT = "TEXT";
    private static final String MODE_DICT = "DICT";
    private static final String MODE_MULTI_TEXT = "MULTI_TEXT";
    private static final String MODE_MULTI_DICT = "MULTI_DICT";
    private static final String DEFAULT_LANGUAGE = "DEFAULT";
    private static final String[] BARCODE_KEYWORDS = {"barcode", "bar code", "штрих", "条码"};
    private static final String[] IMAGE_KEYWORDS = {"image", "images", "photo", "photos", "изображ", "фото", "图片", "图像"};
    private static final Set<String> SUPPORTED_LANGUAGES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(DEFAULT_LANGUAGE, "RU", "EN", "TR", "ZH_HANS"))
    );
    private static final String PRODUCT_META_OBJECT_TYPE = "PRODUCT_META";
    private static final String PRODUCT_API_GROUP = "PRODUCT";
    private static final String CATEGORY_TREE_ENDPOINT = "/v1/description-category/tree";
    private static final String CATEGORY_ATTRIBUTE_ENDPOINT = "/v1/description-category/attribute";

    private final OzonAuthAccessService authAccessService;
    private final OzonProductApiClient productApiClient;
    private final OzonCredentialService credentialService;
    private final Clock clock;
    private final Map<String, CacheEntry<OzonProductCategoryTemplateView>> templateCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<OzonProductCategoryTreeView>> treeCache = new ConcurrentHashMap<>();
    private IOzonOpsService opsService = new IOzonOpsService() {
    };

    @Autowired
    public OzonProductMetadataServiceImpl(
            OzonAuthAccessService authAccessService,
            OzonProductApiClient productApiClient,
            OzonCredentialService credentialService
    ) {
        this(authAccessService, productApiClient, credentialService, Clock.systemUTC());
    }

    public OzonProductMetadataServiceImpl(
            OzonAuthMapper authMapper,
            OzonProductApiClient productApiClient,
            OzonCredentialService credentialService,
            Clock clock
    ) {
        this(new OzonAuthAccessService(authMapper), productApiClient, credentialService, clock);
    }

    public OzonProductMetadataServiceImpl(
            OzonAuthAccessService authAccessService,
            OzonProductApiClient productApiClient,
            OzonCredentialService credentialService,
            Clock clock
    ) {
        this.authAccessService = authAccessService;
        this.productApiClient = productApiClient;
        this.credentialService = credentialService;
        this.clock = clock;
    }

    @Autowired(required = false)
    public void setOpsService(IOzonOpsService opsService) {
        if (opsService != null) {
            this.opsService = opsService;
        }
    }

    @Override
    public OzonProductCategoryTreeView getCategoryTree(UserInfo user, String authId, String keyword, String language) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        String resolvedLanguage = normalizeLanguage(language);
        return filterTree(loadTree(auth, resolvedLanguage, user), keyword);
    }

    @Override
    public OzonProductCategoryTemplateView getTemplate(UserInfo user, String authId, Long descriptionCategoryId, Long typeId, String language) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        String resolvedLanguage = normalizeLanguage(language);
        String cacheKey = buildTemplateCacheKey(auth.getId(), descriptionCategoryId, typeId, resolvedLanguage);
        CacheEntry<OzonProductCategoryTemplateView> cached = templateCache.get(cacheKey);
        if (isFresh(cached)) {
            return cached.value;
        }
        String requestPayload = buildTemplateRequestPayload(descriptionCategoryId, typeId, resolvedLanguage);
        long startedAt = 0L;
        boolean requestedAttributes = false;
        try {
            loadTree(auth, resolvedLanguage, user);
            String apiKey = credentialService.decrypt(auth.getApiKeyCiphertext());
            startedAt = System.currentTimeMillis();
            requestedAttributes = true;
            List<AttributeTemplateItem> payload = productApiClient.listAttributes(
                    auth.getClientId(),
                    apiKey,
                    descriptionCategoryId,
                    typeId,
                    resolvedLanguage
            );
            recordApiLog(
                    auth,
                    user,
                    "LIST_ATTRIBUTES",
                    CATEGORY_ATTRIBUTE_ENDPOINT,
                    descriptionCategoryId + ":" + typeId,
                    requestPayload,
                    JSON.toJSONString(payload),
                    "SUCCESS",
                    null,
                    startedAt
            );
            ResolvedMeta meta = resolveMeta(auth.getId(), descriptionCategoryId, typeId, resolvedLanguage);
            OzonProductCategoryTemplateView view = normalizeTemplate(payload, meta);
            templateCache.put(cacheKey, new CacheEntry<>(view, now() + TTL_MILLIS));
            return view;
        } catch (RuntimeException ex) {
            if (requestedAttributes) {
                recordApiLog(
                        auth,
                        user,
                        "LIST_ATTRIBUTES",
                        CATEGORY_ATTRIBUTE_ENDPOINT,
                        descriptionCategoryId + ":" + typeId,
                        requestPayload,
                        null,
                        "FAILED",
                        ex.getMessage(),
                        startedAt
                );
            }
            if (cached != null && cached.value != null) {
                return cached.value;
            }
            throw ex;
        }
    }

    private OzonProductCategoryTreeView loadTree(OzonAuth auth, String language, UserInfo user) {
        String cacheKey = buildTreeCacheKey(auth.getId(), language);
        CacheEntry<OzonProductCategoryTreeView> cached = treeCache.get(cacheKey);
        if (isFresh(cached)) {
            return cached.value;
        }
        String requestPayload = buildTreeRequestPayload(language);
        long startedAt = 0L;
        try {
            String apiKey = credentialService.decrypt(auth.getApiKeyCiphertext());
            startedAt = System.currentTimeMillis();
            List<CategoryNode> payload = productApiClient.listCategoryTree(auth.getClientId(), apiKey, language);
            recordApiLog(
                    auth,
                    user,
                    "LIST_CATEGORY_TREE",
                    CATEGORY_TREE_ENDPOINT,
                    auth.getId(),
                    requestPayload,
                    JSON.toJSONString(payload),
                    "SUCCESS",
                    null,
                    startedAt
            );
            OzonProductCategoryTreeView tree = normalizeTree(payload);
            treeCache.put(cacheKey, new CacheEntry<>(tree, now() + TTL_MILLIS));
            return tree;
        } catch (RuntimeException ex) {
            recordApiLog(
                    auth,
                    user,
                    "LIST_CATEGORY_TREE",
                    CATEGORY_TREE_ENDPOINT,
                    auth.getId(),
                    requestPayload,
                    null,
                    "FAILED",
                    ex.getMessage(),
                    startedAt
            );
            if (cached != null && cached.value != null) {
                return cached.value;
            }
            throw ex;
        }
    }

    private OzonProductCategoryTemplateView normalizeTemplate(List<AttributeTemplateItem> payload, ResolvedMeta meta) {
        List<OzonProductCategoryTemplateView.AttributeItem> common = new ArrayList<>();
        List<OzonProductCategoryTemplateView.AttributeItem> variant = new ArrayList<>();
        int requiredImageCount = 0;
        boolean requiresBarcode = false;
        for (AttributeTemplateItem item : safeList(payload)) {
            if (isBarcodeAttribute(item)) {
                requiresBarcode = requiresBarcode || Boolean.TRUE.equals(item.getIsRequired());
                continue;
            }
            if (isImageAttribute(item)) {
                requiredImageCount = Math.max(requiredImageCount, resolveImageCount(item));
                continue;
            }
            if (Boolean.TRUE.equals(item.getIsAspect())) {
                variant.add(toAttributeItem(item));
            } else {
                common.add(toAttributeItem(item));
            }
        }
        OzonProductCategoryTemplateView view = new OzonProductCategoryTemplateView();
        view.setDescriptionCategoryId(meta.descriptionCategoryId);
        view.setDescriptionCategoryName(meta.descriptionCategoryName);
        view.setTypeId(meta.typeId);
        view.setTypeName(meta.typeName);
        view.setCommonAttributes(common);
        view.setVariantAttributes(variant);
        view.setRequiredImageCount(requiredImageCount);
        view.setRequiresBarcode(requiresBarcode);
        return view;
    }

    private OzonProductCategoryTemplateView.AttributeItem toAttributeItem(AttributeTemplateItem item) {
        OzonProductCategoryTemplateView.AttributeItem result = new OzonProductCategoryTemplateView.AttributeItem();
        result.setAttributeId(item.getId());
        result.setAttributeName(item.getName());
        result.setMode(resolveMode(item));
        result.setRequired(Boolean.TRUE.equals(item.getIsRequired()));
        result.setValues(toAttributeValues(item));
        return result;
    }

    private List<OzonProductCategoryTemplateView.AttributeValue> toAttributeValues(AttributeTemplateItem item) {
        List<OzonProductCategoryTemplateView.AttributeValue> result = new ArrayList<>();
        if (item.getDictionaryId() != null && item.getDictionaryId() > 0) {
            for (DictionaryValue value : safeList(item.getDictionaryValues())) {
                OzonProductCategoryTemplateView.AttributeValue row = new OzonProductCategoryTemplateView.AttributeValue();
                row.setDictionaryValueId(value.getDictionaryValueId());
                row.setText(value.getText());
                result.add(row);
            }
            return result;
        }
        for (String text : safeList(item.getSampleTexts())) {
            OzonProductCategoryTemplateView.AttributeValue row = new OzonProductCategoryTemplateView.AttributeValue();
            row.setText(text);
            result.add(row);
        }
        return result;
    }

    private OzonProductCategoryTreeView normalizeTree(List<CategoryNode> payload) {
        Map<Long, OzonProductCategoryTreeView.CategoryItem> categories = new LinkedHashMap<>();
        for (CategoryNode node : safeList(payload)) {
            collectLeafCategories(node, categories);
        }
        OzonProductCategoryTreeView result = new OzonProductCategoryTreeView();
        result.setCategories(new ArrayList<>(categories.values()));
        return result;
    }

    private void collectLeafCategories(CategoryNode node, Map<Long, OzonProductCategoryTreeView.CategoryItem> categories) {
        if (node == null) {
            return;
        }
        if (node.getTypeId() != null && node.getDescriptionCategoryId() != null) {
            OzonProductCategoryTreeView.CategoryItem category = categories.computeIfAbsent(
                    node.getDescriptionCategoryId(),
                    key -> newCategoryItem(node.getDescriptionCategoryId(), node.getCategoryName())
            );
            category.getTypes().add(newTypeItem(node.getTypeId(), node.getTypeName()));
            return;
        }
        for (CategoryNode child : safeList(node.getChildren())) {
            collectLeafCategories(carryCategory(node, child), categories);
        }
    }

    private CategoryNode carryCategory(CategoryNode parent, CategoryNode child) {
        if (child == null) {
            return null;
        }
        if (child.getDescriptionCategoryId() == null) {
            return CategoryNode.builder()
                    .descriptionCategoryId(parent.getDescriptionCategoryId())
                    .categoryName(parent.getCategoryName())
                    .typeId(child.getTypeId())
                    .typeName(child.getTypeName())
                    .children(child.getChildren())
                    .build();
        }
        if (!shouldPreferParentCategoryName(parent, child)) {
            return child;
        }
        return CategoryNode.builder()
                .descriptionCategoryId(child.getDescriptionCategoryId())
                .categoryName(parent.getCategoryName())
                .typeId(child.getTypeId())
                .typeName(child.getTypeName())
                .children(child.getChildren())
                .build();
    }

    private boolean shouldPreferParentCategoryName(CategoryNode parent, CategoryNode child) {
        return parent != null
                && child.getTypeId() != null
                && parent.getDescriptionCategoryId() != null
                && Objects.equals(parent.getDescriptionCategoryId(), child.getDescriptionCategoryId())
                && StrUtil.isNotBlank(parent.getCategoryName());
    }

    private OzonProductCategoryTreeView filterTree(OzonProductCategoryTreeView source, String keyword) {
        if (source == null || StrUtil.isBlank(keyword)) {
            return source;
        }
        String match = keyword.trim().toLowerCase(Locale.ROOT);
        List<OzonProductCategoryTreeView.CategoryItem> categories = new ArrayList<>();
        for (OzonProductCategoryTreeView.CategoryItem category : safeList(source.getCategories())) {
            boolean categoryMatched = contains(category.getDescriptionCategoryName(), match);
            List<OzonProductCategoryTreeView.TypeItem> types = new ArrayList<>();
            for (OzonProductCategoryTreeView.TypeItem type : safeList(category.getTypes())) {
                if (categoryMatched || contains(type.getTypeName(), match)) {
                    types.add(type);
                }
            }
            if (categoryMatched || !types.isEmpty()) {
                OzonProductCategoryTreeView.CategoryItem item = newCategoryItem(
                        category.getDescriptionCategoryId(),
                        category.getDescriptionCategoryName()
                );
                item.setTypes(types);
                categories.add(item);
            }
        }
        OzonProductCategoryTreeView result = new OzonProductCategoryTreeView();
        result.setCategories(categories);
        return result;
    }

    private ResolvedMeta resolveMeta(String authId, Long descriptionCategoryId, Long typeId, String language) {
        CacheEntry<OzonProductCategoryTreeView> cached = treeCache.get(buildTreeCacheKey(authId, language));
        if (cached == null || cached.value == null) {
            return new ResolvedMeta(descriptionCategoryId, null, typeId, null);
        }
        for (OzonProductCategoryTreeView.CategoryItem category : safeList(cached.value.getCategories())) {
            if (!descriptionCategoryId.equals(category.getDescriptionCategoryId())) {
                continue;
            }
            for (OzonProductCategoryTreeView.TypeItem type : safeList(category.getTypes())) {
                if (typeId.equals(type.getTypeId())) {
                    return new ResolvedMeta(descriptionCategoryId, category.getDescriptionCategoryName(), typeId, type.getTypeName());
                }
            }
        }
        return new ResolvedMeta(descriptionCategoryId, null, typeId, null);
    }

    private OzonProductCategoryTreeView.CategoryItem newCategoryItem(Long categoryId, String categoryName) {
        OzonProductCategoryTreeView.CategoryItem item = new OzonProductCategoryTreeView.CategoryItem();
        item.setDescriptionCategoryId(categoryId);
        item.setDescriptionCategoryName(categoryName);
        item.setTypes(new ArrayList<>());
        return item;
    }

    private OzonProductCategoryTreeView.TypeItem newTypeItem(Long typeId, String typeName) {
        OzonProductCategoryTreeView.TypeItem item = new OzonProductCategoryTreeView.TypeItem();
        item.setTypeId(typeId);
        item.setTypeName(typeName);
        return item;
    }

    private String buildTreeCacheKey(String authId, String language) {
        return authId + ":" + language;
    }

    private String buildTemplateCacheKey(String authId, Long descriptionCategoryId, Long typeId, String language) {
        return authId + ":" + descriptionCategoryId + ":" + typeId + ":" + language;
    }

    private String normalizeLanguage(String language) {
        if (StrUtil.isBlank(language)) {
            return DEFAULT_LANGUAGE;
        }
        String value = language.trim().toUpperCase(Locale.ROOT);
        return SUPPORTED_LANGUAGES.contains(value) ? value : DEFAULT_LANGUAGE;
    }

    private boolean isFresh(CacheEntry<?> entry) {
        return entry != null && entry.value != null && entry.expiresAt >= now();
    }

    private boolean isBarcodeAttribute(AttributeTemplateItem item) {
        return containsAny(item == null ? null : item.getName(), BARCODE_KEYWORDS);
    }

    private boolean isImageAttribute(AttributeTemplateItem item) {
        return containsAny(item == null ? null : item.getType(), IMAGE_KEYWORDS)
                || containsAny(item == null ? null : item.getName(), IMAGE_KEYWORDS);
    }

    private int resolveImageCount(AttributeTemplateItem item) {
        return item.getMaxValueCount() == null || item.getMaxValueCount() < 1 ? 1 : item.getMaxValueCount();
    }

    private String resolveMode(AttributeTemplateItem item) {
        boolean dict = item.getDictionaryId() != null && item.getDictionaryId() > 0;
        boolean multi = Boolean.TRUE.equals(item.getIsCollection());
        if (dict) {
            return multi ? MODE_MULTI_DICT : MODE_DICT;
        }
        return multi ? MODE_MULTI_TEXT : MODE_TEXT;
    }

    private boolean contains(String text, String keyword) {
        return StrUtil.isNotBlank(text) && text.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean containsAny(String text, String... keywords) {
        if (StrUtil.isBlank(text) || keywords == null) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (StrUtil.isNotBlank(keyword) && normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private long now() {
        return clock.millis();
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? Collections.emptyList() : items;
    }

    private String buildTreeRequestPayload(String language) {
        JSONObject payload = new JSONObject();
        if (StrUtil.isNotBlank(language) && !DEFAULT_LANGUAGE.equals(language)) {
            payload.put("language", language);
        }
        return payload.toJSONString();
    }

    private String buildTemplateRequestPayload(Long descriptionCategoryId, Long typeId, String language) {
        JSONObject payload = new JSONObject();
        payload.put("descriptionCategoryId", descriptionCategoryId);
        payload.put("typeId", typeId);
        payload.put("language", language);
        return payload.toJSONString();
    }

    private void recordApiLog(
            OzonAuth auth,
            UserInfo user,
            String actionName,
            String endpoint,
            String objectId,
            String requestPayload,
            String responsePayload,
            String status,
            String errorMessage,
            long startedAt
    ) {
        long duration = startedAt <= 0 ? 0L : Math.max(System.currentTimeMillis() - startedAt, 0L);
        opsService.recordApiLog(new OzonApiLogRecordCommand(
                auth.getId(),
                auth.getShopId(),
                PRODUCT_API_GROUP,
                actionName,
                endpoint,
                "POST",
                PRODUCT_META_OBJECT_TYPE,
                objectId,
                requestPayload,
                responsePayload,
                status,
                errorMessage,
                duration,
                user == null ? null : user.getId()
        ));
    }

    private static final class CacheEntry<T> {
        private final T value;
        private final long expiresAt;

        private CacheEntry(T value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }

    private static final class ResolvedMeta {
        private final Long descriptionCategoryId;
        private final String descriptionCategoryName;
        private final Long typeId;
        private final String typeName;

        private ResolvedMeta(Long descriptionCategoryId, String descriptionCategoryName, Long typeId, String typeName) {
            this.descriptionCategoryId = descriptionCategoryId;
            this.descriptionCategoryName = descriptionCategoryName;
            this.typeId = typeId;
            this.typeName = typeName;
        }
    }
}
