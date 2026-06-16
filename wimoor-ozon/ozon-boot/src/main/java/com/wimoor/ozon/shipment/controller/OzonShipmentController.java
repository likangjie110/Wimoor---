package com.wimoor.ozon.shipment.controller;

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
import com.wimoor.ozon.shipment.pojo.dto.OzonShipmentPushCommand;
import com.wimoor.ozon.shipment.pojo.entity.OzonShipment;
import com.wimoor.ozon.shipment.pojo.vo.OzonShipmentPushResult;
import com.wimoor.ozon.shipment.service.IOzonShipmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/shipment")
@RequiredArgsConstructor
public class OzonShipmentController {

    private final IOzonShipmentService shipmentService;

    @PostMapping("/pushTracking")
    public Result<OzonShipmentPushResult> pushTracking(@RequestBody OzonShipmentPushCommand command) {
        return execute(() -> shipmentService.pushTracking(currentUser(), command));
    }

    @GetMapping("/list")
    public Result<List<OzonShipment>> list(@RequestParam String authId, @RequestParam String postingId) {
        return execute(() -> shipmentService.listByPosting(currentUser(), authId, postingId));
    }

    private UserInfo currentUser() {
        return UserInfoContext.get();
    }

    private <T> Result<T> execute(ShipmentCall<T> call) {
        try {
            return Result.success(call.run());
        } catch (RuntimeException ex) {
            return Result.failed(ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface ShipmentCall<T> {
        T run();
    }
}
