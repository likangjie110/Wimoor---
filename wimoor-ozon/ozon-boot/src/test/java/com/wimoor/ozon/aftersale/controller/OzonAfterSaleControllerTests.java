package com.wimoor.ozon.aftersale.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.wimoor.ozon.aftersale.pojo.dto.OzonCancellationSaveCommand;
import com.wimoor.ozon.aftersale.pojo.dto.OzonPackageSaveCommand;
import com.wimoor.ozon.aftersale.pojo.dto.OzonReturnSaveCommand;
import com.wimoor.ozon.aftersale.pojo.entity.OzonCancellationRecord;
import com.wimoor.ozon.aftersale.pojo.entity.OzonPackageRecord;
import com.wimoor.ozon.aftersale.pojo.entity.OzonReturnRecord;
import com.wimoor.ozon.aftersale.pojo.vo.OzonAfterSaleDetailView;
import com.wimoor.ozon.aftersale.service.IOzonAfterSaleService;

/**
 * 测试 OzonAfterSaleController - OZON 售后 API Controller
 *
 * 测试覆盖：
 * 1. GET /detail - 获取售后详情
 * 2. POST /package/save - 保存包裹记录
 * 3. POST /return/save - 保存退货记录
 * 4. POST /cancellation/save - 保存取消记录
 * 5. POST /posting/cancel - 取消发货
 * 6. POST /package/sync - 同步包裹数据
 * 7. POST /return/sync - 同步退货数据
 * 8. 错误处理 - 异常捕获和错误响应
 */
@ExtendWith(MockitoExtension.class)
class OzonAfterSaleControllerTests {

    @Mock
    private IOzonAfterSaleService afterSaleService;

    private OzonAfterSaleController controller;
    private UserInfo testUser;

    @BeforeEach
    void setUp() {
        controller = new OzonAfterSaleController(afterSaleService);
        testUser = buildTestUser();
        UserInfoContext.set(testUser);
    }

    @AfterEach
    void tearDown() {
        UserInfoContext.set(null);
    }

    // ==================== GET /detail 测试 ====================

    @Test
    void detailShouldReturnAfterSaleDetailsWhenServiceSucceeds() {
        // Arrange
        String authId = "auth-123";
        String postingId = "posting-456";
        OzonAfterSaleDetailView expectedView = new OzonAfterSaleDetailView();
        when(afterSaleService.getDetail(any(UserInfo.class), eq(authId), eq(postingId)))
                .thenReturn(expectedView);

        // Act
        Result<OzonAfterSaleDetailView> result = controller.detail(authId, postingId);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(expectedView, result.getData());
        verify(afterSaleService).getDetail(testUser, authId, postingId);
    }

