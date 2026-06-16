package com.wimoor.ozon.stock.service;

import java.util.List;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.stock.pojo.dto.OzonStockPushCommand;
import com.wimoor.ozon.stock.pojo.entity.OzonStockSnapshot;
import com.wimoor.ozon.stock.pojo.vo.OzonStockPushResult;
import com.wimoor.ozon.stock.pojo.vo.OzonStockTaskView;

public interface IOzonStockService {

    OzonStockPushResult push(UserInfo user, OzonStockPushCommand command);

    List<OzonStockSnapshot> listSnapshots(UserInfo user, String authId);

    List<OzonStockTaskView> listTasks(UserInfo user, String authId);
}
