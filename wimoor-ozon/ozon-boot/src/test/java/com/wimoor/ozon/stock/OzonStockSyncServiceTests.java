package com.wimoor.ozon.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.wimoor.ozon.product.mapper.OzonProductMapMapper;
import com.wimoor.ozon.product.pojo.entity.OzonProductMap;
import com.wimoor.ozon.security.OzonCredentialService;
import com.wimoor.ozon.stock.mapper.OzonStockSnapshotMapper;
import com.wimoor.ozon.stock.mapper.OzonStockTaskMapper;
import com.wimoor.ozon.stock.pojo.dto.OzonStockPushCommand;
import com.wimoor.ozon.stock.pojo.dto.OzonStockPushItem;
import com.wimoor.ozon.stock.pojo.entity.OzonStockSnapshot;
import com.wimoor.ozon.stock.pojo.entity.OzonStockTask;
import com.wimoor.ozon.stock.pojo.vo.OzonStockPushResult;
import com.wimoor.ozon.stock.pojo.vo.OzonStockTaskView;
import com.wimoor.ozon.stock.service.impl.OzonStockServiceImpl;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;

@ExtendWith(MockitoExtension.class)
class OzonStockSyncServiceTests {

    private static final String AES_KEY = "0123456789abcdef";

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonProductMapMapper productMapMapper;

    @Mock
    private OzonStockTaskMapper stockTaskMapper;

    @Mock
    private OzonStockSnapshotMapper stockSnapshotMapper;

    @Mock
    private OzonSellerApiClient sellerApiClient;

    @Mock
    private OzonSyncJobMapper syncJobMapper;

    @Captor
    private ArgumentCaptor<OzonStockTask> taskCaptor;

    @Captor
    private ArgumentCaptor<OzonStockSnapshot> snapshotCaptor;

    @Captor
    private ArgumentCaptor<OzonSyncJob> syncJobCaptor;

    private OzonStockServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonStockServiceImpl(
                authMapper,
                productMapMapper,
                stockTaskMapper,
                stockSnapshotMapper,
                sellerApiClient,
                syncJobMapper,
                new OzonCredentialService(AES_KEY)
        );
    }

    @Test
    void stockPushWritesTaskAndSnapshot() {
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

        OzonStockPushResult result = service.push(
                buildUser(),
                new OzonStockPushCommand(
                        "auth-1",
                        "1020003216782000",
                        Collections.singletonList(new OzonStockPushItem("ERP-SKU-1", 11))
                )
        );

        assertEquals(1, result.getAccepted());
        verify(stockTaskMapper).insert(taskCaptor.capture());
        verify(stockSnapshotMapper).insert(snapshotCaptor.capture());
        verify(syncJobMapper).insert(syncJobCaptor.capture());
        assertEquals(taskCaptor.getValue().getId(), snapshotCaptor.getValue().getTaskId());
        assertEquals("STOCK_SYNC", syncJobCaptor.getValue().getJobType());
    }

    @Test
    void stockPushRejectsWhenWriteFeatureDisabled() {
        OzonStockServiceImpl disabledService = new OzonStockServiceImpl(
                authMapper,
                productMapMapper,
                stockTaskMapper,
                stockSnapshotMapper,
                sellerApiClient,
                syncJobMapper,
                new OzonCredentialService(AES_KEY),
                disabledGate()
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> disabledService.push(
                buildUser(),
                new OzonStockPushCommand(
                        "auth-1",
                        "1020003216782000",
                        Collections.singletonList(new OzonStockPushItem("ERP-SKU-1", 11))
                )
        ));

        assertEquals("Ozon库存写操作未开启", ex.getMessage());
        verifyNoInteractions(authMapper, productMapMapper, stockTaskMapper, stockSnapshotMapper, sellerApiClient, syncJobMapper);
    }

    @Test
    void listTasksReturnsRecentStockTasks() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
        when(authMapper.selectById("auth-1")).thenReturn(auth);

        OzonStockTask task = new OzonStockTask();
        task.setId("task-1");
        task.setAuthId("auth-1");
        task.setWarehouseId("w-1");
        task.setTaskStatus("SUBMITTED");
        task.setRequestedCount(3);
        task.setSuccessCount(3);
        when(stockTaskMapper.selectList(any())).thenReturn(Collections.singletonList(task));

        java.util.List<OzonStockTaskView> result = service.listTasks(buildUser(), "auth-1");

        assertEquals(1, result.size());
        assertEquals("task-1", result.get(0).getTaskId());
        assertEquals("w-1", result.get(0).getWarehouseId());
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
        properties.setStockWrite(false);
        properties.setPriceWrite(true);
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
