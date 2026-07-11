package com.wimoor.ozon.ads.controller;

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
import com.wimoor.ozon.ads.pojo.dto.OzonAdsImportCommand;
import com.wimoor.ozon.ads.pojo.dto.OzonAdsReportQuery;
import com.wimoor.ozon.ads.pojo.dto.OzonAdsSyncCommand;
import com.wimoor.ozon.ads.pojo.entity.OzonAdsAccount;
import com.wimoor.ozon.ads.pojo.entity.OzonAdsCampaign;
import com.wimoor.ozon.ads.pojo.entity.OzonAdsReport;
import com.wimoor.ozon.ads.pojo.vo.OzonAdsImportResult;
import com.wimoor.ozon.ads.pojo.vo.OzonAdsSummary;
import com.wimoor.ozon.ads.pojo.vo.OzonAdsSyncIntentResult;
import com.wimoor.ozon.ads.service.IOzonAdsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/ads")
@RequiredArgsConstructor
public class OzonAdsController {

    private final IOzonAdsService adsService;
    private final OzonFeatureGate featureGate;

    @PostMapping("/import")
    public Result<OzonAdsImportResult> importAds(@RequestBody OzonAdsImportCommand command) {
        return execute(() -> {
            featureGate.assertAdsEnabled();
            return adsService.importAds(currentUser(), command);
        });
    }

    @GetMapping("/account/list")
    public Result<List<OzonAdsAccount>> listAccounts(@RequestParam String authId) {
        return execute(() -> {
            featureGate.assertAdsEnabled();
            return adsService.listAccounts(currentUser(), authId);
        });
    }

    @GetMapping("/campaign/list")
    public Result<List<OzonAdsCampaign>> listCampaigns(
            @RequestParam String authId,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String keyword
    ) {
        return execute(() -> {
            featureGate.assertAdsEnabled();
            return adsService.listCampaigns(currentUser(), authId, accountId, keyword);
        });
    }

    @GetMapping("/report/list")
    public Result<List<OzonAdsReport>> listReports(OzonAdsReportQuery query) {
        return execute(() -> {
            featureGate.assertAdsEnabled();
            return adsService.listReports(currentUser(), query);
        });
    }

    @GetMapping("/summary")
    public Result<OzonAdsSummary> summary(OzonAdsReportQuery query) {
        return execute(() -> {
            featureGate.assertAdsEnabled();
            return adsService.summary(currentUser(), query);
        });
    }

    @PostMapping("/sync/intent")
    public Result<OzonAdsSyncIntentResult> recordSyncIntent(@RequestBody OzonAdsSyncCommand command) {
        return execute(() -> {
            featureGate.assertAdsEnabled();
            featureGate.assertAdsSyncEnabled();
            return adsService.recordSyncIntent(currentUser(), command);
        });
    }

    @PostMapping("/sync/campaigns")
    public Result<Integer> syncCampaigns(@RequestParam String authId) {
        return execute(() -> {
            featureGate.assertAdsEnabled();
            featureGate.assertAdsSyncEnabled();
            return adsService.syncCampaignsFromApi(currentUser(), authId).size();
        });
    }

    @PostMapping("/sync/reports")
    public Result<Integer> syncReports(
            @RequestParam String authId,
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        return execute(() -> {
            featureGate.assertAdsEnabled();
            featureGate.assertAdsSyncEnabled();
            java.time.LocalDate start = java.time.LocalDate.parse(startDate);
            java.time.LocalDate end = java.time.LocalDate.parse(endDate);
            return adsService.syncReportsFromApi(currentUser(), authId, start, end).size();
        });
    }

    private UserInfo currentUser() {
        return UserInfoContext.get();
    }

    private <T> Result<T> execute(AdsCall<T> call) {
        try {
            return Result.success(call.run());
        } catch (RuntimeException ex) {
            return Result.failed(ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface AdsCall<T> {
        T run();
    }
}
