package com.wimoor.ozon.auth.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.ozon.auth.pojo.dto.OzonAuthBindCommand;
import com.wimoor.ozon.auth.pojo.dto.OzonRotateKeyCommand;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.pojo.vo.OzonAuthView;
import com.wimoor.ozon.auth.service.IOzonAuthService;
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.seller.pojo.vo.OzonWarehouseSyncResult;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class OzonAuthController {

    private final IOzonAuthService ozonAuthService;
    private final OzonFeatureGate featureGate;

    @PostMapping("/bind")
    public Result<OzonAuth> bind(@RequestBody OzonAuthBindCommand command) {
        return execute(() -> {
            featureGate.assertAuthEnabled();
            return ozonAuthService.bindAuth(currentUser(), command);
        });
    }

    @GetMapping("/list")
    public Result<List<OzonAuthView>> list() {
        return execute(() -> {
            featureGate.assertAuthEnabled();
            return ozonAuthService.listAuth(currentUser());
        });
    }

    @GetMapping("/ping")
    public Result<OzonWarehouseSyncResult> ping(@RequestParam String authId) {
        return execute(() -> {
            featureGate.assertAuthEnabled();
            return ozonAuthService.ping(currentUser(), authId);
        });
    }

    @PostMapping("/disable")
    public Result<Boolean> disable(@RequestParam String authId) {
        return execute(() -> {
            featureGate.assertAuthEnabled();
            ozonAuthService.disableAuth(currentUser(), authId);
            return Boolean.TRUE;
        });
    }

    @PostMapping("/rotateKey")
    public Result<OzonAuth> rotateKey(@RequestBody OzonRotateKeyCommand command) {
        return execute(() -> {
            featureGate.assertAuthEnabled();
            return ozonAuthService.rotateKey(currentUser(), command);
        });
    }

    private UserInfo currentUser() {
        return UserInfoContext.get();
    }

    private <T> Result<T> execute(ControllerCall<T> call) {
        try {
            return Result.success(call.run());
        } catch (RuntimeException ex) {
            return Result.failed(ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface ControllerCall<T> {
        T run();
    }
}
