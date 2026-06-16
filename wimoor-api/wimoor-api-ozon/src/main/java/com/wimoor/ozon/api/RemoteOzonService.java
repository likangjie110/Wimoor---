package com.wimoor.ozon.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.wimoor.common.ServiceNameConstants;
import com.wimoor.common.result.Result;

@Component
@FeignClient(contextId = "remoteOzonService", value = ServiceNameConstants.WIMOOR_OZON)
public interface RemoteOzonService {

    @GetMapping("/ozon/api/v1/auth/ping")
    Result<?> ping(@RequestParam String authId);
}
