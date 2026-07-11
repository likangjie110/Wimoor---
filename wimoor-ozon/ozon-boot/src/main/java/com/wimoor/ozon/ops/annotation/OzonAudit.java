package com.wimoor.ozon.ops.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ozon 操作审计注解
 *
 * 标注在业务方法上，自动记录操作审计
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OzonAudit {

    /**
     * 操作类型（如 CREATE、UPDATE、DELETE、SYNC、PUBLISH 等）
     */
    String operationType();

    /**
     * 对象类型（如 Product、Stock、Price、Posting 等）
     */
    String objectType();

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 是否记录请求 payload（默认 true）
     */
    boolean logRequest() default true;

    /**
     * 是否异步记录（默认 true）
     */
    boolean async() default true;
}
