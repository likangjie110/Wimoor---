package com.wimoor.ozon.error.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.error.mapper.OzonErrorEventMapper;
import com.wimoor.ozon.error.pojo.dto.OzonErrorQuery;
import com.wimoor.ozon.error.pojo.entity.OzonErrorEvent;
import com.wimoor.ozon.error.pojo.entity.OzonErrorSourceType;
import com.wimoor.ozon.error.pojo.entity.OzonErrorStatus;
import com.wimoor.ozon.error.pojo.vo.OzonErrorView;
import com.wimoor.ozon.error.service.IOzonErrorCenterService;
import com.wimoor.ozon.posting.service.IOzonPostingService;
import com.wimoor.ozon.shipment.pojo.dto.OzonShipmentPushCommand;
import com.wimoor.ozon.shipment.service.IOzonShipmentService;

import cn.hutool.core.util.StrUtil;

@Service
public class OzonErrorCenterServiceImpl implements IOzonErrorCenterService {

    private final OzonAuthAccessService authAccessService;
    private final OzonErrorEventMapper errorEventMapper;
    private final IOzonPostingService postingService;
    private final IOzonShipmentService shipmentService;

    @Autowired
    public OzonErrorCenterServiceImpl(
            OzonAuthAccessService authAccessService,
            OzonErrorEventMapper errorEventMapper,
            IOzonPostingService postingService,
            IOzonShipmentService shipmentService
    ) {
        this.authAccessService = authAccessService;
        this.errorEventMapper = errorEventMapper;
        this.postingService = postingService;
        this.shipmentService = shipmentService;
    }

    @Override
    public List<OzonErrorView> list(UserInfo user, OzonErrorQuery query) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, query == null ? null : query.getAuthId());
        QueryWrapper<OzonErrorEvent> wrapper = new QueryWrapper<OzonErrorEvent>().eq("auth_id", auth.getId());
        if (query != null && StrUtil.isNotBlank(query.getSourceType())) {
            wrapper.eq("source_type", query.getSourceType().trim());
        }
        if (query != null && StrUtil.isNotBlank(query.getStatus())) {
            wrapper.eq("status", query.getStatus().trim());
        }
        wrapper.orderByDesc("update_time").last("limit 100");
        List<OzonErrorEvent> events = errorEventMapper.selectList(wrapper);
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        String keyword = query == null ? null : trim(query.getKeyword());
        List<OzonErrorView> result = new ArrayList<>(events.size());
        for (OzonErrorEvent event : events) {
            if (matchesKeyword(event, keyword)) {
                result.add(toView(event));
            }
        }
        return result;
    }

    @Override
    public OzonErrorView retryOne(UserInfo user, String errorId) {
        OzonErrorEvent event = requireOwnedEvent(user, errorId);
        Date now = new Date();
        int nextRetryCount = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
        try {
            if (OzonErrorSourceType.POSTING.equals(event.getSourceType())) {
                postingService.retryOne(user, event.getAuthId(), event.getObjectId());
            } else if (OzonErrorSourceType.SHIPMENT.equals(event.getSourceType())) {
                shipmentService.pushTracking(user, parseShipmentCommand(event));
            } else {
                throw new IllegalArgumentException("暂不支持该错误类型重试");
            }
            event.setStatus(OzonErrorStatus.RESOLVED);
            event.setRetryCount(nextRetryCount);
            event.setLastRetryAt(now);
            event.setOperator(user == null ? null : user.getId());
            event.setUpdateTime(now);
            errorEventMapper.updateById(event);
            return toView(event);
        } catch (RuntimeException ex) {
            event.setStatus(OzonErrorStatus.OPEN);
            event.setRetryCount(nextRetryCount);
            event.setLastRetryAt(now);
            event.setErrorMessage(ex.getMessage());
            event.setOperator(user == null ? null : user.getId());
            event.setUpdateTime(now);
            errorEventMapper.updateById(event);
            throw ex;
        }
    }

    @Override
    public OzonErrorView ignore(UserInfo user, String errorId) {
        OzonErrorEvent event = requireOwnedEvent(user, errorId);
        event.setStatus(OzonErrorStatus.IGNORED);
        event.setOperator(user == null ? null : user.getId());
        event.setUpdateTime(new Date());
        errorEventMapper.updateById(event);
        return toView(event);
    }

    private OzonErrorEvent requireOwnedEvent(UserInfo user, String errorId) {
        if (StrUtil.isBlank(errorId)) {
            throw new IllegalArgumentException("errorId不能为空");
        }
        OzonErrorEvent event = errorEventMapper.selectById(errorId.trim());
        if (event == null) {
            throw new IllegalArgumentException("Ozon错误事件不存在");
        }
        authAccessService.requireOwnedAuth(user, event.getAuthId());
        return event;
    }

    private OzonShipmentPushCommand parseShipmentCommand(OzonErrorEvent event) {
        JSONObject payload = StrUtil.isBlank(event.getRequestPayloadJson()) ? null : JSONObject.parseObject(event.getRequestPayloadJson());
        if (payload == null) {
            throw new IllegalArgumentException("履约错误缺少重试载荷");
        }
        String authId = trim(payload.getString("authId"));
        String postingId = trim(payload.getString("postingId"));
        String trackingNumber = trim(payload.getString("trackingNumber"));
        if (StrUtil.hasBlank(authId, postingId, trackingNumber)) {
            throw new IllegalArgumentException("履约重试载荷不完整");
        }
        return new OzonShipmentPushCommand(authId, postingId, trackingNumber, trim(payload.getString("deliveryService")));
    }

    private boolean matchesKeyword(OzonErrorEvent event, String keyword) {
        if (keyword == null) {
            return true;
        }
        return StrUtil.containsIgnoreCase(event.getObjectCode(), keyword)
                || StrUtil.containsIgnoreCase(event.getObjectId(), keyword)
                || StrUtil.containsIgnoreCase(event.getErrorMessage(), keyword);
    }

    private OzonErrorView toView(OzonErrorEvent event) {
        OzonErrorView view = new OzonErrorView();
        view.setId(event.getId());
        view.setAuthId(event.getAuthId());
        view.setSourceType(event.getSourceType());
        view.setObjectId(event.getObjectId());
        view.setObjectCode(event.getObjectCode());
        view.setStatus(event.getStatus());
        view.setErrorMessage(event.getErrorMessage());
        view.setRequestPayloadJson(event.getRequestPayloadJson());
        view.setResponsePayloadJson(event.getResponsePayloadJson());
        view.setRetryCount(event.getRetryCount());
        view.setLastRetryAt(event.getLastRetryAt());
        view.setOperator(event.getOperator());
        view.setCreatedAt(event.getCreateTime());
        view.setUpdatedAt(event.getUpdateTime());
        return view;
    }

    private String trim(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }
}
