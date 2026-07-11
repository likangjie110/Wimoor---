package com.wimoor.ozon.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.stock.mapper.OzonStockTaskMapper;
import com.wimoor.ozon.stock.mapper.OzonStockSnapshotMapper;
import com.wimoor.ozon.stock.pojo.entity.OzonStockSnapshot;
import com.wimoor.ozon.stock.pojo.entity.OzonStockTask;
import com.wimoor.ozon.stock.pojo.vo.OzonStockTaskDetailView;
import com.wimoor.ozon.stock.service.IOzonStockTaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OZON 库存任务查询服务实现
 *
 * @author Development Team
 * @since 2026-06-25
 */
@Service
@RequiredArgsConstructor
public class OzonStockTaskQueryServiceImpl implements IOzonStockTaskQueryService {

    private final OzonAuthAccessService authAccessService;
    private final OzonStockTaskMapper stockTaskMapper;
    private final OzonStockSnapshotMapper stockSnapshotMapper;

    @Override
    public OzonStockTaskDetailView getTaskDetail(UserInfo user, String authId, String taskId) {
        // 验证权限
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);

        // 查询任务详情
        OzonStockTask task = stockTaskMapper.selectById(taskId);

        if (task == null || !task.getAuthId().equals(auth.getId())) {
            throw new IllegalArgumentException("任务不存在或无权限");
        }

        // 转换为视图对象
        OzonStockTaskDetailView view = toDetailView(task);

        view.setItemResults(loadItemResults(auth.getId(), taskId));

        return view;
    }

    @Override
    public List<OzonStockTaskDetailView> listTasksBySku(UserInfo user, String authId, String sku) {
        // 验证权限
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);

        if (sku == null || sku.trim().isEmpty()) {
            throw new IllegalArgumentException("sku不能为空");
        }
        List<OzonStockSnapshot> snapshots = stockSnapshotMapper.selectList(new QueryWrapper<OzonStockSnapshot>()
                .eq("auth_id", auth.getId())
                .and(wrapper -> wrapper.eq("material_sku", sku.trim()).or().eq("ozon_offer_id", sku.trim()))
                .isNotNull("task_id")
                .orderByDesc("synced_at")
                .last("LIMIT 50"));
        if (snapshots == null || snapshots.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> taskIds = snapshots.stream()
                .map(OzonStockSnapshot::getTaskId)
                .filter(Objects::nonNull)
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
        if (taskIds.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper<OzonStockTask> wrapper = new QueryWrapper<>();
        wrapper.eq("auth_id", auth.getId());
        wrapper.eq("shop_id", user.getCompanyid());
        wrapper.in("id", taskIds);
        wrapper.orderByDesc("create_time");
        wrapper.last("LIMIT 10");

        List<OzonStockTask> tasks = stockTaskMapper.selectList(wrapper);

        return tasks.stream().map(task -> {
            OzonStockTaskDetailView view = toDetailView(task);
            view.setItemResults(loadItemResults(auth.getId(), task.getId()));
            return view;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Integer> getErrorSummary(UserInfo user, String authId) {
        // 验证权限
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);

        // 查询失败任务
        QueryWrapper<OzonStockTask> wrapper = new QueryWrapper<>();
        wrapper.eq("auth_id", auth.getId());
        wrapper.eq("shop_id", user.getCompanyid());
        wrapper.eq("task_status", "FAILED");
        wrapper.isNotNull("error_message");
        wrapper.orderByDesc("create_time");
        wrapper.last("LIMIT 100");

        List<OzonStockTask> failedTasks = stockTaskMapper.selectList(wrapper);

        // 统计错误类型
        Map<String, Integer> errorSummary = new HashMap<>();
        for (OzonStockTask task : failedTasks) {
            String errorType = extractErrorType(task.getErrorMessage());
            errorSummary.put(errorType, errorSummary.getOrDefault(errorType, 0) + 1);
        }

        return errorSummary;
    }

    @Override
    public List<OzonStockTaskDetailView> listTaskHistory(UserInfo user, String authId, Integer limit) {
        // 验证权限
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);

        // 查询任务历史
        QueryWrapper<OzonStockTask> wrapper = new QueryWrapper<>();
        wrapper.eq("auth_id", auth.getId());
        wrapper.eq("shop_id", user.getCompanyid());
        wrapper.orderByDesc("create_time");

        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + Math.min(limit, 100));
        } else {
            wrapper.last("LIMIT 50");
        }

        List<OzonStockTask> tasks = stockTaskMapper.selectList(wrapper);

        return tasks.stream().map(this::toDetailView).collect(Collectors.toList());
    }

    /**
     * 转换为详情视图对象
     */
    private OzonStockTaskDetailView toDetailView(OzonStockTask task) {
        OzonStockTaskDetailView view = new OzonStockTaskDetailView();
        view.setTaskId(task.getId());
        view.setAuthId(task.getAuthId());
        view.setWarehouseId(task.getWarehouseId());
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
     * 加载库存项结果列表
     * TODO: 实现库存项结果查询
     */
    private List<OzonStockTaskDetailView.StockItemResult> loadItemResults(String authId, String taskId) {
        List<OzonStockSnapshot> snapshots = stockSnapshotMapper.selectList(new QueryWrapper<OzonStockSnapshot>()
                .eq("auth_id", authId)
                .eq("task_id", taskId)
                .orderByAsc("material_sku"));
        if (snapshots == null || snapshots.isEmpty()) {
            return Collections.emptyList();
        }
        return snapshots.stream().map(snapshot -> {
            OzonStockTaskDetailView.StockItemResult item = new OzonStockTaskDetailView.StockItemResult();
            item.setSku(snapshot.getMaterialSku());
            item.setOfferId(snapshot.getOzonOfferId());
            item.setRequestedStock(snapshot.getQuantity());
            item.setActualStock(snapshot.getQuantity());
            item.setStatus(snapshot.getSyncStatus());
            item.setErrorMessage(snapshot.getSyncMessage());
            return item;
        }).collect(Collectors.toList());
    }
}
