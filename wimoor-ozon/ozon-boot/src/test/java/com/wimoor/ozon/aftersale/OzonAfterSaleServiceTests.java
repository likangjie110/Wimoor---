package com.wimoor.ozon.aftersale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.wimoor.ozon.aftersale.mapper.OzonCancellationRecordMapper;
import com.wimoor.ozon.aftersale.mapper.OzonPackageRecordMapper;
import com.wimoor.ozon.aftersale.mapper.OzonReturnRecordMapper;
import com.wimoor.ozon.aftersale.pojo.dto.OzonCancellationSaveCommand;
import com.wimoor.ozon.aftersale.pojo.dto.OzonPackageSaveCommand;
import com.wimoor.ozon.aftersale.pojo.dto.OzonReturnSaveCommand;
import com.wimoor.ozon.aftersale.pojo.entity.OzonCancellationRecord;
import com.wimoor.ozon.aftersale.pojo.entity.OzonPackageRecord;
import com.wimoor.ozon.aftersale.pojo.entity.OzonReturnRecord;
import com.wimoor.ozon.aftersale.pojo.vo.OzonAfterSaleDetailView;
import com.wimoor.ozon.aftersale.service.impl.OzonAfterSaleServiceImpl;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.config.OzonFeatureProperties;
import com.wimoor.ozon.posting.mapper.OzonPostingMapper;
import com.wimoor.ozon.posting.pojo.entity.OzonPosting;
import com.wimoor.ozon.security.OzonCredentialService;

@ExtendWith(MockitoExtension.class)
class OzonAfterSaleServiceTests {

    private static final String AES_KEY = "0123456789abcdef";

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonPostingMapper postingMapper;

    @Mock
    private OzonPackageRecordMapper packageRecordMapper;

    @Mock
    private OzonReturnRecordMapper returnRecordMapper;

    @Mock
    private OzonCancellationRecordMapper cancellationRecordMapper;

    @Mock
    private OzonSellerApiClient sellerApiClient;

    @Captor
    private ArgumentCaptor<OzonPackageRecord> packageCaptor;

    @Captor
    private ArgumentCaptor<OzonReturnRecord> returnCaptor;

    @Captor
    private ArgumentCaptor<OzonCancellationRecord> cancellationCaptor;

    @Captor
    private ArgumentCaptor<OzonPosting> postingCaptor;

