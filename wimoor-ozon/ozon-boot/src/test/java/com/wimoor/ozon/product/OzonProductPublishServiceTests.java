package com.wimoor.ozon.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.security.ChannelCredentialCipher;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.error.pojo.dto.OzonErrorRecordCommand;
import com.wimoor.ozon.error.service.OzonErrorRecorder;
import com.wimoor.ozon.product.client.OzonProductApiClient;
import com.wimoor.ozon.product.mapper.OzonListingDraftMapper;
import com.wimoor.ozon.product.mapper.OzonListingPublishTaskMapper;
import com.wimoor.ozon.product.mapper.OzonListingVariantMapper;
import com.wimoor.ozon.product.mapper.OzonProductMapMapper;
import com.wimoor.ozon.product.pojo.dto.OzonProductPublishCommand;
import com.wimoor.ozon.product.pojo.entity.OzonListingDraft;
import com.wimoor.ozon.product.pojo.entity.OzonListingPublishTask;
import com.wimoor.ozon.product.pojo.entity.OzonListingVariant;
import com.wimoor.ozon.product.pojo.entity.OzonProductMap;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishTaskHistoryView;
import com.wimoor.ozon.product.pojo.vo.OzonProductPreviewView;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishView;
import com.wimoor.ozon.product.service.IOzonProductPreviewService;
import com.wimoor.ozon.product.service.impl.OzonProductPublishServiceImpl;
import com.wimoor.ozon.security.OzonCredentialService;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;

@ExtendWith(MockitoExtension.class)
class OzonProductPublishServiceTests {

    private static final String AES_KEY = "0123456789abcdef";

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonListingDraftMapper draftMapper;

    @Mock
    private OzonListingVariantMapper variantMapper;

    @Mock
    private OzonProductMapMapper productMapMapper;

    @Mock
    private OzonListingPublishTaskMapper publishTaskMapper;

    @Mock
    private OzonSyncJobMapper syncJobMapper;

    @Mock
    private OzonProductApiClient productApiClient;

    @Mock
    private OzonErrorRecorder errorRecorder;

    @Mock
    private IOzonProductPreviewService previewService;

    @Captor
    private ArgumentCaptor<OzonProductMap> productMapCaptor;

    @Captor
    private ArgumentCaptor<OzonListingVariant> variantCaptor;

    @Captor
    private ArgumentCaptor<OzonListingPublishTask> taskCaptor;

    @Captor
    private ArgumentCaptor<OzonSyncJob> syncJobCaptor;

