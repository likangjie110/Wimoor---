package com.wimoor.ozon.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.product.mapper.OzonListingPublishTaskMapper;
import com.wimoor.ozon.product.pojo.entity.OzonListingPublishTask;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishTaskListView;
import com.wimoor.ozon.product.service.IOzonProductPublishTaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OzonProductPublishTaskQueryServiceImpl implements IOzonProductPublishTaskQueryService {

    private final OzonAuthAccessService authAccessService;
    private final OzonListingPublishTaskMapper publishTaskMapper;

    @Override
    public List<OzonProductPublishTaskListView> listByDraft(UserInfo user, String authId, String draftId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        QueryWrapper<OzonListingPublishTask> wrapper = new QueryWrapper<>();
        wrapper.eq("auth_id", auth.getId());
        wrapper.eq("draft_id", draftId);
        wrapper.orderByDesc("create_time");
        List<OzonListingPublishTask> tasks = publishTaskMapper.selectList(wrapper);
        return tasks.stream().map(this::toView).collect(Collectors.toList());
    }

    @Override
    public OzonProductPublishTaskListView getTaskDetail(UserInfo user, String authId, String taskId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        OzonListingPublishTask task = publishTaskMapper.selectById(taskId);
        if (task == null || !task.getAuthId().equals(auth.getId())) {
            throw new IllegalArgumentException("任务不存在或无权限");
        }
        OzonProductPublishTaskListView view = toView(task);
        view.setVariantResults(loadVariantResults(taskId));
        return view;
    }

    private OzonProductPublishTaskListView toView(OzonListingPublishTask task) {
        OzonProductPublishTaskListView view = new OzonProductPublishTaskListView();
        view.setTaskId(task.getId());
        view.setDraftId(task.getDraftId());
        view.setStatus(task.getStatus());
        view.setCreateTime(task.getCreateTime());
        view.setCompleteTime(task.getCompleteTime());
        view.setTotalVariants(task.getTotalVariants() != null ? task.getTotalVariants() : 0);
        view.setSuccessCount(task.getSuccessCount() != null ? task.getSuccessCount() : 0);
        view.setFailedCount(task.getFailedCount() != null ? task.getFailedCount() : 0);
        view.setErrorSummary(task.getErrorMessage());
        return view;
    }

    private List<OzonProductPublishTaskListView.VariantResult> loadVariantResults(String taskId) {
        return new ArrayList<>();
    }
}
