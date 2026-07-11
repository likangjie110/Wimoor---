package com.wimoor.ozon.ops.aspect;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimoor.ozon.ops.annotation.OzonApiLog;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ozon API 日志 AOP 拦截器
 *
 * 自动拦截标注了 @OzonApiLog 的方法，记录 API 调用日志
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class OzonApiLogAspect {

    private final IOzonOpsService opsService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(ozonApiLog)")
    public Object around(ProceedingJoinPoint joinPoint, OzonApiLog ozonApiLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        String authId = null;
        String shopId = null;
        String requestPayload = null;
        String responsePayload = null;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            // 提取方法参数
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length >= 2) {
                if (args[0] instanceof String) {
                    authId = (String) args[0];
                }
                if (args.length >= 3 && args[2] instanceof String) {
                    requestPayload = (String) args[2];
                }
            }

            // 执行目标方法
            Object result = joinPoint.proceed();

            // 记录响应
            if (ozonApiLog.logResponse() && result != null) {
                if (result instanceof String) {
                    responsePayload = (String) result;
                } else {
                    try {
                        responsePayload = objectMapper.writeValueAsString(result);
                    } catch (Exception e) {
                        responsePayload = result.toString();
                    }
                }
            }

            return result;

        } catch (Throwable ex) {
            status = "FAILED";
            errorMessage = ex.getMessage();
            if (errorMessage != null && errorMessage.length() > 500) {
                errorMessage = errorMessage.substring(0, 500);
            }
            throw ex;

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // 构建日志记录命令
            OzonApiLogRecordCommand command = new OzonApiLogRecordCommand();
            command.setAuthId(authId);
            command.setShopId(shopId);
            command.setApiGroup(ozonApiLog.apiGroup());
            command.setActionName(ozonApiLog.actionName());
            command.setEndpoint(ozonApiLog.apiGroup() + "/" + ozonApiLog.actionName());
            command.setHttpMethod(ozonApiLog.httpMethod());
            command.setObjectType(StrUtil.blankToDefault(ozonApiLog.objectType(), null));
            command.setRequestPayloadJson(ozonApiLog.logRequest() ? truncate(requestPayload, 4000) : null);
            command.setResponsePayloadJson(ozonApiLog.logResponse() ? truncate(responsePayload, 4000) : null);
            command.setStatus(status);
            command.setErrorMessage(errorMessage);
            command.setDurationMs(duration);

            // 异步或同步记录
            if (ozonApiLog.async()) {
                recordAsync(command);
            } else {
                recordSync(command);
            }
        }
    }

    private void recordSync(OzonApiLogRecordCommand command) {
        try {
            opsService.recordApiLog(command);
        } catch (Exception ex) {
            log.error("Failed to record API log synchronously: {}", ex.getMessage());
        }
    }

    @Async
    private void recordAsync(OzonApiLogRecordCommand command) {
        CompletableFuture.runAsync(() -> {
            try {
                opsService.recordApiLog(command);
            } catch (Exception ex) {
                log.error("Failed to record API log asynchronously: {}", ex.getMessage());
            }
        });
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...(truncated)";
    }
}
