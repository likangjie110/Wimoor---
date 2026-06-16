package com.wimoor.ozon.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.wimoor.ozon.auth.pojo.dto.OzonAuthBindCommand;
import com.wimoor.ozon.auth.pojo.dto.OzonRotateKeyCommand;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.impl.OzonAuthServiceImpl;
import com.wimoor.ozon.client.OzonConnectionStatus;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.seller.mapper.OzonShopConfigMapper;
import com.wimoor.ozon.seller.pojo.entity.OzonShopConfig;
import com.wimoor.ozon.security.OzonCredentialService;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJobType;

@ExtendWith(MockitoExtension.class)
class OzonAuthServiceTests {

    private static final String AES_KEY = "0123456789abcdef";

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonShopConfigMapper shopConfigMapper;

    @Mock
    private OzonSyncJobMapper syncJobMapper;

    @Mock
    private OzonSellerApiClient sellerApiClient;

    @Captor
    private ArgumentCaptor<OzonAuth> authCaptor;

    @Captor
    private ArgumentCaptor<OzonShopConfig> shopConfigCaptor;

    @Captor
    private ArgumentCaptor<OzonSyncJob> syncJobCaptor;

    private OzonAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonAuthServiceImpl(
                authMapper,
                shopConfigMapper,
                syncJobMapper,
                sellerApiClient,
                new OzonCredentialService(AES_KEY)
        );
    }

    @Test
    void bindAuthEncryptsApiKeyAndCreatesWarehouseInitJob() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");

        when(sellerApiClient.ping("test-client-id", "test-key"))
                .thenReturn(OzonConnectionStatus.success("connected"));

        OzonAuth saved = service.bindAuth(user, new OzonAuthBindCommand("Ozon RU", "test-client-id", "test-key"));

        assertEquals("test-client-id", saved.getClientId());
        assertNotNull(saved.getApiKeyCiphertext());
        assertNotEquals("test-key", saved.getApiKeyCiphertext());
        assertNull(saved.getApiKeyPlaintext());
        assertEquals("ACTIVE", saved.getStatus());
        assertNotNull(saved.getLastSyncStatus());

        verify(authMapper).insert(authCaptor.capture());
        verify(shopConfigMapper).insert(shopConfigCaptor.capture());
        verify(syncJobMapper, times(2)).insert(syncJobCaptor.capture());

        assertEquals(saved.getId(), authCaptor.getValue().getId());
        assertEquals(saved.getId(), shopConfigCaptor.getValue().getAuthId());

        List<OzonSyncJob> jobs = syncJobCaptor.getAllValues();
        assertTrue(jobs.stream().anyMatch(item -> OzonSyncJobType.INIT_SELLER.name().equals(item.getJobType())));
        assertTrue(jobs.stream().anyMatch(item -> OzonSyncJobType.INIT_WAREHOUSE.name().equals(item.getJobType())));
    }

    @Test
    void rotateKeyReEncryptsCredentialAfterConnectivityCheck() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
        auth.setClientId("test-client-id");
        auth.setStatus("ACTIVE");
        auth.setApiKeyCiphertext("legacy-cipher");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(sellerApiClient.ping("test-client-id", "rotated-key"))
                .thenReturn(OzonConnectionStatus.success("connected"));

        OzonAuth rotated = service.rotateKey(buildUser(), new OzonRotateKeyCommand("auth-1", "rotated-key"));

        assertNotNull(rotated.getApiKeyCiphertext());
        assertNotEquals("legacy-cipher", rotated.getApiKeyCiphertext());
        assertNull(rotated.getApiKeyPlaintext());
        verify(authMapper).updateById(any(OzonAuth.class));
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");
        return user;
    }
}
