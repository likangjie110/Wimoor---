package com.wimoor.ozon.shipment.service;

import java.util.List;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.shipment.pojo.dto.OzonShipmentPushCommand;
import com.wimoor.ozon.shipment.pojo.entity.OzonShipment;
import com.wimoor.ozon.shipment.pojo.vo.OzonShipmentPushResult;

public interface IOzonShipmentService {

    OzonShipmentPushResult pushTracking(UserInfo user, OzonShipmentPushCommand command);

    List<OzonShipment> listByPosting(UserInfo user, String authId, String postingId);
}
