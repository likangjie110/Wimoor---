package com.wimoor.ozon.error.service;

import java.util.List;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.error.pojo.dto.OzonErrorQuery;
import com.wimoor.ozon.error.pojo.vo.OzonErrorView;

public interface IOzonErrorCenterService {

    List<OzonErrorView> list(UserInfo user, OzonErrorQuery query);

    OzonErrorView retryOne(UserInfo user, String errorId);

    OzonErrorView ignore(UserInfo user, String errorId);
}
