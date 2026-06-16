package com.wimoor.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class ChannelCredentialCipherTests {

    @Test
    void encryptAndDecryptRoundTrip() {
        ChannelCredentialCipher cipher = new ChannelCredentialCipher("0123456789abcdef");
        String plaintext = "a89f7d00-xxxx";

        String ciphertext = cipher.encrypt(plaintext);

        assertNotEquals(plaintext, ciphertext);
        assertEquals(plaintext, cipher.decrypt(ciphertext));
    }
}
