package com.wimoor.ozon.auth.service;

import java.util.List;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.pojo.dto.OzonAuthBindCommand;
import com.wimoor.ozon.auth.pojo.dto.OzonRotateKeyCommand;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.pojo.vo.OzonAuthView;
import com.wimoor.ozon.seller.pojo.vo.OzonWarehouseSyncResult;

public interface IOzonAuthService {

    OzonAuth bindAuth(UserInfo user, OzonAuthBindCommand command);

    List<OzonAuthView> listAuth(UserInfo user);

    OzonWarehouseSyncResult ping(UserInfo user, String authId);

    void disableAuth(UserInfo user, String authId);

    OzonAuth rotateKey(UserInfo user, OzonRotateKeyCommand command);
}
