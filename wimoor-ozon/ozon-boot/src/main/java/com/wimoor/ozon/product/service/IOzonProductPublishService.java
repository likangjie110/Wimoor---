package com.wimoor.ozon.product.service;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.product.pojo.dto.OzonProductPublishCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductPublishTaskQuery;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishTaskHistoryView;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishTaskView;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishView;

import java.util.List;

public interface IOzonProductPublishService {

    OzonProductPublishView publish(UserInfo user, OzonProductPublishCommand command);

    OzonProductPublishTaskView getTaskDetail(UserInfo user, OzonProductPublishTaskQuery query);

    List<OzonProductPublishTaskHistoryView> listTaskHistory(UserInfo user, String authId, String draftId);
}
