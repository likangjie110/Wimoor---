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
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditQuery;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.pojo.entity.OzonOperationAudit;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozonops.OzonOpsTestApplication;

/**
 * Phase 7 - 操作审计端到端集成测试
 *
 * 测试完整的操作 → 审计记录 → 查询流程
 */
@SpringBootTest(classes = OzonOpsTestApplication.class)
@ActiveProfiles("test")
@Transactional
class AuditLogEndToEndIntegrationTests {

    @Autowired
    private IOzonOpsService opsService;

    @Test
    void completeOperationAuditFlow() {
        // 1. 模拟操作并记录审计
        OzonOperationAuditRecordCommand command = new OzonOperationAuditRecordCommand(
                "auth-audit-1",
                "shop-audit-1",
                "PRODUCT_PUBLISH",
                "PRODUCT",
                "draft-123",
                "Draft-123",
                "{\"draftId\":\"draft-123\",\"name\":\"Test Product\"}",
                "SUCCESS",
                "Product published successfully",
                "audit-tester"
        );

        opsService.recordOperationAudit(command);

        // 2. 查询审计记录
        UserInfo user = buildUser();
        OzonOperationAuditQuery query = new OzonOperationAuditQuery(
                "auth-audit-1",
                "PRODUCT_PUBLISH",
                null,
                null,
                null
        );

        List<OzonOperationAudit> audits = opsService.listOperationAudits(user, query);

        // 3. 验证结果
        assertNotNull(audits);
        assertTrue(audits.size() > 0);

        OzonOperationAudit audit = audits.stream()
                .filter(a -> "PRODUCT_PUBLISH".equals(a.getOperationType()))
                .findFirst()
                .orElse(null);

        assertNotNull(audit);
        assertEquals("PRODUCT_PUBLISH", audit.getOperationType());
        assertEquals("PRODUCT", audit.getObjectType());
        assertEquals("draft-123", audit.getObjectId());
        assertEquals("SUCCESS", audit.getResultStatus());
    }

