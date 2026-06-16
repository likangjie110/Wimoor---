package com.wimoor.ozon.ads.service;

import java.util.List;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.ads.pojo.dto.OzonAdsImportCommand;
import com.wimoor.ozon.ads.pojo.dto.OzonAdsReportQuery;
import com.wimoor.ozon.ads.pojo.dto.OzonAdsSyncCommand;
import com.wimoor.ozon.ads.pojo.entity.OzonAdsAccount;
import com.wimoor.ozon.ads.pojo.entity.OzonAdsCampaign;
import com.wimoor.ozon.ads.pojo.entity.OzonAdsReport;
import com.wimoor.ozon.ads.pojo.vo.OzonAdsImportResult;
import com.wimoor.ozon.ads.pojo.vo.OzonAdsSummary;
import com.wimoor.ozon.ads.pojo.vo.OzonAdsSyncIntentResult;

public interface IOzonAdsService {

    OzonAdsImportResult importAds(UserInfo user, OzonAdsImportCommand command);

    List<OzonAdsAccount> listAccounts(UserInfo user, String authId);

    List<OzonAdsCampaign> listCampaigns(UserInfo user, String authId, String accountId, String keyword);

    List<OzonAdsReport> listReports(UserInfo user, OzonAdsReportQuery query);

    OzonAdsSummary summary(UserInfo user, OzonAdsReportQuery query);

    OzonAdsSyncIntentResult recordSyncIntent(UserInfo user, OzonAdsSyncCommand command);
}
