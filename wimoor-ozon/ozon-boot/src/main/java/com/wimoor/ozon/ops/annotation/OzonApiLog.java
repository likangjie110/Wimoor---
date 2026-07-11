package com.wimoor.ozon.ops.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ozon API 日志记录注解
 *
 * 标注在 API 客户端方法上，自动记录 API 调用日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OzonApiLog {

    /**
     * API 分组（如 Product、Stock、Price、Posting 等）
     */
    String apiGroup();

    /**
     * 操作名称（如 listProducts、updateStock、importPrice 等）
     */
    String actionName();

    /**
     * 对象类型（如 Product、Stock、Price、Posting 等）
     */
    String objectType() default "";

    /**
     * HTTP 方法
     */
    String httpMethod() default "POST";

    /**
     * 是否记录请求 payload（默认 true）
     */
    boolean logRequest() default true;

    /**
     * 是否记录响应 payload（默认 true）
     */
    boolean logResponse() default true;

    /**
     * 是否异步记录（默认 true，避免阻塞主流程）
     */
    boolean async() default true;
}
