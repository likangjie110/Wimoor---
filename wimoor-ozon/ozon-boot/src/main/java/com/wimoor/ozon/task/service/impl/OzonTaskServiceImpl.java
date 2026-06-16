package com.wimoor.ozon.task.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.pojo.dto.OzonTaskQuery;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;
import com.wimoor.ozon.task.pojo.vo.OzonTaskView;
import com.wimoor.ozon.task.service.IOzonTaskService;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class OzonTaskServiceImpl implements IOzonTaskService {

    private final OzonAuthAccessService authAccessService;
    private final OzonSyncJobMapper syncJobMapper;

    @Autowired
    public OzonTaskServiceImpl(OzonAuthAccessService authAccessService, OzonSyncJobMapper syncJobMapper) {
        this.authAccessService = authAccessService;
        this.syncJobMapper = syncJobMapper;
    }

    @Override
    public List<OzonTaskView> list(UserInfo user, OzonTaskQuery query) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, query == null ? null : query.getAuthId());
        QueryWrapper<OzonSyncJob> wrapper = new QueryWrapper<OzonSyncJob>().eq("auth_id", auth.getId());
        if (query != null && StrUtil.isNotBlank(query.getJobType())) {
            wrapper.eq("job_type", query.getJobType().trim());
        }
        if (query != null && StrUtil.isNotBlank(query.getStatus())) {
            wrapper.eq("status", query.getStatus().trim());
        }
        wrapper.orderByDesc("create_time").last("limit 100");
        List<OzonSyncJob> jobs = syncJobMapper.selectList(wrapper);
        if (jobs == null || jobs.isEmpty()) {
            return Collections.emptyList();
        }
        String expectedJobType = query == null ? null : trim(query.getJobType());
        String expectedStatus = query == null ? null : trim(query.getStatus());
        List<OzonTaskView> result = new ArrayList<>(jobs.size());
        for (OzonSyncJob job : jobs) {
            if (expectedJobType != null && !expectedJobType.equals(job.getJobType())) {
                continue;
            }
            if (expectedStatus != null && !expectedStatus.equals(job.getStatus())) {
                continue;
            }
            OzonTaskView view = new OzonTaskView();
            view.setId(job.getId());
            view.setAuthId(job.getAuthId());
            view.setJobType(job.getJobType());
            view.setStatus(job.getStatus());
            view.setPayload(job.getPayload());
            view.setOperator(job.getOperator());
            view.setCreatedAt(job.getCreateTime());
            view.setUpdatedAt(job.getUpdateTime());
            result.add(view);
        }
        return result;
    }

    private String trim(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }
}
