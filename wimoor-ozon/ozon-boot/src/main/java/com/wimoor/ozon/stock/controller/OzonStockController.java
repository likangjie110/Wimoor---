package com.wimoor.ozon.stock.controller;

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
import com.wimoor.ozon.stock.pojo.dto.OzonStockPushCommand;
import com.wimoor.ozon.stock.pojo.entity.OzonStockSnapshot;
import com.wimoor.ozon.stock.pojo.vo.OzonStockPushResult;
import com.wimoor.ozon.stock.pojo.vo.OzonStockTaskView;
import com.wimoor.ozon.stock.service.IOzonStockService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
public class OzonStockController {

    private final IOzonStockService stockService;

    @PostMapping("/push")
    public Result<OzonStockPushResult> push(@RequestBody OzonStockPushCommand command) {
        return execute(() -> stockService.push(currentUser(), command));
    }

    @GetMapping("/snapshot/list")
    public Result<List<OzonStockSnapshot>> listSnapshots(@RequestParam String authId) {
        return execute(() -> stockService.listSnapshots(currentUser(), authId));
    }

    @GetMapping("/task/list")
    public Result<List<OzonStockTaskView>> listTasks(@RequestParam String authId) {
        return execute(() -> stockService.listTasks(currentUser(), authId));
    }

    private UserInfo currentUser() {
        return UserInfoContext.get();
    }

    private <T> Result<T> execute(StockCall<T> call) {
        try {
            return Result.success(call.run());
        } catch (RuntimeException ex) {
            return Result.failed(ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface StockCall<T> {
        T run();
    }
}
