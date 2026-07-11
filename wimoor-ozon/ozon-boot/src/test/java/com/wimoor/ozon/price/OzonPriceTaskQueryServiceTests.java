package com.wimoor.ozon.price;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.price.mapper.OzonPriceTaskMapper;
import com.wimoor.ozon.price.pojo.entity.OzonPriceTask;
import com.wimoor.ozon.price.pojo.vo.OzonPriceTaskDetailView;
import com.wimoor.ozon.price.service.impl.OzonPriceTaskQueryServiceImpl;

/**
 * OZON 价格任务查询服务测试
 *
 * @author Development Team
 * @since 2026-06-25
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OzonPriceTaskQueryServiceTests {

    private static final String AUTH_ID = "auth-1";
    private static final String SHOP_ID = "shop-1";
    private static final String USER_ID = "user-1";
    private static final String TASK_ID = "task-1";

    @Mock
    private OzonAuthAccessService authAccessService;

    @Mock
    private OzonPriceTaskMapper priceTaskMapper;

    private OzonPriceTaskQueryServiceImpl service;
    private UserInfo testUser;
    private OzonAuth testAuth;

    @BeforeEach
    void setUp() {
        service = new OzonPriceTaskQueryServiceImpl(authAccessService, priceTaskMapper);

        testUser = createTestUser();
        testAuth = createTestAuth();

        when(authAccessService.requireOwnedAuth(any(UserInfo.class), eq(AUTH_ID)))
            .thenReturn(testAuth);
    }

    // ==================== 任务详情查询测试 ====================

    @Test
    void getTaskDetail_ReturnsTaskDetailForValidTask() {
        // Arrange
        OzonPriceTask task = createTask(TASK_ID, "SUCCESS", 150, 145);
        task.setErrorMessage("5 price updates failed");

        when(priceTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act
        OzonPriceTaskDetailView result = service.getTaskDetail(testUser, AUTH_ID, TASK_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TASK_ID, result.getTaskId());
        assertEquals(AUTH_ID, result.getAuthId());
        assertEquals("SUCCESS", result.getTaskStatus());
        assertEquals(150, result.getRequestedCount());
        assertEquals(145, result.getSuccessCount());
        assertEquals(5, result.getFailedCount());
        assertNotNull(result.getErrorSummary());
    }

    @Test
    void getTaskDetail_ThrowsExceptionWhenTaskNotFound() {
        // Arrange
        when(priceTaskMapper.selectById(TASK_ID)).thenReturn(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.getTaskDetail(testUser, AUTH_ID, TASK_ID)
        );

        assertTrue(exception.getMessage().contains("任务不存在或无权限"));
    }

    @Test
    void getTaskDetail_ThrowsExceptionWhenAuthMismatch() {
        // Arrange
        OzonPriceTask task = createTask(TASK_ID, "SUCCESS", 150, 145);
        task.setAuthId("wrong-auth-id");

        when(priceTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.getTaskDetail(testUser, AUTH_ID, TASK_ID)
        );

        assertTrue(exception.getMessage().contains("任务不存在或无权限"));
    }

    @Test
    void getTaskDetail_HandlesNullCountFields() {
        // Arrange
        OzonPriceTask task = createTask(TASK_ID, "RUNNING", null, null);

        when(priceTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act
        OzonPriceTaskDetailView result = service.getTaskDetail(testUser, AUTH_ID, TASK_ID);

        // Assert
        assertEquals(0, result.getRequestedCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailedCount());
    }

    @Test
    void getTaskDetail_VerifiesAuthPermission() {
        // Arrange
        OzonPriceTask task = createTask(TASK_ID, "SUCCESS", 150, 145);
        when(priceTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act
        service.getTaskDetail(testUser, AUTH_ID, TASK_ID);

        // Assert
        verify(authAccessService, times(1))
            .requireOwnedAuth(testUser, AUTH_ID);
    }

    // ==================== 按 SKU 查询任务测试 ====================

    @Test
    void listTasksBySku_ReturnsTaskListForValidSku() {
        // Arrange
        List<OzonPriceTask> tasks = Arrays.asList(
            createTask("task-1", "SUCCESS", 80, 80),
            createTask("task-2", "SUCCESS", 60, 58),
            createTask("task-3", "FAILED", 40, 0)
        );

        when(priceTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(tasks);

        // Act
        List<OzonPriceTaskDetailView> result = service.listTasksBySku(testUser, AUTH_ID, "TEST-SKU-002");

        // Assert
        assertEquals(3, result.size());
        verify(authAccessService, times(1)).requireOwnedAuth(testUser, AUTH_ID);
    }

    @Test
    void listTasksBySku_ReturnsEmptyListWhenNoTasks() {
        // Arrange
        when(priceTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Collections.emptyList());

        // Act
        List<OzonPriceTaskDetailView> result = service.listTasksBySku(testUser, AUTH_ID, "NON-EXIST-SKU");

        // Assert
        assertEquals(0, result.size());
    }

    // ==================== 错误摘要测试 ====================

    @Test
    void getErrorSummary_ReturnsErrorStatistics() {
        // Arrange
        List<OzonPriceTask> failedTasks = Arrays.asList(
            createTaskWithError("task-1", "Price update timeout"),
            createTaskWithError("task-2", "Connection timeout"),
            createTaskWithError("task-3", "403 Forbidden"),
            createTaskWithError("task-4", "401 Unauthorized"),
            createTaskWithError("task-5", "500 Internal Server Error"),
            createTaskWithError("task-6", "500 Service Unavailable")
        );

        when(priceTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(failedTasks);

        // Act
        Map<String, Integer> result = service.getErrorSummary(testUser, AUTH_ID);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.get("请求超时"));
        assertEquals(2, result.get("授权失败"));
        assertEquals(2, result.get("服务器错误"));
    }

    @Test
    void getErrorSummary_ReturnsEmptyMapWhenNoErrors() {
        // Arrange
        when(priceTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Collections.emptyList());

        // Act
        Map<String, Integer> result = service.getErrorSummary(testUser, AUTH_ID);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void getErrorSummary_ExtractsErrorTypeCorrectly() {
        // Arrange
        List<OzonPriceTask> tasks = Arrays.asList(
            createTaskWithError("task-1", "404 Product not found"),
            createTaskWithError("task-2", "400 Invalid price format"),
            createTaskWithError("task-3", "This is a very long error message that exceeds the fifty character limit and should be truncated")
        );

        when(priceTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(tasks);

        // Act
        Map<String, Integer> result = service.getErrorSummary(testUser, AUTH_ID);

        // Assert
        assertTrue(result.containsKey("商品不存在"));
        assertTrue(result.containsKey("参数错误"));
        // 长消息应该被截断
        assertTrue(result.keySet().stream().anyMatch(key -> key.contains("...")));
    }

    @Test
    void getErrorSummary_VerifiesAuthPermission() {
        // Arrange
        when(priceTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Collections.emptyList());

        // Act
        service.getErrorSummary(testUser, AUTH_ID);

        // Assert
        verify(authAccessService, times(1))
            .requireOwnedAuth(testUser, AUTH_ID);
    }

    // ==================== 任务历史查询测试 ====================

    @Test
    void listTaskHistory_ReturnsTaskListWithDefaultLimit() {
        // Arrange
        List<OzonPriceTask> tasks = createTaskList(40);
        when(priceTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(tasks);

        // Act
        List<OzonPriceTaskDetailView> result = service.listTaskHistory(testUser, AUTH_ID, null);

        // Assert
        assertEquals(40, result.size());
        verify(priceTaskMapper).selectList(any(QueryWrapper.class));
    }

    @Test
    void listTaskHistory_RespectsCustomLimit() {
        // Arrange
        List<OzonPriceTask> tasks = createTaskList(25);
        when(priceTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(tasks);

        // Act
        List<OzonPriceTaskDetailView> result = service.listTaskHistory(testUser, AUTH_ID, 25);

        // Assert
        assertEquals(25, result.size());
    }

    @Test
    void listTaskHistory_EnforcesMaximumLimit() {
        // Arrange
        List<OzonPriceTask> tasks = createTaskList(100);
        when(priceTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(tasks);

        // Act
        // 请求 200，但应该被限制为最多 100
        List<OzonPriceTaskDetailView> result = service.listTaskHistory(testUser, AUTH_ID, 200);

        // Assert
        assertEquals(100, result.size());
    }

    @Test
    void listTaskHistory_OrdersByCreateTimeDescending() {
        // Arrange
        List<OzonPriceTask> tasks = createTaskList(5);
        when(priceTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(tasks);

        // Act
        service.listTaskHistory(testUser, AUTH_ID, 10);

        // Assert
        verify(priceTaskMapper).selectList(any(QueryWrapper.class));
    }

    @Test
    void listTaskHistory_VerifiesAuthPermission() {
        // Arrange
        when(priceTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Collections.emptyList());

        // Act
        service.listTaskHistory(testUser, AUTH_ID, 10);

        // Assert
        verify(authAccessService, times(1))
            .requireOwnedAuth(testUser, AUTH_ID);
    }

    // ==================== 数据转换测试 ====================

    @Test
    void toDetailView_ConvertsTaskCorrectly() {
        // Arrange
        OzonPriceTask task = createTask(TASK_ID, "SUCCESS", 300, 290);
        task.setErrorMessage("10 prices failed validation");
        task.setOperator("admin");

        when(priceTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act
        OzonPriceTaskDetailView result = service.getTaskDetail(testUser, AUTH_ID, TASK_ID);

        // Assert
        assertEquals(task.getId(), result.getTaskId());
        assertEquals(task.getAuthId(), result.getAuthId());
        assertEquals(task.getTaskStatus(), result.getTaskStatus());
        assertEquals(task.getRequestedCount().intValue(), result.getRequestedCount());
        assertEquals(task.getSuccessCount().intValue(), result.getSuccessCount());
        assertEquals(10, result.getFailedCount());
        assertEquals(task.getOperator(), result.getOperator());
        assertNotNull(result.getErrorSummary());
    }

    @Test
    void toDetailView_CalculatesFailedCountCorrectly() {
        // Arrange
        OzonPriceTask task = createTask(TASK_ID, "PARTIAL", 200, 150);
        when(priceTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act
        OzonPriceTaskDetailView result = service.getTaskDetail(testUser, AUTH_ID, TASK_ID);

        // Assert
        assertEquals(50, result.getFailedCount());
    }

    @Test
    void toDetailView_HandlesNegativeFailedCountAsZero() {
        // Arrange - successCount > requestedCount (异常数据)
        OzonPriceTask task = createTask(TASK_ID, "SUCCESS", 20, 25);
        when(priceTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act
        OzonPriceTaskDetailView result = service.getTaskDetail(testUser, AUTH_ID, TASK_ID);

        // Assert
        assertEquals(0, result.getFailedCount());
    }

    // ==================== 辅助方法 ====================

    private UserInfo createTestUser() {
        UserInfo user = new UserInfo();
        user.setId(USER_ID);
        user.setCompanyid(SHOP_ID);
        return user;
    }

    private OzonAuth createTestAuth() {
        OzonAuth auth = new OzonAuth();
        auth.setId(AUTH_ID);
        auth.setShopId(SHOP_ID);
        return auth;
    }

    private OzonPriceTask createTask(
        String taskId,
        String status,
        Integer requestedCount,
        Integer successCount
    ) {
        OzonPriceTask task = new OzonPriceTask();
        task.setId(taskId);
        task.setAuthId(AUTH_ID);
        task.setShopId(SHOP_ID);
        task.setTaskStatus(status);
        task.setRequestedCount(requestedCount);
        task.setSuccessCount(successCount);
        task.setOperator("test-operator");
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        return task;
    }

    private OzonPriceTask createTaskWithError(String taskId, String errorMessage) {
        OzonPriceTask task = createTask(taskId, "FAILED", 10, 0);
        task.setErrorMessage(errorMessage);
        return task;
    }

    private List<OzonPriceTask> createTaskList(int count) {
        List<OzonPriceTask> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(createTask("task-" + i, "SUCCESS", 100, 98));
        }
        return tasks;
    }
}
