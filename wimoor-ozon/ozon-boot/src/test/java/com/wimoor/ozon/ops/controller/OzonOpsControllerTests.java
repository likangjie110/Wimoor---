package com.wimoor.ozon.ops.controller;

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
import java.util.Collections;
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
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogQuery;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditQuery;
import com.wimoor.ozon.ops.pojo.entity.OzonApiLog;
import com.wimoor.ozon.ops.pojo.entity.OzonOperationAudit;
import com.wimoor.ozon.ops.pojo.vo.OzonOpsSummaryView;
import com.wimoor.ozon.ops.service.IOzonOpsService;

/**
 * 测试 OzonOpsController - OZON 运营 API Controller
 *
 * 测试覆盖：
 * 1. GET /summary - 获取运营概览
 * 2. GET /api-log/list - 获取 API 调用日志列表
 * 3. GET /operation-audit/list - 获取操作审计列表
 * 4. 错误处理 - 异常捕获和错误响应
 * 5. Feature Gate 检查
 */
@ExtendWith(MockitoExtension.class)
class OzonOpsControllerTests {

    @Mock
    private IOzonOpsService opsService;

    @Mock
    private OzonFeatureGate featureGate;

    private OzonOpsController controller;
    private UserInfo testUser;

    @BeforeEach
    void setUp() {
        controller = new OzonOpsController(opsService, featureGate);
        testUser = buildTestUser();
        UserInfoContext.set(testUser);
    }

    @AfterEach
    void tearDown() {
        UserInfoContext.set(null);
    }

    // ==================== GET /summary 测试 ====================

    @Test
    void summaryShouldReturnSummaryViewWhenServiceSucceeds() {
        // Arrange
        String authId = "auth-123";
        OzonOpsSummaryView expectedSummary = new OzonOpsSummaryView();
        doNothing().when(featureGate).assertAuthEnabled();
        when(opsService.summary(any(UserInfo.class), eq(authId)))
                .thenReturn(expectedSummary);

        // Act
        Result<OzonOpsSummaryView> result = controller.summary(authId);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(expectedSummary, result.getData());
        verify(featureGate).assertAuthEnabled();
        verify(opsService).summary(testUser, authId);
    }

