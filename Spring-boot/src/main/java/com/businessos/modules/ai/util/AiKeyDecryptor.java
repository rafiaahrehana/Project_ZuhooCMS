package com.businessos.modules.ai.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts/decrypts API keys stored in ai_provider_configs using AES-256-GCM.
 * The key comes from ai.key-encryption.secret (AI_KEY_ENCRYPTION_SECRET env var in
 * production) - a base64-encoded 256-bit key. Stored value layout: base64(IV || ciphertext),
 * where the GCM ciphertext already carries its own 16-byte authentication tag.
 */
@Component
public class AiKeyDecryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AiKeyDecryptor(@Value("${ai.key-encryption.secret}") String base64Secret) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("ai.key-encryption.secret must be a valid base64 string", e);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                "ai.key-encryption.secret must decode to a 256-bit (32-byte) key, got " + keyBytes.length + " bytes");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainKey) {
        if (plainKey == null || plainKey.isBlank())
            throw new IllegalArgumentException("Cannot encrypt a null or blank API key");
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plainKey.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt API key", e);
        }
    }

    public String decrypt(String encryptedKey) {
        if (encryptedKey == null || encryptedKey.isBlank())
            throw new IllegalArgumentException("Cannot decrypt a null or blank API key");
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedKey);
            if (combined.length <= GCM_IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Encrypted API key is malformed");
            }
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES);
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt API key", e);
        }
    }
}
