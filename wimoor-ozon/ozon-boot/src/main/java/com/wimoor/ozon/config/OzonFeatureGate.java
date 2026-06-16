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
        properties.setChat(true);
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

    public void assertChatEnabled() {
        require(properties.isChat(), "Ozon聊天功能未开启");
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
        view.setAuth(featureState(properties.isAuth(), "Ozon授权功能未开启"));
        view.setProduct(featureState(properties.isProduct(), "Ozon商品功能未开启"));
        view.setProductWrite(featureState(properties.isProductWrite(), "Ozon商品发布写操作未开启"));
        view.setTask(featureState(properties.isTask(), "Ozon任务中心功能未开启"));
        view.setError(featureState(properties.isError(), "Ozon异常中心功能未开启"));
        view.setFinance(featureState(properties.isFinance(), "Ozon财务功能未开启"));
        view.setChat(featureState(properties.isChat(), "Ozon聊天功能未开启"));
        view.setAds(featureState(properties.isAds(), "Ozon广告功能未开启"));
        view.setStockWrite(featureState(properties.isStockWrite(), "Ozon库存写操作未开启"));
        view.setPriceWrite(featureState(properties.isPriceWrite(), "Ozon价格写操作未开启"));
        view.setPostingWrite(featureState(properties.isPostingWrite(), "Ozon履约写操作未开启"));
        view.setChatSend(featureState(properties.isChatSend(), "Ozon聊天发送功能未开启"));
        view.setAdsSync(featureState(properties.isAdsSync(), "Ozon广告同步功能未开启"));
        return view;
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
