package com.wimoor.ozon.seller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wimoor.common.security.ChannelCredentialCipher;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.client.OzonRemoteWarehouse;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.security.OzonCredentialService;
import com.wimoor.ozon.seller.mapper.OzonShopConfigMapper;
import com.wimoor.ozon.seller.mapper.OzonWarehouseMapper;
import com.wimoor.ozon.seller.pojo.entity.OzonWarehouse;
import com.wimoor.ozon.seller.pojo.vo.OzonWarehouseSyncResult;
import com.wimoor.ozon.seller.service.impl.OzonWarehouseSyncServiceImpl;

@ExtendWith(MockitoExtension.class)
class OzonWarehouseSyncServiceTests {

    private static final String AES_KEY = "0123456789abcdef";

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonWarehouseMapper warehouseMapper;

    @Mock
    private OzonShopConfigMapper shopConfigMapper;

    @Mock
    private OzonSellerApiClient sellerApiClient;

    @Captor
    private ArgumentCaptor<OzonWarehouse> warehouseCaptor;

    private OzonWarehouseSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonWarehouseSyncServiceImpl(
                authMapper,
                warehouseMapper,
                shopConfigMapper,
                sellerApiClient,
                new OzonCredentialService(AES_KEY)
        );
    }

    @Test
    void syncWarehousesPullsRemoteWarehousesAndRefreshesSnapshot() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
        auth.setClientId("test-client-id");
        auth.setStatus("ACTIVE");
        auth.setApiKeyCiphertext(new ChannelCredentialCipher(AES_KEY).encrypt("test-key"));

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(sellerApiClient.listWarehouses("test-client-id", "test-key")).thenReturn(Arrays.asList(
                new OzonRemoteWarehouse(1001L, "Main Warehouse", "ACTIVE", "FULFILLMENT"),
                new OzonRemoteWarehouse(1002L, "Reserve Warehouse", "INACTIVE", "SORTING_CENTER")
        ));

        OzonWarehouseSyncResult result = service.syncWarehouses("auth-1");

        assertEquals(2, result.getWarehouseCount());
        assertNotNull(result.getSyncedAt());
        verify(warehouseMapper).deleteByAuthId("auth-1");
        verify(warehouseMapper, times(2)).insert(warehouseCaptor.capture());
        verify(authMapper).updateById(auth);
        assertEquals("1001", warehouseCaptor.getAllValues().get(0).getWarehouseId());
        assertEquals("Main Warehouse", warehouseCaptor.getAllValues().get(0).getName());
    }
}
