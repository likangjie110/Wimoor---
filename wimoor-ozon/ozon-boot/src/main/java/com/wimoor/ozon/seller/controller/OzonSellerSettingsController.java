package com.wimoor.ozon.seller.controller;

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
import com.wimoor.ozon.seller.pojo.dto.OzonDeliveryMethodSaveCommand;
import com.wimoor.ozon.seller.pojo.entity.OzonDeliveryMethod;
import com.wimoor.ozon.seller.pojo.vo.OzonWarehouseView;
import com.wimoor.ozon.seller.service.IOzonSellerSettingsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
public class OzonSellerSettingsController {

    private final IOzonSellerSettingsService sellerSettingsService;
    private final OzonFeatureGate featureGate;

    @GetMapping("/warehouse/list")
    public Result<List<OzonWarehouseView>> listWarehouses(@RequestParam String authId) {
        return execute(() -> {
            featureGate.assertAuthEnabled();
            return sellerSettingsService.listWarehouses(currentUser(), authId);
        });
    }

    @GetMapping("/deliveryMethod/list")
    public Result<List<OzonDeliveryMethod>> listDeliveryMethods(@RequestParam String authId) {
        return execute(() -> {
            featureGate.assertAuthEnabled();
            return sellerSettingsService.listDeliveryMethods(currentUser(), authId);
        });
    }

    @PostMapping("/deliveryMethod/save")
    public Result<OzonDeliveryMethod> saveDeliveryMethod(@RequestBody OzonDeliveryMethodSaveCommand command) {
        return execute(() -> {
            featureGate.assertAuthEnabled();
            return sellerSettingsService.saveDeliveryMethod(currentUser(), command);
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