    private OzonProductPublishServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonProductPublishServiceImpl(
                authMapper,
                draftMapper,
                variantMapper,
                productMapMapper,
                publishTaskMapper,
                syncJobMapper,
                productApiClient,
                new OzonCredentialService(AES_KEY),
                errorRecorder,
                previewService
        );
        when(authMapper.selectById("auth-1")).thenReturn(Fixtures.auth());
        when(draftMapper.selectByAuthIdAndDraftId("auth-1", "draft-1")).thenReturn(Fixtures.draft());
        when(variantMapper.listByDraftId("draft-1")).thenReturn(Arrays.asList(Fixtures.variantOne(), Fixtures.variantTwo()));
        when(productMapMapper.listByMaterialSkus("auth-1", Arrays.asList("ERP-SKU-1", "ERP-SKU-2")))
                .thenReturn(Arrays.asList(Fixtures.mapOne(), Fixtures.mapTwo()));
    }

    @Test
    void publishMarksTaskPartialAndWritesBackSuccessfulVariantsOnly() {
        when(previewService.preview(any(), any()))
                .thenReturn(Fixtures.previewReady());
        when(productApiClient.submitProductImport(eq("cid"), eq("key"), any()))
                .thenReturn("4036602384");
        when(productApiClient.getProductImportInfo("cid", "key", "4036602384"))
                .thenReturn(Fixtures.partialRemoteResult());
        when(publishTaskMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        OzonProductPublishView result = service.publish(buildUser(), new OzonProductPublishCommand("auth-1", "draft-1"));

        assertEquals("PARTIAL", result.getTaskStatus());
        assertEquals("4036602384", result.getRemoteTaskId());
        assertNotNull(result.getLocalTaskId());
        verify(productMapMapper).updateById(productMapCaptor.capture());
        assertEquals("ERP-SKU-1", productMapCaptor.getValue().getMaterialSku());
        assertEquals("BOOK-001", productMapCaptor.getValue().getOzonOfferId());
        assertEquals("3911142260", productMapCaptor.getValue().getOzonProductId());
        verify(variantMapper, org.mockito.Mockito.atLeastOnce()).updateById(variantCaptor.capture());
        assertTrue(variantCaptor.getAllValues().stream().anyMatch(item -> "PUBLISHED".equals(item.getStatus())));
        assertTrue(variantCaptor.getAllValues().stream().anyMatch(item -> "FAILED".equals(item.getStatus())));
        verify(publishTaskMapper).insert(taskCaptor.capture());
        verify(syncJobMapper).insert(syncJobCaptor.capture());
        assertEquals("PRODUCT_PUBLISH", syncJobCaptor.getValue().getJobType());
        verify(errorRecorder).recordOpen(argThat(command ->
                "PRODUCT".equals(command.getSourceType()) && "draft-1".equals(command.getObjectId())));
    }

    @Test
    void listTaskHistoryReturnsRecentTasksForDraft() {
        OzonListingPublishTask task = new OzonListingPublishTask();
        task.setId("task-1");
        task.setDraftId("draft-1");
        task.setAuthId("auth-1");
        task.setTaskStatus("PARTIAL");
        task.setRemoteTaskId("remote-1");
        task.setErrorMessage("1 variant has remote validation errors");
        task.setOperator("tester");
        task.setCreateTime(new Date(1000L));
        task.setUpdateTime(new Date(2000L));
        when(publishTaskMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.singletonList(task));

        java.util.List<OzonProductPublishTaskHistoryView> result = service.listTaskHistory(buildUser(), "auth-1", "draft-1");

        assertEquals(1, result.size());
        assertEquals("task-1", result.get(0).getTaskId());
        assertEquals("PARTIAL", result.get(0).getTaskStatus());
        assertEquals("remote-1", result.get(0).getRemoteTaskId());
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");
        return user;
    }

    private static final class Fixtures {

        private Fixtures() {
        }

        private static OzonAuth auth() {
            OzonAuth auth = new OzonAuth();
            auth.setId("auth-1");
            auth.setShopId("company-1");
            auth.setClientId("cid");
            auth.setApiKeyCiphertext(new ChannelCredentialCipher(AES_KEY).encrypt("key"));
            return auth;
        }

        private static OzonListingDraft draft() {
            OzonListingDraft draft = new OzonListingDraft();
            draft.setId("draft-1");
            draft.setAuthId("auth-1");
            draft.setShopId("company-1");
            draft.setDraftName("Books draft");
            draft.setDescriptionCategoryId(200001483L);
            draft.setDescriptionCategoryName("Books");
            draft.setTypeId(971445087L);
            draft.setTypeName("Printed Book");
            draft.setTitleOverrideValue("Book title");
            draft.setStatus("READY");
            draft.setCreateTime(new Date());
            draft.setUpdateTime(new Date());
            return draft;
        }

        private static OzonListingVariant variantOne() {
            OzonListingVariant variant = new OzonListingVariant();
            variant.setId("variant-1");
            variant.setDraftId("draft-1");
            variant.setAuthId("auth-1");
            variant.setShopId("company-1");
            variant.setMaterialSku("ERP-SKU-1");
            variant.setMaterialName("ERP Book 1");
            variant.setOfferIdOverride("BOOK-001");
            variant.setStatus("READY");
            return variant;
        }

        private static OzonListingVariant variantTwo() {
            OzonListingVariant variant = new OzonListingVariant();
            variant.setId("variant-2");
            variant.setDraftId("draft-1");
            variant.setAuthId("auth-1");
            variant.setShopId("company-1");
            variant.setMaterialSku("ERP-SKU-2");
            variant.setMaterialName("ERP Book 2");
            variant.setOfferIdOverride("BOOK-002");
            variant.setStatus("READY");
            return variant;
        }

        private static OzonProductMap mapOne() {
            OzonProductMap map = new OzonProductMap();
            map.setId("map-1");
            map.setAuthId("auth-1");
            map.setShopId("company-1");
            map.setMaterialSku("ERP-SKU-1");
            map.setOzonOfferId("BOOK-001");
            return map;
        }

        private static OzonProductMap mapTwo() {
            OzonProductMap map = new OzonProductMap();
            map.setId("map-2");
            map.setAuthId("auth-1");
            map.setShopId("company-1");
            map.setMaterialSku("ERP-SKU-2");
            map.setOzonOfferId("BOOK-002");
            return map;
        }

        private static OzonProductPreviewView previewReady() {
            OzonProductPreviewView view = new OzonProductPreviewView();
            view.setCanPublish(true);
            view.setValidationErrors(Collections.emptyList());
            view.setVariantIssues(Collections.emptyList());
            OzonProductPreviewView.EffectivePayloadSummary summary = new OzonProductPreviewView.EffectivePayloadSummary();
            summary.setDraftId("draft-1");
            summary.setVariants(Arrays.asList(
                    variantSummary("variant-1", "ERP-SKU-1", "BOOK-001"),
                    variantSummary("variant-2", "ERP-SKU-2", "BOOK-002")
            ));
            view.setEffectivePayloadSummary(summary);
            return view;
        }

        private static OzonProductPreviewView.EffectiveVariantSummary variantSummary(String variantId, String materialSku, String offerId) {
            OzonProductPreviewView.EffectiveVariantSummary item = new OzonProductPreviewView.EffectiveVariantSummary();
            item.setVariantId(variantId);
            item.setMaterialSku(materialSku);
            item.setEffectiveOfferId(offerId);
            item.setEffectivePrice("99.00");
            item.setEffectiveWeight("0.60");
            OzonProductPreviewView.EffectiveDimensionSummary dimension = new OzonProductPreviewView.EffectiveDimensionSummary();
            dimension.setDepth("22.00");
            dimension.setWidth("15.00");
            dimension.setHeight("3.00");
            item.setEffectiveDimensions(dimension);
            item.setEffectiveImageCount(1);
            return item;
        }

        private static OzonProductApiClient.ProductImportInfo partialRemoteResult() {
            return new OzonProductApiClient.ProductImportInfo(Arrays.asList(
                    new OzonProductApiClient.ProductImportItem("BOOK-001", "3911142260", "imported", Collections.emptyList()),
                    new OzonProductApiClient.ProductImportItem(
                            "BOOK-002",
                            null,
                            "failed",
                            Collections.singletonList(new OzonProductApiClient.ProductImportError(
                                    "missing_dimension",
                                    "weight",
                                    0L,
                                    "",
                                    "weight is required"
                            ))
                    )
            ));
        }
    }
}
