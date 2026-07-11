package com.wimoor.ozon.stock.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.wimoor.ozon.stock.pojo.dto.OzonStockPushCommand;
import com.wimoor.ozon.stock.pojo.entity.OzonStockSnapshot;
import com.wimoor.ozon.stock.pojo.vo.OzonStockPushResult;
import com.wimoor.ozon.stock.pojo.vo.OzonStockTaskDetailView;
import com.wimoor.ozon.stock.pojo.vo.OzonStockTaskView;
import com.wimoor.ozon.stock.service.IOzonStockService;
import com.wimoor.ozon.stock.service.IOzonStockTaskQueryService;

/**
 * 测试 OzonStockController - OZON 库存 API Controller
 *
 * 测试覆盖：
 * 1. POST /push - 推送库存
 * 2. GET /snapshot/list - 获取快照列表
 * 3. GET /task/list - 获取任务列表
 * 4. GET /task/{taskId}/detail - 获取任务详情
 * 5. GET /task/history - 获取任务历史
 * 6. GET /task/by-sku - 根据 SKU 获取任务
 * 7. GET /task/error-summary - 获取错误摘要
 * 8. 错误处理 - 异常捕获和错误响应
 */
@ExtendWith(MockitoExtension.class)
class OzonStockControllerTests {

    @Mock
    private IOzonStockService stockService;

    @Mock
    private IOzonStockTaskQueryService taskQueryService;

    private OzonStockController controller;
    private UserInfo testUser;

    @BeforeEach
    void setUp() {
        controller = new OzonStockController(stockService, taskQueryService);
        testUser = buildTestUser();
        UserInfoContext.set(testUser);
    }

    @AfterEach
    void tearDown() {
        UserInfoContext.set(null);
    }

    // ==================== POST /push 测试 ====================

    @Test
    void pushShouldReturnPushResultWhenServiceSucceeds() {
        // Arrange
        OzonStockPushCommand command = new OzonStockPushCommand();
        OzonStockPushResult expectedResult = new OzonStockPushResult();
        when(stockService.push(any(UserInfo.class), eq(command)))
                .thenReturn(expectedResult);

        // Act
        Result<OzonStockPushResult> result = controller.push(command);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(expectedResult, result.getData());
        verify(stockService).push(testUser, command);
    }

