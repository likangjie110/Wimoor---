package com.wimoor.ozon.aftersale.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.ozon.aftersale.pojo.dto.OzonCancellationSaveCommand;
import com.wimoor.ozon.aftersale.pojo.dto.OzonPackageSaveCommand;
import com.wimoor.ozon.aftersale.pojo.dto.OzonPostingCancelCommand;
import com.wimoor.ozon.aftersale.pojo.dto.OzonReturnSaveCommand;
import com.wimoor.ozon.aftersale.pojo.entity.OzonCancellationRecord;
import com.wimoor.ozon.aftersale.pojo.entity.OzonPackageRecord;
import com.wimoor.ozon.aftersale.pojo.entity.OzonReturnRecord;
import com.wimoor.ozon.aftersale.pojo.vo.OzonAfterSaleDetailView;
import com.wimoor.ozon.aftersale.service.IOzonAfterSaleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/posting/aftersale")
@RequiredArgsConstructor
public class OzonAfterSaleController {

    private static final long LEGACY_CANCEL_REASON_ID = 352L;

    private final IOzonAfterSaleService afterSaleService;

    @GetMapping("/detail")
    public Result<OzonAfterSaleDetailView> detail(@RequestParam String authId, @RequestParam String postingId) {
        return execute(() -> afterSaleService.getDetail(currentUser(), authId, postingId));
    }

    @PostMapping("/package/save")
    public Result<OzonPackageRecord> savePackage(@RequestBody OzonPackageSaveCommand command) {
        return execute(() -> afterSaleService.savePackage(currentUser(), command));
    }

    @PostMapping("/return/save")
    public Result<OzonReturnRecord> saveReturn(@RequestBody OzonReturnSaveCommand command) {
        return execute(() -> afterSaleService.saveReturn(currentUser(), command));
    }

    @PostMapping("/cancellation/save")
    public Result<OzonCancellationRecord> saveCancellation(@RequestBody OzonCancellationSaveCommand command) {
        return execute(() -> afterSaleService.saveCancellation(currentUser(), command));
    }

    @PostMapping("/posting/cancel")
    public Result<OzonCancellationRecord> cancelPosting(
            @RequestBody(required = false) OzonPostingCancelCommand command,
            @RequestParam(required = false) String authId,
            @RequestParam(required = false) String postingId,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Long cancelReasonId) {
        return execute(() -> afterSaleService.cancelPostingWithApi(
                currentUser(),
                normalizeCancelCommand(command, authId, postingId, reason, cancelReasonId)
        ));
    }

    public Result<OzonCancellationRecord> cancelPosting(String authId, String postingId, String reason) {
        return execute(() -> afterSaleService.cancelPostingWithApi(currentUser(), authId, postingId, reason));
    }

    private OzonPostingCancelCommand normalizeCancelCommand(
            OzonPostingCancelCommand command,
            String authId,
            String postingId,
            String reason,
            Long cancelReasonId
    ) {
        if (command != null) {
            return command;
        }
        return new OzonPostingCancelCommand(authId, postingId,
                cancelReasonId == null ? LEGACY_CANCEL_REASON_ID : cancelReasonId,
                reason);
    }

    @PostMapping("/package/sync")
    public Result<Void> syncPackages(@RequestParam String authId, @RequestParam String postingId) {
        return execute(() -> {
            afterSaleService.syncPackagesFromApi(currentUser(), authId, postingId);
            return null;
        });
    }

    @PostMapping("/return/sync")
    public Result<Void> syncReturns(@RequestParam String authId, @RequestParam String postingId) {
        return execute(() -> {
            afterSaleService.syncReturnsFromApi(currentUser(), authId, postingId);
            return null;
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
