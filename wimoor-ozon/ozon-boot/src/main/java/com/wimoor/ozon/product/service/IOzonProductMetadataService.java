package com.wimoor.ozon.product.service;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.product.pojo.vo.OzonProductCategoryTemplateView;
import com.wimoor.ozon.product.pojo.vo.OzonProductCategoryTreeView;

public interface IOzonProductMetadataService {

    default OzonProductCategoryTreeView getCategoryTree(UserInfo user, String authId, String keyword) {
        return getCategoryTree(user, authId, keyword, null);
    }

    OzonProductCategoryTreeView getCategoryTree(UserInfo user, String authId, String keyword, String language);

    default OzonProductCategoryTemplateView getTemplate(UserInfo user, String authId, Long descriptionCategoryId, Long typeId) {
        return getTemplate(user, authId, descriptionCategoryId, typeId, null);
    }

    OzonProductCategoryTemplateView getTemplate(UserInfo user, String authId, Long descriptionCategoryId, Long typeId, String language);
}