    @Test
    void detailShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        String authId = "auth-123";
        String postingId = "posting-456";
        String errorMessage = "售后详情查询失败";
        when(afterSaleService.getDetail(any(UserInfo.class), eq(authId), eq(postingId)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<OzonAfterSaleDetailView> result = controller.detail(authId, postingId);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== POST /package/save 测试 ====================

    @Test
    void savePackageShouldReturnSavedRecordWhenServiceSucceeds() {
        // Arrange
        OzonPackageSaveCommand command = new OzonPackageSaveCommand();
        OzonPackageRecord expectedRecord = new OzonPackageRecord();
        when(afterSaleService.savePackage(any(UserInfo.class), eq(command)))
                .thenReturn(expectedRecord);

        // Act
        Result<OzonPackageRecord> result = controller.savePackage(command);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(expectedRecord, result.getData());
        verify(afterSaleService).savePackage(testUser, command);
    }

    @Test
    void savePackageShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        OzonPackageSaveCommand command = new OzonPackageSaveCommand();
        String errorMessage = "包裹记录保存失败";
        when(afterSaleService.savePackage(any(UserInfo.class), eq(command)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<OzonPackageRecord> result = controller.savePackage(command);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== POST /return/save 测试 ====================

    @Test
    void saveReturnShouldReturnSavedRecordWhenServiceSucceeds() {
        // Arrange
        OzonReturnSaveCommand command = new OzonReturnSaveCommand();
        OzonReturnRecord expectedRecord = new OzonReturnRecord();
        when(afterSaleService.saveReturn(any(UserInfo.class), eq(command)))
                .thenReturn(expectedRecord);

        // Act
        Result<OzonReturnRecord> result = controller.saveReturn(command);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(expectedRecord, result.getData());
        verify(afterSaleService).saveReturn(testUser, command);
    }

    @Test
    void saveReturnShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        OzonReturnSaveCommand command = new OzonReturnSaveCommand();
        String errorMessage = "退货记录保存失败";
        when(afterSaleService.saveReturn(any(UserInfo.class), eq(command)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<OzonReturnRecord> result = controller.saveReturn(command);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== POST /cancellation/save 测试 ====================

    @Test
    void saveCancellationShouldReturnSavedRecordWhenServiceSucceeds() {
        // Arrange
        OzonCancellationSaveCommand command = new OzonCancellationSaveCommand();
        OzonCancellationRecord expectedRecord = new OzonCancellationRecord();
        when(afterSaleService.saveCancellation(any(UserInfo.class), eq(command)))
                .thenReturn(expectedRecord);

        // Act
        Result<OzonCancellationRecord> result = controller.saveCancellation(command);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(expectedRecord, result.getData());
        verify(afterSaleService).saveCancellation(testUser, command);
    }

    @Test
    void saveCancellationShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        OzonCancellationSaveCommand command = new OzonCancellationSaveCommand();
        String errorMessage = "取消记录保存失败";
        when(afterSaleService.saveCancellation(any(UserInfo.class), eq(command)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<OzonCancellationRecord> result = controller.saveCancellation(command);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== POST /posting/cancel 测试 ====================

    @Test
    void cancelPostingShouldReturnCancellationRecordWhenServiceSucceeds() {
        // Arrange
        String authId = "auth-123";
        String postingId = "posting-456";
        String reason = "客户请求取消";
        OzonCancellationRecord expectedRecord = new OzonCancellationRecord();
        when(afterSaleService.cancelPostingWithApi(any(UserInfo.class), eq(authId), eq(postingId), eq(reason)))
                .thenReturn(expectedRecord);

        // Act
        Result<OzonCancellationRecord> result = controller.cancelPosting(authId, postingId, reason);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(expectedRecord, result.getData());
        verify(afterSaleService).cancelPostingWithApi(testUser, authId, postingId, reason);
    }

    @Test
    void cancelPostingShouldAcceptNullReason() {
        // Arrange
        String authId = "auth-123";
        String postingId = "posting-456";
        OzonCancellationRecord expectedRecord = new OzonCancellationRecord();
        when(afterSaleService.cancelPostingWithApi(any(UserInfo.class), eq(authId), eq(postingId), eq(null)))
                .thenReturn(expectedRecord);

        // Act
        Result<OzonCancellationRecord> result = controller.cancelPosting(authId, postingId, null);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        verify(afterSaleService).cancelPostingWithApi(testUser, authId, postingId, null);
    }

    @Test
    void cancelPostingShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        String authId = "auth-123";
        String postingId = "posting-456";
        String reason = "客户请求取消";
        String errorMessage = "取消发货失败";
        when(afterSaleService.cancelPostingWithApi(any(UserInfo.class), eq(authId), eq(postingId), eq(reason)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<OzonCancellationRecord> result = controller.cancelPosting(authId, postingId, reason);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== POST /package/sync 测试 ====================

    @Test
    void syncPackagesShouldReturnSuccessWhenServiceCompletes() {
        // Arrange
        String authId = "auth-123";
        String postingId = "posting-456";

        // Act
        Result<Void> result = controller.syncPackages(authId, postingId);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNull(result.getData());
        verify(afterSaleService).syncPackagesFromApi(testUser, authId, postingId);
    }

    @Test
    void syncPackagesShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        String authId = "auth-123";
        String postingId = "posting-456";
        String errorMessage = "包裹数据同步失败";
        doThrow(new RuntimeException(errorMessage))
                .when(afterSaleService).syncPackagesFromApi(any(UserInfo.class), eq(authId), eq(postingId));

        // Act
        Result<Void> result = controller.syncPackages(authId, postingId);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== POST /return/sync 测试 ====================

    @Test
    void syncReturnsShouldReturnSuccessWhenServiceCompletes() {
        // Arrange
        String authId = "auth-123";
        String postingId = "posting-456";

        // Act
        Result<Void> result = controller.syncReturns(authId, postingId);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNull(result.getData());
        verify(afterSaleService).syncReturnsFromApi(testUser, authId, postingId);
    }

    @Test
    void syncReturnsShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        String authId = "auth-123";
        String postingId = "posting-456";
        String errorMessage = "退货数据同步失败";
        doThrow(new RuntimeException(errorMessage))
                .when(afterSaleService).syncReturnsFromApi(any(UserInfo.class), eq(authId), eq(postingId));

        // Act
        Result<Void> result = controller.syncReturns(authId, postingId);

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
