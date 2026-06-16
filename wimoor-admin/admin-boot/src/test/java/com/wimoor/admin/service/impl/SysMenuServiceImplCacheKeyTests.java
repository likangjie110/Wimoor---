package com.wimoor.admin.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.wimoor.admin.service.ISysMenuService;
import com.wimoor.common.user.UserInfo;

class SysMenuServiceImplCacheKeyTests {

    private final ExpressionParser parser = new SpelExpressionParser();

    @Test
    void routeCacheKeyResolvesUserIdForInterfaceInvocation() throws Exception {
        Method interfaceMethod = ISysMenuService.class.getMethod("listRoute", UserInfo.class);
        Method implMethod = SysMenuServiceImpl.class.getMethod("listRoute", UserInfo.class);
        Cacheable cacheable = implMethod.getAnnotation(Cacheable.class);

        UserInfo user = new UserInfo();
        user.setId("user-001");

        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                this,
                interfaceMethod,
                new Object[] { user },
                new DefaultParameterNameDiscoverer()
        );

        Object value = parser.parseExpression(cacheable.key()).getValue(context);
        assertEquals("user-001", value);
    }

    @Test
    void routeCacheEvictKeyResolvesUserIdForInterfaceInvocation() throws Exception {
        Method interfaceMethod = ISysMenuService.class.getMethod("cleanCacheByUser", UserInfo.class);
        Method implMethod = SysMenuServiceImpl.class.getMethod("cleanCacheByUser", UserInfo.class);
        CacheEvict cacheEvict = implMethod.getAnnotation(CacheEvict.class);

        UserInfo user = new UserInfo();
        user.setId("user-001");

        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                this,
                interfaceMethod,
                new Object[] { user },
                new DefaultParameterNameDiscoverer()
        );

        Object value = parser.parseExpression(cacheEvict.key()).getValue(context);
        assertEquals("user-001", value);
    }
}
