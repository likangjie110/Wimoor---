package com.wimoor.ozon.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.ops.mapper.OzonApiLogMapper;
import com.wimoor.ozon.ops.mapper.OzonOperationAuditMapper;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogQuery;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogRecordCommand;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditQuery;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.pojo.entity.OzonApiLog;
import com.wimoor.ozon.ops.pojo.entity.OzonOperationAudit;
import com.wimoor.ozon.ops.pojo.vo.OzonOpsSummaryView;
import com.wimoor.ozon.ops.service.impl.OzonOpsServiceImpl;

@ExtendWith(MockitoExtension.class)
class OzonOpsServiceTests {

    @Mock
    private OzonAuthAccessService authAccessService;

    @Mock
    private OzonApiLogMapper apiLogMapper;

    @Mock
    private OzonOperationAuditMapper operationAuditMapper;

    @Captor
    private ArgumentCaptor<OzonApiLog> apiLogCaptor;

    @Captor
    private ArgumentCaptor<OzonOperationAudit> operationAuditCaptor;

    @Captor
    private ArgumentCaptor<QueryWrapper<OzonApiLog>> apiLogQueryCaptor;

    @Captor
    private ArgumentCaptor<QueryWrapper<OzonOperationAudit>> operationAuditQueryCaptor;

    private OzonOpsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonOpsServiceImpl(authAccessService, apiLogMapper, operationAuditMapper);
    }

    @Test
    void recordApiLogPersistsNormalizedRow() {
        service.recordApiLog(new OzonApiLogRecordCommand(
                "auth-1",
                "shop-1",
                "PRODUCT",
                "LIST_ATTRIBUTES",
                "/v1/description-category/attribute",
                "POST",
                "PRODUCT_META",
                "1001:2002",
                "{\"k\":1}",
                "{\"result\":[]}",
                "SUCCESS",
                null,
                120L,
                "tester"
        ));

        verify(apiLogMapper).insert(apiLogCaptor.capture());
        assertEquals("auth-1", apiLogCaptor.getValue().getAuthId());
        assertEquals("PRODUCT", apiLogCaptor.getValue().getApiGroup());
        assertEquals("LIST_ATTRIBUTES", apiLogCaptor.getValue().getActionName());
        assertEquals(120L, apiLogCaptor.getValue().getDurationMs());
    }

    @Test
    void recordOperationAuditPersistsNormalizedRow() {
        service.recordOperationAudit(new OzonOperationAuditRecordCommand(
                "auth-1",
                "shop-1",
                "PRODUCT_PUBLISH",
                "PRODUCT",
                "draft-1",
                "Draft-1",
                "{\"draftId\":\"draft-1\"}",
                "FAILED",
                "remote failed",
                "tester"
        ));

        verify(operationAuditMapper).insert(operationAuditCaptor.capture());
        assertEquals("auth-1", operationAuditCaptor.getValue().getAuthId());
        assertEquals("PRODUCT_PUBLISH", operationAuditCaptor.getValue().getOperationType());
        assertEquals("FAILED", operationAuditCaptor.getValue().getResultStatus());
        assertEquals("Draft-1", operationAuditCaptor.getValue().getObjectCode());
    }

    @Test
    void summaryAggregatesApiAndAuditCounters() {
        OzonAuth auth = buildAuth();
        when(authAccessService.requireOwnedAuth(buildUser(), "auth-1")).thenReturn(auth);
        when(apiLogMapper.selectCount(any())).thenReturn(8, 3);
        when(operationAuditMapper.selectCount(any())).thenReturn(5, 1);

        OzonOpsSummaryView result = service.summary(buildUser(), "auth-1");

        assertEquals(8L, result.getApiLogTotal());
        assertEquals(3L, result.getApiLogFailed());
        assertEquals(5L, result.getOperationAuditTotal());
        assertEquals(1L, result.getOperationAuditFailed());
    }

    @Test
    void listApiLogsBuildsScopedFilters() {
        OzonAuth auth = buildAuth();
        OzonApiLog row = new OzonApiLog();
        row.setId("log-1");
        when(authAccessService.requireOwnedAuth(buildUser(), "auth-1")).thenReturn(auth);
        when(apiLogMapper.selectList(any())).thenReturn(Collections.singletonList(row));

        List<OzonApiLog> result = service.listApiLogs(
                buildUser(),
                new OzonApiLogQuery("auth-1", "PRODUCT", "FAILED", "PRODUCT", "draft-1")
        );

        verify(apiLogMapper).selectList(apiLogQueryCaptor.capture());
        String sql = apiLogQueryCaptor.getValue().getSqlSegment();
        assertEquals(1, result.size());
        assertTrue(sql.contains("api_group"));
        assertTrue(sql.contains("status"));
        assertTrue(sql.contains("object_type"));
        assertTrue(sql.contains("object_id"));
        assertTrue(apiLogQueryCaptor.getValue().getParamNameValuePairs().size() >= 5);
    }

    @Test
    void listOperationAuditsBuildsScopedFilters() {
        OzonAuth auth = buildAuth();
        OzonOperationAudit row = new OzonOperationAudit();
        row.setId("audit-1");
        when(authAccessService.requireOwnedAuth(buildUser(), "auth-1")).thenReturn(auth);
        when(operationAuditMapper.selectList(any())).thenReturn(Collections.singletonList(row));

        List<OzonOperationAudit> result = service.listOperationAudits(
                buildUser(),
                new OzonOperationAuditQuery("auth-1", "PRODUCT_PUBLISH", "FAILED", "PRODUCT", "draft-1")
        );

        verify(operationAuditMapper).selectList(operationAuditQueryCaptor.capture());
        String sql = operationAuditQueryCaptor.getValue().getSqlSegment();
        assertEquals(1, result.size());
        assertTrue(sql.contains("operation_type"));
        assertTrue(sql.contains("result_status"));
        assertTrue(sql.contains("object_type"));
        assertTrue(sql.contains("object_id"));
        assertTrue(operationAuditQueryCaptor.getValue().getParamNameValuePairs().size() >= 5);
    }

    private OzonAuth buildAuth() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("shop-1");
        return auth;
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("shop-1");
        return user;
    }
}
