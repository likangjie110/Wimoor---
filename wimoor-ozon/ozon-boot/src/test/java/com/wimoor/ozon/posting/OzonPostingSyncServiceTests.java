package com.wimoor.ozon.posting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
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

import com.wimoor.common.result.Result;
import com.wimoor.common.security.ChannelCredentialCipher;
import com.wimoor.common.user.UserInfo;
import com.wimoor.erp.api.ErpClientOneFeign;
import com.wimoor.erp.order.pojo.dto.OzonErpOrderUpsertResult;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.config.OzonFeatureProperties;
import com.wimoor.ozon.error.service.OzonErrorRecorder;
import com.wimoor.ozon.posting.mapper.OzonPostingItemMapper;
import com.wimoor.ozon.posting.mapper.OzonPostingMapper;
import com.wimoor.ozon.posting.pojo.dto.OzonPostingSyncCommand;
import com.wimoor.ozon.posting.pojo.entity.OzonPosting;
import com.wimoor.ozon.posting.pojo.entity.OzonPostingItem;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingDetailView;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingSyncResult;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingView;
import com.wimoor.ozon.posting.service.impl.OzonPostingServiceImpl;
import com.wimoor.ozon.product.mapper.OzonProductMapMapper;
import com.wimoor.ozon.product.pojo.entity.OzonProductMap;
import com.wimoor.ozon.security.OzonCredentialService;
import com.wimoor.ozon.shipment.mapper.OzonShipmentMapper;
import com.wimoor.ozon.shipment.pojo.entity.OzonShipment;
import com.wimoor.ozon.task.mapper.OzonSyncCursorMapper;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;

@ExtendWith(MockitoExtension.class)
class OzonPostingSyncServiceTests {

    private static final String AES_KEY = "0123456789abcdef";

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonPostingMapper postingMapper;

    @Mock
    private OzonPostingItemMapper postingItemMapper;

    @Mock
    private OzonProductMapMapper productMapMapper;

    @Mock
    private OzonShipmentMapper shipmentMapper;

    @Mock
    private OzonSyncJobMapper syncJobMapper;

    @Mock
    private OzonSyncCursorMapper syncCursorMapper;

    @Mock
    private OzonSellerApiClient sellerApiClient;

    @Mock
    private ErpClientOneFeign erpClientOneFeign;

    @Mock
    private OzonErrorRecorder errorRecorder;

    @Captor
    private ArgumentCaptor<OzonPosting> postingCaptor;

    @Captor
    private ArgumentCaptor<OzonPostingItem> itemCaptor;

    @Captor
    private ArgumentCaptor<OzonPosting> updatedPostingCaptor;

    @Captor
    private ArgumentCaptor<OzonPostingItem> updatedItemCaptor;

    @Captor
    private ArgumentCaptor<OzonSyncJob> syncJobCaptor;

