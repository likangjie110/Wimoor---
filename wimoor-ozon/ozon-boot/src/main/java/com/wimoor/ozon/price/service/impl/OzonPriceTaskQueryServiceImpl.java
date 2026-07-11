package com.wimoor.ozon.price.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.price.mapper.OzonPriceTaskMapper;
import com.wimoor.ozon.price.pojo.entity.OzonPriceTask;
import com.wimoor.ozon.price.pojo.vo.OzonPriceTaskDetailView;
import com.wimoor.ozon.price.service.IOzonPriceTaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OZON 价格任务查询服务实现
 *
 * @author Development Team
 * @since 2026-06-25
 */
@Service
@RequiredArgsConstructor
public class OzonPriceTaskQueryServiceImpl implements IOzonPriceTaskQueryService {

    private final OzonAuthAccessService authAccessService;
    private final OzonPriceTaskMapper priceTaskMapper;

    @Override
    public OzonPriceTaskDetailView getTaskDetail(UserInfo user, String authId, String taskId) {
        // 验证权限
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);

        // 查询任务详情
        OzonPriceTask task = priceTaskMapper.selectById(taskId);

        if (task == null || !task.getAuthId().equals(auth.getId())) {
            throw new IllegalArgumentException("任务不存在或无权限");
        }

        // 转换为视图对象
        OzonPriceTaskDetailView view = toDetailView(task);

        // TODO: 加载价格项结果列表
        // view.setItemResults(loadItemResults(taskId));

        return view;
    }

    @Override
    public List<OzonPriceTaskDetailView> listTasksBySku(UserInfo user, String authId, String sku) {
        // 验证权限
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);

        // TODO: 实现按 SKU 查询逻辑
        // 需要关联价格快照表或价格项表
        // 当前简化实现：返回该授权下的所有任务
        QueryWrapper<OzonPriceTask> wrapper = new QueryWrapper<>();
        wrapper.eq("auth_id", auth.getId());
        wrapper.eq("shop_id", user.getCompanyid());
        wrapper.orderByDesc("create_time");
        wrapper.last("LIMIT 10");

        List<OzonPriceTask> tasks = priceTaskMapper.selectList(wrapper);

        return tasks.stream().map(this::toDetailView).collect(Collectors.toList());
    }

    @Override
    public Map<String, Integer> getErrorSummary(UserInfo user, String authId) {
        // 验证权限
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);

        // 查询失败任务
        QueryWrapper<OzonPriceTask> wrapper = new QueryWrapper<>();
        wrapper.eq("auth_id", auth.getId());
        wrapper.eq("shop_id", user.getCompanyid());
        wrapper.eq("task_status", "FAILED");
        wrapper.isNotNull("error_message");
        wrapper.orderByDesc("create_time");
        wrapper.last("LIMIT 100");

        List<OzonPriceTask> failedTasks = priceTaskMapper.selectList(wrapper);

        // 统计错误类型
        Map<String, Integer> errorSummary = new HashMap<>();
        for (OzonPriceTask task : failedTasks) {
            String errorType = extractErrorType(task.getErrorMessage());
            errorSummary.put(errorType, errorSummary.getOrDefault(errorType, 0) + 1);
        }

        return errorSummary;
    }

    @Override
    public List<OzonPriceTaskDetailView> listTaskHistory(UserInfo user, String authId, Integer limit) {
        // 验证权限
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);

        // 查询任务历史
        QueryWrapper<OzonPriceTask> wrapper = new QueryWrapper<>();
        wrapper.eq("auth_id", auth.getId());
        wrapper.eq("shop_id", user.getCompanyid());
        wrapper.orderByDesc("create_time");

        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + Math.min(limit, 100));
        } else {
            wrapper.last("LIMIT 50");
        }

        List<OzonPriceTask> tasks = priceTaskMapper.selectList(wrapper);

        return tasks.stream().map(this::toDetailView).collect(Collectors.toList());
    }

    /**
     * 转换为详情视图对象
     */
    private OzonPriceTaskDetailView toDetailView(OzonPriceTask task) {
        OzonPriceTaskDetailView view = new OzonPriceTaskDetailView();
        view.setTaskId(task.getId());
        view.setAuthId(task.getAuthId());
        view.setTaskStatus(task.getTaskStatus());
        view.setRequestedCount(task.getRequestedCount() != null ? task.getRequestedCount() : 0);
        view.setSuccessCount(task.getSuccessCount() != null ? task.getSuccessCount() : 0);

        // 计算失败数
        int failedCount = view.getRequestedCount() - view.getSuccessCount();
        view.setFailedCount(Math.max(0, failedCount));

        view.setErrorMessage(task.getErrorMessage());
        view.setErrorSummary(extractErrorType(task.getErrorMessage()));
        view.setOperator(task.getOperator());
        view.setCreateTime(task.getCreateTime());
        view.setUpdateTime(task.getUpdateTime());

        return view;
    }

    /**
     * 提取错误类型
     */
    private String extractErrorType(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return "未知错误";
        }

        // 简化错误分类逻辑
        if (errorMessage.contains("timeout") || errorMessage.contains("超时")) {
            return "请求超时";
        } else if (errorMessage.contains("401") || errorMessage.contains("403")) {
            return "授权失败";
        } else if (errorMessage.contains("404")) {
            return "商品不存在";
        } else if (errorMessage.contains("400")) {
            return "参数错误";
        } else if (errorMessage.contains("500")) {
            return "服务器错误";
        } else if (errorMessage.length() > 50) {
            return errorMessage.substring(0, 50) + "...";
        } else {
            return errorMessage;
        }
    }

    /**
     * 加载价格项结果列表
     * TODO: 实现价格项结果查询
     */
    private List<OzonPriceTaskDetailView.PriceItemResult> loadItemResults(String taskId) {
        // 预留接口，后续实现
        return new ArrayList<>();
    }
}
