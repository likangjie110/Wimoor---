package com.wimoor.ozon.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.pojo.dto.OzonTaskQuery;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJob;
import com.wimoor.ozon.task.pojo.entity.OzonSyncJobType;
import com.wimoor.ozon.task.pojo.vo.OzonTaskView;
import com.wimoor.ozon.task.service.impl.OzonTaskServiceImpl;

@ExtendWith(MockitoExtension.class)
class OzonTaskServiceTests {

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonSyncJobMapper syncJobMapper;

    private OzonTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonTaskServiceImpl(new OzonAuthAccessService(authMapper), syncJobMapper);
    }

    @Test
    void listFiltersJobsByAuthTypeAndStatus() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");

        OzonSyncJob matched = new OzonSyncJob();
        matched.setId("job-1");
        matched.setAuthId("auth-1");
        matched.setShopId("company-1");
        matched.setJobType(OzonSyncJobType.POSTING_SYNC.name());
        matched.setStatus("PENDING");
        matched.setCreateTime(new Date(0L));
        matched.setUpdateTime(new Date(1000L));

        OzonSyncJob filteredOut = new OzonSyncJob();
        filteredOut.setId("job-2");
        filteredOut.setAuthId("auth-1");
        filteredOut.setShopId("company-1");
        filteredOut.setJobType(OzonSyncJobType.STOCK_SYNC.name());
        filteredOut.setStatus("DONE");

        when(authMapper.selectById("auth-1")).thenReturn(auth);
        when(syncJobMapper.selectList(any())).thenReturn(Arrays.asList(matched, filteredOut));

        List<OzonTaskView> views = service.list(buildUser(), new OzonTaskQuery("auth-1", "POSTING_SYNC", "PENDING"));

        assertEquals(1, views.size());
        assertEquals("job-1", views.get(0).getId());
        assertEquals("POSTING_SYNC", views.get(0).getJobType());
        assertEquals("PENDING", views.get(0).getStatus());
        assertFalse(views.get(0).getUpdatedAt().before(views.get(0).getCreatedAt()));
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");
        return user;
    }
}
