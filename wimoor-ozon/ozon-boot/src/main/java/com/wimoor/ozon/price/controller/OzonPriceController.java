package com.wimoor.ozon.price.controller;

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
import com.wimoor.ozon.price.pojo.dto.OzonPricePushCommand;
import com.wimoor.ozon.price.pojo.entity.OzonPriceSnapshot;
import com.wimoor.ozon.price.pojo.vo.OzonPricePushResult;
import com.wimoor.ozon.price.pojo.vo.OzonPriceTaskView;
import com.wimoor.ozon.price.service.IOzonPriceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/price")
@RequiredArgsConstructor
public class OzonPriceController {

    private final IOzonPriceService priceService;

    @PostMapping("/push")
    public Result<OzonPricePushResult> push(@RequestBody OzonPricePushCommand command) {
        return execute(() -> priceService.push(currentUser(), command));
    }

    @GetMapping("/snapshot/list")
    public Result<List<OzonPriceSnapshot>> listSnapshots(@RequestParam String authId) {
        return execute(() -> priceService.listSnapshots(currentUser(), authId));
    }

    @GetMapping("/task/list")
    public Result<List<OzonPriceTaskView>> listTasks(@RequestParam String authId) {
        return execute(() -> priceService.listTasks(currentUser(), authId));
    }

    private UserInfo currentUser() {
        return UserInfoContext.get();
    }

    private <T> Result<T> execute(PriceCall<T> call) {
        try {
            return Result.success(call.run());
        } catch (RuntimeException ex) {
            return Result.failed(ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface PriceCall<T> {
        T run();
    }
}
