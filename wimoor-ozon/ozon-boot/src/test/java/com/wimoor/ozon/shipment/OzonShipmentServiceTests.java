package com.wimoor.ozon.shipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

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
import com.wimoor.ozon.error.service.OzonErrorRecorder;
import com.wimoor.ozon.posting.mapper.OzonPostingMapper;
import com.wimoor.ozon.posting.pojo.entity.OzonPosting;
import com.wimoor.ozon.security.OzonCredentialService;
import com.wimoor.ozon.shipment.mapper.OzonShipmentMapper;
import com.wimoor.ozon.shipment.pojo.dto.OzonShipmentPushCommand;
import com.wimoor.ozon.shipment.pojo.entity.OzonShipment;
import com.wimoor.ozon.shipment.pojo.vo.OzonShipmentPushResult;
import com.wimoor.ozon.shipment.service.impl.OzonShipmentServiceImpl;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;

@ExtendWith(MockitoExtension.class)
class OzonShipmentServiceTests {

    private static final String AES_KEY = "0123456789abcdef";

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonPostingMapper postingMapper;

    @Mock
    private OzonShipmentMapper shipmentMapper;

    @Mock
    private OzonSellerApiClient sellerApiClient;

    @Mock
    private OzonSyncJobMapper syncJobMapper;

    @Mock
    private OzonErrorRecorder errorRecorder;

    @Captor
    private ArgumentCaptor<OzonShipment> shipmentCaptor;

    @Captor
    private ArgumentCaptor<OzonSyncJob> syncJobCaptor;

    private OzonShipmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonShipmentServiceImpl(
                authMapper,
                postingMapper,
                shipmentMapper,
                sellerApiClient,
                syncJobMapper,
                new OzonCredentialService(AES_KEY),
                errorRecorder
        );
    }

    @Test
    void pushTrackingPersistsShipmentRecord() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
        auth.setClientId("test-client-id");
        auth.setStatus("ACTIVE");
        auth.setApiKeyCiphertext(new ChannelCredentialCipher(AES_KEY).encrypt("test-key"));

        OzonPosting posting = new OzonPosting();
        posting.setId("posting-1");
        posting.setAuthId("auth-1");
        posting.setShopId("company-1");
        posting.setPostingNumber("posting-number-1");
        posting.setFulfillmentType("FBS");
        posting.setOrderCreatedAt(new Date(0L));

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(posting);
        when(sellerApiClient.setTrackingNumber(
                eq("test-client-id"),
                eq("test-key"),
                contains("\"tracking_number\":\"TRACK-1\"")))
                .thenReturn("{\"result\":true}");

        OzonShipmentPushResult result = service.pushTracking(
                buildUser(),
                new OzonShipmentPushCommand("auth-1", "posting-1", "TRACK-1", "CDEK")
        );

        assertEquals("TRACK-1", result.getTrackingNumber());
        assertEquals("posting-number-1", result.getPostingNumber());
        verify(shipmentMapper).insert(shipmentCaptor.capture());
        verify(syncJobMapper).insert(syncJobCaptor.capture());
        assertEquals("TRACK-1", shipmentCaptor.getValue().getTrackingNumber());
        assertEquals("CDEK", shipmentCaptor.getValue().getDeliveryService());
        assertEquals("TRACKING_SET", shipmentCaptor.getValue().getShipmentStatus());
        assertEquals("TRACKING_PUSH", syncJobCaptor.getValue().getJobType());
    }

    @Test
    void listByPostingReturnsShipmentHistory() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");

        OzonPosting posting = new OzonPosting();
        posting.setId("posting-1");
        posting.setAuthId("auth-1");
        posting.setShopId("company-1");
        posting.setPostingNumber("posting-number-1");

        OzonShipment latest = new OzonShipment();
        latest.setPostingId("posting-1");
        latest.setTrackingNumber("TRACK-2");
        latest.setShipmentStatus("TRACKING_SET");

        OzonShipment first = new OzonShipment();
        first.setPostingId("posting-1");
        first.setTrackingNumber("TRACK-1");
        first.setShipmentStatus("TRACKING_SET");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(posting);
        when(shipmentMapper.selectList(any())).thenReturn(java.util.Arrays.asList(latest, first));

        List<OzonShipment> history = service.listByPosting(buildUser(), "auth-1", "posting-1");

        assertEquals(2, history.size());
        assertEquals("TRACK-2", history.get(0).getTrackingNumber());
        assertTrue(history.stream().anyMatch(item -> "TRACK-1".equals(item.getTrackingNumber())));
    }

    @Test
    void pushTrackingRecordsShipmentErrorWhenApiFails() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
        auth.setClientId("test-client-id");
        auth.setStatus("ACTIVE");
        auth.setApiKeyCiphertext(new ChannelCredentialCipher(AES_KEY).encrypt("test-key"));

        OzonPosting posting = new OzonPosting();
        posting.setId("posting-1");
        posting.setAuthId("auth-1");
        posting.setShopId("company-1");
        posting.setPostingNumber("posting-number-1");
        posting.setFulfillmentType("FBS");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(posting);
        when(sellerApiClient.setTrackingNumber(
                eq("test-client-id"),
                eq("test-key"),
                contains("\"tracking_number\":\"TRACK-1\"")))
                .thenThrow(new IllegalStateException("ozon api error"));

        assertThrows(IllegalStateException.class, () -> service.pushTracking(
                buildUser(),
                new OzonShipmentPushCommand("auth-1", "posting-1", "TRACK-1", "CDEK")
        ));

        verify(errorRecorder).recordOpen(argThat(command ->
                "SHIPMENT".equals(command.getSourceType())
                        && "auth-1".equals(command.getAuthId())
                        && "posting-1".equals(command.getObjectId())
                        && "posting-number-1".equals(command.getObjectCode())
                        && command.getRequestPayloadJson() != null
                        && command.getRequestPayloadJson().contains("\"trackingNumber\":\"TRACK-1\"")
        ));
    }

    @Test
    void pushTrackingRejectsWhenWriteFeatureDisabled() {
        OzonShipmentServiceImpl disabledService = new OzonShipmentServiceImpl(
                authMapper,
                postingMapper,
                shipmentMapper,
                sellerApiClient,
                syncJobMapper,
                new OzonCredentialService(AES_KEY),
                errorRecorder,
                disabledGate()
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> disabledService.pushTracking(
                buildUser(),
                new OzonShipmentPushCommand("auth-1", "posting-1", "TRACK-1", "CDEK")
        ));

        assertEquals("Ozon履约写操作未开启", ex.getMessage());
        verifyNoInteractions(authMapper, postingMapper, shipmentMapper, sellerApiClient, syncJobMapper, errorRecorder);
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
        properties.setPriceWrite(true);
        properties.setPostingWrite(false);
        return new OzonFeatureGate(properties);
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");
        return user;
    }
}
