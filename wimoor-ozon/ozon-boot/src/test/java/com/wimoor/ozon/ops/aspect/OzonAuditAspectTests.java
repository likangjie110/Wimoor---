package com.wimoor.ozon.ops.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimoor.ozon.ops.annotation.OzonAudit;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozon.product.pojo.dto.OzonProductPublishCommand;

/**
 * OzonAuditAspect 测试
 *
 * 测试操作审计拦截器的核心功能：
 * - 注解识别
 * - 审计记录（操作类型/对象类型/操作人/参数）
 * - 异步记录
 * - 异常容错
 * - 敏感信息脱敏
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OzonAuditAspectTests {

    @Mock
    private IOzonOpsService opsService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private OzonAudit annotation;

    @Captor
    private ArgumentCaptor<OzonOperationAuditRecordCommand> commandCaptor;

    private OzonAuditAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new OzonAuditAspect(opsService, objectMapper);
    }

    @Test
    void annotationRecognized_AuditsSuccessfully() throws Throwable {
        // Arrange
        when(annotation.operationType()).thenReturn("PRODUCT_PUBLISH");
        when(annotation.objectType()).thenReturn("PRODUCT");
        when(annotation.logRequest()).thenReturn(true);
        when(annotation.async()).thenReturn(false);

        OzonProductPublishCommand command = new OzonProductPublishCommand();
        command.setDraftId("draft-1");

        when(joinPoint.getArgs()).thenReturn(new Object[]{command});
        when(joinPoint.proceed()).thenReturn("Published successfully");
        when(objectMapper.writeValueAsString(command)).thenReturn("{\"draftId\":\"draft-1\"}");

        doNothing().when(opsService).recordOperationAudit(any());

        // Act
        Object result = aspect.around(joinPoint, annotation);

        // Assert
        assertEquals("Published successfully", result);
        verify(opsService).recordOperationAudit(commandCaptor.capture());

        OzonOperationAuditRecordCommand auditCommand = commandCaptor.getValue();
        assertEquals("PRODUCT_PUBLISH", auditCommand.getOperationType());
        assertEquals("PRODUCT", auditCommand.getObjectType());
        assertEquals("SUCCESS", auditCommand.getResultStatus());
        assertEquals("Operation completed successfully", auditCommand.getResultMessage());
    }

    @Test
    void operationTypeRecorded_FromAnnotation() throws Throwable {
        // Arrange
        when(annotation.operationType()).thenReturn("STOCK_UPDATE");
        when(annotation.objectType()).thenReturn("STOCK");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.async()).thenReturn(false);

        when(joinPoint.getArgs()).thenReturn(new Object[]{"stock-1", 100});
        when(joinPoint.proceed()).thenReturn("Stock updated");

        doNothing().when(opsService).recordOperationAudit(any());

        // Act
        aspect.around(joinPoint, annotation);

        // Assert
        verify(opsService).recordOperationAudit(commandCaptor.capture());
        OzonOperationAuditRecordCommand command = commandCaptor.getValue();
        assertEquals("STOCK_UPDATE", command.getOperationType());
    }

    @Test
    void objectTypeRecorded_FromAnnotation() throws Throwable {
        // Arrange
        when(annotation.operationType()).thenReturn("PRICE_IMPORT");
        when(annotation.objectType()).thenReturn("PRICE");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.async()).thenReturn(false);

        when(joinPoint.getArgs()).thenReturn(new Object[]{"auth-1"});
        when(joinPoint.proceed()).thenReturn("Price imported");

        doNothing().when(opsService).recordOperationAudit(any());

        // Act
        aspect.around(joinPoint, annotation);

        // Assert
        verify(opsService).recordOperationAudit(commandCaptor.capture());
        OzonOperationAuditRecordCommand command = commandCaptor.getValue();
        assertEquals("PRICE", command.getObjectType());
    }

    @Test
    void operationParamsRecorded_WhenLogRequestEnabled() throws Throwable {
        // Arrange
        when(annotation.operationType()).thenReturn("POSTING_SHIP");
        when(annotation.objectType()).thenReturn("POSTING");
        when(annotation.logRequest()).thenReturn(true);
        when(annotation.async()).thenReturn(false);

        Object requestObj = new Object() {
            public String postingId = "posting-1";
            public String trackingNumber = "TRACK123";
        };

        when(joinPoint.getArgs()).thenReturn(new Object[]{requestObj});
        when(joinPoint.proceed()).thenReturn("Shipped");
        when(objectMapper.writeValueAsString(requestObj))
                .thenReturn("{\"postingId\":\"posting-1\",\"trackingNumber\":\"TRACK123\"}");

        doNothing().when(opsService).recordOperationAudit(any());

        // Act
        aspect.around(joinPoint, annotation);

        // Assert
        verify(opsService).recordOperationAudit(commandCaptor.capture());
        OzonOperationAuditRecordCommand command = commandCaptor.getValue();
        assertNotNull(command.getRequestPayloadJson());
        assertTrue(command.getRequestPayloadJson().contains("posting-1"));
    }

    @Test
    void successStatusRecorded_WhenOperationSucceeds() throws Throwable {
        // Arrange
        when(annotation.operationType()).thenReturn("FINANCE_SYNC");
        when(annotation.objectType()).thenReturn("FINANCE");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.async()).thenReturn(false);

        when(joinPoint.getArgs()).thenReturn(new Object[]{"auth-1"});
        when(joinPoint.proceed()).thenReturn("Sync completed");

        doNothing().when(opsService).recordOperationAudit(any());

        // Act
        aspect.around(joinPoint, annotation);

        // Assert
        verify(opsService).recordOperationAudit(commandCaptor.capture());
        OzonOperationAuditRecordCommand command = commandCaptor.getValue();
        assertEquals("SUCCESS", command.getResultStatus());
        assertEquals("Operation completed successfully", command.getResultMessage());
    }

    @Test
    void failedStatusRecorded_WhenOperationFails() throws Throwable {
        // Arrange
        when(annotation.operationType()).thenReturn("CHAT_SEND");
        when(annotation.objectType()).thenReturn("CHAT");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.async()).thenReturn(false);

        RuntimeException error = new RuntimeException("Chat service unavailable");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"chat-1", "message"});
        when(joinPoint.proceed()).thenThrow(error);

        doNothing().when(opsService).recordOperationAudit(any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> aspect.around(joinPoint, annotation));

        verify(opsService).recordOperationAudit(commandCaptor.capture());
        OzonOperationAuditRecordCommand command = commandCaptor.getValue();
        assertEquals("FAILED", command.getResultStatus());
        assertEquals("Chat service unavailable", command.getResultMessage());
    }

    @Test
    void asyncRecording_DoesNotBlockOperation() throws Throwable {
        // Arrange
        when(annotation.operationType()).thenReturn("ADS_CREATE");
        when(annotation.objectType()).thenReturn("ADS");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.async()).thenReturn(true); // 异步

        when(joinPoint.getArgs()).thenReturn(new Object[]{"campaign-1"});
        when(joinPoint.proceed()).thenReturn("Campaign created");

        // Act
        Object result = aspect.around(joinPoint, annotation);

        // Assert
        assertEquals("Campaign created", result);
        // 异步调用无法在单元测试中直接验证，需要集成测试
    }

    @Test
    void exceptionInAuditing_DoesNotFailOperation() throws Throwable {
        // Arrange
        when(annotation.operationType()).thenReturn("WAREHOUSE_SYNC");
        when(annotation.objectType()).thenReturn("WAREHOUSE");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.async()).thenReturn(false);

        when(joinPoint.getArgs()).thenReturn(new Object[]{"auth-1"});
        when(joinPoint.proceed()).thenReturn("Warehouse synced");

        // 模拟审计记录失败
        doThrow(new RuntimeException("Database down")).when(opsService).recordOperationAudit(any());

        // Act - 主流程不应被阻塞
        Object result = aspect.around(joinPoint, annotation);

        // Assert
        assertEquals("Warehouse synced", result);
    }

    @Test
    void payloadTruncated_WhenTooLong() throws Throwable {
        // Arrange
        when(annotation.operationType()).thenReturn("PRODUCT_BULK_CREATE");
        when(annotation.objectType()).thenReturn("PRODUCT");
        when(annotation.logRequest()).thenReturn(true);
        when(annotation.async()).thenReturn(false);

        Object largeRequest = new Object() {
            public String data = buildRepeatedChar('x', 5000);
        };

        when(joinPoint.getArgs()).thenReturn(new Object[]{largeRequest});
        when(joinPoint.proceed()).thenReturn("Bulk created");
        when(objectMapper.writeValueAsString(largeRequest))
                .thenReturn("{\"data\":\"" + buildRepeatedChar('x', 5000) + "\"}");

        doNothing().when(opsService).recordOperationAudit(any());

        // Act
        aspect.around(joinPoint, annotation);

        // Assert
        verify(opsService).recordOperationAudit(commandCaptor.capture());
        OzonOperationAuditRecordCommand command = commandCaptor.getValue();
        assertNotNull(command.getRequestPayloadJson());
        assertTrue(command.getRequestPayloadJson().length() <= 4100); // 4000 + "...(truncated)"
    }

    @Test
    void errorMessageTruncated_WhenTooLong() throws Throwable {
        // Arrange
        when(annotation.operationType()).thenReturn("SHIPMENT_CREATE");
        when(annotation.objectType()).thenReturn("SHIPMENT");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.async()).thenReturn(false);

        String longErrorMessage = "Error details: " + buildRepeatedChar('x', 600);
        RuntimeException error = new RuntimeException(longErrorMessage);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"shipment-1"});
        when(joinPoint.proceed()).thenThrow(error);

        doNothing().when(opsService).recordOperationAudit(any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> aspect.around(joinPoint, annotation));

        verify(opsService).recordOperationAudit(commandCaptor.capture());
        OzonOperationAuditRecordCommand command = commandCaptor.getValue();
        assertNotNull(command.getResultMessage());
        assertTrue(command.getResultMessage().length() <= 500);
    }

    @Test
    void jsonSerializationFallback_UsesToString() throws Throwable {
        // Arrange
        when(annotation.operationType()).thenReturn("ORDER_PROCESS");
        when(annotation.objectType()).thenReturn("ORDER");
        when(annotation.logRequest()).thenReturn(true);
        when(annotation.async()).thenReturn(false);

        Object nonSerializableObj = new Object() {
            @Override
            public String toString() {
                return "NonSerializableObject";
            }
        };

        when(joinPoint.getArgs()).thenReturn(new Object[]{nonSerializableObj});
        when(joinPoint.proceed()).thenReturn("Order processed");
        when(objectMapper.writeValueAsString(nonSerializableObj))
                .thenThrow(new RuntimeException("Cannot serialize"));

        doNothing().when(opsService).recordOperationAudit(any());

        // Act
        aspect.around(joinPoint, annotation);

        // Assert
        verify(opsService).recordOperationAudit(commandCaptor.capture());
        OzonOperationAuditRecordCommand command = commandCaptor.getValue();
        assertEquals("NonSerializableObject", command.getRequestPayloadJson());
    }

    @Test
    void nullResult_HandledGracefully() throws Throwable {
        // Arrange
        when(annotation.operationType()).thenReturn("DELETE_PRODUCT");
        when(annotation.objectType()).thenReturn("PRODUCT");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.async()).thenReturn(false);

        when(joinPoint.getArgs()).thenReturn(new Object[]{"product-1"});
        when(joinPoint.proceed()).thenReturn(null);

        doNothing().when(opsService).recordOperationAudit(any());

        // Act
        Object result = aspect.around(joinPoint, annotation);

        // Assert
        assertEquals(null, result);
        verify(opsService).recordOperationAudit(commandCaptor.capture());
        OzonOperationAuditRecordCommand command = commandCaptor.getValue();
        assertEquals("SUCCESS", command.getResultStatus());
    }

    @Test
    void emptyArgs_HandledGracefully() throws Throwable {
        // Arrange
        when(annotation.operationType()).thenReturn("SYSTEM_HEALTH_CHECK");
        when(annotation.objectType()).thenReturn("SYSTEM");
        when(annotation.logRequest()).thenReturn(true);
        when(annotation.async()).thenReturn(false);

        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("Healthy");

        doNothing().when(opsService).recordOperationAudit(any());

        // Act
        Object result = aspect.around(joinPoint, annotation);

        // Assert
        assertEquals("Healthy", result);
        verify(opsService).recordOperationAudit(commandCaptor.capture());
        OzonOperationAuditRecordCommand command = commandCaptor.getValue();
        assertEquals("SUCCESS", command.getResultStatus());
    }

    @Test
    void nullArgs_HandledGracefully() throws Throwable {
        // Arrange
        when(annotation.operationType()).thenReturn("CACHE_CLEAR");
        when(annotation.objectType()).thenReturn("SYSTEM");
        when(annotation.logRequest()).thenReturn(true);
        when(annotation.async()).thenReturn(false);

        when(joinPoint.getArgs()).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("Cache cleared");

        doNothing().when(opsService).recordOperationAudit(any());

        // Act
        Object result = aspect.around(joinPoint, annotation);

        // Assert
        assertEquals("Cache cleared", result);
        verify(opsService).recordOperationAudit(commandCaptor.capture());
    }

    private String buildRepeatedChar(char ch, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(ch);
        }
        return builder.toString();
    }
}
