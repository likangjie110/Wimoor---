package com.wimoor.ozon.ops.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimoor.ozon.ops.annotation.OzonApiLog;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;

/**
 * OzonApiLogAspect 测试
 *
 * 测试 API 日志拦截器的核心功能：
 * - 注解识别
 * - 日志记录（请求/响应/耗时/状态）
 * - 异步记录
 * - 异常容错
 * - Payload 截断
 * - 不阻塞主流程
 */
@ExtendWith(MockitoExtension.class)
class OzonApiLogAspectTests {

    @Mock
    private IOzonOpsService opsService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private OzonApiLog annotation;

    @Captor
    private ArgumentCaptor<OzonApiLogRecordCommand> commandCaptor;

    private OzonApiLogAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new OzonApiLogAspect(opsService, objectMapper);
    }

    @Test
    void annotationRecognized_LogsSuccessfully() throws Throwable {
        // Arrange
        when(annotation.apiGroup()).thenReturn("PRODUCT");
        when(annotation.actionName()).thenReturn("LIST");
        when(annotation.httpMethod()).thenReturn("POST");
        when(annotation.objectType()).thenReturn("PRODUCT");
        when(annotation.logRequest()).thenReturn(true);
        when(annotation.logResponse()).thenReturn(true);
        when(annotation.async()).thenReturn(false);

        when(joinPoint.getArgs()).thenReturn(new Object[]{"auth-1", "shop-1", "{\"limit\":10}"});
        when(joinPoint.proceed()).thenReturn("{\"result\":[]}");

        doNothing().when(opsService).recordApiLog(any());

        // Act
        Object result = aspect.around(joinPoint, annotation);

        // Assert
        assertEquals("{\"result\":[]}", result);
        verify(opsService).recordApiLog(commandCaptor.capture());

        OzonApiLogRecordCommand command = commandCaptor.getValue();
        assertEquals("PRODUCT", command.getApiGroup());
        assertEquals("LIST", command.getActionName());
        assertEquals("POST", command.getHttpMethod());
        assertEquals("SUCCESS", command.getStatus());
        assertNotNull(command.getDurationMs());
        assertTrue(command.getDurationMs() >= 0);
    }

    @Test
    void requestParametersRecorded_WhenLogRequestEnabled() throws Throwable {
        // Arrange
        when(annotation.apiGroup()).thenReturn("STOCK");
        when(annotation.actionName()).thenReturn("UPDATE");
        when(annotation.httpMethod()).thenReturn("POST");
        when(annotation.logRequest()).thenReturn(true);
        when(annotation.logResponse()).thenReturn(false);
        when(annotation.async()).thenReturn(false);

        String requestPayload = "{\"stockId\":\"stock-1\",\"quantity\":100}";
        when(joinPoint.getArgs()).thenReturn(new Object[]{"auth-1", "shop-1", requestPayload});
        when(joinPoint.proceed()).thenReturn("OK");

        doNothing().when(opsService).recordApiLog(any());

        // Act
        aspect.around(joinPoint, annotation);

        // Assert
        verify(opsService).recordApiLog(commandCaptor.capture());
        OzonApiLogRecordCommand command = commandCaptor.getValue();
        assertEquals(requestPayload, command.getRequestPayloadJson());
    }

    @Test
    void responseResultRecorded_WhenLogResponseEnabled() throws Throwable {
        // Arrange
        when(annotation.apiGroup()).thenReturn("PRICE");
        when(annotation.actionName()).thenReturn("GET");
        when(annotation.httpMethod()).thenReturn("GET");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.logResponse()).thenReturn(true);
        when(annotation.async()).thenReturn(false);

        String responsePayload = "{\"price\":99.99,\"currency\":\"RUB\"}";
        when(joinPoint.getArgs()).thenReturn(new Object[]{"auth-1", "price-1"});
        when(joinPoint.proceed()).thenReturn(responsePayload);

        doNothing().when(opsService).recordApiLog(any());

        // Act
        aspect.around(joinPoint, annotation);

        // Assert
        verify(opsService).recordApiLog(commandCaptor.capture());
        OzonApiLogRecordCommand command = commandCaptor.getValue();
        assertEquals(responsePayload, command.getResponsePayloadJson());
        assertEquals("SUCCESS", command.getStatus());
    }

    @Test
    void durationRecorded_ForAllCalls() throws Throwable {
        // Arrange
        when(annotation.apiGroup()).thenReturn("POSTING");
        when(annotation.actionName()).thenReturn("LIST");
        when(annotation.httpMethod()).thenReturn("GET");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.logResponse()).thenReturn(false);
        when(annotation.async()).thenReturn(false);

        when(joinPoint.getArgs()).thenReturn(new Object[]{"auth-1"});
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(50); // 模拟耗时操作
            return "[]";
        });

        doNothing().when(opsService).recordApiLog(any());

        // Act
        aspect.around(joinPoint, annotation);

        // Assert
        verify(opsService).recordApiLog(commandCaptor.capture());
        OzonApiLogRecordCommand command = commandCaptor.getValue();
        assertNotNull(command.getDurationMs());
        assertTrue(command.getDurationMs() >= 50);
    }

    @Test
    void statusRecorded_WhenCallFails() throws Throwable {
        // Arrange
        when(annotation.apiGroup()).thenReturn("FINANCE");
        when(annotation.actionName()).thenReturn("IMPORT");
        when(annotation.httpMethod()).thenReturn("POST");
        when(annotation.logRequest()).thenReturn(true);
        when(annotation.logResponse()).thenReturn(false);
        when(annotation.async()).thenReturn(false);

        RuntimeException apiError = new RuntimeException("API rate limit exceeded");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"auth-1"});
        when(joinPoint.proceed()).thenThrow(apiError);

        doNothing().when(opsService).recordApiLog(any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> aspect.around(joinPoint, annotation));

        verify(opsService).recordApiLog(commandCaptor.capture());
        OzonApiLogRecordCommand command = commandCaptor.getValue();
        assertEquals("FAILED", command.getStatus());
        assertEquals("API rate limit exceeded", command.getErrorMessage());
    }

    @Test
    void asyncRecording_DoesNotBlockMainFlow() throws Throwable {
        // Arrange
        when(annotation.apiGroup()).thenReturn("CHAT");
        when(annotation.actionName()).thenReturn("SEND");
        when(annotation.httpMethod()).thenReturn("POST");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.logResponse()).thenReturn(false);
        when(annotation.async()).thenReturn(true); // 异步记录

        when(joinPoint.getArgs()).thenReturn(new Object[]{"auth-1"});
        when(joinPoint.proceed()).thenReturn("Message sent");

        // Act
        Object result = aspect.around(joinPoint, annotation);

        // Assert
        assertEquals("Message sent", result);
        // 注意：异步调用难以在单元测试中验证，需要集成测试
    }

    @Test
    void exceptionInLogging_DoesNotFailMainFlow() throws Throwable {
        // Arrange
        when(annotation.apiGroup()).thenReturn("ADS");
        when(annotation.actionName()).thenReturn("CREATE_CAMPAIGN");
        when(annotation.httpMethod()).thenReturn("POST");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.logResponse()).thenReturn(false);
        when(annotation.async()).thenReturn(false);

        when(joinPoint.getArgs()).thenReturn(new Object[]{"auth-1"});
        when(joinPoint.proceed()).thenReturn("Campaign created");

        // 模拟日志记录失败
        doThrow(new RuntimeException("Database down")).when(opsService).recordApiLog(any());

        // Act - 主流程不应被阻塞
        Object result = aspect.around(joinPoint, annotation);

        // Assert
        assertEquals("Campaign created", result);
        // 日志记录失败不应影响业务逻辑
    }

    @Test
    void payloadTruncated_WhenTooLong() throws Throwable {
        // Arrange
        when(annotation.apiGroup()).thenReturn("PRODUCT");
        when(annotation.actionName()).thenReturn("BULK_CREATE");
        when(annotation.httpMethod()).thenReturn("POST");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.logResponse()).thenReturn(true);
        when(annotation.async()).thenReturn(false);

        // 创建超长响应（5000字符）
        String longResponse = buildRepeatedChar('x', 5000);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"auth-1"});
        when(joinPoint.proceed()).thenReturn(longResponse);

        doNothing().when(opsService).recordApiLog(any());

        // Act
        aspect.around(joinPoint, annotation);

        // Assert
        verify(opsService).recordApiLog(commandCaptor.capture());
        OzonApiLogRecordCommand command = commandCaptor.getValue();
        assertNotNull(command.getResponsePayloadJson());
        assertTrue(command.getResponsePayloadJson().length() <= 4100); // 4000 + "...(truncated)"
    }

    @Test
    void errorMessageTruncated_WhenTooLong() throws Throwable {
        // Arrange
        when(annotation.apiGroup()).thenReturn("SHIPMENT");
        when(annotation.actionName()).thenReturn("CREATE");
        when(annotation.httpMethod()).thenReturn("POST");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.logResponse()).thenReturn(false);
        when(annotation.async()).thenReturn(false);

        String longErrorMessage = "Error: " + buildRepeatedChar('x', 600);
        RuntimeException error = new RuntimeException(longErrorMessage);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"auth-1"});
        when(joinPoint.proceed()).thenThrow(error);

        doNothing().when(opsService).recordApiLog(any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> aspect.around(joinPoint, annotation));

        verify(opsService).recordApiLog(commandCaptor.capture());
        OzonApiLogRecordCommand command = commandCaptor.getValue();
        assertNotNull(command.getErrorMessage());
        assertTrue(command.getErrorMessage().length() <= 500);
    }

    @Test
    void objectTypeRecorded_WhenProvided() throws Throwable {
        // Arrange
        when(annotation.apiGroup()).thenReturn("PRODUCT");
        when(annotation.actionName()).thenReturn("GET_INFO");
        when(annotation.httpMethod()).thenReturn("GET");
        when(annotation.objectType()).thenReturn("PRODUCT");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.logResponse()).thenReturn(false);
        when(annotation.async()).thenReturn(false);

        when(joinPoint.getArgs()).thenReturn(new Object[]{"auth-1", "product-1"});
        when(joinPoint.proceed()).thenReturn("{\"name\":\"Product A\"}");

        doNothing().when(opsService).recordApiLog(any());

        // Act
        aspect.around(joinPoint, annotation);

        // Assert
        verify(opsService).recordApiLog(commandCaptor.capture());
        OzonApiLogRecordCommand command = commandCaptor.getValue();
        assertEquals("PRODUCT", command.getObjectType());
    }

    @Test
    void endpointGenerated_FromApiGroupAndAction() throws Throwable {
        // Arrange
        when(annotation.apiGroup()).thenReturn("WAREHOUSE");
        when(annotation.actionName()).thenReturn("SYNC");
        when(annotation.httpMethod()).thenReturn("POST");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.logResponse()).thenReturn(false);
        when(annotation.async()).thenReturn(false);

        when(joinPoint.getArgs()).thenReturn(new Object[]{"auth-1"});
        when(joinPoint.proceed()).thenReturn("Sync completed");

        doNothing().when(opsService).recordApiLog(any());

        // Act
        aspect.around(joinPoint, annotation);

        // Assert
        verify(opsService).recordApiLog(commandCaptor.capture());
        OzonApiLogRecordCommand command = commandCaptor.getValue();
        assertEquals("WAREHOUSE/SYNC", command.getEndpoint());
    }

    @Test
    void logNotRecorded_WhenOpsServiceThrowsException() throws Throwable {
        // Arrange
        when(annotation.apiGroup()).thenReturn("SELLER");
        when(annotation.actionName()).thenReturn("GET_INFO");
        when(annotation.httpMethod()).thenReturn("GET");
        when(annotation.logRequest()).thenReturn(false);
        when(annotation.logResponse()).thenReturn(false);
        when(annotation.async()).thenReturn(false);

        when(joinPoint.getArgs()).thenReturn(new Object[]{"auth-1"});
        when(joinPoint.proceed()).thenReturn("{\"sellerId\":\"123\"}");

        // 模拟日志记录失败（例如数据库连接失败）
        doThrow(new RuntimeException("Database connection failed")).when(opsService).recordApiLog(any());

        // Act - 不应抛出异常
        Object result = aspect.around(joinPoint, annotation);

        // Assert
        assertEquals("{\"sellerId\":\"123\"}", result);
        verify(opsService).recordApiLog(any());
    }

    private String buildRepeatedChar(char ch, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(ch);
        }
        return builder.toString();
    }
}
