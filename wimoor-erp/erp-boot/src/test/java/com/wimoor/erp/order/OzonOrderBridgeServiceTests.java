package com.wimoor.erp.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wimoor.erp.order.mapper.OrderMapper;
import com.wimoor.erp.order.mapper.OrderPlatformMapper;
import com.wimoor.erp.order.pojo.dto.OzonErpOrderUpsertCommand;
import com.wimoor.erp.order.pojo.dto.OzonErpOrderUpsertResult;
import com.wimoor.erp.order.pojo.entity.Order;
import com.wimoor.erp.order.pojo.entity.OrderPlatform;
import com.wimoor.erp.order.service.impl.OzonOrderBridgeService;

@ExtendWith(MockitoExtension.class)
class OzonOrderBridgeServiceTests {

    @Mock
    private OrderPlatformMapper orderPlatformMapper;

    @Mock
    private OrderMapper orderMapper;

    @Captor
    private ArgumentCaptor<OrderPlatform> platformCaptor;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private OzonOrderBridgeService service;

    @BeforeEach
    void setUp() {
        service = new OzonOrderBridgeService(orderPlatformMapper, orderMapper);
    }

    @Test
    void upsertCreatesPlatformAndOrder() {
        when(orderPlatformMapper.selectOne(any())).thenReturn(null);
        when(orderMapper.selectOne(any())).thenReturn(null);

        OzonErpOrderUpsertResult result = service.upsert(new OzonErpOrderUpsertCommand(
                "company-1",
                "posting-1",
                "ERP-SKU-1",
                "warehouse-1",
                "third-wh-1",
                "RU",
                "RUB",
                2,
                new BigDecimal("12.50"),
                new Date(0L)
        ));

        assertNotNull(result.getErpOrderId());
        verify(orderPlatformMapper).insert(platformCaptor.capture());
        verify(orderMapper).insert(orderCaptor.capture());
        assertEquals("Ozon", platformCaptor.getValue().getName());
        assertNull(platformCaptor.getValue().getOperator());
        assertEquals("posting-1", orderCaptor.getValue().getOrderId());
        assertEquals("ERP-SKU-1", orderCaptor.getValue().getSku());
        assertEquals(Integer.valueOf(2), orderCaptor.getValue().getQuantity());
        assertNull(orderCaptor.getValue().getOperator());
    }
}
