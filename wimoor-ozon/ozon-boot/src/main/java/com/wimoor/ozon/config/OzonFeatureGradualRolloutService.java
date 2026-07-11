package com.wimoor.ozon.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ozon 功能灰度发布服务
 *
 * 支持多种灰度策略：
 * 1. 按 ShopId 白名单灰度
 * 2. 按百分比灰度
 * 3. 全量开放
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OzonFeatureGradualRolloutService {

    /**
     * 灰度策略类型
     */
    public enum RolloutStrategy {
        /** 白名单策略 */
        WHITELIST,
        /** 百分比策略 */
        PERCENTAGE,
        /** 全量开放 */
        ALL,
        /** 全部关闭 */
        NONE
    }

    /**
     * 灰度配置
     */
    public static class RolloutConfig {
        private String featureKey;
        private RolloutStrategy strategy;
        private String rolloutValue; // 白名单（逗号分隔）或百分比（0-100）
        private boolean enabled;

        public RolloutConfig(String featureKey, RolloutStrategy strategy, String rolloutValue, boolean enabled) {
            this.featureKey = featureKey;
            this.strategy = strategy;
            this.rolloutValue = rolloutValue;
            this.enabled = enabled;
        }

        public String getFeatureKey() {
            return featureKey;
        }

        public RolloutStrategy getStrategy() {
            return strategy;
        }

        public String getRolloutValue() {
            return rolloutValue;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    /**
     * 检查功能是否对指定 ShopId 开放
     *
     * @param featureKey 功能键
     * @param shopId     店铺 ID
     * @param config     灰度配置
     * @return true 表示开放，false 表示未开放
     */
    public boolean isEnabledForShop(String featureKey, String shopId, RolloutConfig config) {
        if (!config.isEnabled()) {
            log.debug("Feature {} is disabled globally", featureKey);
            return false;
        }

        switch (config.getStrategy()) {
            case ALL:
                log.debug("Feature {} is enabled for all shops", featureKey);
                return true;

            case NONE:
                log.debug("Feature {} is disabled for all shops", featureKey);
                return false;

            case WHITELIST:
                boolean inWhitelist = isInWhitelist(featureKey, shopId, config.getRolloutValue());
                log.debug("Feature {} whitelist check for shop {}: {}", featureKey, shopId, inWhitelist);
                return inWhitelist;

            case PERCENTAGE:
                boolean inPercentage = isInPercentage(featureKey, shopId, config.getRolloutValue());
                log.debug("Feature {} percentage check for shop {}: {}", featureKey, shopId, inPercentage);
                return inPercentage;

            default:
                log.warn("Unknown rollout strategy {} for feature {}", config.getStrategy(), featureKey);
                return false;
        }
    }

    /**
     * 检查 ShopId 是否在白名单中
     *
     * @param featureKey   功能键
     * @param shopId       店铺 ID
     * @param whitelistStr 白名单字符串（逗号分隔）
     * @return true 表示在白名单中
     */
    public boolean isInWhitelist(String featureKey, String shopId, String whitelistStr) {
        if (whitelistStr == null || whitelistStr.trim().isEmpty()) {
            return false;
        }

        Set<String> whitelist = new HashSet<>(Arrays.asList(whitelistStr.split(",")));
        return whitelist.contains(shopId.trim());
    }

    /**
     * 检查 ShopId 是否在百分比范围内（基于哈希）
     *
     * @param featureKey    功能键
     * @param shopId        店铺 ID
     * @param percentageStr 百分比字符串（0-100）
     * @return true 表示在百分比范围内
     */
    public boolean isInPercentage(String featureKey, String shopId, String percentageStr) {
        try {
            int percentage = Integer.parseInt(percentageStr.trim());
            if (percentage < 0 || percentage > 100) {
                log.warn("Invalid percentage {} for feature {}, must be 0-100", percentage, featureKey);
                return false;
            }

            if (percentage == 0) {
                return false;
            }

            if (percentage == 100) {
                return true;
            }

            // 使用 ShopId 和 FeatureKey 的哈希值计算百分比
            // 确保同一个 ShopId 对同一个 Feature 的结果是稳定的
            String hashInput = featureKey + ":" + shopId;
            int hash = Math.abs(hashInput.hashCode());
            int bucket = hash % 100;

            return bucket < percentage;

        } catch (NumberFormatException e) {
            log.warn("Invalid percentage format {} for feature {}", percentageStr, featureKey, e);
            return false;
        }
    }

    /**
     * 获取灰度配置的描述信息
     *
     * @param config 灰度配置
     * @return 描述信息
     */
    public String getConfigDescription(RolloutConfig config) {
        if (!config.isEnabled()) {
            return "功能已关闭";
        }

        switch (config.getStrategy()) {
            case ALL:
                return "全量开放";
            case NONE:
                return "全部关闭";
            case WHITELIST:
                String[] shops = config.getRolloutValue().split(",");
                return String.format("白名单灰度（共 %d 个店铺）", shops.length);
            case PERCENTAGE:
                return String.format("百分比灰度（%s%%）", config.getRolloutValue());
            default:
                return "未知策略";
        }
    }

    /**
     * 创建全量开放配置
     *
     * @param featureKey 功能键
     * @return 配置对象
     */
    public static RolloutConfig createAllConfig(String featureKey) {
        return new RolloutConfig(featureKey, RolloutStrategy.ALL, "", true);
    }

    /**
     * 创建白名单配置
     *
     * @param featureKey 功能键
     * @param shopIds    店铺 ID 列表（逗号分隔）
     * @return 配置对象
     */
    public static RolloutConfig createWhitelistConfig(String featureKey, String shopIds) {
        return new RolloutConfig(featureKey, RolloutStrategy.WHITELIST, shopIds, true);
    }

    /**
     * 创建百分比配置
     *
     * @param featureKey 功能键
     * @param percentage 百分比（0-100）
     * @return 配置对象
     */
    public static RolloutConfig createPercentageConfig(String featureKey, int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100");
        }
        return new RolloutConfig(featureKey, RolloutStrategy.PERCENTAGE, String.valueOf(percentage), true);
    }

    /**
     * 创建关闭配置
     *
     * @param featureKey 功能键
     * @return 配置对象
     */
    public static RolloutConfig createNoneConfig(String featureKey) {
        return new RolloutConfig(featureKey, RolloutStrategy.NONE, "", false);
    }
}
