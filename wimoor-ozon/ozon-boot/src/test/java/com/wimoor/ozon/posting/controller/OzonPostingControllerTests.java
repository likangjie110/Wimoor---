package com.wimoor.ozon.posting.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wimoor.common.result.Result;
import com.wimoor.common.result.ResultCode;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.ozon.posting.pojo.dto.OzonPostingSyncCommand;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingDetailView;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingSyncResult;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingView;
import com.wimoor.ozon.posting.service.IOzonPostingService;

/**
 * 测试 OzonPostingController - OZON 发货 API Controller
 *
 * 测试覆盖：
 * 1. POST /sync - 同步发货订单
 * 2. POST /retryOne - 重试单个订单
 * 3. GET /list - 获取发货订单列表
 * 4. GET /detail - 获取发货订单详情
 * 5. POST /assignDeliveryMethod - 分配配送方式
 * 6. GET /listByDeliveryMethod - 根据配送方式获取订单列表
 * 7. 错误处理 - 异常捕获和错误响应
 */
@ExtendWith(MockitoExtension.class)
class OzonPostingControllerTests {

    @Mock
    private IOzonPostingService postingService;

    private OzonPostingController controller;
    private UserInfo testUser;

    @BeforeEach
    void setUp() {
        controller = new OzonPostingController(postingService);
        testUser = buildTestUser();
        UserInfoContext.set(testUser);
    }

    @AfterEach
    void tearDown() {
        UserInfoContext.set(null);
    }

    // ==================== POST /sync 测试 ====================

    @Test
    void syncShouldReturnSyncResultWhenServiceSucceeds() {
        // Arrange
        OzonPostingSyncCommand command = new OzonPostingSyncCommand();
        OzonPostingSyncResult expectedResult = new OzonPostingSyncResult();
        when(postingService.syncIncremental(any(UserInfo.class), eq(command)))
                .thenReturn(expectedResult);

        // Act
        Result<OzonPostingSyncResult> result = controller.sync(command);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(expectedResult, result.getData());
        verify(postingService).syncIncremental(testUser, command);
    }