    @Test
    void pushShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        OzonStockPushCommand command = new OzonStockPushCommand();
        String errorMessage = "库存推送失败";
        when(stockService.push(any(UserInfo.class), eq(command)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<OzonStockPushResult> result = controller.push(command);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== GET /snapshot/list 测试 ====================

    @Test
    void listSnapshotsShouldReturnSnapshotListWhenServiceSucceeds() {
        // Arrange
        String authId = "auth-123";
        List<OzonStockSnapshot> expectedSnapshots = Arrays.asList(
                new OzonStockSnapshot(),
                new OzonStockSnapshot()
        );
        when(stockService.listSnapshots(any(UserInfo.class), eq(authId)))
                .thenReturn(expectedSnapshots);

        // Act
        Result<List<OzonStockSnapshot>> result = controller.listSnapshots(authId);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals(expectedSnapshots, result.getData());
        verify(stockService).listSnapshots(testUser, authId);
    }

    @Test
    void listSnapshotsShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        String authId = "auth-123";
        String errorMessage = "快照列表查询失败";
        when(stockService.listSnapshots(any(UserInfo.class), eq(authId)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<List<OzonStockSnapshot>> result = controller.listSnapshots(authId);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== GET /task/list 测试 ====================

    @Test
    void listTasksShouldReturnTaskListWhenServiceSucceeds() {
        // Arrange
        String authId = "auth-123";
        List<OzonStockTaskView> expectedTasks = Arrays.asList(
                new OzonStockTaskView(),
                new OzonStockTaskView()
        );
        when(stockService.listTasks(any(UserInfo.class), eq(authId)))
                .thenReturn(expectedTasks);

        // Act
        Result<List<OzonStockTaskView>> result = controller.listTasks(authId);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals(expectedTasks, result.getData());
        verify(stockService).listTasks(testUser, authId);
    }

    @Test
    void listTasksShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        String authId = "auth-123";
        String errorMessage = "任务列表查询失败";
        when(stockService.listTasks(any(UserInfo.class), eq(authId)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<List<OzonStockTaskView>> result = controller.listTasks(authId);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== GET /task/{taskId}/detail 测试 ====================

    @Test
    void getTaskDetailShouldReturnTaskDetailWhenServiceSucceeds() {
        // Arrange
        String authId = "auth-123";
        String taskId = "task-456";
        OzonStockTaskDetailView expectedDetail = new OzonStockTaskDetailView();
        when(taskQueryService.getTaskDetail(any(UserInfo.class), eq(authId), eq(taskId)))
                .thenReturn(expectedDetail);

        // Act
        Result<OzonStockTaskDetailView> result = controller.getTaskDetail(authId, taskId);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(expectedDetail, result.getData());
        verify(taskQueryService).getTaskDetail(testUser, authId, taskId);
    }

    @Test
    void getTaskDetailShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        String authId = "auth-123";
        String taskId = "task-456";
        String errorMessage = "任务详情查询失败";
        when(taskQueryService.getTaskDetail(any(UserInfo.class), eq(authId), eq(taskId)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<OzonStockTaskDetailView> result = controller.getTaskDetail(authId, taskId);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== GET /task/history 测试 ====================

    @Test
    void listTaskHistoryShouldReturnHistoryListWhenServiceSucceeds() {
        // Arrange
        String authId = "auth-123";
        Integer limit = 10;
        List<OzonStockTaskDetailView> expectedHistory = Arrays.asList(
                new OzonStockTaskDetailView(),
                new OzonStockTaskDetailView()
        );
        when(taskQueryService.listTaskHistory(any(UserInfo.class), eq(authId), eq(limit)))
                .thenReturn(expectedHistory);

        // Act
        Result<List<OzonStockTaskDetailView>> result = controller.listTaskHistory(authId, limit);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals(expectedHistory, result.getData());
        verify(taskQueryService).listTaskHistory(testUser, authId, limit);
    }

    @Test
    void listTaskHistoryShouldAcceptNullLimit() {
        // Arrange
        String authId = "auth-123";
        List<OzonStockTaskDetailView> expectedHistory = Arrays.asList(
                new OzonStockTaskDetailView()
        );
        when(taskQueryService.listTaskHistory(any(UserInfo.class), eq(authId), eq(null)))
                .thenReturn(expectedHistory);

        // Act
        Result<List<OzonStockTaskDetailView>> result = controller.listTaskHistory(authId, null);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        verify(taskQueryService).listTaskHistory(testUser, authId, null);
    }

    @Test
    void listTaskHistoryShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        String authId = "auth-123";
        Integer limit = 10;
        String errorMessage = "任务历史查询失败";
        when(taskQueryService.listTaskHistory(any(UserInfo.class), eq(authId), eq(limit)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<List<OzonStockTaskDetailView>> result = controller.listTaskHistory(authId, limit);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== GET /task/by-sku 测试 ====================

    @Test
    void listTasksBySkuShouldReturnTaskListWhenServiceSucceeds() {
        // Arrange
        String authId = "auth-123";
        String sku = "SKU-12345";
        List<OzonStockTaskDetailView> expectedTasks = Arrays.asList(
                new OzonStockTaskDetailView(),
                new OzonStockTaskDetailView()
        );
        when(taskQueryService.listTasksBySku(any(UserInfo.class), eq(authId), eq(sku)))
                .thenReturn(expectedTasks);

        // Act
        Result<List<OzonStockTaskDetailView>> result = controller.listTasksBySku(authId, sku);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals(expectedTasks, result.getData());
        verify(taskQueryService).listTasksBySku(testUser, authId, sku);
    }

    @Test
    void listTasksBySkuShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        String authId = "auth-123";
        String sku = "SKU-12345";
        String errorMessage = "SKU 任务查询失败";
        when(taskQueryService.listTasksBySku(any(UserInfo.class), eq(authId), eq(sku)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<List<OzonStockTaskDetailView>> result = controller.listTasksBySku(authId, sku);

        // Assert
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(errorMessage, result.getMsg());
        assertNull(result.getData());
    }

    // ==================== GET /task/error-summary 测试 ====================

    @Test
    void getErrorSummaryShouldReturnSummaryMapWhenServiceSucceeds() {
        // Arrange
        String authId = "auth-123";
        Map<String, Integer> expectedSummary = new HashMap<>();
        expectedSummary.put("TIMEOUT_ERROR", 5);
        expectedSummary.put("VALIDATION_ERROR", 3);
        when(taskQueryService.getErrorSummary(any(UserInfo.class), eq(authId)))
                .thenReturn(expectedSummary);

        // Act
        Result<Map<String, Integer>> result = controller.getErrorSummary(authId);

        // Assert
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals(5, result.getData().get("TIMEOUT_ERROR"));
        assertEquals(3, result.getData().get("VALIDATION_ERROR"));
        verify(taskQueryService).getErrorSummary(testUser, authId);
    }

    @Test
    void getErrorSummaryShouldReturnFailedResultWhenServiceThrowsException() {
        // Arrange
        String authId = "auth-123";
        String errorMessage = "错误摘要查询失败";
        when(taskQueryService.getErrorSummary(any(UserInfo.class), eq(authId)))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        Result<Map<String, Integer>> result = controller.getErrorSummary(authId);

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
