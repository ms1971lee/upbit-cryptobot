package com.cryptobot.upbit.controller;

import com.cryptobot.upbit.entity.ApiKey;
import com.cryptobot.upbit.entity.User;
import com.cryptobot.upbit.repository.ApiKeyRepository;
import com.cryptobot.upbit.repository.UserRepository;
import com.cryptobot.upbit.service.EncryptionService;
import com.cryptobot.upbit.service.UpbitApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TestController {

    private final UpbitApiService upbitApiService;
    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final EncryptionService encryptionService;

    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Upbit Cryptobot Backend is running!");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "OK");
        return response;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "upbit-cryptobot");
        return response;
    }

    @GetMapping("/upbit/test/{keyName}")
    public Mono<Map<String, Object>> testUpbitApi(@PathVariable String keyName) {
        return upbitApiService.testConnection(keyName);
    }

    /**
     * 데이터베이스 API 키 디버그 조회 (암호화된 상태로 확인)
     * WARNING: This is a debug endpoint and should be removed in production!
     */
    @GetMapping("/test/debug-apikeys")
    public ResponseEntity<Map<String, Object>> debugApiKeys() {
        log.info("=== Debug API Keys Request (NO AUTH) ===");

        // Get all users and their API keys for debugging
        List<User> allUsers = userRepository.findAll();
        List<Map<String, Object>> allDebugData = new ArrayList<>();

        for (User user : allUsers) {
            List<ApiKey> apiKeys = apiKeyRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

            for (ApiKey apiKey : apiKeys) {
                Map<String, Object> keyData = new HashMap<>();
                keyData.put("userId", user.getId());
                keyData.put("userEmail", user.getEmail());
                keyData.put("id", apiKey.getId());
                keyData.put("name", apiKey.getName());
                keyData.put("isActive", apiKey.getIsActive());
                keyData.put("accessKeyLength", apiKey.getAccessKey() != null ? apiKey.getAccessKey().length() : 0);
                keyData.put("secretKeyLength", apiKey.getSecretKey() != null ? apiKey.getSecretKey().length() : 0);
                keyData.put("createdAt", apiKey.getCreatedAt());
                keyData.put("updatedAt", apiKey.getUpdatedAt());

                // 복호화해서 일부만 보여주기
                try {
                    String decryptedAccess = encryptionService.decrypt(apiKey.getAccessKey());
                    String decryptedSecret = encryptionService.decrypt(apiKey.getSecretKey());
                    keyData.put("accessKeyPreview", decryptedAccess.substring(0, Math.min(8, decryptedAccess.length())) + "...");
                    keyData.put("secretKeyPreview", decryptedSecret.substring(0, Math.min(8, decryptedSecret.length())) + "...");
                } catch (Exception e) {
                    keyData.put("decryptionError", e.getMessage());
                }

                allDebugData.add(keyData);
                log.info("API Key Debug - User: {}, ID: {}, Name: '{}', IsActive: {}", user.getEmail(), apiKey.getId(), apiKey.getName(), apiKey.getIsActive());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalUsers", allUsers.size());
        response.put("totalApiKeys", allDebugData.size());
        response.put("apiKeys", allDebugData);

        return ResponseEntity.ok(response);
    }

    /**
     * API 키 이름 강제 수정 (인코딩 문제 해결용)
     * WARNING: This is a debug endpoint and should be removed in production!
     */
    @GetMapping("/test/fix-apikey-name")
    public ResponseEntity<Map<String, Object>> fixApiKeyName(
            @org.springframework.web.bind.annotation.RequestParam Long id,
            @org.springframework.web.bind.annotation.RequestParam String newName) {
        log.info("=== Fix API Key Name Request ===");
        log.info("ID: {}, New Name: '{}'", id, newName);

        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("API Key not found with ID: " + id));

        String oldName = apiKey.getName();
        apiKey.setName(newName);
        ApiKey saved = apiKeyRepository.save(apiKey);

        log.info("Updated API Key - ID: {}, Old Name: '{}', New Name: '{}'", id, oldName, saved.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("id", saved.getId());
        response.put("oldName", oldName);
        response.put("newName", saved.getName());
        response.put("message", "API key name updated successfully");

        return ResponseEntity.ok(response);
    }
}