    private OzonAfterSaleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonAfterSaleServiceImpl(
                authMapper,
                postingMapper,
                packageRecordMapper,
                returnRecordMapper,
                cancellationRecordMapper,
                sellerApiClient,
                new OzonCredentialService(AES_KEY)
        );
    }

    @Test
    void detailReturnsAfterSaleCollections() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        when(postingMapper.selectById("posting-1")).thenReturn(buildPosting());

        OzonPackageRecord packageRecord = new OzonPackageRecord();
        packageRecord.setId("pkg-1");
        packageRecord.setPostingId("posting-1");
        packageRecord.setPackageNumber("PKG-001");

        OzonReturnRecord returnRecord = new OzonReturnRecord();
        returnRecord.setId("ret-1");
        returnRecord.setPostingId("posting-1");
        returnRecord.setReturnNumber("RET-001");

        OzonCancellationRecord cancellationRecord = new OzonCancellationRecord();
        cancellationRecord.setId("can-1");
        cancellationRecord.setPostingId("posting-1");
        cancellationRecord.setCancellationNumber("CAN-001");

        when(packageRecordMapper.selectList(any())).thenReturn(Collections.singletonList(packageRecord));
        when(returnRecordMapper.selectList(any())).thenReturn(Collections.singletonList(returnRecord));
        when(cancellationRecordMapper.selectList(any())).thenReturn(Collections.singletonList(cancellationRecord));

        OzonAfterSaleDetailView detail = service.getDetail(buildUser(), "auth-1", "posting-1");

        assertEquals(1, detail.getPackages().size());
        assertEquals("PKG-001", detail.getPackages().get(0).getPackageNumber());
        assertEquals(1, detail.getReturns().size());
        assertEquals("RET-001", detail.getReturns().get(0).getReturnNumber());
        assertEquals(1, detail.getCancellations().size());
        assertEquals("CAN-001", detail.getCancellations().get(0).getCancellationNumber());
    }

    @Test
    void savePackageCreatesPackageRecord() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        when(postingMapper.selectById("posting-1")).thenReturn(buildPosting());
        when(packageRecordMapper.selectOne(any())).thenReturn(null);

        OzonPackageRecord result = service.savePackage(
                buildUser(),
                new OzonPackageSaveCommand("auth-1", "posting-1", null, "PKG-001", "CREATED", "TRACK-1", "{\"x\":1}")
        );

        assertEquals("PKG-001", result.getPackageNumber());
        verify(packageRecordMapper).insert(packageCaptor.capture());
        assertEquals("posting-1", packageCaptor.getValue().getPostingId());
    }

    @Test
    void saveReturnAndCancellationRequirePostingWriteFeature() {
        OzonFeatureProperties properties = new OzonFeatureProperties();
        properties.setPostingWrite(false);
        OzonAfterSaleServiceImpl disabledService = new OzonAfterSaleServiceImpl(
                new OzonAuthAccessService(authMapper),
                postingMapper,
                packageRecordMapper,
                returnRecordMapper,
                cancellationRecordMapper,
                new OzonFeatureGate(properties),
                sellerApiClient,
                new OzonCredentialService(AES_KEY)
        );

        IllegalStateException returnEx = assertThrows(IllegalStateException.class, () -> disabledService.saveReturn(
                buildUser(),
                new OzonReturnSaveCommand("auth-1", "posting-1", null, "RET-001", "OPEN", "damaged", 1, null)
        ));
        IllegalStateException cancellationEx = assertThrows(IllegalStateException.class, () -> disabledService.saveCancellation(
                buildUser(),
                new OzonCancellationSaveCommand("auth-1", "posting-1", null, "CAN-001", "OPEN", "buyer request", null)
        ));

        assertEquals("Ozon履约写操作未开启", returnEx.getMessage());
        assertEquals("Ozon履约写操作未开启", cancellationEx.getMessage());
        verifyNoInteractions(authMapper, postingMapper, returnRecordMapper, cancellationRecordMapper);
    }

    // ==================== API 集成测试 ====================

    @Test
    void syncReturnsFromApiCreatesReturnRecords() {
        // Arrange
        OzonAuth auth = buildAuth();
        OzonPosting posting = buildPosting();

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(posting);
        when(returnRecordMapper.selectOne(any())).thenReturn(null);
        when(sellerApiClient.listReturns(eq("test-client-id"), eq("test-key"), any()))
                .thenReturn("{\"returns\":[{\"return_number\":\"RET-001\",\"status\":\"PENDING\",\"reason\":\"damaged\",\"quantity\":1}]}");

        // Act
        service.syncReturnsFromApi(buildUser(), "auth-1", "posting-1");

        // Assert
        verify(returnRecordMapper).insert(returnCaptor.capture());
        OzonReturnRecord captured = returnCaptor.getValue();
        assertEquals("RET-001", captured.getReturnNumber());
        assertEquals("PENDING", captured.getReturnStatus());
        assertEquals("damaged", captured.getReason());
        assertEquals(Integer.valueOf(1), captured.getQuantity());
        assertEquals("auth-1", captured.getAuthId());
        assertEquals("posting-1", captured.getPostingId());
    }

    @Test
    void syncReturnsFromApiHandlesEmptyResponse() {
        // Arrange
        OzonAuth auth = buildAuth();
        OzonPosting posting = buildPosting();

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(posting);
        when(sellerApiClient.listReturns(eq("test-client-id"), eq("test-key"), any()))
                .thenReturn("{\"returns\":[]}");

        // Act
        service.syncReturnsFromApi(buildUser(), "auth-1", "posting-1");

        // Assert
        verify(returnRecordMapper, org.mockito.Mockito.never()).insert(any());
    }

    @Test
    void syncReturnsFromApiRequiresValidAuth() {
        // Arrange
        when(authMapper.selectById("auth-1")).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                service.syncReturnsFromApi(buildUser(), "auth-1", "posting-1")
        );
        verifyNoInteractions(sellerApiClient);
    }

    @Test
    void syncReturnsFromApiRequiresOwnedPosting() {
        // Arrange
        OzonAuth auth = buildAuth();
        OzonPosting posting = buildPosting();
        posting.setAuthId("different-auth");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(posting);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                service.syncReturnsFromApi(buildUser(), "auth-1", "posting-1")
        );
        verifyNoInteractions(sellerApiClient);
    }

    @Test
    void cancelPostingWithApiCallsOzonApiAndSavesCancellation() {
        // Arrange
        OzonAuth auth = buildAuth();
        OzonPosting posting = buildPosting();

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(posting);
        when(sellerApiClient.cancelPosting(eq("test-client-id"), eq("test-key"), any()))
                .thenReturn("{\"result\":{\"status\":\"cancelled\"}}");

        // Act
        OzonCancellationRecord result = service.cancelPostingWithApi(buildUser(), "auth-1", "posting-1", "buyer request");

        // Assert
        assertNotNull(result);
        assertEquals("buyer request", result.getReason());
        assertEquals("CANCELLED", result.getCancellationStatus());
        verify(cancellationRecordMapper).insert(cancellationCaptor.capture());
        verify(postingMapper).updateById(postingCaptor.capture());
        assertEquals("cancelled", postingCaptor.getValue().getPostingStatus());
    }

    @Test
    void cancelPostingWithApiRequiresPostingWriteFeature() {
        // Arrange
        OzonFeatureProperties properties = new OzonFeatureProperties();
        properties.setPostingWrite(false);
        OzonAfterSaleServiceImpl disabledService = new OzonAfterSaleServiceImpl(
                new OzonAuthAccessService(authMapper),
                postingMapper,
                packageRecordMapper,
                returnRecordMapper,
                cancellationRecordMapper,
                new OzonFeatureGate(properties),
                sellerApiClient,
                new OzonCredentialService(AES_KEY)
        );

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                disabledService.cancelPostingWithApi(buildUser(), "auth-1", "posting-1", "reason")
        );
        assertEquals("Ozon履约写操作未开启", ex.getMessage());
        verifyNoInteractions(authMapper, sellerApiClient);
    }

    @Test
    void cancelPostingWithApiRollbacksOnFailure() {
        // Arrange
        OzonAuth auth = buildAuth();
        OzonPosting posting = buildPosting();

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(posting);
        when(sellerApiClient.cancelPosting(any(), any(), any()))
                .thenThrow(new RuntimeException("API call failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                service.cancelPostingWithApi(buildUser(), "auth-1", "posting-1", "reason")
        );
        verify(cancellationRecordMapper, org.mockito.Mockito.never()).insert(any());
        verify(postingMapper, org.mockito.Mockito.never()).updateById(any());
    }

    @Test
    void syncPackagesFromApiCreatesPackageRecords() {
        // Arrange
        OzonAuth auth = buildAuth();
        OzonPosting posting = buildPosting();

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(posting);
        when(packageRecordMapper.selectOne(any())).thenReturn(null);
        when(sellerApiClient.getPostingPackages(eq("test-client-id"), eq("test-key"), any()))
                .thenReturn("{\"result\":[{\"posting_number\":\"POSTING-001\",\"status\":\"DELIVERED\",\"tracking_number\":\"TRACK-123\"}]}");

        // Act
        service.syncPackagesFromApi(buildUser(), "auth-1", "posting-1");

        // Assert
        verify(packageRecordMapper).insert(packageCaptor.capture());
        OzonPackageRecord captured = packageCaptor.getValue();
        assertEquals("POSTING-001", captured.getPackageNumber());
        assertEquals("DELIVERED", captured.getPackageStatus());
        assertEquals("TRACK-123", captured.getTrackingNumber());
    }

    @Test
    void syncPackagesFromApiUpdatesExistingPackages() {
        // Arrange
        OzonAuth auth = buildAuth();
        OzonPosting posting = buildPosting();
        OzonPackageRecord existing = new OzonPackageRecord();
        existing.setId("pkg-1");
        existing.setAuthId("auth-1");
        existing.setPackageNumber("POSTING-001");
        existing.setPackageStatus("IN_TRANSIT");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(posting);
        when(packageRecordMapper.selectOne(any())).thenReturn(existing);
        when(packageRecordMapper.selectById("pkg-1")).thenReturn(existing);
        when(sellerApiClient.getPostingPackages(eq("test-client-id"), eq("test-key"), any()))
                .thenReturn("{\"result\":[{\"posting_number\":\"POSTING-001\",\"status\":\"DELIVERED\",\"tracking_number\":\"TRACK-123\"}]}");

        // Act
        service.syncPackagesFromApi(buildUser(), "auth-1", "posting-1");

        // Assert
        verify(packageRecordMapper).updateById(argThat(pkg ->
                "DELIVERED".equals(pkg.getPackageStatus()) && "TRACK-123".equals(pkg.getTrackingNumber())
        ));
    }

    @Test
    void syncPackagesFromApiHandlesNullResult() {
        // Arrange
        OzonAuth auth = buildAuth();
        OzonPosting posting = buildPosting();

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(posting);
        when(sellerApiClient.getPostingPackages(any(), any(), any()))
                .thenReturn("{\"result\":null}");

        // Act
        service.syncPackagesFromApi(buildUser(), "auth-1", "posting-1");

        // Assert
        verify(packageRecordMapper, org.mockito.Mockito.never()).insert(any());
    }

    @Test
    void syncPackagesFromApiRequiresValidPosting() {
        // Arrange
        OzonAuth auth = buildAuth();

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                service.syncPackagesFromApi(buildUser(), "auth-1", "posting-1")
        );
        verifyNoInteractions(sellerApiClient);
    }

    private OzonAuth buildAuth() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
        auth.setClientId("test-client-id");
        auth.setApiKeyCiphertext(new ChannelCredentialCipher(AES_KEY).encrypt("test-key"));
        return auth;
    }

    private OzonPosting buildPosting() {
        OzonPosting posting = new OzonPosting();
        posting.setId("posting-1");
        posting.setAuthId("auth-1");
        posting.setShopId("company-1");
        posting.setPostingNumber("POSTING-001");
        return posting;
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");
        return user;
    }
}
