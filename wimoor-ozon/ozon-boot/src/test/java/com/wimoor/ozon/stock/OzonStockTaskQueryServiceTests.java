package com.wimoor.ozon.stock;

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
import com.wimoor.ozon.stock.mapper.OzonStockTaskMapper;
import com.wimoor.ozon.stock.mapper.OzonStockSnapshotMapper;
import com.wimoor.ozon.stock.pojo.entity.OzonStockTask;
import com.wimoor.ozon.stock.pojo.entity.OzonStockSnapshot;
import com.wimoor.ozon.stock.pojo.vo.OzonStockTaskDetailView;
import com.wimoor.ozon.stock.service.impl.OzonStockTaskQueryServiceImpl;

/**
 * OZON 库存任务查询服务测试
 *
 * @author Development Team
 * @since 2026-06-25
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OzonStockTaskQueryServiceTests {

    private static final String AUTH_ID = "auth-1";
    private static final String SHOP_ID = "shop-1";
    private static final String USER_ID = "user-1";
    private static final String TASK_ID = "task-1";
    private static final String WAREHOUSE_ID = "warehouse-1";

    @Mock
    private OzonAuthAccessService authAccessService;

    @Mock
    private OzonStockTaskMapper stockTaskMapper;

    @Mock
    private OzonStockSnapshotMapper stockSnapshotMapper;

    private OzonStockTaskQueryServiceImpl service;
    private UserInfo testUser;
    private OzonAuth testAuth;

    @BeforeEach
    void setUp() {
        service = new OzonStockTaskQueryServiceImpl(authAccessService, stockTaskMapper, stockSnapshotMapper);

        testUser = createTestUser();
        testAuth = createTestAuth();

        when(authAccessService.requireOwnedAuth(any(UserInfo.class), eq(AUTH_ID)))
            .thenReturn(testAuth);
    }

    // ==================== 任务详情查询测试 ====================

    @Test
    void getTaskDetail_ReturnsTaskDetailForValidTask() {
        // Arrange
        OzonStockTask task = createTask(TASK_ID, "SUCCESS", 100, 95);
        task.setErrorMessage("5 items failed");

        when(stockTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act
        OzonStockTaskDetailView result = service.getTaskDetail(testUser, AUTH_ID, TASK_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TASK_ID, result.getTaskId());
        assertEquals(AUTH_ID, result.getAuthId());
        assertEquals(WAREHOUSE_ID, result.getWarehouseId());
        assertEquals("SUCCESS", result.getTaskStatus());
        assertEquals(100, result.getRequestedCount());
        assertEquals(95, result.getSuccessCount());
        assertEquals(5, result.getFailedCount());
        assertNotNull(result.getErrorSummary());
    }

    @Test
    void getTaskDetail_ThrowsExceptionWhenTaskNotFound() {
        // Arrange
        when(stockTaskMapper.selectById(TASK_ID)).thenReturn(null);

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
        OzonStockTask task = createTask(TASK_ID, "SUCCESS", 100, 95);
        task.setAuthId("wrong-auth-id");

        when(stockTaskMapper.selectById(TASK_ID)).thenReturn(task);

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
        OzonStockTask task = createTask(TASK_ID, "RUNNING", null, null);

        when(stockTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act
        OzonStockTaskDetailView result = service.getTaskDetail(testUser, AUTH_ID, TASK_ID);

        // Assert
        assertEquals(0, result.getRequestedCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailedCount());
    }

    @Test
    void getTaskDetail_VerifiesAuthPermission() {
        // Arrange
        OzonStockTask task = createTask(TASK_ID, "SUCCESS", 100, 95);
        when(stockTaskMapper.selectById(TASK_ID)).thenReturn(task);

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
        List<OzonStockTask> tasks = Arrays.asList(
            createTask("task-1", "SUCCESS", 50, 50),
            createTask("task-2", "SUCCESS", 30, 28),
            createTask("task-3", "FAILED", 20, 0)
        );

        List<OzonStockSnapshot> snapshots = Arrays.asList(
            createSnapshot("task-1", "TEST-SKU-001"),
            createSnapshot("task-2", "TEST-SKU-001"),
            createSnapshot("task-3", "TEST-SKU-001")
        );
        when(stockSnapshotMapper.selectList(any(QueryWrapper.class))).thenReturn(snapshots);
        when(stockTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(tasks);

        // Act
        List<OzonStockTaskDetailView> result = service.listTasksBySku(testUser, AUTH_ID, "TEST-SKU-001");

        // Assert
        assertEquals(3, result.size());
        verify(authAccessService, times(1)).requireOwnedAuth(testUser, AUTH_ID);
    }

    private OzonStockSnapshot createSnapshot(String taskId, String sku) {
        OzonStockSnapshot snapshot = new OzonStockSnapshot();
        snapshot.setTaskId(taskId);
        snapshot.setAuthId(AUTH_ID);
        snapshot.setMaterialSku(sku);
        snapshot.setQuantity(10);
        snapshot.setSyncStatus("SUCCESS");
        return snapshot;
    }

    @Test
    void listTasksBySku_ReturnsEmptyListWhenNoTasks() {
        // Arrange
        when(stockTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Collections.emptyList());

        // Act
        List<OzonStockTaskDetailView> result = service.listTasksBySku(testUser, AUTH_ID, "NON-EXIST-SKU");

        // Assert
        assertEquals(0, result.size());
    }

    // ==================== 错误摘要测试 ====================

    @Test
    void getErrorSummary_ReturnsErrorStatistics() {
        // Arrange
        List<OzonStockTask> failedTasks = Arrays.asList(
            createTaskWithError("task-1", "Request timeout occurred"),
            createTaskWithError("task-2", "Connection timeout"),
            createTaskWithError("task-3", "401 Unauthorized"),
            createTaskWithError("task-4", "401 Unauthorized"),
            createTaskWithError("task-5", "500 Internal Server Error")
        );

        when(stockTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(failedTasks);

        // Act
        Map<String, Integer> result = service.getErrorSummary(testUser, AUTH_ID);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.get("请求超时"));
        assertEquals(2, result.get("授权失败"));
        assertEquals(1, result.get("服务器错误"));
    }

    @Test
    void getErrorSummary_ReturnsEmptyMapWhenNoErrors() {
        // Arrange
        when(stockTaskMapper.selectList(any(QueryWrapper.class)))
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
        List<OzonStockTask> tasks = Arrays.asList(
            createTaskWithError("task-1", "404 Not Found"),
            createTaskWithError("task-2", "400 Bad Request"),
            createTaskWithError("task-3", "Unknown error message that is very long and exceeds fifty characters limit")
        );

        when(stockTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(tasks);

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
        when(stockTaskMapper.selectList(any(QueryWrapper.class)))
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
        List<OzonStockTask> tasks = createTaskList(30);
        when(stockTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(tasks);

        // Act
        List<OzonStockTaskDetailView> result = service.listTaskHistory(testUser, AUTH_ID, null);

        // Assert
        assertEquals(30, result.size());
        verify(stockTaskMapper).selectList(any(QueryWrapper.class));
    }

    @Test
    void listTaskHistory_RespectsCustomLimit() {
        // Arrange
        List<OzonStockTask> tasks = createTaskList(20);
        when(stockTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(tasks);

        // Act
        List<OzonStockTaskDetailView> result = service.listTaskHistory(testUser, AUTH_ID, 20);

        // Assert
        assertEquals(20, result.size());
    }

    @Test
    void listTaskHistory_EnforcesMaximumLimit() {
        // Arrange
        List<OzonStockTask> tasks = createTaskList(100);
        when(stockTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(tasks);

        // Act
        // 请求 150，但应该被限制为最多 100
        List<OzonStockTaskDetailView> result = service.listTaskHistory(testUser, AUTH_ID, 150);

        // Assert
        assertEquals(100, result.size());
    }

    @Test
    void listTaskHistory_OrdersByCreateTimeDescending() {
        // Arrange
        List<OzonStockTask> tasks = createTaskList(5);
        when(stockTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(tasks);

        // Act
        service.listTaskHistory(testUser, AUTH_ID, 10);

        // Assert
        verify(stockTaskMapper).selectList(any(QueryWrapper.class));
    }

    @Test
    void listTaskHistory_VerifiesAuthPermission() {
        // Arrange
        when(stockTaskMapper.selectList(any(QueryWrapper.class)))
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
        OzonStockTask task = createTask(TASK_ID, "SUCCESS", 200, 195);
        task.setErrorMessage("5 items failed due to validation");
        task.setOperator("admin");

        when(stockTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act
        OzonStockTaskDetailView result = service.getTaskDetail(testUser, AUTH_ID, TASK_ID);

        // Assert
        assertEquals(task.getId(), result.getTaskId());
        assertEquals(task.getAuthId(), result.getAuthId());
        assertEquals(task.getWarehouseId(), result.getWarehouseId());
        assertEquals(task.getTaskStatus(), result.getTaskStatus());
        assertEquals(task.getRequestedCount().intValue(), result.getRequestedCount());
        assertEquals(task.getSuccessCount().intValue(), result.getSuccessCount());
        assertEquals(5, result.getFailedCount());
        assertEquals(task.getOperator(), result.getOperator());
        assertNotNull(result.getErrorSummary());
    }

    @Test
    void toDetailView_CalculatesFailedCountCorrectly() {
        // Arrange
        OzonStockTask task = createTask(TASK_ID, "PARTIAL", 100, 80);
        when(stockTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act
        OzonStockTaskDetailView result = service.getTaskDetail(testUser, AUTH_ID, TASK_ID);

        // Assert
        assertEquals(20, result.getFailedCount());
    }

    @Test
    void toDetailView_HandlesNegativeFailedCountAsZero() {
        // Arrange - successCount > requestedCount (异常数据)
        OzonStockTask task = createTask(TASK_ID, "SUCCESS", 10, 15);
        when(stockTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act
        OzonStockTaskDetailView result = service.getTaskDetail(testUser, AUTH_ID, TASK_ID);

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

    private OzonStockTask createTask(
        String taskId,
        String status,
        Integer requestedCount,
        Integer successCount
    ) {
        OzonStockTask task = new OzonStockTask();
        task.setId(taskId);
        task.setAuthId(AUTH_ID);
        task.setShopId(SHOP_ID);
        task.setWarehouseId(WAREHOUSE_ID);
        task.setTaskStatus(status);
        task.setRequestedCount(requestedCount);
        task.setSuccessCount(successCount);
        task.setOperator("test-operator");
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        return task;
    }

    private OzonStockTask createTaskWithError(String taskId, String errorMessage) {
        OzonStockTask task = createTask(taskId, "FAILED", 10, 0);
        task.setErrorMessage(errorMessage);
        return task;
    }

    private List<OzonStockTask> createTaskList(int count) {
        List<OzonStockTask> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(createTask("task-" + i, "SUCCESS", 100, 95));
        }
        return tasks;
    }
}
