package com.wimoor.ozon.finance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.wimoor.ozon.finance.mapper.OzonFinTransactionMapper;
import com.wimoor.ozon.finance.mapper.OzonReportFileMapper;
import com.wimoor.ozon.finance.mapper.OzonReportTaskMapper;
import com.wimoor.ozon.finance.pojo.dto.OzonFinanceImportCommand;
import com.wimoor.ozon.finance.pojo.dto.OzonFinanceTransactionQuery;
import com.wimoor.ozon.finance.pojo.entity.OzonFinTransaction;
import com.wimoor.ozon.finance.pojo.entity.OzonReportFile;
import com.wimoor.ozon.finance.pojo.entity.OzonReportTask;
import com.wimoor.ozon.finance.pojo.vo.OzonFinanceImportResult;
import com.wimoor.ozon.finance.pojo.vo.OzonFinanceTaskView;
import com.wimoor.ozon.finance.service.impl.OzonFinanceServiceImpl;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;

@ExtendWith(MockitoExtension.class)
class OzonFinanceImportTests {

    @Mock
    private OzonAuthMapper authMapper;

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
    private ArgumentCaptor<OzonReportFile> fileCaptor;

    @Captor
    private ArgumentCaptor<OzonFinTransaction> transactionCaptor;

    @Captor
    private ArgumentCaptor<OzonSyncJob> syncJobCaptor;

    private OzonFinanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonFinanceServiceImpl(
                new OzonAuthAccessService(authMapper),
                reportTaskMapper,
                reportFileMapper,
                finTransactionMapper,
                syncJobMapper
        );
    }

    @Test
    void importReportCreatesTaskFileTransactionsAndSyncJob() {
        OzonAuth auth = buildAuth();
        when(authMapper.selectById("auth-1")).thenReturn(auth);

        OzonFinanceImportResult result = service.importReport(
                buildUser(),
                new OzonFinanceImportCommand(
                        "auth-1",
                        "report-1",
                        "2026-03-26",
                        "{\"transactions\":["
                                + "{\"transactionId\":\"txn-1\",\"operationType\":\"sale\",\"postingNumber\":\"posting-1\","
                                + "\"amount\":12.50,\"currencyCode\":\"RUB\",\"transactionTime\":\"2026-03-26T08:00:00Z\"},"
                                + "{\"transactionId\":\"txn-2\",\"operationType\":\"commission\",\"postingNumber\":\"posting-1\","
                                + "\"amount\":-1.25,\"currencyCode\":\"RUB\",\"transactionTime\":\"2026-03-26T08:05:00Z\"}"
                                + "]}"
                )
        );

        assertEquals(2, result.getImportedCount());
        verify(reportTaskMapper).insert(taskCaptor.capture());
        verify(reportFileMapper).insert(fileCaptor.capture());
        verify(finTransactionMapper).deleteByAuthIdAndReportId("auth-1", "report-1");
        verify(finTransactionMapper, times(2)).insert(transactionCaptor.capture());
        verify(syncJobMapper).insert(syncJobCaptor.capture());
        assertEquals("report-1", taskCaptor.getValue().getReportId());
        assertEquals(taskCaptor.getValue().getId(), fileCaptor.getValue().getTaskId());
        assertEquals("FINANCE_IMPORT", syncJobCaptor.getValue().getJobType());
        assertEquals(2, transactionCaptor.getAllValues().size());
    }

    @Test
    void listTasksReturnsRecentImports() {
        OzonAuth auth = buildAuth();
        OzonReportTask task = new OzonReportTask();
        task.setId("task-1");
        task.setAuthId("auth-1");
        task.setShopId("company-1");
        task.setReportId("report-1");
        task.setTaskStatus("DONE");
        task.setImportedCount(2);
        task.setCreateTime(new Date(0L));
        task.setUpdateTime(new Date(1000L));

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(reportTaskMapper.selectList(any())).thenReturn(Collections.singletonList(task));

        List<OzonFinanceTaskView> views = service.listTasks(buildUser(), "auth-1");

        assertEquals(1, views.size());
        assertEquals("task-1", views.get(0).getId());
        assertEquals("report-1", views.get(0).getReportId());
        assertEquals("DONE", views.get(0).getTaskStatus());
        assertEquals("LOCAL_IMPORT", views.get(0).getSourceMode());
        assertEquals(Boolean.TRUE, views.get(0).getRawContentReady());
    }

    @Test
    void listTransactionsFiltersByAuthDateAndReportId() {
        OzonAuth auth = buildAuth();
        OzonFinTransaction transaction = new OzonFinTransaction();
        transaction.setId("txn-row-1");
        transaction.setAuthId("auth-1");
        transaction.setReportId("report-1");
        transaction.setTransactionId("txn-1");
        transaction.setOperationType("sale");
        transaction.setPostingNumber("posting-1");
        transaction.setAmount(new BigDecimal("12.50"));
        transaction.setCurrencyCode("RUB");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(finTransactionMapper.selectList(any())).thenReturn(Collections.singletonList(transaction));

        List<OzonFinTransaction> rows = service.listTransactions(
                buildUser(),
                new OzonFinanceTransactionQuery("auth-1", "report-1", "2026-03-01", "2026-03-31")
        );

        assertEquals(1, rows.size());
        assertEquals("txn-1", rows.get(0).getTransactionId());
        assertEquals("sale", rows.get(0).getOperationType());
    }

    @Test
    void getRawContentReturnsSavedReportPayload() {
        OzonAuth auth = buildAuth();
        OzonReportTask task = new OzonReportTask();
        task.setId("task-1");
        task.setAuthId("auth-1");
        task.setShopId("company-1");
        task.setReportId("report-1");

        OzonReportFile reportFile = new OzonReportFile();
        reportFile.setTaskId("task-1");
        reportFile.setAuthId("auth-1");
        reportFile.setRawContent("{\"transactions\":[]}");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(reportTaskMapper.selectById("task-1")).thenReturn(task);
        when(reportFileMapper.selectOne(any())).thenReturn(reportFile);

        String rawContent = service.getRawContent(buildUser(), "auth-1", "task-1");

        assertTrue(rawContent.contains("\"transactions\""));
    }

    private OzonAuth buildAuth() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
        auth.setClientId("client-1");
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
