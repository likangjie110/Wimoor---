package com.wimoor.ozon.finance.controller;

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
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.finance.pojo.dto.OzonFinanceImportCommand;
import com.wimoor.ozon.finance.pojo.dto.OzonFinanceTransactionQuery;
import com.wimoor.ozon.finance.pojo.entity.OzonFinTransaction;
import com.wimoor.ozon.finance.pojo.vo.OzonFinanceImportResult;
import com.wimoor.ozon.finance.pojo.vo.OzonFinanceTaskView;
import com.wimoor.ozon.finance.service.IOzonFinanceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class OzonFinanceController {

    private final IOzonFinanceService financeService;
    private final OzonFeatureGate featureGate;

    @PostMapping("/import")
    public Result<OzonFinanceImportResult> importReport(@RequestBody OzonFinanceImportCommand command) {
        return execute(() -> {
            featureGate.assertFinanceEnabled();
            return financeService.importReport(currentUser(), command);
        });
    }

    @GetMapping("/task/list")
    public Result<List<OzonFinanceTaskView>> listTasks(@RequestParam String authId) {
        return execute(() -> {
            featureGate.assertFinanceEnabled();
            return financeService.listTasks(currentUser(), authId);
        });
    }

    @GetMapping("/transaction/list")
    public Result<List<OzonFinTransaction>> listTransactions(OzonFinanceTransactionQuery query) {
        return execute(() -> {
            featureGate.assertFinanceEnabled();
            return financeService.listTransactions(currentUser(), query);
        });
    }

    @GetMapping("/task/raw")
    public Result<String> getRawContent(@RequestParam String authId, @RequestParam String taskId) {
        return execute(() -> {
            featureGate.assertFinanceEnabled();
            return financeService.getRawContent(currentUser(), authId, taskId);
        });
    }

    private UserInfo currentUser() {
        return UserInfoContext.get();
    }

    private <T> Result<T> execute(FinanceCall<T> call) {
        try {
            return Result.success(call.run());
        } catch (RuntimeException ex) {
            return Result.failed(ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface FinanceCall<T> {
        T run();
    }
}
