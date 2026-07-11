package com.wimoor.ozon.posting.service;

import java.util.List;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.posting.pojo.dto.OzonPostingSyncCommand;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingDetailView;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingSyncResult;
import com.wimoor.ozon.posting.pojo.vo.OzonPostingView;

public interface IOzonPostingService {

    OzonPostingSyncResult syncIncremental(UserInfo user, OzonPostingSyncCommand command);

    OzonPostingSyncResult retryOne(UserInfo user, String authId, String postingId);

    List<OzonPostingView> list(UserInfo user, String authId, String status, String fulfillmentType, String keyword);

    OzonPostingDetailView getDetail(UserInfo user, String authId, String postingId);

    void assignDeliveryMethod(UserInfo user, String authId, String postingId, String deliveryMethodId);

    List<OzonPostingView> getPostingsByDeliveryMethod(UserInfo user, String authId, String deliveryMethodId);
}
