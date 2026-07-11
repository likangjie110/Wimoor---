package com.wimoor.ozon.config;

import org.springframework.stereotype.Component;

import com.wimoor.ozon.config.pojo.vo.OzonFeatureView;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OzonFeatureGate {

    private final OzonFeatureProperties properties;

    public static OzonFeatureGate allEnabled() {
        OzonFeatureProperties properties = new OzonFeatureProperties();
        properties.setAuth(true);
        properties.setProduct(true);
        properties.setProductWrite(true);
        properties.setTask(true);
        properties.setError(true);
        properties.setFinance(true);
        properties.setFinanceSync(true);
        properties.setChat(true);
        properties.setChatSync(true);
        properties.setAds(true);
        properties.setStockWrite(true);
        properties.setPriceWrite(true);
        properties.setPostingWrite(true);
        properties.setChatSend(true);
        properties.setAdsSync(true);
        return new OzonFeatureGate(properties);
    }

    public void assertAuthEnabled() {
        require(properties.isAuth(), "Ozon授权功能未开启");
    }

    public void assertProductEnabled() {
        require(properties.isProduct(), "Ozon商品功能未开启");
    }

    public void assertProductWriteEnabled() {
        require(properties.isProductWrite(), "Ozon商品发布写操作未开启");
    }

    public void assertTaskEnabled() {
        require(properties.isTask(), "Ozon任务中心功能未开启");
    }

    public void assertErrorEnabled() {
        require(properties.isError(), "Ozon异常中心功能未开启");
    }

    public void assertFinanceEnabled() {
        require(properties.isFinance(), "Ozon财务功能未开启");
    }

    public void assertFinanceSyncEnabled() {
        require(properties.isFinanceSync(), "Ozon财务远程同步功能未开启");
    }

    public void assertChatEnabled() {
        require(properties.isChat(), "Ozon聊天功能未开启");
    }

    public void assertChatSyncEnabled() {
        require(properties.isChatSync(), "Ozon聊天远程同步功能未开启");
    }

    public void assertAdsEnabled() {
        require(properties.isAds(), "Ozon广告功能未开启");
    }

    public void assertStockWriteEnabled() {
        require(properties.isStockWrite(), "Ozon库存写操作未开启");
    }

    public void assertPriceWriteEnabled() {
        require(properties.isPriceWrite(), "Ozon价格写操作未开启");
    }

    public void assertPostingWriteEnabled() {
        require(properties.isPostingWrite(), "Ozon履约写操作未开启");
    }

    public void assertChatSendEnabled() {
        require(properties.isChatSend(), "Ozon聊天发送功能未开启");
    }

    public void assertAdsSyncEnabled() {
        require(properties.isAdsSync(), "Ozon广告同步功能未开启");
    }

    public OzonFeatureView describeFeatures() {
        OzonFeatureView view = new OzonFeatureView();

        view.setAuth(createFeatureItem(
            properties.isAuth(),
            "店铺授权",
            "管理 Ozon 店铺授权、仓库和配送方式",
            "read",
            "授权功能未开启"
        ));

        view.setProduct(createFeatureItem(
            properties.isProduct(),
            "商品管理",
            "管理商品草稿、映射和类目属性",
            "read",
            "商品功能未开启"
        ));

        view.setProductWrite(createFeatureItem(
            properties.isProductWrite(),
            "商品发布",
            "发布商品到 Ozon 平台",
            "write",
            "等待灰度验证通过"
        ));

        view.setTask(createFeatureItem(
            properties.isTask(),
            "任务中心",
            "查看和管理同步任务",
            "read",
            "任务中心功能未开启"
        ));

        view.setError(createFeatureItem(
            properties.isError(),
            "错误中心",
            "查看和处理错误事件",
            "read",
            "错误中心功能未开启"
        ));

        view.setFinance(createFeatureItem(
            properties.isFinance(),
            "财务管理",
            "管理财务交易和报表",
            "read",
            "财务功能未开启"
        ));
        view.setFinanceSync(createFeatureItem(
            properties.isFinanceSync(),
            "财务远程同步",
            "从 Ozon API 同步财务交易和结算数据",
            "sync",
            "需要完成 Ozon 财务合同验证"
        ));

        view.setChat(createFeatureItem(
            properties.isChat(),
            "客户聊天",
            "管理客户聊天消息",
            "read",
            "聊天功能未开启"
        ));
        view.setChatSync(createFeatureItem(
            properties.isChatSync(),
            "聊天远程同步",
            "从 Ozon API 同步会话和消息",
            "sync",
            "需要完成 Ozon 聊天合同验证"
        ));

        view.setAds(createFeatureItem(
            properties.isAds(),
            "广告管理",
            "管理广告活动和报表",
            "read",
            "广告功能未开启"
        ));

        view.setStockWrite(createFeatureItem(
            properties.isStockWrite(),
            "库存推送",
            "推送库存到 Ozon",
            "write",
            "等待灰度验证通过"
        ));

        view.setPriceWrite(createFeatureItem(
            properties.isPriceWrite(),
            "价格推送",
            "推送价格到 Ozon",
            "write",
            "等待灰度验证通过"
        ));

        view.setPostingWrite(createFeatureItem(
            properties.isPostingWrite(),
            "订单履约",
            "推送追踪号等履约信息",
            "write",
            "等待灰度验证通过"
        ));

        view.setChatSend(createFeatureItem(
            properties.isChatSend(),
            "客户消息发送",
            "发送聊天回复到 Ozon",
            "write",
            "需要完成 OZON 官方合同验证"
        ));

        view.setAdsSync(createFeatureItem(
            properties.isAdsSync(),
            "广告数据同步",
            "同步广告数据和报表",
            "sync",
            "需要完成 OZON 官方合同验证"
        ));

        return view;
    }

    /**
     * 创建功能项
     */
    private OzonFeatureView.FeatureItem createFeatureItem(
            boolean enabled,
            String name,
            String description,
            String category,
            String disabledReason) {
        OzonFeatureView.FeatureItem item = new OzonFeatureView.FeatureItem();
        item.setEnabled(enabled);
        item.setName(name);
        item.setDescription(description);
        item.setCategory(category);
        item.setReason(enabled ? null : disabledReason);
        return item;
    }

    /**
     * 统计已开启的写操作数量
     */
    public int countEnabledWrites() {
        int count = 0;
        if (properties.isProductWrite()) count++;
        if (properties.isStockWrite()) count++;
        if (properties.isPriceWrite()) count++;
        if (properties.isPostingWrite()) count++;
        if (properties.isChatSend()) count++;
        if (properties.isFinanceSync()) count++;
        if (properties.isChatSync()) count++;
        if (properties.isAdsSync()) count++;
        return count;
    }

    private OzonFeatureView.FeatureItem featureState(boolean enabled, String disabledReason) {
        return new OzonFeatureView.FeatureItem(enabled, enabled ? null : disabledReason);
    }

    private void require(boolean enabled, String message) {
        if (!enabled) {
            throw new IllegalStateException(message);
        }
    }
}