    @Test
    void auditQueryByOperationType() {
        // 1. 记录不同操作类型的审计
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                "auth-type-1",
                "shop-type-1",
                "STOCK_UPDATE",
                "STOCK",
                "stock-001",
                "Stock-001",
                "{}",
                "SUCCESS",
                "Stock updated",
                "tester"
        ));

        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                "auth-type-1",
                "shop-type-1",
                "PRICE_IMPORT",
                "PRICE",
                "price-001",
                "Price-001",
                "{}",
                "SUCCESS",
                "Price imported",
                "tester"
        ));

        // 2. 查询特定操作类型
        UserInfo user = buildUser();
        OzonOperationAuditQuery query = new OzonOperationAuditQuery(
                "auth-type-1",
                "STOCK_UPDATE",
                null,
                null,
                null
        );

        List<OzonOperationAudit> audits = opsService.listOperationAudits(user, query);

        // 3. 验证只返回 STOCK_UPDATE
        assertNotNull(audits);
        assertTrue(audits.size() > 0);
        assertTrue(audits.stream().allMatch(a -> "STOCK_UPDATE".equals(a.getOperationType())));
    }

    @Test
    void auditQueryByResultStatus() {
        // 1. 记录成功和失败的审计
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                "auth-status-1",
                "shop-status-1",
                "POSTING_SHIP",
                "POSTING",
                "posting-001",
                "Posting-001",
                "{}",
                "SUCCESS",
                "Shipped successfully",
                "tester"
        ));

        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                "auth-status-1",
                "shop-status-1",
                "POSTING_SHIP",
                "POSTING",
                "posting-002",
                "Posting-002",
                "{}",
                "FAILED",
                "Invalid tracking number",
                "tester"
        ));

        // 2. 查询失败的审计
        UserInfo user = buildUser();
        OzonOperationAuditQuery query = new OzonOperationAuditQuery(
                "auth-status-1",
                null,
                "FAILED",
                null,
                null
        );

        List<OzonOperationAudit> failedAudits = opsService.listOperationAudits(user, query);

        // 3. 验证只返回失败记录
        assertNotNull(failedAudits);
        assertTrue(failedAudits.size() > 0);
        assertTrue(failedAudits.stream().allMatch(a -> "FAILED".equals(a.getResultStatus())));
    }

    @Test
    void auditQueryByObjectType() {
        // 1. 记录不同对象类型的审计
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                "auth-obj-audit-1",
                "shop-obj-audit-1",
                "CREATE",
                "PRODUCT",
                "product-obj-1",
                "Product-1",
                "{}",
                "SUCCESS",
                "Created",
                "tester"
        ));

        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                "auth-obj-audit-1",
                "shop-obj-audit-1",
                "UPDATE",
                "STOCK",
                "stock-obj-1",
                "Stock-1",
                "{}",
                "SUCCESS",
                "Updated",
                "tester"
        ));

        // 2. 查询特定对象类型
        UserInfo user = buildUser();
        OzonOperationAuditQuery query = new OzonOperationAuditQuery(
                "auth-obj-audit-1",
                null,
                null,
                "PRODUCT",
                null
        );

        List<OzonOperationAudit> productAudits = opsService.listOperationAudits(user, query);

        // 3. 验证只返回 PRODUCT 类型
        assertNotNull(productAudits);
        assertTrue(productAudits.size() > 0);
        assertTrue(productAudits.stream().allMatch(a -> "PRODUCT".equals(a.getObjectType())));
    }

    @Test
    void auditQueryByObjectId() {
        // 1. 记录审计
        String targetObjectId = "draft-target-456";
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                "auth-id-audit-1",
                "shop-id-audit-1",
                "PRODUCT_PUBLISH",
                "PRODUCT",
                targetObjectId,
                "Draft-Target",
                "{}",
                "SUCCESS",
                "Published",
                "tester"
        ));

        // 2. 查询特定对象ID
        UserInfo user = buildUser();
        OzonOperationAuditQuery query = new OzonOperationAuditQuery(
                "auth-id-audit-1",
                null,
                null,
                null,
                targetObjectId
        );

        List<OzonOperationAudit> audits = opsService.listOperationAudits(user, query);

        // 3. 验证返回正确的对象ID
        assertNotNull(audits);
        assertTrue(audits.size() > 0);
        assertTrue(audits.stream().allMatch(a -> targetObjectId.equals(a.getObjectId())));
    }

    @Test
    void auditRecordsOperator() {
        // 1. 记录审计
        String operator = "specific-operator";
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                "auth-operator-1",
                "shop-operator-1",
                "FINANCE_SYNC",
                "FINANCE",
                "finance-001",
                "Finance-001",
                "{}",
                "SUCCESS",
                "Synced",
                operator
        ));

        // 2. 查询审计记录
        UserInfo user = buildUser();
        OzonOperationAuditQuery query = new OzonOperationAuditQuery(
                "auth-operator-1",
                "FINANCE_SYNC",
                null,
                null,
                null
        );

        List<OzonOperationAudit> audits = opsService.listOperationAudits(user, query);

        // 3. 验证操作人记录
        assertNotNull(audits);
        assertTrue(audits.size() > 0);
        OzonOperationAudit audit = audits.get(0);
        assertEquals(operator, audit.getOperator());
    }

    @Test
    void auditRecordsTimestamps() throws InterruptedException {
        // 1. 记录第一条审计
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                "auth-time-audit-1",
                "shop-time-audit-1",
                "CHAT_SEND",
                "CHAT",
                "chat-001",
                "Chat-001",
                "{}",
                "SUCCESS",
                "Sent",
                "tester"
        ));

        // 等待一秒
        Thread.sleep(1000);

        // 2. 记录第二条审计
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                "auth-time-audit-1",
                "shop-time-audit-1",
                "CHAT_REPLY",
                "CHAT",
                "chat-002",
                "Chat-002",
                "{}",
                "SUCCESS",
                "Replied",
                "tester"
        ));

        // 3. 查询审计记录
        UserInfo user = buildUser();
        OzonOperationAuditQuery query = new OzonOperationAuditQuery(
                "auth-time-audit-1",
                null,
                null,
                "CHAT",
                null
        );

        List<OzonOperationAudit> audits = opsService.listOperationAudits(user, query);

        // 4. 验证时间戳
        assertNotNull(audits);
        assertTrue(audits.size() >= 2);

        // 验证按时间倒序排列
        for (int i = 0; i < audits.size() - 1; i++) {
            assertTrue(audits.get(i).getCreateTime().compareTo(audits.get(i + 1).getCreateTime()) >= 0);
        }
    }

    @Test
    void auditRecordsObjectCode() {
        // 1. 记录审计（带对象代码）
        String objectCode = "DRAFT-CODE-789";
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                "auth-code-1",
                "shop-code-1",
                "PRODUCT_DELETE",
                "PRODUCT",
                "product-789",
                objectCode,
                "{}",
                "SUCCESS",
                "Deleted",
                "tester"
        ));

        // 2. 查询审计记录
        UserInfo user = buildUser();
        OzonOperationAuditQuery query = new OzonOperationAuditQuery(
                "auth-code-1",
                "PRODUCT_DELETE",
                null,
                null,
                null
        );

        List<OzonOperationAudit> audits = opsService.listOperationAudits(user, query);

        // 3. 验证对象代码
        assertNotNull(audits);
        assertTrue(audits.size() > 0);
        OzonOperationAudit audit = audits.get(0);
        assertEquals(objectCode, audit.getObjectCode());
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("audit-tester");
        user.setCompanyid("shop-audit-1");
        return user;
    }
}