    @Test
    void summaryShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        String authId = "auth-123";
        String errorMessage = "运营概览查询失败";
        doNothing().when(featureGate).assertAuthEnabled();
        when(opsService.summary(any(UserInfo.class), eq(authId)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<OzonOpsSummaryView> result = controller.summary(authId);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void summaryShouldReturnFailedResultWhenFeatureGateThrowsException() {
        // Arrange
        String authId = "auth-123";
        String errorMessage = "功能未启用";
        doThrow(new RuntimeException(errorMessage)).when(featureGate).assertAuthEnabled();

        // Act
        Result<OzonOpsSummaryView> result = controller.summary(authId);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
        verify(featureGate).assertAuthEnabled();
    }

    // ==================== GET /api-log/list 测试 ====================

    @Test
    void apiLogListShouldReturnApiLogsWhenServiceSucceeds() {
        // Arrange
        OzonApiLogQuery query = new OzonApiLogQuery();
        List<OzonApiLog> expectedLogs = Arrays.asList(
                new OzonApiLog(),
                new OzonApiLog()
        );
        doNothing().when(featureGate).assertAuthEnabled();
        when(opsService.listApiLogs(any(UserInfo.class), eq(query)))
                .thenReturn(expectedLogs);

        // Act
        Result<List<OzonApiLog>> result = controller.apiLogList(query);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals(expectedLogs, result.getData());
        verify(featureGate).assertAuthEnabled();
        verify(opsService).listApiLogs(testUser, query);
    }

    @Test
    void apiLogListShouldReturnEmptyListWhenNoLogsFound() {
        // Arrange
        OzonApiLogQuery query = new OzonApiLogQuery();
        List<OzonApiLog> emptyLogs = Collections.emptyList();
        doNothing().when(featureGate).assertAuthEnabled();
        when(opsService.listApiLogs(any(UserInfo.class), eq(query)))
                .thenReturn(emptyLogs);

        // Act
        Result<List<OzonApiLog>> result = controller.apiLogList(query);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(0, result.getData().size());
        verify(featureGate).assertAuthEnabled();
        verify(opsService).listApiLogs(testUser, query);
    }

    @Test
    void apiLogListShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        OzonApiLogQuery query = new OzonApiLogQuery();
        String errorMessage = "API日志查询失败";
        doNothing().when(featureGate).assertAuthEnabled();
        when(opsService.listApiLogs(any(UserInfo.class), eq(query)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<List<OzonApiLog>> result = controller.apiLogList(query);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void apiLogListShouldReturnFailedResultWhenFeatureGateThrowsException() {
        // Arrange
        OzonApiLogQuery query = new OzonApiLogQuery();
        String errorMessage = "功能未启用";
        doThrow(new RuntimeException(errorMessage)).when(featureGate).assertAuthEnabled();

        // Act
        Result<List<OzonApiLog>> result = controller.apiLogList(query);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
        verify(featureGate).assertAuthEnabled();
    }

    // ==================== GET /operation-audit/list 测试 ====================

    @Test
    void operationAuditListShouldReturnAuditsWhenServiceSucceeds() {
        // Arrange
        OzonOperationAuditQuery query = new OzonOperationAuditQuery();
        List<OzonOperationAudit> expectedAudits = Arrays.asList(
                new OzonOperationAudit(),
                new OzonOperationAudit(),
                new OzonOperationAudit()
        );
        doNothing().when(featureGate).assertAuthEnabled();
        when(opsService.listOperationAudits(any(UserInfo.class), eq(query)))
                .thenReturn(expectedAudits);

        // Act
        Result<List<OzonOperationAudit>> result = controller.operationAuditList(query);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(3, result.getData().size());
        assertEquals(expectedAudits, result.getData());
        verify(featureGate).assertAuthEnabled();
        verify(opsService).listOperationAudits(testUser, query);
    }

    @Test
    void operationAuditListShouldReturnEmptyListWhenNoAuditsFound() {
        // Arrange
        OzonOperationAuditQuery query = new OzonOperationAuditQuery();
        List<OzonOperationAudit> emptyAudits = Collections.emptyList();
        doNothing().when(featureGate).assertAuthEnabled();
        when(opsService.listOperationAudits(any(UserInfo.class), eq(query)))
                .thenReturn(emptyAudits);

        // Act
        Result<List<OzonOperationAudit>> result = controller.operationAuditList(query);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(0, result.getData().size());
        verify(featureGate).assertAuthEnabled();
        verify(opsService).listOperationAudits(testUser, query);
    }

    @Test
    void operationAuditListShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        OzonOperationAuditQuery query = new OzonOperationAuditQuery();
        String errorMessage = "操作审计查询失败";
        doNothing().when(featureGate).assertAuthEnabled();
        when(opsService.listOperationAudits(any(UserInfo.class), eq(query)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<List<OzonOperationAudit>> result = controller.operationAuditList(query);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void operationAuditListShouldReturnFailedResultWhenFeatureGateThrowsException() {
        // Arrange
        OzonOperationAuditQuery query = new OzonOperationAuditQuery();
        String errorMessage = "功能未启用";
        doThrow(new RuntimeException(errorMessage)).when(featureGate).assertAuthEnabled();

        // Act
        Result<List<OzonOperationAudit>> result = controller.operationAuditList(query);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
        verify(featureGate).assertAuthEnabled();
    }

    // ==================== 边界测试 ====================

    @Test
    void summaryShouldHandleNullAuthId() {
        // Arrange
        doNothing().when(featureGate).assertAuthEnabled();
        OzonOpsSummaryView expectedSummary = new OzonOpsSummaryView();
        when(opsService.summary(any(UserInfo.class), eq(null)))
                .thenReturn(expectedSummary);

        // Act
        Result<OzonOpsSummaryView> result = controller.summary(null);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        verify(opsService).summary(testUser, null);
    }

    // ==================== 辅助方法 ====================

    private UserInfo buildTestUser() {
        UserInfo user = new UserInfo();
        user.setId("test-user-123");
        user.setCompanyid("company-456");
        return user;
    }
}
