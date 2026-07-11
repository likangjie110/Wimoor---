package com.wimoor.ozon.product.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftArchiveCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftCloneCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftDetailQuery;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftImportCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftListQuery;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftSaveCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductMapSaveCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductPreviewCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductPublishCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductPublishTaskQuery;
import com.wimoor.ozon.product.pojo.entity.OzonProductMap;
import com.wimoor.ozon.product.pojo.vo.OzonProductCategoryTemplateView;
import com.wimoor.ozon.product.pojo.vo.OzonProductCategoryTreeView;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftDetailView;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftImportResult;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftListView;
import com.wimoor.ozon.product.pojo.vo.OzonProductMapView;
import com.wimoor.ozon.product.pojo.vo.OzonProductPreviewView;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishTaskListView;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishTaskView;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishTaskHistoryView;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishView;
import com.wimoor.ozon.product.service.IOzonListingDraftService;
import com.wimoor.ozon.product.service.IOzonProductMapService;
import com.wimoor.ozon.product.service.IOzonProductMetadataService;
import com.wimoor.ozon.product.service.IOzonProductPreviewService;
import com.wimoor.ozon.product.service.IOzonProductPublishService;
import com.wimoor.ozon.product.service.IOzonProductPublishTaskQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class OzonProductController {

    private final IOzonProductMapService productMapService;
    private final IOzonListingDraftService listingDraftService;
    private final IOzonProductMetadataService metadataService;
    private final IOzonProductPreviewService previewService;
    private final IOzonProductPublishService publishService;
    private final OzonFeatureGate featureGate;
    private final IOzonProductPublishTaskQueryService taskQueryService;

    @GetMapping("/list")
    public Result<List<OzonProductMapView>> list(@RequestParam String authId, @RequestParam(required = false) String keyword) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            return productMapService.list(currentUser(), authId, keyword);
        });
    }

    @PostMapping("/map/save")
    public Result<OzonProductMap> saveMapping(@RequestBody OzonProductMapSaveCommand command) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            return productMapService.saveMapping(currentUser(), command);
        });
    }

    @PostMapping("/importDraft")
    public Result<OzonProductDraftImportResult> importDraft(@RequestBody OzonProductDraftImportCommand command) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            return listingDraftService.importDraft(currentUser(), command);
        });
    }

    @GetMapping("/draft/list")
    public Result<List<OzonProductDraftListView>> draftList(
            @RequestParam String authId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            return listingDraftService.listDrafts(currentUser(), new OzonProductDraftListQuery(authId, status, keyword));
        });
    }

    @PostMapping("/draft/save")
    public Result<OzonProductDraftDetailView> saveDraft(@RequestBody OzonProductDraftSaveCommand command) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            return listingDraftService.saveDraft(currentUser(), command);
        });
    }

    @GetMapping("/draft/detail")
    public Result<OzonProductDraftDetailView> draftDetail(@RequestParam String authId, @RequestParam String draftId) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            return listingDraftService.getDraftDetail(currentUser(), new OzonProductDraftDetailQuery(authId, draftId));
        });
    }

    @GetMapping("/category/tree")
    public Result<OzonProductCategoryTreeView> categoryTree(
            @RequestParam String authId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String language
    ) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            return metadataService.getCategoryTree(currentUser(), authId, keyword, language);
        });
    }

    @GetMapping("/category/template")
    public Result<OzonProductCategoryTemplateView> categoryTemplate(
            @RequestParam String authId,
            @RequestParam Long descriptionCategoryId,
            @RequestParam Long typeId,
            @RequestParam(required = false) String language
    ) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            return metadataService.getTemplate(currentUser(), authId, descriptionCategoryId, typeId, language);
        });
    }

    @PostMapping("/preview")
    public Result<OzonProductPreviewView> preview(@RequestBody OzonProductPreviewCommand command) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            return previewService.preview(currentUser(), command);
        });
    }

    @PostMapping("/publish")
    public Result<OzonProductPublishView> publish(@RequestBody OzonProductPublishCommand command) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            featureGate.assertProductWriteEnabled();
            return publishService.publish(currentUser(), command);
        });
    }

    @GetMapping("/publish/task/detail")
    public Result<OzonProductPublishTaskView> publishTaskDetail(@RequestParam String authId, @RequestParam String taskId) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            return publishService.getTaskDetail(currentUser(), new OzonProductPublishTaskQuery(authId, taskId));
        });
    }

    @GetMapping("/publish/task/list")
    public Result<List<OzonProductPublishTaskHistoryView>> publishTaskList(@RequestParam String authId, @RequestParam String draftId) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            return publishService.listTaskHistory(currentUser(), authId, draftId);
        });
    }

    // ========== 草稿生命周期管理接口 ==========

    @PostMapping("/draft/clone")
    public Result<OzonProductDraftDetailView> cloneDraft(@RequestBody OzonProductDraftCloneCommand command) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            return listingDraftService.cloneDraft(currentUser(), command);
        });
    }

    @PostMapping("/draft/archive")
    public Result<Void> archiveDraft(@RequestBody OzonProductDraftArchiveCommand command) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            listingDraftService.archiveDraft(currentUser(), command);
            return null;
        });
    }

    @DeleteMapping("/draft/delete")
    public Result<Void> deleteDraft(@RequestParam String authId, @RequestParam String draftId) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            listingDraftService.deleteDraft(currentUser(), authId, draftId);
            return null;
        });
    }

    @GetMapping("/draft/listByStatus")
    public Result<List<OzonProductDraftListView>> listDraftsByStatus(
            @RequestParam String authId,
            @RequestParam(required = false) String status
    ) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            return listingDraftService.listByStatus(currentUser(), authId, status);
        });
    }

    // ========== 任务历史查询接口 ==========

    @GetMapping("/publish/task/history")
    public Result<List<OzonProductPublishTaskListView>> getTaskHistory(
            @RequestParam String authId,
            @RequestParam String draftId
    ) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            return taskQueryService.listByDraft(currentUser(), authId, draftId);
        });
    }

    @GetMapping("/publish/task/query/detail")
    public Result<OzonProductPublishTaskListView> getTaskDetailNew(
            @RequestParam String authId,
            @RequestParam String taskId
    ) {
        return execute(() -> {
            featureGate.assertProductEnabled();
            return taskQueryService.getTaskDetail(currentUser(), authId, taskId);
        });
    }

    private UserInfo currentUser() {
        return UserInfoContext.get();
    }

    private <T> Result<T> execute(ProductCall<T> call) {
        try {
            return Result.success(call.run());
        } catch (RuntimeException ex) {
            return Result.failed(ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface ProductCall<T> {
        T run();
    }
}
