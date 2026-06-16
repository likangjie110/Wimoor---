package com.wimoor.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ChannelCredentialCipher {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final String KEY_PROPERTY = "channel.credential.aes-key";
    private static final String KEY_ENV = "CHANNEL_CREDENTIAL_AES_KEY";

    private final byte[] secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public ChannelCredentialCipher() {
        this(resolveKeyFromEnvironment());
    }

    public ChannelCredentialCipher(String secretKey) {
        byte[] rawKey = Objects.requireNonNull(secretKey, "secretKey must not be null")
                .getBytes(StandardCharsets.UTF_8);
        if (!isValidKeyLength(rawKey.length)) {
            throw new IllegalArgumentException("AES key must be 16, 24, or 32 bytes");
        }
        this.secretKey = Arrays.copyOf(rawKey, rawKey.length);
    }

    public String encrypt(String plaintext) {
        Objects.requireNonNull(plaintext, "plaintext must not be null");
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        byte[] encrypted = doCipher(Cipher.ENCRYPT_MODE, iv, plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] payload = new byte[IV_LENGTH_BYTES + encrypted.length];
        System.arraycopy(iv, 0, payload, 0, IV_LENGTH_BYTES);
        System.arraycopy(encrypted, 0, payload, IV_LENGTH_BYTES, encrypted.length);
        return Base64.getEncoder().encodeToString(payload);
    }

    public String decrypt(String ciphertext) {
        Objects.requireNonNull(ciphertext, "ciphertext must not be null");
        byte[] payload = Base64.getDecoder().decode(ciphertext);
        if (payload.length <= IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("ciphertext payload is invalid");
        }
        byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH_BYTES);
        byte[] encrypted = Arrays.copyOfRange(payload, IV_LENGTH_BYTES, payload.length);
        return new String(doCipher(Cipher.DECRYPT_MODE, iv, encrypted), StandardCharsets.UTF_8);
    }

    public String fingerprint(String value) {
        Objects.requireNonNull(value, "value must not be null");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to fingerprint credential", ex);
        }
    }

    private byte[] doCipher(int mode, byte[] iv, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(mode, new SecretKeySpec(secretKey, KEY_ALGORITHM), spec);
            return cipher.doFinal(input);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to process channel credential", ex);
        }
    }

    private static String resolveKeyFromEnvironment() {
        String secretKey = System.getProperty(KEY_PROPERTY);
        if (secretKey == null || secretKey.trim().isEmpty()) {
            secretKey = System.getenv(KEY_ENV);
        }
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException("Missing AES key from property " + KEY_PROPERTY + " or env " + KEY_ENV);
        }
        return secretKey.trim();
    }

    private static boolean isValidKeyLength(int length) {
        return length == 16 || length == 24 || length == 32;
    }
}
