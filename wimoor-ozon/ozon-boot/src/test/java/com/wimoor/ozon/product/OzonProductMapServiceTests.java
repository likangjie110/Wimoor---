package com.wimoor.ozon.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.erp.api.ErpClientOneFeign;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.product.mapper.OzonProductMapMapper;
import com.wimoor.ozon.product.pojo.dto.OzonProductMapSaveCommand;
import com.wimoor.ozon.product.pojo.entity.OzonProductMap;
import com.wimoor.ozon.product.service.impl.OzonProductMapServiceImpl;

@ExtendWith(MockitoExtension.class)
class OzonProductMapServiceTests {

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonProductMapMapper productMapMapper;

    @Mock
    private ErpClientOneFeign erpClientOneFeign;

    @Captor
    private ArgumentCaptor<OzonProductMap> mapCaptor;

    private OzonProductMapServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonProductMapServiceImpl(authMapper, productMapMapper, erpClientOneFeign);
    }

    @Test
    void saveMappingPersistsOfferAndErpSku() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(erpClientOneFeign.findMaterialMapBySku(Collections.singletonList("ERP-SKU-1")))
                .thenReturn(Result.success(buildMaterialMap()));

        OzonProductMap saved = service.saveMapping(
                buildUser(),
                new OzonProductMapSaveCommand("auth-1", "ERP-SKU-1", "offer-1", "ozonSku1", "10001")
        );

        assertEquals("ERP-SKU-1", saved.getMaterialSku());
        assertEquals("offer-1", saved.getOzonOfferId());
        assertNotNull(saved.getId());

        verify(productMapMapper).insert(mapCaptor.capture());
        assertEquals("ERP-SKU-1", mapCaptor.getValue().getMaterialSku());
        assertEquals("offer-1", mapCaptor.getValue().getOzonOfferId());
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");
        return user;
    }

    private Map<String, Object> buildMaterialMap() {
        Map<String, Object> material = new HashMap<>();
        material.put("msku", "ERP-SKU-1");
        material.put("name", "示例产品");
        material.put("ownername", "tester");
        material.put("image", "https://img.test/item.png");
        material.put("price", "12.50");

        Map<String, Object> result = new HashMap<>();
        result.put("ERP-SKU-1", material);
        return result;
    }
}
