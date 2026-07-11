package com.wimoor.ozon.posting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wimoor.common.user.UserInfo;
import com.wimoor.erp.api.ErpClientOneFeign;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.error.service.OzonErrorRecorder;
import com.wimoor.ozon.posting.mapper.OzonPostingItemMapper;
import com.wimoor.ozon.posting.mapper.OzonPostingMapper;
import com.wimoor.ozon.posting.pojo.entity.OzonPosting;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingView;
import com.wimoor.ozon.posting.service.impl.OzonPostingServiceImpl;
import com.wimoor.ozon.product.mapper.OzonProductMapMapper;
import com.wimoor.ozon.security.OzonCredentialService;
import com.wimoor.ozon.shipment.mapper.OzonShipmentMapper;
import com.wimoor.ozon.task.mapper.OzonSyncCursorMapper;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;

/**
 * Phase 5: Posting 配送方式功能单元测试
 *
 * 测试内容:
 * - assignDeliveryMethod() 分配配送方式
 * - getPostingsByDeliveryMethod() 查询配送方式订单
 * - 权限验证
 * - 边界条件测试
 */
@ExtendWith(MockitoExtension.class)
class OzonPostingDeliveryMethodTests {

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

    // ==================== assignDeliveryMethod 测试 ====================

    @Test
    void assignDeliveryMethodUpdatesPostingSuccessfully() {
        // Arrange
        OzonAuth auth = buildAuth();
        OzonPosting posting = buildPosting();

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(posting);

        // Act
        service.assignDeliveryMethod(buildUser(), "auth-1", "posting-1", "delivery-method-1");

        // Assert
        verify(postingMapper).updateById(postingCaptor.capture());
        OzonPosting updated = postingCaptor.getValue();
        assertEquals("delivery-method-1", updated.getDeliveryMethodId());
        assertNotNull(updated.getUpdateTime());
    }

    @Test
    void assignDeliveryMethodAcceptsNullDeliveryMethodId() {
        // Arrange
        OzonAuth auth = buildAuth();
        OzonPosting posting = buildPosting();
        posting.setDeliveryMethodId("old-method");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(posting);

        // Act
        service.assignDeliveryMethod(buildUser(), "auth-1", "posting-1", null);

        // Assert
        verify(postingMapper).updateById(postingCaptor.capture());
        assertEquals(null, postingCaptor.getValue().getDeliveryMethodId());
    }

    @Test
    void assignDeliveryMethodTrimsDeliveryMethodId() {
        // Arrange
        OzonAuth auth = buildAuth();
        OzonPosting posting = buildPosting();

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(posting);

        // Act
        service.assignDeliveryMethod(buildUser(), "auth-1", "posting-1", "  delivery-method-1  ");

        // Assert
        verify(postingMapper).updateById(postingCaptor.capture());
        assertEquals("delivery-method-1", postingCaptor.getValue().getDeliveryMethodId());
    }

    @Test
    void assignDeliveryMethodRequiresValidAuth() {
        // Arrange
        when(authMapper.selectById("auth-1")).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                service.assignDeliveryMethod(buildUser(), "auth-1", "posting-1", "method-1")
        );
    }

    @Test
    void assignDeliveryMethodRequiresValidPostingId() {
        // Arrange
        OzonAuth auth = buildAuth();

        when(authMapper.selectById("auth-1")).thenReturn(auth);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                service.assignDeliveryMethod(buildUser(), "auth-1", null, "method-1")
        );
        assertThrows(IllegalArgumentException.class, () ->
                service.assignDeliveryMethod(buildUser(), "auth-1", "   ", "method-1")
        );
    }

    @Test
    void assignDeliveryMethodRequiresOwnedPosting() {
        // Arrange
        OzonAuth auth = buildAuth();
        OzonPosting posting = buildPosting();
        posting.setAuthId("different-auth");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(posting);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                service.assignDeliveryMethod(buildUser(), "auth-1", "posting-1", "method-1")
        );
    }

    @Test
    void assignDeliveryMethodRequiresExistingPosting() {
        // Arrange
        OzonAuth auth = buildAuth();

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectById("posting-1")).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                service.assignDeliveryMethod(buildUser(), "auth-1", "posting-1", "method-1")
        );
    }

    // ==================== getPostingsByDeliveryMethod 测试 ====================

    @Test
    void getPostingsByDeliveryMethodReturnsFilteredList() {
        // Arrange
        OzonAuth auth = buildAuth();
        OzonPosting posting1 = buildPosting();
        posting1.setId("posting-1");
        posting1.setDeliveryMethodId("method-1");

        OzonPosting posting2 = buildPosting();
        posting2.setId("posting-2");
        posting2.setDeliveryMethodId("method-1");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectList(argThat(wrapper ->
                wrapper.getSqlSegment() != null
        ))).thenReturn(Arrays.asList(posting1, posting2));

        // Act
        List<OzonPostingView> result = service.getPostingsByDeliveryMethod(buildUser(), "auth-1", "method-1");

        // Assert
        assertEquals(2, result.size());
        verify(postingMapper).selectList(any());
    }

    @Test
    void getPostingsByDeliveryMethodReturnsEmptyForNoMatches() {
        // Arrange
        OzonAuth auth = buildAuth();

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Act
        List<OzonPostingView> result = service.getPostingsByDeliveryMethod(buildUser(), "auth-1", "method-1");

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    void getPostingsByDeliveryMethodRequiresValidAuth() {
        // Arrange
        when(authMapper.selectById("auth-1")).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                service.getPostingsByDeliveryMethod(buildUser(), "auth-1", "method-1")
        );
    }

    @Test
    void getPostingsByDeliveryMethodRequiresNonBlankDeliveryMethodId() {
        // Arrange
        OzonAuth auth = buildAuth();

        when(authMapper.selectById("auth-1")).thenReturn(auth);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                service.getPostingsByDeliveryMethod(buildUser(), "auth-1", null)
        );
        assertThrows(IllegalArgumentException.class, () ->
                service.getPostingsByDeliveryMethod(buildUser(), "auth-1", "   ")
        );
    }

    @Test
    void getPostingsByDeliveryMethodOrdersByCreatedAtDesc() {
        // Arrange
        OzonAuth auth = buildAuth();
        OzonPosting posting = buildPosting();

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(postingMapper.selectList(any())).thenReturn(Arrays.asList(posting));

        // Act
        service.getPostingsByDeliveryMethod(buildUser(), "auth-1", "method-1");

        // Assert
        verify(postingMapper).selectList(any());
    }

    // ==================== Helper Methods ====================

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
        posting.setWarehouseId("warehouse-1");
        return posting;
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");
        return user;
    }
}
