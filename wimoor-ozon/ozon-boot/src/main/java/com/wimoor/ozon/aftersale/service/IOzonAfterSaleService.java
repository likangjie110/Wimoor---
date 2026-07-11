package com.wimoor.ozon.aftersale.service;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.aftersale.pojo.dto.OzonCancellationSaveCommand;
import com.wimoor.ozon.aftersale.pojo.dto.OzonPackageSaveCommand;
import com.wimoor.ozon.aftersale.pojo.dto.OzonPostingCancelCommand;
import com.wimoor.ozon.aftersale.pojo.dto.OzonReturnSaveCommand;
import com.wimoor.ozon.aftersale.pojo.entity.OzonCancellationRecord;
import com.wimoor.ozon.aftersale.pojo.entity.OzonPackageRecord;
import com.wimoor.ozon.aftersale.pojo.entity.OzonReturnRecord;
import com.wimoor.ozon.aftersale.pojo.vo.OzonAfterSaleDetailView;

public interface IOzonAfterSaleService {

    OzonAfterSaleDetailView getDetail(UserInfo user, String authId, String postingId);

    OzonPackageRecord savePackage(UserInfo user, OzonPackageSaveCommand command);

    OzonReturnRecord saveReturn(UserInfo user, OzonReturnSaveCommand command);

    OzonCancellationRecord saveCancellation(UserInfo user, OzonCancellationSaveCommand command);

    // API-integrated methods
    OzonCancellationRecord cancelPostingWithApi(UserInfo user, OzonPostingCancelCommand command);

    default OzonCancellationRecord cancelPostingWithApi(UserInfo user, String authId, String postingId, String reason) {
        return cancelPostingWithApi(user, new OzonPostingCancelCommand(authId, postingId, 352L, reason));
    }

    void syncPackagesFromApi(UserInfo user, String authId, String postingId);

    void syncReturnsFromApi(UserInfo user, String authId, String postingId);
}
