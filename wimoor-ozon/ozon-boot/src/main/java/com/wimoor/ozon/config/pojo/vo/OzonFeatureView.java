package com.wimoor.ozon.config.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OZON 功能开关视图
 * 用于前端感知后端功能开关状态
 *
 * @author Development Team
 * @since 2026-06-25
 */
@Data
@NoArgsConstructor
public class OzonFeatureView {

    private FeatureItem auth;
    private FeatureItem product;
    private FeatureItem productWrite;
    private FeatureItem task;
    private FeatureItem error;
    private FeatureItem finance;
    private FeatureItem financeSync;
    private FeatureItem chat;
    private FeatureItem chatSync;
    private FeatureItem ads;
    private FeatureItem stockWrite;
    private FeatureItem priceWrite;
    private FeatureItem postingWrite;
    private FeatureItem chatSend;
    private FeatureItem adsSync;

    /**
     * 转换为 Map 格式，方便前端使用
     */
    public Map<String, FeatureItem> toMap() {
        Map<String, FeatureItem> map = new LinkedHashMap<>();
        map.put("auth", auth);
        map.put("product", product);
        map.put("productWrite", productWrite);
        map.put("task", task);
        map.put("error", error);
        map.put("finance", finance);
        map.put("financeSync", financeSync);
        map.put("chat", chat);
        map.put("chatSync", chatSync);
        map.put("ads", ads);
        map.put("stockWrite", stockWrite);
        map.put("priceWrite", priceWrite);
        map.put("postingWrite", postingWrite);
        map.put("chatSend", chatSend);
        map.put("adsSync", adsSync);
        return map;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureItem {
        /**
         * 是否启用
         */
        private boolean enabled;

        /**
         * 禁用原因（仅当 enabled=false 时有值）
         */
        private String reason;

        /**
         * 功能名称（中文）
         */
        private String name;

        /**
         * 功能描述
         */
        private String description;

        /**
         * 功能类别: read, write, sync
         */
        private String category;

        /**
         * 兼容旧版本的构造函数
         */
        public FeatureItem(boolean enabled, String reason) {
            this.enabled = enabled;
            this.reason = reason;
        }
    }
}
