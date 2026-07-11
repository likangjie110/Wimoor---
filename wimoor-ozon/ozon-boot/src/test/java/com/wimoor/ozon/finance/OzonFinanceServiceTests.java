package com.wimoor.ozon.finance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
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
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.finance.mapper.OzonFinTransactionMapper;
import com.wimoor.ozon.finance.mapper.OzonReportFileMapper;
import com.wimoor.ozon.finance.mapper.OzonReportTaskMapper;
import com.wimoor.ozon.finance.pojo.dto.OzonFinanceTransactionQuery;
import com.wimoor.ozon.finance.pojo.entity.OzonFinTransaction;
import com.wimoor.ozon.finance.pojo.entity.OzonReportFile;
import com.wimoor.ozon.finance.pojo.entity.OzonReportTask;
import com.wimoor.ozon.finance.pojo.vo.OzonFinanceImportResult;
import com.wimoor.ozon.finance.pojo.vo.OzonFinanceTaskView;
import com.wimoor.ozon.finance.service.impl.OzonFinanceServiceImpl;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;

/**
 * Phase 6: Finance 模块单元测试
 *
 * 测试范围：
 * 1. syncTransactionsFromApi - API 同步交易
 * 2. syncRealizationsFromApi - API 同步销售明细
 * 3. fetchReportFromApi - API 获取报告
 * 4. 权限验证
 * 5. API 失败处理
 */
@ExtendWith(MockitoExtension.class)
class OzonFinanceServiceTests {

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonSellerApiClient apiClient;

    @Mock
    private OzonReportTaskMapper reportTaskMapper;

    @Mock
    private OzonReportFileMapper reportFileMapper;

    @Mock
    private OzonFinTransactionMapper finTransactionMapper;

    @Mock
    private OzonSyncJobMapper syncJobMapper;

    @Captor
    private ArgumentCaptor<OzonReportTask> taskCaptor;

    @Captor
    private ArgumentCaptor<OzonReportFile> reportFileCaptor;

    @Captor
    private ArgumentCaptor<OzonFinTransaction> transactionCaptor;

    @Captor
    private ArgumentCaptor<OzonSyncJob> syncJobCaptor;

