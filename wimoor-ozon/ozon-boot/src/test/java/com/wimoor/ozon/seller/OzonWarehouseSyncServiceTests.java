package com.wimoor.ozon.seller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;

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

/**
 * OZON 仓库同步服务测试
 *
 * @author Development Team
 * @since 2026-06-25
 */
@ExtendWith(MockitoExtension.class)
class OzonWarehouseSyncServiceTests {

    private static final String AES_KEY = "0123456789abcdef";
    private static final String AUTH_ID = "auth-1";
    private static final String SHOP_ID = "company-1";
    private static final String CLIENT_ID = "test-client-id";
    private static final String API_KEY = "test-key";

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

    @Captor
    private ArgumentCaptor<OzonAuth> authCaptor;

    private OzonWarehouseSyncServiceImpl service;
    private OzonAuth testAuth;

    @BeforeEach
    void setUp() {
        service = new OzonWarehouseSyncServiceImpl(
                authMapper,
                warehouseMapper,
                shopConfigMapper,
                sellerApiClient,
                new OzonCredentialService(AES_KEY)
        );

        testAuth = createTestAuth();
    }

    // ==================== 仓库同步测试 ====================

    @Test
    void syncWarehousesPullsRemoteWarehousesAndRefreshesSnapshot() {
        when(authMapper.selectById(AUTH_ID)).thenReturn(testAuth);
        when(sellerApiClient.listWarehouses(CLIENT_ID, API_KEY)).thenReturn(Arrays.asList(
                new OzonRemoteWarehouse(1001L, "Main Warehouse", "ACTIVE", "FULFILLMENT"),
                new OzonRemoteWarehouse(1002L, "Reserve Warehouse", "INACTIVE", "SORTING_CENTER")
        ));

        OzonWarehouseSyncResult result = service.syncWarehouses(AUTH_ID);

        assertEquals(2, result.getWarehouseCount());
        assertNotNull(result.getSyncedAt());
        verify(warehouseMapper).deleteByAuthId(AUTH_ID);
        verify(warehouseMapper, times(2)).insert(warehouseCaptor.capture());
        verify(authMapper).updateById(authCaptor.capture());
        assertEquals("1001", warehouseCaptor.getAllValues().get(0).getWarehouseId());
        assertEquals("Main Warehouse", warehouseCaptor.getAllValues().get(0).getName());
        assertEquals("SUCCESS", authCaptor.getValue().getLastSyncStatus());
    }

    @Test
    void syncWarehouses_HandlesEmptyWarehouseList() {
        when(authMapper.selectById(AUTH_ID)).thenReturn(testAuth);
        when(sellerApiClient.listWarehouses(CLIENT_ID, API_KEY))
                .thenReturn(Collections.emptyList());

        OzonWarehouseSyncResult result = service.syncWarehouses(AUTH_ID);

        assertEquals(0, result.getWarehouseCount());
        verify(warehouseMapper).deleteByAuthId(AUTH_ID);
        verify(warehouseMapper, never()).insert(any(OzonWarehouse.class));
    }

    @Test
    void syncWarehouses_UpdatesAuthStatusOnFailure() {
        when(authMapper.selectById(AUTH_ID)).thenReturn(testAuth);
        when(sellerApiClient.listWarehouses(CLIENT_ID, API_KEY))
                .thenThrow(new RuntimeException("API connection failed"));

        assertThrows(RuntimeException.class, () -> service.syncWarehouses(AUTH_ID));

        verify(authMapper).updateById(authCaptor.capture());
        assertEquals("FAILED", authCaptor.getValue().getLastSyncStatus());
    }

    @Test
    void syncWarehouses_ThrowsExceptionWhenAuthIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.syncWarehouses(null)
        );

        assertTrue(exception.getMessage().contains("authId不能为空"));
    }

    @Test
    void syncWarehouses_ThrowsExceptionWhenAuthNotFound() {
        when(authMapper.selectById(AUTH_ID)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.syncWarehouses(AUTH_ID)
        );

        assertTrue(exception.getMessage().contains("授权不存在"));
    }

    // ==================== 仓库统计测试 ====================

    @Test
    void countByAuth_ReturnsWarehouseCount() {
        when(warehouseMapper.countByAuthId(AUTH_ID)).thenReturn(5);

        int count = service.countByAuth(AUTH_ID);

        assertEquals(5, count);
        verify(warehouseMapper).countByAuthId(AUTH_ID);
    }

    @Test
    void countByAuth_ReturnsZeroWhenAuthIdIsNull() {
        int count = service.countByAuth(null);

        assertEquals(0, count);
        verify(warehouseMapper, never()).countByAuthId(any());
    }

    // ==================== 默认仓库名称查询测试 ====================

    @Test
    void getDefaultWarehouseName_ReturnsNameWhenWarehouseExists() {
        OzonWarehouse defaultWarehouse = new OzonWarehouse();
        defaultWarehouse.setName("Default Warehouse");

        when(warehouseMapper.selectDefaultByAuthId(AUTH_ID)).thenReturn(defaultWarehouse);

        String name = service.getDefaultWarehouseName(AUTH_ID);

        assertEquals("Default Warehouse", name);
    }

    @Test
    void getDefaultWarehouseName_ReturnsNullWhenNoDefaultWarehouse() {
        when(warehouseMapper.selectDefaultByAuthId(AUTH_ID)).thenReturn(null);

        String name = service.getDefaultWarehouseName(AUTH_ID);

        assertNull(name);
    }

    @Test
    void getDefaultWarehouseName_ReturnsNullWhenAuthIdIsNull() {
        String name = service.getDefaultWarehouseName(null);

        assertNull(name);
        verify(warehouseMapper, never()).selectDefaultByAuthId(any());
    }

    // ==================== 辅助方法 ====================

    private OzonAuth createTestAuth() {
        OzonAuth auth = new OzonAuth();
        auth.setId(AUTH_ID);
        auth.setShopId(SHOP_ID);
        auth.setClientId(CLIENT_ID);
        auth.setStatus("ACTIVE");
        auth.setApiKeyCiphertext(new ChannelCredentialCipher(AES_KEY).encrypt(API_KEY));
        return auth;
    }
}
