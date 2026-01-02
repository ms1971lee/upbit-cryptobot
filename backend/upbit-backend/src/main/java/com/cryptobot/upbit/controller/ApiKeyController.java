package com.cryptobot.upbit.controller;

import com.cryptobot.upbit.dto.apikey.ApiKeyDto;
import com.cryptobot.upbit.dto.apikey.CreateApiKeyRequest;
import com.cryptobot.upbit.dto.apikey.UpdateApiKeyRequest;
import com.cryptobot.upbit.entity.User;
import com.cryptobot.upbit.repository.UserRepository;
import com.cryptobot.upbit.service.ApiKeyService;
import com.cryptobot.upbit.service.UpbitApiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final UpbitApiService upbitApiService;
    private final UserRepository userRepository;

    /**
     * 모든 API 키 조회
     */
    @GetMapping
    public ResponseEntity<List<ApiKeyDto>> getAllApiKeys(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        log.info("Get all API keys request for user: {}", email);

        List<ApiKeyDto> apiKeys = apiKeyService.getAllApiKeys(email);

        return ResponseEntity.ok(apiKeys);
    }

    /**
     * 활성화된 API 키 조회
     */
    @GetMapping("/active")
    public ResponseEntity<ApiKeyDto> getActiveApiKey(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        log.info("Get active API key request for user: {}", email);

        ApiKeyDto apiKey = apiKeyService.getActiveApiKey(email);

        return ResponseEntity.ok(apiKey);
    }

    /**
     * API 키 생성
     */
    @PostMapping
    public ResponseEntity<ApiKeyDto> createApiKey(
            Authentication authentication,
            @Valid @RequestBody CreateApiKeyRequest request) {
        String email = (String) authentication.getPrincipal();
        log.info("Create API key request for user: {}, name: {}", email, request.getName());

        ApiKeyDto apiKey = apiKeyService.createApiKey(email, request);

        return ResponseEntity.ok(apiKey);
    }

    /**
     * API 키 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiKeyDto> updateApiKey(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateApiKeyRequest request) {
        String email = (String) authentication.getPrincipal();
        log.info("Update API key request for user: {}, id: {}", email, id);

        ApiKeyDto apiKey = apiKeyService.updateApiKey(email, id, request);

        return ResponseEntity.ok(apiKey);
    }

    /**
     * API 키 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApiKey(
            Authentication authentication,
            @PathVariable Long id) {
        String email = (String) authentication.getPrincipal();
        log.info("Delete API key request for user: {}, id: {}", email, id);

        apiKeyService.deleteApiKey(email, id);

        return ResponseEntity.noContent().build();
    }

    /**
     * API 키 활성화
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiKeyDto> activateApiKey(
            Authentication authentication,
            @PathVariable Long id) {
        String email = (String) authentication.getPrincipal();
        log.info("Activate API key request for user: {}, id: {}", email, id);

        ApiKeyDto apiKey = apiKeyService.activateApiKey(email, id);

        return ResponseEntity.ok(apiKey);
    }

    /**
     * API 키 테스트 (업비트 계좌 조회)
     */
    @PostMapping("/{id}/test")
    public Mono<ResponseEntity<Map<String, Object>>> testApiKey(
            Authentication authentication,
            @PathVariable Long id) {
        String email = (String) authentication.getPrincipal();
        log.info("Test API key request for user: {}, id: {}", email, id);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        return upbitApiService.testApiKeyById(id, user.getId())
                .map(ResponseEntity::ok);
    }
}
