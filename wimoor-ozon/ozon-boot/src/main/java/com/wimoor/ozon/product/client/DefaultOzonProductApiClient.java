package com.wimoor.ozon.product.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wimoor.common.HttpClientUtil;

import cn.hutool.core.util.StrUtil;

@Component
public class DefaultOzonProductApiClient implements OzonProductApiClient {

    private static final String CATEGORY_TREE_PATH = "/v1/description-category/tree";
    private static final String CATEGORY_ATTRIBUTE_PATH = "/v1/description-category/attribute";
    private static final String PRODUCT_IMPORT_PATH = "/v3/product/import";
    private static final String PRODUCT_IMPORT_INFO_PATH = "/v1/product/import/info";

    private final String baseUrl;

    public DefaultOzonProductApiClient(@Value("${ozon.api.base-url:https://api-seller.ozon.ru}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public List<CategoryNode> listCategoryTree(String clientId, String apiKey, String language) {
        try {
            JSONObject request = new JSONObject();
            if (StrUtil.isNotBlank(language)) {
                request.put("language", language);
            }
            JSONArray result = extractResultArray(postJson(CATEGORY_TREE_PATH, request.toJSONString(), clientId, apiKey));
            if (result == null) {
                return Collections.emptyList();
            }
            List<CategoryNode> categories = new ArrayList<>(result.size());
            for (int index = 0; index < result.size(); index++) {
                categories.add(toCategoryNode(result.getJSONObject(index)));
            }
            return categories;
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public List<AttributeTemplateItem> listAttributes(String clientId, String apiKey, Long descriptionCategoryId, Long typeId, String language) {
        try {
            JSONObject request = new JSONObject();
            request.put("description_category_id", descriptionCategoryId);
            request.put("type_id", typeId);
            if (StrUtil.isNotBlank(language)) {
                request.put("language", language);
            }
            JSONArray result = extractResultArray(postJson(CATEGORY_ATTRIBUTE_PATH, request.toJSONString(), clientId, apiKey));
            if (result == null) {
                return Collections.emptyList();
            }
            List<AttributeTemplateItem> attributes = new ArrayList<>(result.size());
            for (int index = 0; index < result.size(); index++) {
                attributes.add(toAttributeItem(result.getJSONObject(index)));
            }
            return attributes;
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public String submitProductImport(String clientId, String apiKey, String requestPayloadJson) {
        try {
            JSONObject payload = JSONObject.parseObject(postJson(PRODUCT_IMPORT_PATH, requestPayloadJson, clientId, apiKey));
            JSONObject result = payload == null ? null : payload.getJSONObject("result");
            String taskId = result == null ? null : String.valueOf(result.get("task_id"));
            if (StrUtil.isBlank(taskId) || "null".equalsIgnoreCase(taskId)) {
                throw new IllegalStateException("ozon product import did not return task_id");
            }
            return taskId;
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public ProductImportInfo getProductImportInfo(String clientId, String apiKey, String remoteTaskId) {
        try {
            JSONObject request = new JSONObject();
            request.put("task_id", Long.valueOf(remoteTaskId));
            JSONObject payload = JSONObject.parseObject(postJson(PRODUCT_IMPORT_INFO_PATH, request.toJSONString(), clientId, apiKey));
            JSONObject result = payload == null ? null : payload.getJSONObject("result");
            JSONArray items = result == null ? null : result.getJSONArray("items");
            if (items == null) {
                return new ProductImportInfo(Collections.emptyList());
            }
            List<ProductImportItem> normalized = new ArrayList<>(items.size());
            for (int index = 0; index < items.size(); index++) {
                JSONObject item = items.getJSONObject(index);
                normalized.add(new ProductImportItem(
                        item == null ? null : item.getString("offer_id"),
                        item == null || item.get("product_id") == null ? null : String.valueOf(item.get("product_id")),
                        item == null ? null : item.getString("status"),
                        toImportErrors(item == null ? null : item.getJSONArray("errors"))
                ));
            }
            return new ProductImportInfo(normalized);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    private JSONArray extractResultArray(String body) {
        if (StrUtil.isBlank(body)) {
            return null;
        }
        JSONObject payload = JSONObject.parseObject(body);
        return payload == null ? null : payload.getJSONArray("result");
    }

    private CategoryNode toCategoryNode(JSONObject item) {
        List<CategoryNode> children = new ArrayList<>();
        JSONArray childPayload = item == null ? null : item.getJSONArray("children");
        if (childPayload != null) {
            for (int index = 0; index < childPayload.size(); index++) {
                children.add(toCategoryNode(childPayload.getJSONObject(index)));
            }
        }
        return CategoryNode.builder()
                .descriptionCategoryId(item == null ? null : item.getLong("description_category_id"))
                .categoryName(item == null ? null : item.getString("category_name"))
                .typeId(item == null ? null : item.getLong("type_id"))
                .typeName(item == null ? null : item.getString("type_name"))
                .children(children)
                .build();
    }

    private AttributeTemplateItem toAttributeItem(JSONObject item) {
        return AttributeTemplateItem.builder()
                .id(item == null ? null : item.getLong("id"))
                .name(item == null ? null : item.getString("name"))
                .description(item == null ? null : item.getString("description"))
                .type(item == null ? null : item.getString("type"))
                .isAspect(item != null && Boolean.TRUE.equals(item.getBoolean("is_aspect")))
                .isCollection(item != null && Boolean.TRUE.equals(item.getBoolean("is_collection")))
                .isRequired(item != null && Boolean.TRUE.equals(item.getBoolean("is_required")))
                .dictionaryId(item == null ? null : item.getLong("dictionary_id"))
                .maxValueCount(item == null ? null : item.getInteger("max_value_count"))
                .dictionaryValues(resolveDictionaryValues(item))
                .sampleTexts(resolveSampleTexts(item))
                .build();
    }

    private List<DictionaryValue> resolveDictionaryValues(JSONObject item) {
        JSONArray values = item == null ? null : item.getJSONArray("dictionary_values");
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<DictionaryValue> result = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            JSONObject value = values.getJSONObject(index);
            result.add(DictionaryValue.builder()
                    .dictionaryValueId(value == null ? null : value.getLong("id"))
                    .text(firstText(value, "value", "name"))
                    .build());
        }
        return result;
    }

    private List<String> resolveSampleTexts(JSONObject item) {
        JSONArray examples = item == null ? null : item.getJSONArray("examples");
        if (examples == null || examples.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(examples.size());
        for (int index = 0; index < examples.size(); index++) {
            JSONObject example = examples.getJSONObject(index);
            String text = firstText(example, "value", "text");
            if (StrUtil.isNotBlank(text)) {
                result.add(text.trim());
            }
        }
        return result;
    }

    private List<ProductImportError> toImportErrors(JSONArray errors) {
        if (errors == null || errors.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProductImportError> result = new ArrayList<>(errors.size());
        for (int index = 0; index < errors.size(); index++) {
            JSONObject item = errors.getJSONObject(index);
            result.add(new ProductImportError(
                    item == null ? null : item.getString("code"),
                    item == null ? null : item.getString("field"),
                    item == null ? null : item.getLong("attribute_id"),
                    item == null ? null : item.getString("attribute_name"),
                    firstText(item, "message", "description")
            ));
        }
        return result;
    }

    private String firstText(JSONObject item, String... keys) {
        for (String key : keys) {
            String value = item == null ? null : item.getString(key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private Map<String, String> buildHeaders(String clientId, String apiKey) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Client-Id", clientId);
        headers.put("Api-Key", apiKey);
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private String postJson(String path, String payload, String clientId, String apiKey) throws HttpException {
        return HttpClientUtil.postUrl(buildUrl(path), payload, buildHeaders(clientId, apiKey));
    }

    private String buildUrl(String path) {
        return StrUtil.removeSuffix(baseUrl, "/") + path;
    }
}
