package com.wimoor.ozon.ops.aspect;

import java.util.concurrent.CompletableFuture;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimoor.ozon.ops.annotation.OzonAudit;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ozon 操作审计 AOP 拦截器
 *
 * 自动拦截标注了 @OzonAudit 的方法，记录操作审计
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class OzonAuditAspect {

    private final IOzonOpsService opsService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(ozonAudit)")
    public Object around(ProceedingJoinPoint joinPoint, OzonAudit ozonAudit) throws Throwable {
        String authId = null;
        String shopId = null;
        String objectId = null;
        String objectCode = null;
        String requestPayload = null;
        String resultStatus = "SUCCESS";
        String resultMessage = null;

        try {
            // 提取方法参数
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                // 跳过 UserInfo 参数，提取实际业务参数
                for (Object arg : args) {
                    if (arg == null) continue;

                    // 跳过 UserInfo
                    if (arg.getClass().getName().contains("UserInfo")) {
                        continue;
                    }

                    // 尝试提取 authId（通过反射）
                    if (authId == null) {
                        authId = extractField(arg, "authId");
                    }
                    if (shopId == null) {
                        shopId = extractField(arg, "shopId");
                    }
                    if (objectId == null) {
                        objectId = extractField(arg, "id");
                        if (objectId == null) {
                            objectId = extractField(arg, "draftId");
                        }
                        if (objectId == null) {
                            objectId = extractField(arg, "taskId");
                        }
                    }

                    // 记录请求负载（仅记录第一个业务对象）
                    if (ozonAudit.logRequest() && requestPayload == null) {
                        try {
                            requestPayload = objectMapper.writeValueAsString(arg);
                        } catch (Exception e) {
                            requestPayload = arg.toString();
                        }
                    }
                }

                // 如果 authId 仍为空，检查是否为简单字符串参数
                if (authId == null && args.length > 1 && args[1] instanceof String) {
                    authId = (String) args[1];
                }
            }

            // 执行目标方法
            Object result = joinPoint.proceed();

            // 从返回结果提取信息
            if (result != null) {
                resultMessage = "Operation completed successfully";

                // 尝试从结果提取 objectId
                if (objectId == null) {
                    objectId = extractField(result, "id");
                    if (objectId == null) {
                        objectId = extractField(result, "draftId");
                    }
                    if (objectId == null) {
                        objectId = extractField(result, "taskId");
                    }
                }
                if (authId == null) {
                    authId = extractField(result, "authId");
                }
            }

            return result;

        } catch (Throwable ex) {
            resultStatus = "FAILED";
            resultMessage = ex.getMessage();
            if (resultMessage != null && resultMessage.length() > 500) {
                resultMessage = resultMessage.substring(0, 500);
            }
            throw ex;

        } finally {
            // 构建审计记录命令
            OzonOperationAuditRecordCommand command = new OzonOperationAuditRecordCommand();
            command.setAuthId(authId);
            command.setShopId(shopId);
            command.setOperationType(ozonAudit.operationType());
            command.setObjectType(ozonAudit.objectType());
            command.setObjectId(objectId);
            command.setObjectCode(objectCode);
            command.setRequestPayloadJson(truncate(requestPayload, 4000));
            command.setResultStatus(resultStatus);
            command.setResultMessage(resultMessage);

            // 异步或同步记录
            if (ozonAudit.async()) {
                recordAsync(command);
            } else {
                recordSync(command);
            }
        }
    }

    /**
     * 通过反射提取对象字段值
     */
    private String extractField(Object obj, String fieldName) {
        if (obj == null || StrUtil.isBlank(fieldName)) {
            return null;
        }
        try {
            java.lang.reflect.Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(obj);
                return value == null ? null : value.toString();
            }
        } catch (Exception e) {
            // 忽略反射异常
        }
        return null;
    }

    /**
     * 在类及其父类中查找字段
     */
    private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private void recordSync(OzonOperationAuditRecordCommand command) {
        try {
            opsService.recordOperationAudit(command);
        } catch (Exception ex) {
            log.error("Failed to record operation audit synchronously: {}", ex.getMessage());
        }
    }

    @Async
    private void recordAsync(OzonOperationAuditRecordCommand command) {
        CompletableFuture.runAsync(() -> {
            try {
                opsService.recordOperationAudit(command);
            } catch (Exception ex) {
                log.error("Failed to record operation audit asynchronously: {}", ex.getMessage());
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
