package com.wimoor.ozon.product.service;

import java.util.List;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftImportCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductMapSaveCommand;
import com.wimoor.ozon.product.pojo.entity.OzonProductMap;
import com.wimoor.ozon.product.pojo.vo.OzonProductMapView;

public interface IOzonProductMapService {

    List<OzonProductMapView> list(UserInfo user, String authId, String keyword);

    OzonProductMap saveMapping(UserInfo user, OzonProductMapSaveCommand command);

    int importDraft(UserInfo user, OzonProductDraftImportCommand command);
}
