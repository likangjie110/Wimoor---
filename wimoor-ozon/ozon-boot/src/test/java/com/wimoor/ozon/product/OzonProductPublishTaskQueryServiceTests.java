package com.wimoor.ozon.product;

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
import com.wimoor.ozon.product.mapper.OzonListingPublishTaskMapper;
import com.wimoor.ozon.product.pojo.entity.OzonListingPublishTask;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishTaskListView;
import com.wimoor.ozon.product.service.impl.OzonProductPublishTaskQueryServiceImpl;

/**
 * OZON 商品发布任务查询服务测试
 *
 * @author Development Team
 * @since 2026-06-25
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OzonProductPublishTaskQueryServiceTests {

    private static final String AUTH_ID = "auth-1";
    private static final String SHOP_ID = "shop-1";
    private static final String USER_ID = "user-1";
    private static final String DRAFT_ID = "draft-1";
    private static final String TASK_ID = "task-1";

    @Mock
    private OzonAuthAccessService authAccessService;

    @Mock
    private OzonListingPublishTaskMapper publishTaskMapper;

    private OzonProductPublishTaskQueryServiceImpl service;
    private UserInfo testUser;
    private OzonAuth testAuth;

    @BeforeEach
    void setUp() {
        service = new OzonProductPublishTaskQueryServiceImpl(
            authAccessService, publishTaskMapper
        );

        testUser = createTestUser();
        testAuth = createTestAuth();

        when(authAccessService.requireOwnedAuth(any(UserInfo.class), eq(AUTH_ID)))
            .thenReturn(testAuth);
    }

    // ==================== 查询草稿任务历史测试 ====================

    @Test
    void listByDraft_ReturnsTaskListForValidDraft() {
        // Arrange
        List<OzonListingPublishTask> tasks = Arrays.asList(
            createTask(TASK_ID, "SUCCESS", 10, 8, 2),
            createTask("task-2", "FAILED", 5, 2, 3),
            createTask("task-3", "RUNNING", 8, 0, 0)
        );

        when(publishTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(tasks);

        // Act
        List<OzonProductPublishTaskListView> result = service.listByDraft(testUser, AUTH_ID, DRAFT_ID);

        // Assert
        assertEquals(3, result.size());

        OzonProductPublishTaskListView firstTask = result.get(0);
        assertEquals(TASK_ID, firstTask.getTaskId());
        assertEquals("SUCCESS", firstTask.getStatus());
        assertEquals(10, firstTask.getTotalVariants());
        assertEquals(8, firstTask.getSuccessCount());
        assertEquals(2, firstTask.getFailedCount());
    }

    @Test
    void listByDraft_ReturnsEmptyListWhenNoTasks() {
        // Arrange
        when(publishTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Collections.emptyList());

        // Act
        List<OzonProductPublishTaskListView> result = service.listByDraft(testUser, AUTH_ID, DRAFT_ID);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    void listByDraft_OrdersByCreateTimeDescending() {
        // Arrange
        Calendar cal = Calendar.getInstance();

        OzonListingPublishTask oldTask = createTask("task-old", "SUCCESS", 5, 5, 0);
        cal.add(Calendar.DAY_OF_MONTH, -2);
        oldTask.setCreateTime(cal.getTime());

        OzonListingPublishTask newTask = createTask("task-new", "SUCCESS", 5, 5, 0);
        newTask.setCreateTime(new Date());

        when(publishTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Arrays.asList(newTask, oldTask));

        // Act
        List<OzonProductPublishTaskListView> result = service.listByDraft(testUser, AUTH_ID, DRAFT_ID);

        // Assert
        assertEquals(2, result.size());
        // 验证查询条件包含排序
        verify(publishTaskMapper).selectList(any(QueryWrapper.class));
    }

    @Test
    void listByDraft_HandlesNullCountFields() {
        // Arrange
        OzonListingPublishTask task = createTask(TASK_ID, "RUNNING", null, null, null);

        when(publishTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Collections.singletonList(task));

        // Act
        List<OzonProductPublishTaskListView> result = service.listByDraft(testUser, AUTH_ID, DRAFT_ID);

        // Assert
        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getTotalVariants());
        assertEquals(0, result.get(0).getSuccessCount());
        assertEquals(0, result.get(0).getFailedCount());
    }

    // ==================== 查询任务详情测试 ====================

    @Test
    void getTaskDetail_ReturnsTaskWithDetails() {
        // Arrange
        OzonListingPublishTask task = createTask(TASK_ID, "SUCCESS", 10, 8, 2);
        task.setErrorMessage("Some errors occurred");

        when(publishTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act
        OzonProductPublishTaskListView result = service.getTaskDetail(testUser, AUTH_ID, TASK_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TASK_ID, result.getTaskId());
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("Some errors occurred", result.getErrorSummary());
        assertEquals(10, result.getTotalVariants());
        assertEquals(8, result.getSuccessCount());
        assertEquals(2, result.getFailedCount());
    }

    @Test
    void getTaskDetail_ThrowsExceptionWhenTaskNotFound() {
        // Arrange
        when(publishTaskMapper.selectById(TASK_ID)).thenReturn(null);

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
        OzonListingPublishTask task = createTask(TASK_ID, "SUCCESS", 10, 8, 2);
        task.setAuthId("wrong-auth-id");

        when(publishTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.getTaskDetail(testUser, AUTH_ID, TASK_ID)
        );

        assertTrue(exception.getMessage().contains("任务不存在或无权限"));
    }

    @Test
    void getTaskDetail_HandlesTasksWithoutCompleteTime() {
        // Arrange
        OzonListingPublishTask task = createTask(TASK_ID, "RUNNING", 5, 0, 0);
        task.setCompleteTime(null);

        when(publishTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act
        OzonProductPublishTaskListView result = service.getTaskDetail(testUser, AUTH_ID, TASK_ID);

        // Assert
        assertNotNull(result);
        assertEquals("RUNNING", result.getStatus());
        assertNull(result.getCompleteTime());
    }

    // ==================== 权限验证测试 ====================

    @Test
    void listByDraft_VerifiesAuthPermission() {
        // Arrange
        when(publishTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Collections.emptyList());

        // Act
        service.listByDraft(testUser, AUTH_ID, DRAFT_ID);

        // Assert
        verify(authAccessService, times(1))
            .requireOwnedAuth(testUser, AUTH_ID);
    }

    @Test
    void getTaskDetail_VerifiesAuthPermission() {
        // Arrange
        OzonListingPublishTask task = createTask(TASK_ID, "SUCCESS", 10, 8, 2);
        when(publishTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act
        service.getTaskDetail(testUser, AUTH_ID, TASK_ID);

        // Assert
        verify(authAccessService, times(1))
            .requireOwnedAuth(testUser, AUTH_ID);
    }

    // ==================== 数据转换测试 ====================

    @Test
    void toView_ConvertsTaskToViewCorrectly() {
        // Arrange
        OzonListingPublishTask task = createTask(TASK_ID, "SUCCESS", 20, 18, 2);
        task.setErrorMessage("Error details");

        when(publishTaskMapper.selectById(TASK_ID)).thenReturn(task);

        // Act
        OzonProductPublishTaskListView result = service.getTaskDetail(testUser, AUTH_ID, TASK_ID);

        // Assert
        assertEquals(task.getId(), result.getTaskId());
        assertEquals(task.getDraftId(), result.getDraftId());
        assertEquals(task.getStatus(), result.getStatus());
        assertEquals(task.getCreateTime(), result.getCreateTime());
        assertEquals(task.getCompleteTime(), result.getCompleteTime());
        assertEquals(task.getTotalVariants().intValue(), result.getTotalVariants());
        assertEquals(task.getSuccessCount().intValue(), result.getSuccessCount());
        assertEquals(task.getFailedCount().intValue(), result.getFailedCount());
        assertEquals(task.getErrorMessage(), result.getErrorSummary());
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

    private OzonListingPublishTask createTask(
        String taskId,
        String status,
        Integer totalVariants,
        Integer successCount,
        Integer failedCount
    ) {
        OzonListingPublishTask task = new OzonListingPublishTask();
        task.setId(taskId);
        task.setAuthId(AUTH_ID);
        task.setDraftId(DRAFT_ID);
        task.setStatus(status);
        task.setTotalVariants(totalVariants);
        task.setSuccessCount(successCount);
        task.setFailedCount(failedCount);
        task.setCreateTime(new Date());

        if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
            task.setCompleteTime(new Date());
        }

        return task;
    }
}
