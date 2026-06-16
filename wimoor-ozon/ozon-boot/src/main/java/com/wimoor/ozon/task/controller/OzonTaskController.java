package com.wimoor.ozon.task.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.task.pojo.dto.OzonTaskQuery;
import com.wimoor.ozon.task.pojo.vo.OzonTaskView;
import com.wimoor.ozon.task.service.IOzonTaskService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/task")
@RequiredArgsConstructor
public class OzonTaskController {

    private final IOzonTaskService taskService;
    private final OzonFeatureGate featureGate;

    @GetMapping("/list")
    public Result<List<OzonTaskView>> list(OzonTaskQuery query) {
        return execute(() -> {
            featureGate.assertTaskEnabled();
            return taskService.list(currentUser(), query);
        });
    }

    private UserInfo currentUser() {
        return UserInfoContext.get();
    }

    private <T> Result<T> execute(TaskCall<T> call) {
        try {
            return Result.success(call.run());
        } catch (RuntimeException ex) {
            return Result.failed(ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface TaskCall<T> {
        T run();
    }
}
