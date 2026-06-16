package com.wimoor.ozon.aftersale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.config.OzonFeatureProperties;
import com.wimoor.ozon.posting.mapper.OzonPostingMapper;
import com.wimoor.ozon.posting.pojo.entity.OzonPosting;

@ExtendWith(MockitoExtension.class)
class OzonAfterSaleServiceTests {

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

    @Captor
    private ArgumentCaptor<OzonPackageRecord> packageCaptor;

    private OzonAfterSaleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonAfterSaleServiceImpl(
                new OzonAuthAccessService(authMapper),
                postingMapper,
                packageRecordMapper,
                returnRecordMapper,
                cancellationRecordMapper,
                OzonFeatureGate.allEnabled()
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
                new OzonFeatureGate(properties)
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

    private OzonAuth buildAuth() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
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