    private OzonFinanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonFinanceServiceImpl(
                new OzonAuthAccessService(authMapper),
                apiClient,
                reportTaskMapper,
                reportFileMapper,
                finTransactionMapper,
                syncJobMapper
        );
    }

    // ==================== syncTransactionsFromApi 测试 ====================

    @Test
    void syncTransactionsFromApiCallsApiAndSavesTransactions() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        when(apiClient.listFinanceTransactions(
                eq("client-1"),
                eq("api-key-1"),
                argThat(payload -> payload.contains("\"date\"") && payload.contains("from") && payload.contains("to"))))
                .thenReturn("{\"result\":{\"transactions\":[{\"transactionId\":\"tx-1\",\"operationType\":\"sale\",\"postingNumber\":\"post-1\",\"amount\":1000.50,\"currencyCode\":\"RUB\",\"transactionTime\":\"2026-06-20T10:00:00Z\"}]}}");

        OzonFinanceImportResult result = service.syncTransactionsFromApi(
                buildUser(),
                "auth-1",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        assertNotNull(result);
        assertEquals(1, result.getImportedCount());
        assertTrue(result.getReportId().contains("api-transactions-2026-06-01-to-2026-06-30"));

        verify(reportTaskMapper).insert(taskCaptor.capture());
        verify(reportFileMapper).insert(reportFileCaptor.capture());
        verify(finTransactionMapper).deleteByAuthIdAndReportId(eq("auth-1"), any());
        verify(finTransactionMapper, times(1)).insert(transactionCaptor.capture());
        verify(syncJobMapper).insert(syncJobCaptor.capture());
        verify(reportTaskMapper).updateById(taskCaptor.capture());
        verify(syncJobMapper).updateById(syncJobCaptor.capture());

        assertEquals("auth-1", taskCaptor.getAllValues().get(0).getAuthId());
        assertEquals("DONE", taskCaptor.getAllValues().get(1).getTaskStatus());
        assertEquals("tx-1", transactionCaptor.getValue().getTransactionId());
    }

    @Test
    void syncTransactionsFromApiValidatesDateRange() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        assertThrows(IllegalArgumentException.class, () ->
                service.syncTransactionsFromApi(buildUser(), "auth-1", null, LocalDate.of(2026, 6, 30)));

        assertThrows(IllegalArgumentException.class, () ->
                service.syncTransactionsFromApi(buildUser(), "auth-1", LocalDate.of(2026, 6, 1), null));
    }

    @Test
    void syncTransactionsFromApiFailsTaskOnApiError() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        when(apiClient.listFinanceTransactions(any(), any(), any()))
                .thenThrow(new RuntimeException("API connection failed"));

        assertThrows(RuntimeException.class, () ->
                service.syncTransactionsFromApi(
                        buildUser(),
                        "auth-1",
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 30)
                ));

        verify(reportTaskMapper).updateById(taskCaptor.capture());
        assertEquals("FAILED", taskCaptor.getValue().getTaskStatus());
        assertNotNull(taskCaptor.getValue().getErrorMessage());
    }

    @Test
    void syncTransactionsFromApiRequiresAuthPermission() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuthWithDifferentShop());

        assertThrows(IllegalArgumentException.class, () ->
                service.syncTransactionsFromApi(
                        buildUser(),
                        "auth-1",
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 30)
                ));
    }

    // ==================== syncRealizationsFromApi 测试 ====================

    @Test
    void syncRealizationsFromApiCallsApiAndSavesRealizations() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        when(apiClient.listFinanceRealizations(
                eq("client-1"),
                eq("api-key-1"),
                argThat(payload -> payload.contains("\"date\"") && payload.contains("from") && payload.contains("to"))))
                .thenReturn("{\"result\":{\"transactions\":[{\"transactionId\":\"real-1\",\"operationType\":\"realization\",\"postingNumber\":\"post-2\",\"amount\":2500.75,\"currencyCode\":\"RUB\",\"transactionTime\":\"2026-06-21T11:00:00Z\"}]}}");

        OzonFinanceImportResult result = service.syncRealizationsFromApi(
                buildUser(),
                "auth-1",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        assertNotNull(result);
        assertEquals(1, result.getImportedCount());
        assertTrue(result.getReportId().contains("api-realizations-2026-06-01-to-2026-06-30"));

        verify(reportTaskMapper).insert(taskCaptor.capture());
        verify(reportFileMapper).insert(reportFileCaptor.capture());
        verify(finTransactionMapper).deleteByAuthIdAndReportId(eq("auth-1"), any());
        verify(finTransactionMapper, times(1)).insert(transactionCaptor.capture());
        verify(syncJobMapper).insert(syncJobCaptor.capture());

        assertEquals("real-1", transactionCaptor.getValue().getTransactionId());
        assertEquals("realization", transactionCaptor.getValue().getOperationType());
    }

    @Test
    void syncRealizationsFromApiValidatesDateRange() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        assertThrows(IllegalArgumentException.class, () ->
                service.syncRealizationsFromApi(buildUser(), "auth-1", null, LocalDate.of(2026, 6, 30)));

        assertThrows(IllegalArgumentException.class, () ->
                service.syncRealizationsFromApi(buildUser(), "auth-1", LocalDate.of(2026, 6, 1), null));
    }

    @Test
    void syncRealizationsFromApiFailsTaskOnApiError() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        when(apiClient.listFinanceRealizations(any(), any(), any()))
                .thenThrow(new RuntimeException("API timeout"));

        assertThrows(RuntimeException.class, () ->
                service.syncRealizationsFromApi(
                        buildUser(),
                        "auth-1",
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 30)
                ));

        verify(reportTaskMapper).updateById(taskCaptor.capture());
        assertEquals("FAILED", taskCaptor.getValue().getTaskStatus());
    }

    // ==================== fetchReportFromApi 测试 ====================

    @Test
    void fetchReportFromApiCallsApiAndSavesReport() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        when(apiClient.getFinanceReportInfo(
                eq("client-1"),
                eq("api-key-1"),
                argThat(payload -> payload.contains("\"code\":\"seller_report\""))))
                .thenReturn("{\"result\":{\"reportId\":\"report-123\",\"status\":\"success\",\"url\":\"https://ozon.ru/reports/123\"}}");

        OzonFinanceImportResult result = service.fetchReportFromApi(
                buildUser(),
                "auth-1",
                "seller_report"
        );

        assertNotNull(result);
        assertEquals(1, result.getImportedCount());
        assertTrue(result.getReportId().contains("api-report-seller_report"));

        verify(reportTaskMapper).insert(taskCaptor.capture());
        verify(reportFileMapper).insert(reportFileCaptor.capture());
        verify(syncJobMapper).insert(syncJobCaptor.capture());
        verify(reportTaskMapper).updateById(taskCaptor.capture());

        assertEquals("DONE", taskCaptor.getValue().getTaskStatus());
    }

    @Test
    void fetchReportFromApiValidatesReportType() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        assertThrows(IllegalArgumentException.class, () ->
                service.fetchReportFromApi(buildUser(), "auth-1", null));

        assertThrows(IllegalArgumentException.class, () ->
                service.fetchReportFromApi(buildUser(), "auth-1", ""));

        assertThrows(IllegalArgumentException.class, () ->
                service.fetchReportFromApi(buildUser(), "auth-1", "   "));
    }

    @Test
    void fetchReportFromApiFailsTaskOnApiError() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        when(apiClient.getFinanceReportInfo(any(), any(), any()))
                .thenThrow(new RuntimeException("Report not available"));

        assertThrows(RuntimeException.class, () ->
                service.fetchReportFromApi(buildUser(), "auth-1", "seller_report"));

        verify(reportTaskMapper).updateById(taskCaptor.capture());
        assertEquals("FAILED", taskCaptor.getValue().getTaskStatus());
        assertEquals("Report not available", taskCaptor.getValue().getErrorMessage());
    }

    // ==================== listTransactions 测试 ====================

    @Test
    void listTransactionsReturnsFilteredResults() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonFinTransaction transaction = new OzonFinTransaction();
        transaction.setAuthId("auth-1");
        transaction.setReportId("report-1");
        transaction.setTransactionId("tx-1");
        transaction.setAmount(java.math.BigDecimal.valueOf(1000.50));
        when(finTransactionMapper.selectList(any())).thenReturn(Collections.singletonList(transaction));

        OzonFinanceTransactionQuery query = new OzonFinanceTransactionQuery();
        query.setAuthId("auth-1");
        query.setReportId("report-1");
        query.setFromDate("2026-06-01");
        query.setToDate("2026-06-30");

        List<OzonFinTransaction> transactions = service.listTransactions(buildUser(), query);

        assertEquals(1, transactions.size());
        assertEquals("tx-1", transactions.get(0).getTransactionId());
    }

    // ==================== listTasks 测试 ====================

    @Test
    void listTasksReturnsRecentTasks() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonReportTask task = new OzonReportTask();
        task.setId("task-1");
        task.setAuthId("auth-1");
        task.setReportId("report-1");
        task.setTaskStatus("DONE");
        task.setImportedCount(10);
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        when(reportTaskMapper.selectList(any())).thenReturn(Collections.singletonList(task));

        List<OzonFinanceTaskView> tasks = service.listTasks(buildUser(), "auth-1");

        assertEquals(1, tasks.size());
        assertEquals("task-1", tasks.get(0).getId());
        assertEquals("DONE", tasks.get(0).getTaskStatus());
        assertEquals(10, tasks.get(0).getImportedCount());
    }

    // ==================== Helper Methods ====================

    private OzonAuth buildAuth() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
        auth.setClientId("client-1");
        auth.setApiKey("api-key-1");
        auth.setStatus("ACTIVE");
        return auth;
    }

    private OzonAuth buildAuthWithDifferentShop() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("different-company");
        auth.setClientId("client-1");
        auth.setApiKey("api-key-1");
        auth.setStatus("ACTIVE");
        return auth;
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");
        return user;
    }
}
