package com.wimoor.ozon.seller.service;

import java.util.List;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.seller.pojo.dto.OzonDeliveryMethodSaveCommand;
import com.wimoor.ozon.seller.pojo.entity.OzonDeliveryMethod;
import com.wimoor.ozon.seller.pojo.vo.OzonWarehouseView;

public interface IOzonSellerSettingsService {

    List<OzonWarehouseView> listWarehouses(UserInfo user, String authId);

    List<OzonDeliveryMethod> listDeliveryMethods(UserInfo user, String authId);

    OzonDeliveryMethod saveDeliveryMethod(UserInfo user, OzonDeliveryMethodSaveCommand command);

    OzonDeliveryMethod setDefaultDeliveryMethod(UserInfo user, String authId, String methodId);

    void deleteDeliveryMethod(UserInfo user, String authId, String methodId);
}
