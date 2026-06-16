package com.wimoor.ozon.error.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.error.pojo.dto.OzonErrorQuery;
import com.wimoor.ozon.error.pojo.vo.OzonErrorView;
import com.wimoor.ozon.error.service.IOzonErrorCenterService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/error")
@RequiredArgsConstructor
public class OzonErrorCenterController {

    private final IOzonErrorCenterService errorCenterService;
    private final OzonFeatureGate featureGate;

    @GetMapping("/list")
    public Result<List<OzonErrorView>> list(OzonErrorQuery query) {
        return execute(() -> {
            featureGate.assertErrorEnabled();
            return errorCenterService.list(currentUser(), query);
        });
    }

    @PostMapping("/retryOne")
    public Result<OzonErrorView> retryOne(@RequestParam String errorId) {
        return execute(() -> {
            featureGate.assertErrorEnabled();
            return errorCenterService.retryOne(currentUser(), errorId);
        });
    }

    @PostMapping("/ignore")
    public Result<OzonErrorView> ignore(@RequestParam String errorId) {
        return execute(() -> {
            featureGate.assertErrorEnabled();
            return errorCenterService.ignore(currentUser(), errorId);
        });
    }

    private UserInfo currentUser() {
        return UserInfoContext.get();
    }

    private <T> Result<T> execute(ErrorCall<T> call) {
        try {
            return Result.success(call.run());
        } catch (RuntimeException ex) {
            return Result.failed(ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface ErrorCall<T> {
        T run();
    }
}
