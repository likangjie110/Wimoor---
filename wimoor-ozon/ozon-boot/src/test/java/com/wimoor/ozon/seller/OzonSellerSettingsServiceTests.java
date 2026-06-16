package com.wimoor.ozon.seller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.seller.mapper.OzonDeliveryMethodMapper;
import com.wimoor.ozon.seller.mapper.OzonShopConfigMapper;
import com.wimoor.ozon.seller.mapper.OzonWarehouseMapper;
import com.wimoor.ozon.seller.pojo.dto.OzonDeliveryMethodSaveCommand;
import com.wimoor.ozon.seller.pojo.entity.OzonDeliveryMethod;
import com.wimoor.ozon.seller.pojo.entity.OzonShopConfig;
import com.wimoor.ozon.seller.pojo.entity.OzonWarehouse;
import com.wimoor.ozon.seller.pojo.vo.OzonWarehouseView;
import com.wimoor.ozon.seller.service.impl.OzonSellerSettingsServiceImpl;

@ExtendWith(MockitoExtension.class)
class OzonSellerSettingsServiceTests {

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonWarehouseMapper warehouseMapper;

    @Mock
    private OzonShopConfigMapper shopConfigMapper;

    @Mock
    private OzonDeliveryMethodMapper deliveryMethodMapper;

    @Captor
    private ArgumentCaptor<OzonDeliveryMethod> deliveryMethodCaptor;

    private OzonSellerSettingsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonSellerSettingsServiceImpl(
                authMapper,
                warehouseMapper,
                shopConfigMapper,
                deliveryMethodMapper
        );
    }

    @Test
    void listWarehousesMarksDefaultWarehouse() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        when(shopConfigMapper.selectOne(any(QueryWrapper.class))).thenReturn(buildShopConfig());
        when(warehouseMapper.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList(
                warehouse("w-1", "1001", "Main"),
                warehouse("w-2", "1002", "Backup")
        ));

        List<OzonWarehouseView> result = service.listWarehouses(buildUser(), "auth-1");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(item -> "1001".equals(item.getWarehouseId()) && Boolean.TRUE.equals(item.getDefaultWarehouse())));
    }

    @Test
    void listDeliveryMethodsReturnsCurrentRows() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        when(deliveryMethodMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.singletonList(method("dm-1", "fbs-main")));

        List<OzonDeliveryMethod> result = service.listDeliveryMethods(buildUser(), "auth-1");

        assertEquals(1, result.size());
        assertEquals("fbs-main", result.get(0).getMethodCode());
    }

    @Test
    void saveDeliveryMethodClearsOldDefaultWhenNewDefaultSelected() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        when(deliveryMethodMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        OzonDeliveryMethod result = service.saveDeliveryMethod(
                buildUser(),
                new OzonDeliveryMethodSaveCommand("auth-1", null, "fbs-main", "FBS 主配送", "默认履约方式", true, true)
        );

        assertEquals("fbs-main", result.getMethodCode());
        assertEquals("FBS 主配送", result.getMethodName());
        assertEquals(Boolean.TRUE, result.getDefaultMethod());
        verify(deliveryMethodMapper).clearDefaultByAuthId("auth-1");
        verify(deliveryMethodMapper).insert(deliveryMethodCaptor.capture());
        assertEquals("company-1", deliveryMethodCaptor.getValue().getShopId());
    }

    private OzonAuth buildAuth() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
        auth.setClientId("client-1");
        auth.setStatus("ACTIVE");
        return auth;
    }

    private OzonShopConfig buildShopConfig() {
        OzonShopConfig config = new OzonShopConfig();
        config.setAuthId("auth-1");
        config.setShopId("company-1");
        config.setDefaultWarehouseId("1001");
        config.setLastWarehouseSyncTime(new Date());
        return config;
    }

    private OzonWarehouse warehouse(String id, String warehouseId, String name) {
        OzonWarehouse warehouse = new OzonWarehouse();
        warehouse.setId(id);
        warehouse.setAuthId("auth-1");
        warehouse.setShopId("company-1");
        warehouse.setWarehouseId(warehouseId);
        warehouse.setName(name);
        warehouse.setStatus("ACTIVE");
        warehouse.setWarehouseType("FULFILLMENT");
        warehouse.setActive(Boolean.TRUE);
        warehouse.setSyncedAt(new Date());
        return warehouse;
    }

    private OzonDeliveryMethod method(String id, String code) {
        OzonDeliveryMethod method = new OzonDeliveryMethod();
        method.setId(id);
        method.setAuthId("auth-1");
        method.setShopId("company-1");
        method.setMethodCode(code);
        method.setMethodName("Method");
        method.setEnabled(Boolean.TRUE);
        method.setDefaultMethod(Boolean.TRUE);
        return method;
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");
        return user;
    }
}
