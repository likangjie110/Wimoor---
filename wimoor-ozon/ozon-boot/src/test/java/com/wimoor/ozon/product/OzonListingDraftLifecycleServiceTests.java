package com.wimoor.ozon.product;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.product.mapper.*;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftArchiveCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftCloneCommand;
import com.wimoor.ozon.product.pojo.entity.*;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftDetailView;
import com.wimoor.ozon.product.service.impl.OzonListingDraftLifecycleService;

/**
 * OZON 草稿生命周期服务测试
 *
 * @author Development Team
 * @since 2026-06-25
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OzonListingDraftLifecycleServiceTests {

    private static final String AUTH_ID = "auth-1";
    private static final String SHOP_ID = "shop-1";
    private static final String USER_ID = "user-1";
    private static final String DRAFT_ID = "draft-1";

    @Mock
    private OzonAuthAccessService authAccessService;

    @Mock
    private OzonListingDraftMapper draftMapper;

    @Mock
    private OzonListingVariantMapper variantMapper;

    @Mock
    private OzonListingAttributeMapper attributeMapper;

    @Mock
    private OzonListingImageMapper imageMapper;

    private OzonListingDraftLifecycleService service;
    private UserInfo testUser;
    private OzonAuth testAuth;

    @BeforeEach
    void setUp() {
        service = new OzonListingDraftLifecycleService(
            authAccessService, draftMapper, variantMapper, attributeMapper, imageMapper
        );

        testUser = createTestUser();
        testAuth = createTestAuth();

        when(authAccessService.requireOwnedAuth(any(UserInfo.class), eq(AUTH_ID)))
            .thenReturn(testAuth);
    }

    // ==================== 克隆草稿测试 ====================

    @Test
    void cloneDraft_CreatesNewDraftWithAllRelatedData() {
        // Arrange
        OzonListingDraft sourceDraft = createSourceDraft();
        List<OzonListingAttribute> sourceAttributes = createSourceAttributes();
        List<OzonListingImage> sourceImages = createSourceImages();
        List<OzonListingVariant> sourceVariants = createSourceVariants();

        when(draftMapper.selectById(DRAFT_ID)).thenReturn(sourceDraft);
        when(attributeMapper.selectList(any(QueryWrapper.class))).thenReturn(sourceAttributes);
        when(imageMapper.selectList(any(QueryWrapper.class))).thenReturn(sourceImages);
        when(variantMapper.selectList(any(QueryWrapper.class))).thenReturn(sourceVariants);

        OzonProductDraftCloneCommand command = new OzonProductDraftCloneCommand();
        command.setAuthId(AUTH_ID);
        command.setSourceDraftId(DRAFT_ID);
        command.setNewDraftName("Cloned Draft");

        // Act
        OzonProductDraftDetailView result = service.cloneDraft(testUser, command);

        // Assert
        assertNotNull(result);
        assertEquals("Cloned Draft", result.getDraftName());
        assertEquals("DRAFT", result.getStatus());

        // 验证插入调用
        verify(draftMapper, times(1)).insert(any(OzonListingDraft.class));
        verify(attributeMapper, times(sourceAttributes.size())).insert(any(OzonListingAttribute.class));
        verify(imageMapper, times(sourceImages.size())).insert(any(OzonListingImage.class));
        verify(variantMapper, times(sourceVariants.size())).insert(any(OzonListingVariant.class));
    }

    @Test
    void cloneDraft_ThrowsExceptionWhenSourceDraftNotFound() {
        // Arrange
        when(draftMapper.selectById(DRAFT_ID)).thenReturn(null);

        OzonProductDraftCloneCommand command = new OzonProductDraftCloneCommand();
        command.setAuthId(AUTH_ID);
        command.setSourceDraftId(DRAFT_ID);
        command.setNewDraftName("Cloned Draft");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.cloneDraft(testUser, command)
        );

        assertTrue(exception.getMessage().contains("源草稿不存在或无权限"));
    }

    @Test
    void cloneDraft_ThrowsExceptionWhenAuthMismatch() {
        // Arrange
        OzonListingDraft sourceDraft = createSourceDraft();
        sourceDraft.setAuthId("wrong-auth-id");

        when(draftMapper.selectById(DRAFT_ID)).thenReturn(sourceDraft);

        OzonProductDraftCloneCommand command = new OzonProductDraftCloneCommand();
        command.setAuthId(AUTH_ID);
        command.setSourceDraftId(DRAFT_ID);
        command.setNewDraftName("Cloned Draft");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.cloneDraft(testUser, command)
        );

        assertTrue(exception.getMessage().contains("源草稿不存在或无权限"));
    }

    @Test
    void cloneDraft_AppendsCloneSuffixToSkus() {
        // Arrange
        OzonListingDraft sourceDraft = createSourceDraft();
        sourceDraft.setMaterialSku("ORIGINAL-SKU");

        List<OzonListingVariant> sourceVariants = Arrays.asList(
            createVariant("variant-1", "VARIANT-SKU-1"),
            createVariant("variant-2", "VARIANT-SKU-2")
        );

        when(draftMapper.selectById(DRAFT_ID)).thenReturn(sourceDraft);
        when(attributeMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
        when(imageMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
        when(variantMapper.selectList(any(QueryWrapper.class))).thenReturn(sourceVariants);

        OzonProductDraftCloneCommand command = new OzonProductDraftCloneCommand();
        command.setAuthId(AUTH_ID);
        command.setSourceDraftId(DRAFT_ID);
        command.setNewDraftName("Cloned Draft");

        // Act
        OzonProductDraftDetailView result = service.cloneDraft(testUser, command);

        // Assert
        assertEquals("Cloned Draft", result.getDraftName());

        // 验证变体 SKU 添加了后缀
        verify(variantMapper, times(2)).insert(argThat(variant ->
            variant.getVariantSku().endsWith("-CLONE")
        ));
    }

    // ==================== 归档草稿测试 ====================

    @Test
    void archiveDraft_UpdatesStatusToArchived() {
        // Arrange
        OzonListingDraft draft = createSourceDraft();
        draft.setStatus("DRAFT");

        when(draftMapper.selectById(DRAFT_ID)).thenReturn(draft);

        OzonProductDraftArchiveCommand command = new OzonProductDraftArchiveCommand();
        command.setAuthId(AUTH_ID);
        command.setDraftId(DRAFT_ID);
        command.setArchiveReason("Not needed anymore");

        // Act
        service.archiveDraft(testUser, command);

        // Assert
        verify(draftMapper, times(1)).updateById(argThat(d ->
            "ARCHIVED".equals(d.getStatus())
        ));
    }

    @Test
    void archiveDraft_ThrowsExceptionWhenDraftNotFound() {
        // Arrange
        when(draftMapper.selectById(DRAFT_ID)).thenReturn(null);

        OzonProductDraftArchiveCommand command = new OzonProductDraftArchiveCommand();
        command.setAuthId(AUTH_ID);
        command.setDraftId(DRAFT_ID);
        command.setArchiveReason("Test");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.archiveDraft(testUser, command)
        );

        assertTrue(exception.getMessage().contains("草稿不存在或无权限"));
    }

    @Test
    void archiveDraft_ThrowsExceptionWhenAuthMismatch() {
        // Arrange
        OzonListingDraft draft = createSourceDraft();
        draft.setAuthId("wrong-auth-id");

        when(draftMapper.selectById(DRAFT_ID)).thenReturn(draft);

        OzonProductDraftArchiveCommand command = new OzonProductDraftArchiveCommand();
        command.setAuthId(AUTH_ID);
        command.setDraftId(DRAFT_ID);
        command.setArchiveReason("Test");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.archiveDraft(testUser, command)
        );

        assertTrue(exception.getMessage().contains("草稿不存在或无权限"));
    }

    // ==================== 删除草稿测试 ====================

    @Test
    void deleteDraft_RemovesDraftAndAllRelatedData() {
        // Arrange
        OzonListingDraft draft = createSourceDraft();

        when(draftMapper.selectById(DRAFT_ID)).thenReturn(draft);

        // Act
        service.deleteDraft(testUser, AUTH_ID, DRAFT_ID);

        // Assert
        verify(attributeMapper, times(1)).delete(any(QueryWrapper.class));
        verify(imageMapper, times(1)).delete(any(QueryWrapper.class));
        verify(variantMapper, times(1)).delete(any(QueryWrapper.class));
        verify(draftMapper, times(1)).deleteById(DRAFT_ID);
    }

    @Test
    void deleteDraft_ThrowsExceptionWhenDraftNotFound() {
        // Arrange
        when(draftMapper.selectById(DRAFT_ID)).thenReturn(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.deleteDraft(testUser, AUTH_ID, DRAFT_ID)
        );

        assertTrue(exception.getMessage().contains("草稿不存在或无权限"));
    }

    @Test
    void deleteDraft_ThrowsExceptionWhenAuthMismatch() {
        // Arrange
        OzonListingDraft draft = createSourceDraft();
        draft.setAuthId("wrong-auth-id");

        when(draftMapper.selectById(DRAFT_ID)).thenReturn(draft);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.deleteDraft(testUser, AUTH_ID, DRAFT_ID)
        );

        assertTrue(exception.getMessage().contains("草稿不存在或无权限"));
    }

    // ==================== 按状态查询测试 ====================

    @Test
    void listByStatus_ReturnsAllDraftsWhenStatusNull() {
        // Arrange
        List<OzonListingDraft> drafts = Arrays.asList(
            createSourceDraft(),
            createSourceDraft()
        );

        when(draftMapper.selectList(any(QueryWrapper.class))).thenReturn(drafts);

        // Act
        List<OzonListingDraft> result = service.listByStatus(testUser, AUTH_ID, null);

        // Assert
        assertEquals(2, result.size());
        verify(draftMapper, times(1)).selectList(any(QueryWrapper.class));
    }

    @Test
    void listByStatus_FiltersOnlyDraftStatus() {
        // Arrange
        List<OzonListingDraft> drafts = Collections.singletonList(createSourceDraft());

        when(draftMapper.selectList(any(QueryWrapper.class))).thenReturn(drafts);

        // Act
        List<OzonListingDraft> result = service.listByStatus(testUser, AUTH_ID, "DRAFT");

        // Assert
        assertEquals(1, result.size());
    }

    @Test
    void listByStatus_FiltersOnlyArchivedStatus() {
        // Arrange
        OzonListingDraft archivedDraft = createSourceDraft();
        archivedDraft.setStatus("ARCHIVED");

        when(draftMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Collections.singletonList(archivedDraft));

        // Act
        List<OzonListingDraft> result = service.listByStatus(testUser, AUTH_ID, "ARCHIVED");

        // Assert
        assertEquals(1, result.size());
        assertEquals("ARCHIVED", result.get(0).getStatus());
    }

    // ==================== 辅助方法 ====================

    private UserInfo createTestUser() {
        UserInfo user = new UserInfo();
        user.setId(USER_ID);
        user.setCompanyid(SHOP_ID);
        return user;
    }

    private OzonAuth createTestAuth() {
        OzonAuth auth = new OzonAuth();
        auth.setId(AUTH_ID);
        auth.setShopId(SHOP_ID);
        return auth;
    }

    private OzonListingDraft createSourceDraft() {
        OzonListingDraft draft = new OzonListingDraft();
        draft.setId(DRAFT_ID);
        draft.setAuthId(AUTH_ID);
        draft.setShopId(SHOP_ID);
        draft.setMaterialSku("SOURCE-SKU");
        draft.setName("Source Draft");
        draft.setStatus("DRAFT");
        draft.setCategoryId(17028994L);
        draft.setDescription("Test description");
        draft.setCreateTime(new Date());
        draft.setUpdateTime(new Date());
        return draft;
    }

    private List<OzonListingAttribute> createSourceAttributes() {
        OzonListingAttribute attr1 = new OzonListingAttribute();
        attr1.setId("attr-1");
        attr1.setDraftId(DRAFT_ID);
        attr1.setAttributeId(1001L);
        attr1.setAttributeName("Color");
        attr1.setAttributeValue("Red");
        attr1.setScope("COMMON");

        OzonListingAttribute attr2 = new OzonListingAttribute();
        attr2.setId("attr-2");
        attr2.setDraftId(DRAFT_ID);
        attr2.setAttributeId(1002L);
        attr2.setAttributeName("Size");
        attr2.setAttributeValue("M");
        attr2.setScope("VARIANT");

        return Arrays.asList(attr1, attr2);
    }

    private List<OzonListingImage> createSourceImages() {
        OzonListingImage img1 = new OzonListingImage();
        img1.setId("img-1");
        img1.setDraftId(DRAFT_ID);
        img1.setImageUrl("https://example.com/img1.png");
        img1.setImageType("MAIN");
        img1.setSortOrder(0);

        OzonListingImage img2 = new OzonListingImage();
        img2.setId("img-2");
        img2.setDraftId(DRAFT_ID);
        img2.setImageUrl("https://example.com/img2.png");
        img2.setImageType("ADDITIONAL");
        img2.setSortOrder(1);

        return Arrays.asList(img1, img2);
    }

    private List<OzonListingVariant> createSourceVariants() {
        return Arrays.asList(
            createVariant("variant-1", "VARIANT-SKU-1"),
            createVariant("variant-2", "VARIANT-SKU-2")
        );
    }

    private OzonListingVariant createVariant(String id, String sku) {
        OzonListingVariant variant = new OzonListingVariant();
        variant.setId(id);
        variant.setDraftId(DRAFT_ID);
        variant.setVariantSku(sku);
        variant.setPrice(java.math.BigDecimal.valueOf(100.0));
        variant.setOldPrice(java.math.BigDecimal.valueOf(120.0));
        variant.setVat("0.2");
        variant.setCreateTime(new Date());
        return variant;
    }
}
