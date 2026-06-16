package com.wimoor.ozon.config.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wimoor.common.result.Result;
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.config.pojo.vo.OzonFeatureView;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/meta")
@RequiredArgsConstructor
public class OzonMetaController {

    private final OzonFeatureGate featureGate;

    @GetMapping("/features")
    public Result<OzonFeatureView> features() {
        return Result.success(featureGate.describeFeatures());
    }
}
