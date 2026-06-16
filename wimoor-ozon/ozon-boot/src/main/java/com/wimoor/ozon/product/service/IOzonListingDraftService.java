package com.wimoor.ozon.product.service;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftDetailQuery;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftImportCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftListQuery;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftSaveCommand;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftDetailView;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftImportResult;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftListView;

import java.util.List;

public interface IOzonListingDraftService {

    OzonProductDraftDetailView saveDraft(UserInfo user, OzonProductDraftSaveCommand command);

    OzonProductDraftImportResult importDraft(UserInfo user, OzonProductDraftImportCommand command);

    List<OzonProductDraftListView> listDrafts(UserInfo user, OzonProductDraftListQuery query);

    OzonProductDraftDetailView getDraftDetail(UserInfo user, OzonProductDraftDetailQuery query);
}
