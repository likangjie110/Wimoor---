package com.wimoor.admin.security;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.wimoor.common.CacheConstants;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;

@Component
@Order(0)
@RequiredArgsConstructor
public class AdminSessionUserInfoFilter implements Filter {

    private static final long SESSION_REFRESH_HOURS = 3L;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest) {
                bindUserInfo((HttpServletRequest) request);
            }
            chain.doFilter(request, response);
        } finally {
            UserInfoContext.set(null);
        }
    }

    private void bindUserInfo(HttpServletRequest request) {
        if (hasUserInfoHeader(request)) {
            return;
        }
        String sessionToken = request.getHeader("jsessionid");
        if (StrUtil.isBlank(sessionToken)) {
            return;
        }
        String sessionKey = CacheConstants.LOGIN_TOKEN_KEY + sessionToken.trim();
        String userJson = stringRedisTemplate.opsForValue().get(sessionKey);
        if (StrUtil.isBlank(userJson)) {
            return;
        }
        UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
        if (userInfo == null || !sessionToken.trim().equals(userInfo.getSession())) {
            return;
        }
        stringRedisTemplate.expire(sessionKey, SESSION_REFRESH_HOURS, TimeUnit.HOURS);
        UserInfoContext.set(userInfo);
    }

    private boolean hasUserInfoHeader(HttpServletRequest request) {
        return StrUtil.isNotBlank(request.getHeader(UserInfoContext.HEADER_USER_INFO));
    }
}
