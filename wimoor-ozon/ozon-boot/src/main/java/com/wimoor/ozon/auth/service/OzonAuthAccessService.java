package com.wimoor.ozon.auth.service;

import org.springframework.stereotype.Component;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OzonAuthAccessService {

    private final OzonAuthMapper authMapper;

    public OzonAuth requireOwnedAuth(UserInfo user, String authId) {
        String shopId = requireShopId(user);
        if (StrUtil.isBlank(authId)) {
            throw new IllegalArgumentException("authId不能为空");
        }
        OzonAuth auth = authMapper.selectById(authId.trim());
        if (auth == null) {
            throw new IllegalArgumentException("Ozon授权不存在");
        }
        if (!shopId.equals(auth.getShopId())) {
            throw new IllegalArgumentException("无权操作该Ozon授权");
        }
        return auth;
    }

    private String requireShopId(UserInfo user) {
        if (user == null || StrUtil.isBlank(user.getCompanyid())) {
            throw new IllegalArgumentException("当前用户缺少店铺上下文");
        }
        return user.getCompanyid();
    }
}
