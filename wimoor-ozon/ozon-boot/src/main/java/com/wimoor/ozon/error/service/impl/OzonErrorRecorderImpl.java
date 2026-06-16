package com.wimoor.ozon.error.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.ozon.error.mapper.OzonErrorEventMapper;
import com.wimoor.ozon.error.pojo.dto.OzonErrorRecordCommand;
import com.wimoor.ozon.error.pojo.entity.OzonErrorEvent;
import com.wimoor.ozon.error.pojo.entity.OzonErrorStatus;
import com.wimoor.ozon.error.service.OzonErrorRecorder;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OzonErrorRecorderImpl implements OzonErrorRecorder {

    private final OzonErrorEventMapper errorEventMapper;

    @Override
    public void recordOpen(OzonErrorRecordCommand command) {
        if (command == null || StrUtil.hasBlank(command.getAuthId(), command.getSourceType(), command.getObjectId())) {
            return;
        }
        Date now = new Date();
        OzonErrorEvent existing = errorEventMapper.selectOne(new QueryWrapper<OzonErrorEvent>()
                .eq("auth_id", command.getAuthId())
                .eq("source_type", command.getSourceType())
                .eq("object_id", command.getObjectId())
                .eq("status", OzonErrorStatus.OPEN)
                .last("limit 1"));
        if (existing == null) {
            OzonErrorEvent event = new OzonErrorEvent();
            event.setId(IdUtil.getSnowflakeNextIdStr());
            event.setAuthId(command.getAuthId());
            event.setShopId(command.getShopId());
            event.setSourceType(command.getSourceType());
            event.setObjectId(command.getObjectId());
            event.setObjectCode(command.getObjectCode());
            event.setStatus(OzonErrorStatus.OPEN);
            event.setErrorMessage(command.getErrorMessage());
            event.setRequestPayloadJson(command.getRequestPayloadJson());
            event.setResponsePayloadJson(command.getResponsePayloadJson());
            event.setRetryCount(0);
            event.setOperator(command.getOperator());
            event.setCreateTime(now);
            event.setUpdateTime(now);
            errorEventMapper.insert(event);
            return;
        }
        existing.setObjectCode(command.getObjectCode());
        existing.setErrorMessage(command.getErrorMessage());
        existing.setRequestPayloadJson(command.getRequestPayloadJson());
        existing.setResponsePayloadJson(command.getResponsePayloadJson());
        existing.setOperator(command.getOperator());
        existing.setUpdateTime(now);
        errorEventMapper.updateById(existing);
    }

    @Override
    public void markResolved(String authId, String sourceType, String objectId) {
        if (StrUtil.hasBlank(authId, sourceType, objectId)) {
            return;
        }
        List<OzonErrorEvent> openEvents = errorEventMapper.selectList(new QueryWrapper<OzonErrorEvent>()
                .eq("auth_id", authId)
                .eq("source_type", sourceType)
                .eq("object_id", objectId)
                .eq("status", OzonErrorStatus.OPEN));
        if (openEvents == null || openEvents.isEmpty()) {
            return;
        }
        Date now = new Date();
        for (OzonErrorEvent event : openEvents) {
            event.setStatus(OzonErrorStatus.RESOLVED);
            event.setUpdateTime(now);
            errorEventMapper.updateById(event);
        }
    }
}
