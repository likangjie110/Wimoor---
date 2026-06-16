package com.wimoor.ozon.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.error.mapper.OzonErrorEventMapper;
import com.wimoor.ozon.error.pojo.dto.OzonErrorQuery;
import com.wimoor.ozon.error.pojo.entity.OzonErrorEvent;
import com.wimoor.ozon.error.pojo.vo.OzonErrorView;
import com.wimoor.ozon.error.service.impl.OzonErrorCenterServiceImpl;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingSyncResult;
import com.wimoor.ozon.posting.service.IOzonPostingService;
import com.wimoor.ozon.shipment.pojo.dto.OzonShipmentPushCommand;
import com.wimoor.ozon.shipment.pojo.vo.OzonShipmentPushResult;
import com.wimoor.ozon.shipment.service.IOzonShipmentService;

@ExtendWith(MockitoExtension.class)
class OzonErrorCenterServiceTests {

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonErrorEventMapper errorEventMapper;

    @Mock
    private IOzonPostingService postingService;

    @Mock
    private IOzonShipmentService shipmentService;

    @Captor
    private ArgumentCaptor<OzonErrorEvent> errorCaptor;

    private OzonErrorCenterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonErrorCenterServiceImpl(
                new OzonAuthAccessService(authMapper),
                errorEventMapper,
                postingService,
                shipmentService
        );
    }

    @Test
    void listFiltersEventsByAuthSourceAndStatus() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");

        OzonErrorEvent matched = new OzonErrorEvent();
        matched.setId("error-1");
        matched.setAuthId("auth-1");
        matched.setShopId("company-1");
        matched.setSourceType("POSTING");
        matched.setObjectId("posting-1");
        matched.setObjectCode("posting-no-1");
        matched.setStatus("OPEN");
        matched.setErrorMessage("bridge unmapped");
        matched.setCreateTime(new Date(0L));
        matched.setUpdateTime(new Date(1000L));

        OzonErrorEvent filtered = new OzonErrorEvent();
        filtered.setId("error-2");
        filtered.setAuthId("auth-1");
        filtered.setShopId("company-1");
        filtered.setSourceType("SHIPMENT");
        filtered.setStatus("IGNORED");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(errorEventMapper.selectList(any())).thenReturn(Arrays.asList(matched, filtered));

        List<OzonErrorView> result = service.list(buildUser(), new OzonErrorQuery("auth-1", "POSTING", "OPEN", "posting-no-1"));

        assertEquals(1, result.size());
        assertEquals("error-1", result.get(0).getId());
        assertEquals("POSTING", result.get(0).getSourceType());
        assertEquals("OPEN", result.get(0).getStatus());
    }

    @Test
    void retryOneReplaysShipmentCommandAndMarksResolved() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");

        OzonErrorEvent event = new OzonErrorEvent();
        event.setId("error-1");
        event.setAuthId("auth-1");
        event.setShopId("company-1");
        event.setSourceType("SHIPMENT");
        event.setObjectId("posting-1");
        event.setObjectCode("posting-no-1");
        event.setStatus("OPEN");
        event.setRetryCount(0);
        event.setRequestPayloadJson("{\"authId\":\"auth-1\",\"postingId\":\"posting-1\",\"trackingNumber\":\"TRACK-1\",\"deliveryService\":\"CDEK\"}");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(errorEventMapper.selectById("error-1")).thenReturn(event);
        when(shipmentService.pushTracking(any(), any())).thenReturn(new OzonShipmentPushResult());

        service.retryOne(buildUser(), "error-1");

        verify(shipmentService).pushTracking(any(), argThat((OzonShipmentPushCommand command) ->
                "auth-1".equals(command.getAuthId())
                        && "posting-1".equals(command.getPostingId())
                        && "TRACK-1".equals(command.getTrackingNumber())
                        && "CDEK".equals(command.getDeliveryService())
        ));
        verify(errorEventMapper).updateById(errorCaptor.capture());
        assertEquals("RESOLVED", errorCaptor.getValue().getStatus());
        assertEquals(Integer.valueOf(1), errorCaptor.getValue().getRetryCount());
    }

    @Test
    void ignoreMarksOpenEventIgnored() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");

        OzonErrorEvent event = new OzonErrorEvent();
        event.setId("error-1");
        event.setAuthId("auth-1");
        event.setShopId("company-1");
        event.setSourceType("POSTING");
        event.setObjectId("posting-1");
        event.setStatus("OPEN");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(errorEventMapper.selectById("error-1")).thenReturn(event);

        service.ignore(buildUser(), "error-1");

        verify(errorEventMapper).updateById(errorCaptor.capture());
        assertEquals("IGNORED", errorCaptor.getValue().getStatus());
    }

    @Test
    void retryOneRejectsUnsupportedSourceType() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");

        OzonErrorEvent event = new OzonErrorEvent();
        event.setId("error-1");
        event.setAuthId("auth-1");
        event.setShopId("company-1");
        event.setSourceType("PRICE");
        event.setObjectId("snapshot-1");
        event.setStatus("OPEN");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(errorEventMapper.selectById("error-1")).thenReturn(event);

        assertThrows(IllegalArgumentException.class, () -> service.retryOne(buildUser(), "error-1"));
    }

    @Test
    void retryOneCallsPostingRetryForPostingErrors() {
        UserInfo user = buildUser();
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");

        OzonErrorEvent event = new OzonErrorEvent();
        event.setId("error-1");
        event.setAuthId("auth-1");
        event.setShopId("company-1");
        event.setSourceType("POSTING");
        event.setObjectId("posting-1");
        event.setStatus("OPEN");
        event.setRetryCount(1);

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(errorEventMapper.selectById("error-1")).thenReturn(event);
        when(postingService.retryOne(any(), any(), any())).thenReturn(new OzonPostingSyncResult());

        service.retryOne(user, "error-1");

        verify(postingService).retryOne(user, "auth-1", "posting-1");
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");
        return user;
    }
}
