package com.wimoor.ozon.task.service;

import java.util.List;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.task.pojo.dto.OzonTaskQuery;
import com.wimoor.ozon.task.pojo.vo.OzonTaskView;

public interface IOzonTaskService {

    List<OzonTaskView> list(UserInfo user, OzonTaskQuery query);
}
