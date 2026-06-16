package com.wimoor.ozon.price.service;

import java.util.List;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.price.pojo.dto.OzonPricePushCommand;
import com.wimoor.ozon.price.pojo.entity.OzonPriceSnapshot;
import com.wimoor.ozon.price.pojo.vo.OzonPricePushResult;
import com.wimoor.ozon.price.pojo.vo.OzonPriceTaskView;

public interface IOzonPriceService {

    OzonPricePushResult push(UserInfo user, OzonPricePushCommand command);

    List<OzonPriceSnapshot> listSnapshots(UserInfo user, String authId);

    List<OzonPriceTaskView> listTasks(UserInfo user, String authId);
}
