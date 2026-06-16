package com.wimoor.ozon.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.wimoor.common.security.ChannelCredentialCipher;

import cn.hutool.core.util.StrUtil;

@Component
public class OzonCredentialService {

    private final String configuredKey;

    public OzonCredentialService(@Value("${ozon.security.aes-key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    public String encrypt(String plaintext) {
        return createCipher().encrypt(plaintext);
    }

    public String decrypt(String ciphertext) {
        return createCipher().decrypt(ciphertext);
    }

    public String fingerprint(String value) {
        return createCipher().fingerprint(value);
    }

    private ChannelCredentialCipher createCipher() {
        if (StrUtil.isNotBlank(configuredKey)) {
            return new ChannelCredentialCipher(configuredKey.trim());
        }
        return new ChannelCredentialCipher();
    }
}
