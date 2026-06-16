package com.wimoor.ozon.product.service;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.product.pojo.dto.OzonProductPreviewCommand;
import com.wimoor.ozon.product.pojo.vo.OzonProductPreviewView;

public interface IOzonProductPreviewService {

    OzonProductPreviewView preview(UserInfo user, OzonProductPreviewCommand command);
}