    private OzonPostingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonPostingServiceImpl(
                authMapper,
                postingMapper,
                postingItemMapper,
                productMapMapper,
                shipmentMapper,
                syncJobMapper,
                syncCursorMapper,
                sellerApiClient,
                erpClientOneFeign,
                new OzonCredentialService(AES_KEY),
                errorRecorder
        );
    }

    @Test
    void syncIncrementalCreatesPostingAndErpOrderFact() {
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
        productMap.setMaterialPrice(new BigDecimal("12.50"));

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(productMapMapper.selectList(any())).thenReturn(Collections.singletonList(productMap));
        when(postingMapper.selectOne(any())).thenReturn(null);
        when(sellerApiClient.listFbsPostings(eq("test-client-id"), eq("test-key"), contains("\"limit\":50")))
                .thenReturn("{\"result\":{\"postings\":[{\"posting_number\":\"posting-1\",\"status\":\"awaiting_packaging\","
                        + "\"substatus\":\"posting_awaiting_passport_data\",\"in_process_at\":\"2026-03-23T10:00:00Z\","
                        + "\"products\":[{\"offer_id\":\"offer-1\",\"quantity\":2}]}]}}");
        when(erpClientOneFeign.upsertOzonOrder(any()))
                .thenReturn(Result.success(new OzonErpOrderUpsertResult("erp-order-1")));

        OzonPostingSyncResult result = service.syncIncremental(buildUser(), new OzonPostingSyncCommand("auth-1", 7, false));

        assertEquals(1, result.getImported());
        assertFalse(result.getErpOrderIds().isEmpty());
        assertEquals("erp-order-1", result.getErpOrderIds().get(0));
        assertNotNull(result.getSyncedAt());

        verify(postingMapper).insert(postingCaptor.capture());
        verify(postingItemMapper).insert(itemCaptor.capture());
        verify(syncJobMapper).insert(syncJobCaptor.capture());
        assertEquals("posting-1", postingCaptor.getValue().getPostingNumber());
        assertEquals("ERP-SKU-1", itemCaptor.getValue().getMaterialSku());
        assertEquals(Integer.valueOf(2), itemCaptor.getValue().getQuantity());
        assertEquals("POSTING_SYNC", syncJobCaptor.getValue().getJobType());
    }

    @Test
    void syncIncrementalUsesCursorWhenRequested() {
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
        productMap.setMaterialPrice(new BigDecimal("12.50"));

        com.wimoor.ozon.task.pojo.entity.OzonSyncCursor cursor = new com.wimoor.ozon.task.pojo.entity.OzonSyncCursor();
        cursor.setId("cursor-1");
        cursor.setAuthId("auth-1");
        cursor.setCursorType("POSTING_SYNC");
        cursor.setCursorValue("2026-03-23T00:00:00Z");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(syncCursorMapper.selectOne(any())).thenReturn(cursor);
        when(syncCursorMapper.selectById("cursor-1")).thenReturn(cursor);
        when(productMapMapper.selectList(any())).thenReturn(Collections.singletonList(productMap));
        when(postingMapper.selectOne(any())).thenReturn(null);
        when(sellerApiClient.listFbsPostings(eq("test-client-id"), eq("test-key"), contains("\"since\":\"2026-03-23T00:00:00Z\"")))
                .thenReturn("{\"result\":{\"postings\":[{\"posting_number\":\"posting-1\",\"status\":\"awaiting_packaging\",\"products\":[{\"offer_id\":\"offer-1\",\"quantity\":1}]}],\"has_next\":false}}");
        when(erpClientOneFeign.upsertOzonOrder(any()))
                .thenReturn(Result.success(new OzonErpOrderUpsertResult("erp-order-1")));

        OzonPostingSyncResult result = service.syncIncremental(buildUser(), new OzonPostingSyncCommand("auth-1", null, true));

        assertEquals(true, result.isCursorUsed());
        assertEquals("2026-03-23T00:00:00Z", result.getSyncSince());
    }

    @Test
    void syncIncrementalFetchesAllPagesWhenHasNextIsTrue() {
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
        productMap.setMaterialPrice(new BigDecimal("12.50"));

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(productMapMapper.selectList(any())).thenReturn(Collections.singletonList(productMap));
        when(postingMapper.selectOne(any())).thenReturn(null);
        when(sellerApiClient.listFbsPostings(eq("test-client-id"), eq("test-key"), contains("\"offset\":0")))
                .thenReturn("{\"result\":{\"postings\":[{\"posting_number\":\"posting-1\",\"status\":\"awaiting_packaging\","
                        + "\"products\":[{\"offer_id\":\"offer-1\",\"quantity\":1}]}],\"has_next\":true}}");
        when(sellerApiClient.listFbsPostings(eq("test-client-id"), eq("test-key"), contains("\"offset\":50")))
                .thenReturn("{\"result\":{\"postings\":[{\"posting_number\":\"posting-2\",\"status\":\"awaiting_packaging\","
                        + "\"products\":[{\"offer_id\":\"offer-1\",\"quantity\":1}]}],\"has_next\":false}}");
        when(erpClientOneFeign.upsertOzonOrder(any()))
                .thenReturn(Result.success(new OzonErpOrderUpsertResult("erp-order-1")));

        OzonPostingSyncResult result = service.syncIncremental(buildUser(), new OzonPostingSyncCommand("auth-1", 7, false));

        assertEquals(2, result.getImported());
        verify(sellerApiClient).listFbsPostings(eq("test-client-id"), eq("test-key"), contains("\"offset\":0"));
        verify(sellerApiClient).listFbsPostings(eq("test-client-id"), eq("test-key"), contains("\"offset\":50"));
    }

    @Test
    void retryOneRebuildsMappingAndUpdatesBridgeStatus() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
        auth.setClientId("test-client-id");
        auth.setStatus("ACTIVE");
        auth.setApiKeyCiphertext(new ChannelCredentialCipher(AES_KEY).encrypt("test-key"));

        OzonPosting posting = new OzonPosting();
        posting.setId("posting-id-1");
        posting.setAuthId("auth-1");
        posting.setShopId("company-1");
        posting.setPostingNumber("posting-1");
        posting.setWarehouseId("warehouse-1");
        posting.setBridgeStatus("UNMAPPED");

        OzonPostingItem item = new OzonPostingItem();
        item.setId("item-1");
        item.setPostingId("posting-id-1");
        item.setAuthId("auth-1");
        item.setShopId("company-1");
        item.setPostingNumber("posting-1");
        item.setOzonOfferId("offer-1");
        item.setQuantity(2);

        OzonProductMap productMap = new OzonProductMap();
        productMap.setAuthId("auth-1");
        productMap.setMaterialSku("ERP-SKU-1");
        productMap.setOzonOfferId("offer-1");
        productMap.setMaterialPrice(new BigDecimal("12.50"));

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-id-1")).thenReturn(posting);
        when(postingItemMapper.selectList(any())).thenReturn(Collections.singletonList(item));
        when(productMapMapper.selectList(any())).thenReturn(Collections.singletonList(productMap));
        when(erpClientOneFeign.upsertOzonOrder(any()))
                .thenReturn(Result.success(new OzonErpOrderUpsertResult("erp-order-1")));

        OzonPostingSyncResult result = service.retryOne(buildUser(), "auth-1", "posting-id-1");

        assertEquals(1, result.getImported());
        assertEquals("erp-order-1", result.getErpOrderIds().get(0));

        verify(postingItemMapper).updateById(updatedItemCaptor.capture());
        assertEquals("ERP-SKU-1", updatedItemCaptor.getValue().getMaterialSku());

        verify(postingMapper).updateById(updatedPostingCaptor.capture());
        assertEquals("SYNCED", updatedPostingCaptor.getValue().getBridgeStatus());
        assertEquals("erp-order-1", updatedPostingCaptor.getValue().getErpOrderId());
    }

    @Test
    void listIncludesLatestShipmentTrackingInfo() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");

        OzonPosting posting = new OzonPosting();
        posting.setId("posting-id-1");
        posting.setAuthId("auth-1");
        posting.setShopId("company-1");
        posting.setPostingNumber("posting-1");
        posting.setFulfillmentType("FBS");
        posting.setPostingStatus("awaiting_packaging");
        posting.setBridgeStatus("SYNCED");

        OzonPostingItem item = new OzonPostingItem();
        item.setPostingId("posting-id-1");
        item.setMaterialSku("ERP-SKU-1");
        item.setQuantity(2);

        OzonShipment shipment = new OzonShipment();
        shipment.setPostingId("posting-id-1");
        shipment.setTrackingNumber("TRACK-1");
        shipment.setDeliveryService("CDEK");
        shipment.setShipmentStatus("TRACKING_SET");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectList(any())).thenReturn(Collections.singletonList(posting));
        when(postingItemMapper.selectList(any())).thenReturn(Collections.singletonList(item));
        when(shipmentMapper.selectList(any())).thenReturn(Collections.singletonList(shipment));

        OzonPostingView view = service.list(buildUser(), "auth-1", null, "FBS", null).get(0);

        assertEquals("TRACK-1", view.getLatestTrackingNumber());
        assertEquals("CDEK", view.getLatestDeliveryService());
        assertEquals("TRACKING_SET", view.getLatestShipmentStatus());
    }

    @Test
    void detailIncludesItemsAndShipments() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");

        OzonPosting posting = new OzonPosting();
        posting.setId("posting-id-1");
        posting.setAuthId("auth-1");
        posting.setShopId("company-1");
        posting.setPostingNumber("posting-1");
        posting.setBridgeStatus("SYNCED");
        posting.setErpOrderId("erp-order-1");

        OzonPostingItem item = new OzonPostingItem();
        item.setId("item-1");
        item.setPostingId("posting-id-1");
        item.setMaterialSku("ERP-SKU-1");
        item.setOzonOfferId("offer-1");
        item.setQuantity(2);

        OzonShipment shipment = new OzonShipment();
        shipment.setId("shipment-1");
        shipment.setPostingId("posting-id-1");
        shipment.setTrackingNumber("TRACK-1");
        shipment.setDeliveryService("CDEK");
        shipment.setShipmentStatus("TRACKING_SET");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-id-1")).thenReturn(posting);
        when(postingItemMapper.selectList(any())).thenReturn(Collections.singletonList(item));
        when(shipmentMapper.selectList(any())).thenReturn(Collections.singletonList(shipment));

        OzonPostingDetailView detail = service.getDetail(buildUser(), "auth-1", "posting-id-1");

        assertEquals("posting-1", detail.getPostingNumber());
        assertEquals(1, detail.getItems().size());
        assertEquals("ERP-SKU-1", detail.getItems().get(0).getMaterialSku());
        assertEquals(1, detail.getShipments().size());
        assertEquals("TRACK-1", detail.getShipments().get(0).getTrackingNumber());
    }

    @Test
    void retryOneRecordsPostingErrorWhenBridgeStillUnmapped() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
        auth.setClientId("test-client-id");
        auth.setStatus("ACTIVE");
        auth.setApiKeyCiphertext(new ChannelCredentialCipher(AES_KEY).encrypt("test-key"));

        OzonPosting posting = new OzonPosting();
        posting.setId("posting-id-1");
        posting.setAuthId("auth-1");
        posting.setShopId("company-1");
        posting.setPostingNumber("posting-1");
        posting.setWarehouseId("warehouse-1");
        posting.setBridgeStatus("UNMAPPED");
        posting.setCustomerPayloadJson("{\"posting_number\":\"posting-1\"}");

        OzonPostingItem item = new OzonPostingItem();
        item.setId("item-1");
        item.setPostingId("posting-id-1");
        item.setAuthId("auth-1");
        item.setShopId("company-1");
        item.setPostingNumber("posting-1");
        item.setOzonOfferId("offer-unmapped");
        item.setQuantity(1);

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-id-1")).thenReturn(posting);
        when(postingItemMapper.selectList(any())).thenReturn(Collections.singletonList(item));
        when(productMapMapper.selectList(any())).thenReturn(Collections.emptyList());

        OzonPostingSyncResult result = service.retryOne(buildUser(), "auth-1", "posting-id-1");

        assertEquals(1, result.getImported());
        assertTrue(result.getErpOrderIds().isEmpty());
        verify(errorRecorder).recordOpen(argThat(command ->
                "POSTING".equals(command.getSourceType())
                        && "auth-1".equals(command.getAuthId())
                        && "posting-id-1".equals(command.getObjectId())
                        && "posting-1".equals(command.getObjectCode())
        ));
    }

    @Test
    void syncIncrementalRejectsWhenWriteFeatureDisabled() {
        OzonPostingServiceImpl disabledService = new OzonPostingServiceImpl(
                authMapper,
                postingMapper,
                postingItemMapper,
                productMapMapper,
                shipmentMapper,
                syncJobMapper,
                syncCursorMapper,
                sellerApiClient,
                erpClientOneFeign,
                new OzonCredentialService(AES_KEY),
                errorRecorder,
                disabledGate()
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                disabledService.syncIncremental(buildUser(), new OzonPostingSyncCommand("auth-1", 7, false))
        );

        assertEquals("Ozon履约写操作未开启", ex.getMessage());
        verifyNoInteractions(authMapper, postingMapper, postingItemMapper, productMapMapper, shipmentMapper,
                syncJobMapper, syncCursorMapper, sellerApiClient, erpClientOneFeign, errorRecorder);
    }

    @Test
    void retryOneRejectsWhenWriteFeatureDisabled() {
        OzonPostingServiceImpl disabledService = new OzonPostingServiceImpl(
                authMapper,
                postingMapper,
                postingItemMapper,
                productMapMapper,
                shipmentMapper,
                syncJobMapper,
                syncCursorMapper,
                sellerApiClient,
                erpClientOneFeign,
                new OzonCredentialService(AES_KEY),
                errorRecorder,
                disabledGate()
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                disabledService.retryOne(buildUser(), "auth-1", "posting-id-1")
        );

        assertEquals("Ozon履约写操作未开启", ex.getMessage());
        verifyNoInteractions(authMapper, postingMapper, postingItemMapper, productMapMapper, shipmentMapper,
                syncJobMapper, syncCursorMapper, sellerApiClient, erpClientOneFeign, errorRecorder);
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
