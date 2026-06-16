package com.wimoor.ozon.aftersale.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.aftersale.mapper.OzonCancellationRecordMapper;
import com.wimoor.ozon.aftersale.mapper.OzonPackageRecordMapper;
import com.wimoor.ozon.aftersale.mapper.OzonReturnRecordMapper;
import com.wimoor.ozon.aftersale.pojo.dto.OzonCancellationSaveCommand;
import com.wimoor.ozon.aftersale.pojo.dto.OzonPackageSaveCommand;
import com.wimoor.ozon.aftersale.pojo.dto.OzonReturnSaveCommand;
import com.wimoor.ozon.aftersale.pojo.entity.OzonCancellationRecord;
import com.wimoor.ozon.aftersale.pojo.entity.OzonPackageRecord;
import com.wimoor.ozon.aftersale.pojo.entity.OzonReturnRecord;
import com.wimoor.ozon.aftersale.pojo.vo.OzonAfterSaleDetailView;
import com.wimoor.ozon.aftersale.service.IOzonAfterSaleService;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozon.posting.mapper.OzonPostingMapper;
import com.wimoor.ozon.posting.pojo.entity.OzonPosting;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class OzonAfterSaleServiceImpl implements IOzonAfterSaleService {

    private final OzonAuthAccessService authAccessService;
    private final OzonPostingMapper postingMapper;
    private final OzonPackageRecordMapper packageRecordMapper;
    private final OzonReturnRecordMapper returnRecordMapper;
    private final OzonCancellationRecordMapper cancellationRecordMapper;
    private final OzonFeatureGate featureGate;
    private IOzonOpsService opsService = new IOzonOpsService() {
    };

    @Autowired
    public OzonAfterSaleServiceImpl(
            OzonAuthAccessService authAccessService,
            OzonPostingMapper postingMapper,
            OzonPackageRecordMapper packageRecordMapper,
            OzonReturnRecordMapper returnRecordMapper,
            OzonCancellationRecordMapper cancellationRecordMapper,
            OzonFeatureGate featureGate
    ) {
        this.authAccessService = authAccessService;
        this.postingMapper = postingMapper;
        this.packageRecordMapper = packageRecordMapper;
        this.returnRecordMapper = returnRecordMapper;
        this.cancellationRecordMapper = cancellationRecordMapper;
        this.featureGate = featureGate;
    }

    public OzonAfterSaleServiceImpl(
            OzonAuthMapper authMapper,
            OzonPostingMapper postingMapper,
            OzonPackageRecordMapper packageRecordMapper,
            OzonReturnRecordMapper returnRecordMapper,
            OzonCancellationRecordMapper cancellationRecordMapper
    ) {
        this(new OzonAuthAccessService(authMapper), postingMapper, packageRecordMapper, returnRecordMapper, cancellationRecordMapper,
                OzonFeatureGate.allEnabled());
    }

    @Autowired(required = false)
    public void setOpsService(IOzonOpsService opsService) {
        if (opsService != null) {
            this.opsService = opsService;
        }
    }

    @Override
    public OzonAfterSaleDetailView getDetail(UserInfo user, String authId, String postingId) {
        OzonPosting posting = requireOwnedPosting(user, authId, postingId);
        OzonAfterSaleDetailView detail = new OzonAfterSaleDetailView();
        for (OzonPackageRecord item : safeList(packageRecordMapper.selectList(new QueryWrapper<OzonPackageRecord>()
                .eq("posting_id", posting.getId())
                .orderByDesc("create_time")))) {
            OzonAfterSaleDetailView.PackageItem view = new OzonAfterSaleDetailView.PackageItem();
            view.setId(item.getId());
            view.setPackageNumber(item.getPackageNumber());
            view.setPackageStatus(item.getPackageStatus());
            view.setTrackingNumber(item.getTrackingNumber());
            view.setRawPayloadJson(item.getRawPayloadJson());
            view.setCreatedAt(item.getCreateTime());
            view.setUpdatedAt(item.getUpdateTime());
            detail.getPackages().add(view);
        }
        for (OzonReturnRecord item : safeList(returnRecordMapper.selectList(new QueryWrapper<OzonReturnRecord>()
                .eq("posting_id", posting.getId())
                .orderByDesc("create_time")))) {
            OzonAfterSaleDetailView.ReturnItem view = new OzonAfterSaleDetailView.ReturnItem();
            view.setId(item.getId());
            view.setReturnNumber(item.getReturnNumber());
            view.setReturnStatus(item.getReturnStatus());
            view.setReason(item.getReason());
            view.setQuantity(item.getQuantity());
            view.setRawPayloadJson(item.getRawPayloadJson());
            view.setCreatedAt(item.getCreateTime());
            view.setUpdatedAt(item.getUpdateTime());
            detail.getReturns().add(view);
        }
        for (OzonCancellationRecord item : safeList(cancellationRecordMapper.selectList(new QueryWrapper<OzonCancellationRecord>()
                .eq("posting_id", posting.getId())
                .orderByDesc("create_time")))) {
            OzonAfterSaleDetailView.CancellationItem view = new OzonAfterSaleDetailView.CancellationItem();
            view.setId(item.getId());
            view.setCancellationNumber(item.getCancellationNumber());
            view.setCancellationStatus(item.getCancellationStatus());
            view.setReason(item.getReason());
            view.setRawPayloadJson(item.getRawPayloadJson());
            view.setCreatedAt(item.getCreateTime());
            view.setUpdatedAt(item.getUpdateTime());
            detail.getCancellations().add(view);
        }
        return detail;
    }

    @Override
    public OzonPackageRecord savePackage(UserInfo user, OzonPackageSaveCommand command) {
        featureGate.assertPostingWriteEnabled();
        OzonPosting posting = requireOwnedPosting(user, command == null ? null : command.getAuthId(), command == null ? null : command.getPostingId());
        String payload = JSON.toJSONString(command);
        String packageNumber = requireText(command == null ? null : command.getPackageNumber(), "packageNumber不能为空");
        try {
            OzonPackageRecord record = loadOrCreatePackage(posting, command, packageNumber);
            Date now = new Date();
            if (record.getCreateTime() == null) {
                record.setCreateTime(now);
            }
            record.setAuthId(posting.getAuthId());
            record.setShopId(posting.getShopId());
            record.setPostingId(posting.getId());
            record.setPostingNumber(posting.getPostingNumber());
            record.setPackageNumber(packageNumber);
            record.setPackageStatus(trim(command.getPackageStatus()));
            record.setTrackingNumber(trim(command.getTrackingNumber()));
            record.setRawPayloadJson(trim(command.getRawPayloadJson()));
            record.setUpdateTime(now);
            persistPackage(record);
            recordOperationAudit(posting, user, "AFTERSALE_PACKAGE_SAVE", payload, "SUCCESS", "saved");
            return record;
        } catch (RuntimeException ex) {
            recordOperationAudit(posting, user, "AFTERSALE_PACKAGE_SAVE", payload, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    @Override
    public OzonReturnRecord saveReturn(UserInfo user, OzonReturnSaveCommand command) {
        featureGate.assertPostingWriteEnabled();
        OzonPosting posting = requireOwnedPosting(user, command == null ? null : command.getAuthId(), command == null ? null : command.getPostingId());
        String payload = JSON.toJSONString(command);
        String returnNumber = requireText(command == null ? null : command.getReturnNumber(), "returnNumber不能为空");
        try {
            OzonReturnRecord record = loadOrCreateReturn(posting, command, returnNumber);
            Date now = new Date();
            if (record.getCreateTime() == null) {
                record.setCreateTime(now);
            }
            record.setAuthId(posting.getAuthId());
            record.setShopId(posting.getShopId());
            record.setPostingId(posting.getId());
            record.setPostingNumber(posting.getPostingNumber());
            record.setReturnNumber(returnNumber);
            record.setReturnStatus(trim(command.getReturnStatus()));
            record.setReason(trim(command.getReason()));
            record.setQuantity(command.getQuantity());
            record.setRawPayloadJson(trim(command.getRawPayloadJson()));
            record.setUpdateTime(now);
            persistReturn(record);
            recordOperationAudit(posting, user, "AFTERSALE_RETURN_SAVE", payload, "SUCCESS", "saved");
            return record;
        } catch (RuntimeException ex) {
            recordOperationAudit(posting, user, "AFTERSALE_RETURN_SAVE", payload, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    @Override
    public OzonCancellationRecord saveCancellation(UserInfo user, OzonCancellationSaveCommand command) {
        featureGate.assertPostingWriteEnabled();
        OzonPosting posting = requireOwnedPosting(user, command == null ? null : command.getAuthId(), command == null ? null : command.getPostingId());
        String payload = JSON.toJSONString(command);
        String cancellationNumber = requireText(command == null ? null : command.getCancellationNumber(), "cancellationNumber不能为空");
        try {
            OzonCancellationRecord record = loadOrCreateCancellation(posting, command, cancellationNumber);
            Date now = new Date();
            if (record.getCreateTime() == null) {
                record.setCreateTime(now);
            }
            record.setAuthId(posting.getAuthId());
            record.setShopId(posting.getShopId());
            record.setPostingId(posting.getId());
            record.setPostingNumber(posting.getPostingNumber());
            record.setCancellationNumber(cancellationNumber);
            record.setCancellationStatus(trim(command.getCancellationStatus()));
            record.setReason(trim(command.getReason()));
            record.setRawPayloadJson(trim(command.getRawPayloadJson()));
            record.setUpdateTime(now);
            persistCancellation(record);
            recordOperationAudit(posting, user, "AFTERSALE_CANCELLATION_SAVE", payload, "SUCCESS", "saved");
            return record;
        } catch (RuntimeException ex) {
            recordOperationAudit(posting, user, "AFTERSALE_CANCELLATION_SAVE", payload, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    private OzonPosting requireOwnedPosting(UserInfo user, String authId, String postingId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        if (StrUtil.isBlank(postingId)) {
            throw new IllegalArgumentException("postingId不能为空");
        }
        OzonPosting posting = postingMapper.selectById(postingId.trim());
        if (posting == null || !StrUtil.equals(posting.getAuthId(), auth.getId())) {
            throw new IllegalArgumentException("Ozon posting不存在");
        }
        return posting;
    }

    private OzonPackageRecord loadOrCreatePackage(OzonPosting posting, OzonPackageSaveCommand command, String packageNumber) {
        if (command != null && StrUtil.isNotBlank(command.getId())) {
            OzonPackageRecord existing = packageRecordMapper.selectById(command.getId().trim());
            if (existing != null && StrUtil.equals(existing.getAuthId(), posting.getAuthId())) {
                return existing;
            }
        }
        OzonPackageRecord existing = packageRecordMapper.selectOne(new QueryWrapper<OzonPackageRecord>()
                .eq("auth_id", posting.getAuthId())
                .eq("posting_id", posting.getId())
                .eq("package_number", packageNumber)
                .last("limit 1"));
        return existing == null ? new OzonPackageRecord() : existing;
    }

    private OzonReturnRecord loadOrCreateReturn(OzonPosting posting, OzonReturnSaveCommand command, String returnNumber) {
        if (command != null && StrUtil.isNotBlank(command.getId())) {
            OzonReturnRecord existing = returnRecordMapper.selectById(command.getId().trim());
            if (existing != null && StrUtil.equals(existing.getAuthId(), posting.getAuthId())) {
                return existing;
            }
        }
        OzonReturnRecord existing = returnRecordMapper.selectOne(new QueryWrapper<OzonReturnRecord>()
                .eq("auth_id", posting.getAuthId())
                .eq("posting_id", posting.getId())
                .eq("return_number", returnNumber)
                .last("limit 1"));
        return existing == null ? new OzonReturnRecord() : existing;
    }

    private OzonCancellationRecord loadOrCreateCancellation(OzonPosting posting, OzonCancellationSaveCommand command, String cancellationNumber) {
        if (command != null && StrUtil.isNotBlank(command.getId())) {
            OzonCancellationRecord existing = cancellationRecordMapper.selectById(command.getId().trim());
            if (existing != null && StrUtil.equals(existing.getAuthId(), posting.getAuthId())) {
                return existing;
            }
        }
        OzonCancellationRecord existing = cancellationRecordMapper.selectOne(new QueryWrapper<OzonCancellationRecord>()
                .eq("auth_id", posting.getAuthId())
                .eq("posting_id", posting.getId())
                .eq("cancellation_number", cancellationNumber)
                .last("limit 1"));
        return existing == null ? new OzonCancellationRecord() : existing;
    }

    private void persistPackage(OzonPackageRecord record) {
        if (StrUtil.isBlank(record.getId())) {
            record.setId(nextId());
            packageRecordMapper.insert(record);
            return;
        }
        if (packageRecordMapper.selectById(record.getId()) == null) {
            packageRecordMapper.insert(record);
        } else {
            packageRecordMapper.updateById(record);
        }
    }

    private void persistReturn(OzonReturnRecord record) {
        if (StrUtil.isBlank(record.getId())) {
            record.setId(nextId());
            returnRecordMapper.insert(record);
            return;
        }
        if (returnRecordMapper.selectById(record.getId()) == null) {
            returnRecordMapper.insert(record);
        } else {
            returnRecordMapper.updateById(record);
        }
    }

    private void persistCancellation(OzonCancellationRecord record) {
        if (StrUtil.isBlank(record.getId())) {
            record.setId(nextId());
            cancellationRecordMapper.insert(record);
            return;
        }
        if (cancellationRecordMapper.selectById(record.getId()) == null) {
            cancellationRecordMapper.insert(record);
        } else {
            cancellationRecordMapper.updateById(record);
        }
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
        return IdUtil.getSnowflakeNextIdStr();
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? Collections.emptyList() : rows;
    }

    private void recordOperationAudit(
            OzonPosting posting,
            UserInfo user,
            String operationType,
            String requestPayload,
            String resultStatus,
            String resultMessage
    ) {
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                posting.getAuthId(),
                posting.getShopId(),
                operationType,
                "POSTING",
                posting.getId(),
                posting.getPostingNumber(),
                requestPayload,
                resultStatus,
                resultMessage,
                user == null ? null : user.getId()
        ));
    }
}