    @Test
    void syncShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        OzonPostingSyncCommand command = new OzonPostingSyncCommand();
        String errorMessage = "同步发货订单失败";
        when(postingService.syncIncremental(any(UserInfo.class), eq(command)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<OzonPostingSyncResult> result = controller.sync(command);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== POST /retryOne 测试 ====================

    @Test
    void retryOneShouldReturnSyncResultWhenServiceSucceeds() {
        // Arrange
        String authId = "auth-123";
        String postingId = "posting-456";
        OzonPostingSyncResult expectedResult = new OzonPostingSyncResult();
        when(postingService.retryOne(any(UserInfo.class), eq(authId), eq(postingId)))
                .thenReturn(expectedResult);

        // Act
        Result<OzonPostingSyncResult> result = controller.retryOne(authId, postingId);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(expectedResult, result.getData());
        verify(postingService).retryOne(testUser, authId, postingId);
    }

    @Test
    void retryOneShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        String authId = "auth-123";
        String postingId = "posting-456";
        String errorMessage = "重试订单失败";
        when(postingService.retryOne(any(UserInfo.class), eq(authId), eq(postingId)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<OzonPostingSyncResult> result = controller.retryOne(authId, postingId);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== GET /list 测试 ====================

    @Test
    void listShouldReturnPostingListWhenServiceSucceeds() {
        // Arrange
        String authId = "auth-123";
        String status = "awaiting_packaging";
        String fulfillmentType = "FBS";
        String keyword = "test";
        List<OzonPostingView> expectedPostings = Arrays.asList(
                new OzonPostingView(),
                new OzonPostingView()
        );
        when(postingService.list(any(UserInfo.class), eq(authId), eq(status), eq(fulfillmentType), eq(keyword)))
                .thenReturn(expectedPostings);

        // Act
        Result<List<OzonPostingView>> result = controller.list(authId, status, fulfillmentType, keyword);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals(expectedPostings, result.getData());
        verify(postingService).list(testUser, authId, status, fulfillmentType, keyword);
    }

    @Test
    void listShouldAcceptNullOptionalParameters() {
        // Arrange
        String authId = "auth-123";
        List<OzonPostingView> expectedPostings = Arrays.asList(
                new OzonPostingView()
        );
        when(postingService.list(any(UserInfo.class), eq(authId), eq(null), eq(null), eq(null)))
                .thenReturn(expectedPostings);

        // Act
        Result<List<OzonPostingView>> result = controller.list(authId, null, null, null);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        verify(postingService).list(testUser, authId, null, null, null);
    }

    @Test
    void listShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        String authId = "auth-123";
        String errorMessage = "发货订单列表查询失败";
        when(postingService.list(any(UserInfo.class), eq(authId), eq(null), eq(null), eq(null)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<List<OzonPostingView>> result = controller.list(authId, null, null, null);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== GET /detail 测试 ====================

    @Test
    void detailShouldReturnPostingDetailWhenServiceSucceeds() {
        // Arrange
        String authId = "auth-123";
        String postingId = "posting-456";
        OzonPostingDetailView expectedDetail = new OzonPostingDetailView();
        when(postingService.getDetail(any(UserInfo.class), eq(authId), eq(postingId)))
                .thenReturn(expectedDetail);

        // Act
        Result<OzonPostingDetailView> result = controller.detail(authId, postingId);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(expectedDetail, result.getData());
        verify(postingService).getDetail(testUser, authId, postingId);
    }

    @Test
    void detailShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        String authId = "auth-123";
        String postingId = "posting-456";
        String errorMessage = "发货订单详情查询失败";
        when(postingService.getDetail(any(UserInfo.class), eq(authId), eq(postingId)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<OzonPostingDetailView> result = controller.detail(authId, postingId);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== POST /assignDeliveryMethod 测试 ====================

    @Test
    void assignDeliveryMethodShouldReturnSuccessWhenServiceSucceeds() {
        // Arrange
        String authId = "auth-123";
        String postingId = "posting-456";
        String deliveryMethodId = "delivery-789";
        doNothing().when(postingService)
                .assignDeliveryMethod(any(UserInfo.class), eq(authId), eq(postingId), eq(deliveryMethodId));

        // Act
        Result<Void> result = controller.assignDeliveryMethod(authId, postingId, deliveryMethodId);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNull(result.getData());
        verify(postingService).assignDeliveryMethod(testUser, authId, postingId, deliveryMethodId);
    }

    @Test
    void assignDeliveryMethodShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        String authId = "auth-123";
        String postingId = "posting-456";
        String deliveryMethodId = "delivery-789";
        String errorMessage = "分配配送方式失败";
        doThrow(new RuntimeException(errorMessage))
                .when(postingService).assignDeliveryMethod(any(UserInfo.class), eq(authId), eq(postingId), eq(deliveryMethodId));

        // Act
        Result<Void> result = controller.assignDeliveryMethod(authId, postingId, deliveryMethodId);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== GET /listByDeliveryMethod 测试 ====================

    @Test
    void listByDeliveryMethodShouldReturnPostingListWhenServiceSucceeds() {
        // Arrange
        String authId = "auth-123";
        String deliveryMethodId = "delivery-789";
        List<OzonPostingView> expectedPostings = Arrays.asList(
                new OzonPostingView(),
                new OzonPostingView()
        );
        when(postingService.getPostingsByDeliveryMethod(any(UserInfo.class), eq(authId), eq(deliveryMethodId)))
                .thenReturn(expectedPostings);

        // Act
        Result<List<OzonPostingView>> result = controller.listByDeliveryMethod(authId, deliveryMethodId);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals(expectedPostings, result.getData());
        verify(postingService).getPostingsByDeliveryMethod(testUser, authId, deliveryMethodId);
    }

    @Test
    void listByDeliveryMethodShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        String authId = "auth-123";
        String deliveryMethodId = "delivery-789";
        String errorMessage = "配送方式订单列表查询失败";
        when(postingService.getPostingsByDeliveryMethod(any(UserInfo.class), eq(authId), eq(deliveryMethodId)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<List<OzonPostingView>> result = controller.listByDeliveryMethod(authId, deliveryMethodId);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== 辅助方法 ====================

    private UserInfo buildTestUser() {
        UserInfo user = new UserInfo();
        user.setId("test-user-123");
        user.setCompanyid("company-456");
        return user;
    }
}
