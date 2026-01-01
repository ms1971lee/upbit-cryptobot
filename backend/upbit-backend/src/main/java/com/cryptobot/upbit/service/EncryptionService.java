package com.cryptobot.upbit.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
@Service
public class EncryptionService {

    @Value("${encryption.aes-key}")
    private String aesKey;

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String AES = "AES";

    /**
     * 업비트 API 키 암호화
     */
    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }

        try {
            SecretKeySpec secretKeySpec = generateKey();
            IvParameterSpec ivParameterSpec = generateIv();

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * 업비트 API 키 복호화
     */
    public String decrypt(String encryptedText) {
        if (!StringUtils.hasText(encryptedText)) {
            return null;
        }

        try {
            SecretKeySpec secretKeySpec = generateKey();
            IvParameterSpec ivParameterSpec = generateIv();

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private SecretKeySpec generateKey() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = aesKey.getBytes(StandardCharsets.UTF_8);
        key = sha.digest(key);
        key = Arrays.copyOf(key, 32); // AES-256은 32바이트 키 사용
        return new SecretKeySpec(key, AES);
    }

    private IvParameterSpec generateIv() {
        // 고정 IV 사용 (실제 운영 환경에서는 랜덤 IV 사용 및 암호문과 함께 저장 권장)
        byte[] iv = Arrays.copyOf(aesKey.getBytes(StandardCharsets.UTF_8), 16);
        return new IvParameterSpec(iv);
    }
}
