package com.wimoor.ozon.ads.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.ads.mapper.OzonAdsAccountMapper;
import com.wimoor.ozon.ads.mapper.OzonAdsCampaignMapper;
import com.wimoor.ozon.ads.mapper.OzonAdsReportMapper;
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
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class OzonAdsServiceImpl implements IOzonAdsService {

    private final OzonAuthAccessService authAccessService;
    private final OzonAdsAccountMapper accountMapper;
    private final OzonAdsCampaignMapper campaignMapper;
    private final OzonAdsReportMapper reportMapper;
    private IOzonOpsService opsService = new IOzonOpsService() {
    };

    public OzonAdsServiceImpl(
            OzonAuthAccessService authAccessService,
            OzonAdsAccountMapper accountMapper,
            OzonAdsCampaignMapper campaignMapper,
            OzonAdsReportMapper reportMapper
    ) {
        this.authAccessService = authAccessService;
        this.accountMapper = accountMapper;
        this.campaignMapper = campaignMapper;
        this.reportMapper = reportMapper;
    }

    @Autowired(required = false)
    public void setOpsService(IOzonOpsService opsService) {
        if (opsService != null) {
            this.opsService = opsService;
        }
    }

    @Override
    public OzonAdsImportResult importAds(UserInfo user, OzonAdsImportCommand command) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command == null ? null : command.getAuthId());
        String rawContent = requireText(command == null ? null : command.getRawContent(), "rawContent不能为空");
        String auditPayload = JSON.toJSONString(command);
        try {
            JSONObject payload = JSON.parseObject(rawContent);
            JSONObject accountPayload = payload == null ? null : payload.getJSONObject("account");
            JSONArray campaigns = payload == null ? null : payload.getJSONArray("campaigns");
            JSONArray reports = payload == null ? null : payload.getJSONArray("reports");
            if (accountPayload == null) {
                throw new IllegalArgumentException("未找到广告账号数据");
            }
            if (campaigns == null || campaigns.isEmpty()) {
                throw new IllegalArgumentException("未找到广告活动数据");
            }
            Date now = new Date();
            OzonAdsAccount account = buildAccount(auth, accountPayload, now);
            accountMapper.upsert(account);
            int campaignCount = importCampaigns(auth, account, campaigns, now);
            int reportCount = importReports(auth, account, reports, now);
            OzonAdsImportResult result = new OzonAdsImportResult();
            result.setCampaignCount(campaignCount);
            result.setReportCount(reportCount);
            result.setImportedAt(now);
            recordOperationAudit(
                    auth,
                    user,
                    "ADS_IMPORT",
                    account.getAccountId(),
                    account.getAccountName(),
                    auditPayload,
                    "DONE",
                    "campaigns " + campaignCount + ", reports " + reportCount
            );
            return result;
        } catch (RuntimeException ex) {
            recordOperationAudit(
                    auth,
                    user,
                    "ADS_IMPORT",
                    auth.getId(),
                    "ADS_IMPORT",
                    auditPayload,
                    "FAILED",
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    public List<OzonAdsAccount> listAccounts(UserInfo user, String authId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        List<OzonAdsAccount> accounts = accountMapper.selectList(new QueryWrapper<OzonAdsAccount>()
                .eq("auth_id", auth.getId())
                .orderByDesc("update_time")
                .last("limit 20"));
        return accounts == null ? Collections.emptyList() : accounts;
    }

    @Override
    public List<OzonAdsCampaign> listCampaigns(UserInfo user, String authId, String accountId, String keyword) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        QueryWrapper<OzonAdsCampaign> wrapper = new QueryWrapper<OzonAdsCampaign>().eq("auth_id", auth.getId());
        if (StrUtil.isNotBlank(accountId)) {
            wrapper.eq("account_id", accountId.trim());
        }
        String cleanKeyword = trim(keyword);
        if (cleanKeyword != null) {
            wrapper.and(query -> query.like("campaign_name", cleanKeyword)
                    .or().like("campaign_id", cleanKeyword)
                    .or().like("campaign_type", cleanKeyword));
        }
        wrapper.orderByDesc("update_time").last("limit 100");
        List<OzonAdsCampaign> campaigns = campaignMapper.selectList(wrapper);
        return campaigns == null ? Collections.emptyList() : campaigns;
    }

    @Override
    public List<OzonAdsReport> listReports(UserInfo user, OzonAdsReportQuery query) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, query == null ? null : query.getAuthId());
        QueryWrapper<OzonAdsReport> wrapper = new QueryWrapper<OzonAdsReport>().eq("auth_id", auth.getId());
        if (query != null && StrUtil.isNotBlank(query.getAccountId())) {
            wrapper.eq("account_id", query.getAccountId().trim());
        }
        if (query != null && StrUtil.isNotBlank(query.getCampaignId())) {
            wrapper.eq("campaign_id", query.getCampaignId().trim());
        }
        if (query != null && StrUtil.isNotBlank(query.getFromDate())) {
            wrapper.ge("report_date", requireDay(query.getFromDate(), "fromDate格式不正确"));
        }
        if (query != null && StrUtil.isNotBlank(query.getToDate())) {
            wrapper.le("report_date", requireDay(query.getToDate(), "toDate格式不正确"));
        }
        wrapper.orderByDesc("report_date").last("limit 200");
        List<OzonAdsReport> reports = reportMapper.selectList(wrapper);
        return reports == null ? Collections.emptyList() : reports;
    }

    @Override
    public OzonAdsSummary summary(UserInfo user, OzonAdsReportQuery query) {
        List<OzonAdsReport> reports = listReports(user, query);
        OzonAdsSummary summary = new OzonAdsSummary();
        long impressions = 0L;
        long clicks = 0L;
        long orders = 0L;
        BigDecimal spend = BigDecimal.ZERO;
        BigDecimal sales = BigDecimal.ZERO;
        for (OzonAdsReport item : reports) {
            impressions += safeLong(item.getImpressions());
            clicks += safeLong(item.getClicks());
            orders += safeLong(item.getOrders());
            spend = spend.add(safeDecimal(item.getSpend()));
            sales = sales.add(safeDecimal(item.getSales()));
        }
        summary.setImpressions(impressions);
        summary.setClicks(clicks);
        summary.setOrders(orders);
        summary.setSpend(spend);
        summary.setSales(sales);
        summary.setAcos(resolveAcos(spend, sales));
        summary.setRoas(resolveRoas(spend, sales));
        return summary;
    }

    @Override
    public OzonAdsSyncIntentResult recordSyncIntent(UserInfo user, OzonAdsSyncCommand command) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command == null ? null : command.getAuthId());
        String accountId = requireText(command == null ? null : command.getAccountId(), "accountId不能为空");
        OzonAdsAccount account = accountMapper.selectOne(new QueryWrapper<OzonAdsAccount>()
                .eq("auth_id", auth.getId())
                .eq("account_id", accountId)
                .last("limit 1"));
        if (account == null) {
            throw new IllegalArgumentException("Ozon广告账号不存在");
        }
        String campaignId = trim(command.getCampaignId());
        if (campaignId != null) {
            OzonAdsCampaign campaign = campaignMapper.selectOne(new QueryWrapper<OzonAdsCampaign>()
                    .eq("auth_id", auth.getId())
                    .eq("account_id", accountId)
                    .eq("campaign_id", campaignId)
                    .last("limit 1"));
            if (campaign == null) {
                throw new IllegalArgumentException("Ozon广告活动不存在");
            }
        }
        String requestId = nextId();
        Date requestedAt = new Date();
        JSONObject payload = new JSONObject();
        payload.put("authId", auth.getId());
        payload.put("accountId", accountId);
        payload.put("campaignId", campaignId);
        payload.put("fromDate", trim(command.getFromDate()));
        payload.put("toDate", trim(command.getToDate()));
        recordOperationAudit(
                auth,
                user,
                "ADS_SYNC_INTENT",
                requestId,
                account.getAccountName(),
                payload.toJSONString(),
                "PENDING",
                "等待接入官方 Performance API"
        );
        OzonAdsSyncIntentResult result = new OzonAdsSyncIntentResult();
        result.setRequestId(requestId);
        result.setAccountId(accountId);
        result.setCampaignId(campaignId);
        result.setRequestStatus("PENDING");
        result.setMessage("已记录同步意图，等待官方 Performance API 接入");
        result.setRequestedAt(requestedAt);
        return result;
    }

    private OzonAdsAccount buildAccount(OzonAuth auth, JSONObject payload, Date now) {
        OzonAdsAccount account = new OzonAdsAccount();
        account.setId(nextId());
        account.setAuthId(auth.getId());
        account.setShopId(auth.getShopId());
        account.setAccountId(requireText(firstText(payload, "accountId", "account_id"), "accountId不能为空"));
        account.setAccountName(firstText(payload, "accountName", "account_name"));
        account.setStatus(firstText(payload, "status"));
        account.setCurrencyCode(StrUtil.blankToDefault(firstText(payload, "currencyCode", "currency_code"), "RUB"));
        account.setCreateTime(now);
        account.setUpdateTime(now);
        return account;
    }

    private int importCampaigns(OzonAuth auth, OzonAdsAccount account, JSONArray campaigns, Date now) {
        int count = 0;
        for (int index = 0; index < campaigns.size(); index++) {
            JSONObject item = campaigns.getJSONObject(index);
            String campaignId = requireText(firstText(item, "campaignId", "campaign_id"), "campaignId不能为空");
            OzonAdsCampaign campaign = new OzonAdsCampaign();
            campaign.setId(nextId());
            campaign.setAuthId(auth.getId());
            campaign.setShopId(auth.getShopId());
            campaign.setAccountId(account.getAccountId());
            campaign.setCampaignId(campaignId);
            campaign.setCampaignName(firstText(item, "campaignName", "campaign_name"));
            campaign.setCampaignType(firstText(item, "campaignType", "campaign_type"));
            campaign.setCampaignStatus(firstText(item, "campaignStatus", "campaign_status"));
            campaign.setBudget(parseDecimal(firstText(item, "budget")));
            campaign.setCreateTime(now);
            campaign.setUpdateTime(now);
            campaignMapper.upsert(campaign);
            count++;
        }
        return count;
    }

    private int importReports(OzonAuth auth, OzonAdsAccount account, JSONArray reports, Date now) {
        if (reports == null || reports.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int index = 0; index < reports.size(); index++) {
            JSONObject item = reports.getJSONObject(index);
            String campaignId = requireText(firstText(item, "campaignId", "campaign_id"), "campaignId不能为空");
            OzonAdsReport report = new OzonAdsReport();
            report.setId(nextId());
            report.setAuthId(auth.getId());
            report.setShopId(auth.getShopId());
            report.setAccountId(account.getAccountId());
            report.setCampaignId(campaignId);
            report.setReportDate(requireDay(firstText(item, "reportDate", "report_date"), "reportDate不能为空"));
            report.setImpressions(parseLong(firstText(item, "impressions")));
            report.setClicks(parseLong(firstText(item, "clicks")));
            report.setSpend(parseDecimal(firstText(item, "spend")));
            report.setOrders(parseLong(firstText(item, "orders")));
            report.setSales(parseDecimal(firstText(item, "sales")));
            report.setCtr(parseDecimal(firstText(item, "ctr")));
            report.setCpc(parseDecimal(firstText(item, "cpc")));
            report.setAcos(parseDecimal(firstText(item, "acos")));
            report.setRoas(parseDecimal(firstText(item, "roas")));
            report.setRawLineJson(item.toJSONString());
            report.setCreateTime(now);
            reportMapper.upsert(report);
            count++;
        }
        return count;
    }

    private BigDecimal resolveAcos(BigDecimal spend, BigDecimal sales) {
        if (sales == null || sales.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return spend.multiply(new BigDecimal("100")).divide(sales, 2, BigDecimal.ROUND_HALF_UP);
    }

    private BigDecimal resolveRoas(BigDecimal spend, BigDecimal sales) {
        if (spend == null || spend.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return sales.divide(spend, 2, BigDecimal.ROUND_HALF_UP);
    }

    private String firstText(JSONObject item, String... keys) {
        for (String key : keys) {
            String value = item.getString(key);
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private Long parseLong(String value) {
        if (StrUtil.isBlank(value)) {
            return 0L;
        }
        return Long.valueOf(value.trim());
    }

    private BigDecimal parseDecimal(String value) {
        if (StrUtil.isBlank(value)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.trim());
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Date requireDay(String value, String message) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return Date.from(LocalDate.parse(value.trim()).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private String requireText(String value, String message) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String trim(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private String nextId() {
        try {
            return IdUtil.getSnowflakeNextIdStr();
        } catch (IllegalStateException ex) {
            long fallback = System.currentTimeMillis() * 1000L + ThreadLocalRandom.current().nextInt(1000);
            return String.valueOf(fallback);
        }
    }

    private void recordOperationAudit(
            OzonAuth auth,
            UserInfo user,
            String operationType,
            String objectId,
            String objectCode,
            String requestPayload,
            String resultStatus,
            String resultMessage
    ) {
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                auth.getId(),
                auth.getShopId(),
                operationType,
                "ADS",
                objectId,
                objectCode,
                requestPayload,
                resultStatus,
                resultMessage,
                user == null ? null : user.getId()
        ));
    }
}
