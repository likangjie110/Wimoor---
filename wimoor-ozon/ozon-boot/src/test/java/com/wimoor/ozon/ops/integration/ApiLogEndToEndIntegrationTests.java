package com.wimoor.ozon.ops.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogQuery;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogRecordCommand;
import com.wimoor.ozon.ops.pojo.entity.OzonApiLog;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozonops.OzonOpsTestApplication;

/**
 * Phase 7 - API 日志端到端集成测试
 *
 * 测试完整的 API 调用 → 日志记录 → 查询流程
 */
@SpringBootTest(classes = OzonOpsTestApplication.class)
@ActiveProfiles("test")
@Transactional
class ApiLogEndToEndIntegrationTests {

    @Autowired
    private IOzonOpsService opsService;

    @Test
    void completeApiCallLoggingFlow() {
        // 1. 模拟 API 调用并记录日志
        OzonApiLogRecordCommand command = new OzonApiLogRecordCommand(
                "auth-integration-1",
                "shop-integration-1",
                "PRODUCT",
                "LIST",
                "/v1/product/list",
                "POST",
                "PRODUCT",
                "product-123",
                "{\"limit\":10}",
                "{\"result\":[{\"id\":\"product-123\"}]}",
                "SUCCESS",
                null,
                150L,
                "integration-tester"
        );

        opsService.recordApiLog(command);

        // 2. 查询日志
        UserInfo user = buildUser();
        OzonApiLogQuery query = new OzonApiLogQuery(
                "auth-integration-1",
                "PRODUCT",
                null,
                null,
                null
        );

        List<OzonApiLog> logs = opsService.listApiLogs(user, query);

        // 3. 验证结果
        assertNotNull(logs);
        assertTrue(logs.size() > 0);

        OzonApiLog log = logs.stream()
                .filter(l -> "LIST".equals(l.getActionName()))
                .findFirst()
                .orElse(null);

        assertNotNull(log);
        assertEquals("PRODUCT", log.getApiGroup());
        assertEquals("LIST", log.getActionName());
        assertEquals("SUCCESS", log.getStatus());
        assertEquals(150L, log.getDurationMs());
    }

    @Test
    void apiLogQueryByStatus() {
        // 1. 记录成功日志
        opsService.recordApiLog(new OzonApiLogRecordCommand(
                "auth-test-1",
                "shop-test-1",
                "STOCK",
                "UPDATE",
                "/v1/stock/update",
                "POST",
                "STOCK",
                "stock-456",
                "{\"quantity\":100}",
                "{\"updated\":true}",
                "SUCCESS",
                null,
                80L,
                "tester"
        ));

        // 2. 记录失败日志
        opsService.recordApiLog(new OzonApiLogRecordCommand(
                "auth-test-1",
                "shop-test-1",
                "STOCK",
                "UPDATE",
                "/v1/stock/update",
                "POST",
                "STOCK",
                "stock-789",
                "{\"quantity\":200}",
                null,
                "FAILED",
                "Stock not found",
                60L,
                "tester"
        ));

        // 3. 查询失败日志
        UserInfo user = buildUser();
        OzonApiLogQuery query = new OzonApiLogQuery(
                "auth-test-1",
                null,
                "FAILED",
                null,
                null
        );

        List<OzonApiLog> failedLogs = opsService.listApiLogs(user, query);

        // 4. 验证只返回失败日志
        assertNotNull(failedLogs);
        assertTrue(failedLogs.size() > 0);
        assertTrue(failedLogs.stream().allMatch(log -> "FAILED".equals(log.getStatus())));
    }

    @Test
    void apiLogQueryByObjectType() {
        // 1. 记录不同对象类型的日志
        opsService.recordApiLog(new OzonApiLogRecordCommand(
                "auth-obj-1",
                "shop-obj-1",
                "PRODUCT",
                "CREATE",
                "/v1/product/create",
                "POST",
                "PRODUCT",
                "product-001",
                "{}",
                "{}",
                "SUCCESS",
                null,
                100L,
                "tester"
        ));

        opsService.recordApiLog(new OzonApiLogRecordCommand(
                "auth-obj-1",
                "shop-obj-1",
                "STOCK",
                "SYNC",
                "/v1/stock/sync",
                "POST",
                "STOCK",
                "stock-001",
                "{}",
                "{}",
                "SUCCESS",
                null,
                120L,
                "tester"
        ));

        // 2. 查询特定对象类型
        UserInfo user = buildUser();
        OzonApiLogQuery query = new OzonApiLogQuery(
                "auth-obj-1",
                null,
                null,
                "PRODUCT",
                null
        );

        List<OzonApiLog> productLogs = opsService.listApiLogs(user, query);

        // 3. 验证只返回 PRODUCT 类型
        assertNotNull(productLogs);
        assertTrue(productLogs.size() > 0);
        assertTrue(productLogs.stream().allMatch(log -> "PRODUCT".equals(log.getObjectType())));
    }

    @Test
    void apiLogQueryByObjectId() {
        // 1. 记录日志
        String targetObjectId = "product-target-123";
        opsService.recordApiLog(new OzonApiLogRecordCommand(
                "auth-id-1",
                "shop-id-1",
                "PRODUCT",
                "GET",
                "/v1/product/info",
                "GET",
                "PRODUCT",
                targetObjectId,
                "{}",
                "{}",
                "SUCCESS",
                null,
                90L,
                "tester"
        ));

        // 2. 查询特定对象ID
        UserInfo user = buildUser();
        OzonApiLogQuery query = new OzonApiLogQuery(
                "auth-id-1",
                null,
                null,
                null,
                targetObjectId
        );

        List<OzonApiLog> logs = opsService.listApiLogs(user, query);

        // 3. 验证返回正确的对象ID
        assertNotNull(logs);
        assertTrue(logs.size() > 0);
        assertTrue(logs.stream().allMatch(log -> targetObjectId.equals(log.getObjectId())));
    }

    @Test
    void apiLogRecordsTimestamps() throws InterruptedException {
        // 1. 记录第一条日志
        opsService.recordApiLog(new OzonApiLogRecordCommand(
                "auth-time-1",
                "shop-time-1",
                "FINANCE",
                "SYNC",
                "/v1/finance/sync",
                "POST",
                "FINANCE",
                "finance-1",
                "{}",
                "{}",
                "SUCCESS",
                null,
                200L,
                "tester"
        ));

        // 等待一秒
        Thread.sleep(1000);

        // 2. 记录第二条日志
        opsService.recordApiLog(new OzonApiLogRecordCommand(
                "auth-time-1",
                "shop-time-1",
                "FINANCE",
                "IMPORT",
                "/v1/finance/import",
                "POST",
                "FINANCE",
                "finance-2",
                "{}",
                "{}",
                "SUCCESS",
                null,
                180L,
                "tester"
        ));

        // 3. 查询日志
        UserInfo user = buildUser();
        OzonApiLogQuery query = new OzonApiLogQuery(
                "auth-time-1",
                "FINANCE",
                null,
                null,
                null
        );

        List<OzonApiLog> logs = opsService.listApiLogs(user, query);

        // 4. 验证时间戳
        assertNotNull(logs);
        assertTrue(logs.size() >= 2);

        // 验证按时间倒序排列
        for (int i = 0; i < logs.size() - 1; i++) {
            assertTrue(logs.get(i).getCreateTime().compareTo(logs.get(i + 1).getCreateTime()) >= 0);
        }
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("integration-tester");
        user.setCompanyid("shop-integration-1");
        return user;
    }
}
