package com.wimoor.ozon.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.alibaba.fastjson.JSONObject;
import com.wimoor.common.CacheConstants;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;

class OzonSessionUserInfoFilterTests {

    @AfterEach
    void clearContext() {
        UserInfoContext.set(null);
    }

    @Test
    void filterLoadsUserInfoFromRedisWhenJsessionIdProvided() throws ServletException, IOException {
        String token = "session-token";
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        UserInfo userInfo = new UserInfo();
        userInfo.setSession(token);
        userInfo.setCompanyid("shop-001");
        userInfo.setAccount("admin@wimoor.com");
        when(valueOperations.get(CacheConstants.LOGIN_TOKEN_KEY + token)).thenReturn(JSONObject.toJSONString(userInfo));

        OzonSessionUserInfoFilter filter = new OzonSessionUserInfoFilter(stringRedisTemplate);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("jsessionid", token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, assertUserLoaded("shop-001"));
    }

    private FilterChain assertUserLoaded(String expectedShopId) {
        return (request, response) -> {
            UserInfo currentUser = UserInfoContext.get();
            assertNotNull(currentUser);
            assertEquals(expectedShopId, currentUser.getCompanyid());
        };
    }
}
