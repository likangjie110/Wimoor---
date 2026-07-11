package com.wimoor.ozon.posting.controller;

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
import com.wimoor.ozon.posting.pojo.dto.OzonPostingSyncCommand;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingDetailView;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingSyncResult;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingView;
import com.wimoor.ozon.posting.service.IOzonPostingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/posting")
@RequiredArgsConstructor
public class OzonPostingController {

    private final IOzonPostingService postingService;

    @PostMapping("/sync")
    public Result<OzonPostingSyncResult> sync(@RequestBody OzonPostingSyncCommand command) {
        return execute(() -> postingService.syncIncremental(currentUser(), command));
    }

    @PostMapping("/retryOne")
    public Result<OzonPostingSyncResult> retryOne(@RequestParam String authId, @RequestParam String postingId) {
        return execute(() -> postingService.retryOne(currentUser(), authId, postingId));
    }

    @GetMapping("/list")
    public Result<List<OzonPostingView>> list(
            @RequestParam String authId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fulfillmentType,
            @RequestParam(required = false) String keyword
    ) {
        return execute(() -> postingService.list(currentUser(), authId, status, fulfillmentType, keyword));
    }

    @GetMapping("/detail")
    public Result<OzonPostingDetailView> detail(@RequestParam String authId, @RequestParam String postingId) {
        return execute(() -> postingService.getDetail(currentUser(), authId, postingId));
    }

    @PostMapping("/assignDeliveryMethod")
    public Result<Void> assignDeliveryMethod(
            @RequestParam String authId,
            @RequestParam String postingId,
            @RequestParam String deliveryMethodId
    ) {
        return execute(() -> {
            postingService.assignDeliveryMethod(currentUser(), authId, postingId, deliveryMethodId);
            return null;
        });
    }

    @GetMapping("/listByDeliveryMethod")
    public Result<List<OzonPostingView>> listByDeliveryMethod(
            @RequestParam String authId,
            @RequestParam String deliveryMethodId
    ) {
        return execute(() -> postingService.getPostingsByDeliveryMethod(currentUser(), authId, deliveryMethodId));
    }

    private UserInfo currentUser() {
        return UserInfoContext.get();
    }

    private <T> Result<T> execute(PostingCall<T> call) {
        try {
            return Result.success(call.run());
        } catch (RuntimeException ex) {
            return Result.failed(ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface PostingCall<T> {
        T run();
    }
}
