package com.wimoor.ozon.chat;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.chat.mapper.OzonChatMessageMapper;
import com.wimoor.ozon.chat.mapper.OzonChatReplyAuditMapper;
import com.wimoor.ozon.chat.mapper.OzonChatSessionMapper;
import com.wimoor.ozon.chat.service.impl.OzonChatServiceImpl;
import com.wimoor.ozon.security.OzonCredentialService;

class OzonChatServiceSpringBeanTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(OzonAuthAccessService.class, () -> mock(OzonAuthAccessService.class))
            .withBean(OzonChatSessionMapper.class, () -> mock(OzonChatSessionMapper.class))
            .withBean(OzonChatMessageMapper.class, () -> mock(OzonChatMessageMapper.class))
            .withBean(OzonChatReplyAuditMapper.class, () -> mock(OzonChatReplyAuditMapper.class))
            .withBean(OzonSellerApiClient.class, () -> mock(OzonSellerApiClient.class))
            .withBean(OzonCredentialService.class, () -> mock(OzonCredentialService.class))
            .withBean(OzonChatServiceImpl.class);

    @Test
    void springCanConstructChatServiceBean() {
        contextRunner.run(context -> {
            OzonChatServiceImpl bean = context.getBean(OzonChatServiceImpl.class);
            assertNotNull(bean);
        });
    }
}
