package com.wimoor.ozon.price;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wimoor.common.security.ChannelCredentialCipher;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.config.OzonFeatureProperties;
import com.wimoor.ozon.price.mapper.OzonPriceSnapshotMapper;
import com.wimoor.ozon.price.mapper.OzonPriceTaskMapper;
import com.wimoor.ozon.price.pojo.dto.OzonPricePushCommand;
import com.wimoor.ozon.price.pojo.dto.OzonPricePushItem;
import com.wimoor.ozon.price.pojo.entity.OzonPriceSnapshot;
import com.wimoor.ozon.price.pojo.entity.OzonPriceTask;
import com.wimoor.ozon.price.pojo.vo.OzonPricePushResult;
import com.wimoor.ozon.price.pojo.vo.OzonPriceTaskView;
import com.wimoor.ozon.price.service.impl.OzonPriceServiceImpl;
import com.wimoor.ozon.product.mapper.OzonProductMapMapper;
import com.wimoor.ozon.product.pojo.entity.OzonProductMap;
import com.wimoor.ozon.security.OzonCredentialService;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;

@ExtendWith(MockitoExtension.class)
class OzonPriceSyncServiceTests {

    private static final String AES_KEY = "0123456789abcdef";

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonProductMapMapper productMapMapper;

    @Mock
    private OzonPriceTaskMapper priceTaskMapper;

    @Mock
    private OzonPriceSnapshotMapper priceSnapshotMapper;

    @Mock
    private OzonSellerApiClient sellerApiClient;

    @Mock
    private OzonSyncJobMapper syncJobMapper;

    @Captor
    private ArgumentCaptor<OzonPriceTask> taskCaptor;

    @Captor
    private ArgumentCaptor<OzonPriceSnapshot> snapshotCaptor;

    @Captor
    private ArgumentCaptor<OzonSyncJob> syncJobCaptor;

    private OzonPriceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonPriceServiceImpl(
                authMapper,
                productMapMapper,
                priceTaskMapper,
                priceSnapshotMapper,
                sellerApiClient,
                syncJobMapper,
                new OzonCredentialService(AES_KEY)
        );
    }

    @Test
    void pricePushWritesTaskAndSnapshot() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
        auth.setClientId("test-client-id");
        auth.setStatus("ACTIVE");
        auth.setApiKeyCiphertext(new ChannelCredentialCipher(AES_KEY).encrypt("test-key"));

        OzonProductMap productMap = new OzonProductMap();
        productMap.setAuthId("auth-1");
        productMap.setMaterialSku("ERP-SKU-1");
        productMap.setOzonOfferId("offer-1");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(productMapMapper.listByMaterialSkus("auth-1", Collections.singletonList("ERP-SKU-1")))
                .thenReturn(Collections.singletonList(productMap));
        when(sellerApiClient.importPrices(eq("test-client-id"), eq("test-key"), contains("\"currency_code\":\"RUB\"")))
                .thenReturn("{\"result\":[]}");

        OzonPricePushResult result = service.push(
                buildUser(),
                new OzonPricePushCommand(
                        "auth-1",
                        "RUB",
                        Collections.singletonList(new OzonPricePushItem(
                                "ERP-SKU-1",
                                new BigDecimal("12.50"),
                                new BigDecimal("15.00")
                        ))
                )
        );

        assertEquals(1, result.getAccepted());
        verify(priceTaskMapper).insert(taskCaptor.capture());
        verify(priceSnapshotMapper).insert(snapshotCaptor.capture());
        verify(syncJobMapper).insert(syncJobCaptor.capture());
        assertEquals(taskCaptor.getValue().getId(), snapshotCaptor.getValue().getTaskId());
        assertEquals("PRICE_SYNC", syncJobCaptor.getValue().getJobType());
    }

    @Test
    void pricePushRejectsWhenWriteFeatureDisabled() {
        OzonPriceServiceImpl disabledService = new OzonPriceServiceImpl(
                authMapper,
                productMapMapper,
                priceTaskMapper,
                priceSnapshotMapper,
                sellerApiClient,
                syncJobMapper,
                new OzonCredentialService(AES_KEY),
                disabledGate()
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> disabledService.push(
                buildUser(),
                new OzonPricePushCommand(
                        "auth-1",
                        "RUB",
                        Collections.singletonList(new OzonPricePushItem(
                                "ERP-SKU-1",
                                new BigDecimal("12.50"),
                                new BigDecimal("15.00")
                        ))
                )
        ));

        assertEquals("Ozon价格写操作未开启", ex.getMessage());
        verifyNoInteractions(authMapper, productMapMapper, priceTaskMapper, priceSnapshotMapper, sellerApiClient, syncJobMapper);
    }

    @Test
    void listTasksReturnsRecentPriceTasks() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
        when(authMapper.selectById("auth-1")).thenReturn(auth);

        OzonPriceTask task = new OzonPriceTask();
        task.setId("task-1");
        task.setAuthId("auth-1");
        task.setTaskStatus("SUBMITTED");
        task.setRequestedCount(2);
        task.setSuccessCount(2);
        when(priceTaskMapper.selectList(any())).thenReturn(Collections.singletonList(task));

        java.util.List<OzonPriceTaskView> result = service.listTasks(buildUser(), "auth-1");

        assertEquals(1, result.size());
        assertEquals("task-1", result.get(0).getTaskId());
        assertEquals("SUBMITTED", result.get(0).getTaskStatus());
    }

    private OzonFeatureGate disabledGate() {
        OzonFeatureProperties properties = new OzonFeatureProperties();
        properties.setAuth(true);
        properties.setProduct(true);
        properties.setTask(true);
        properties.setError(true);
        properties.setFinance(true);
        properties.setChat(true);
        properties.setAds(true);
        properties.setStockWrite(true);
        properties.setPriceWrite(false);
        properties.setPostingWrite(true);
        return new OzonFeatureGate(properties);
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");
        return user;
    }
}
