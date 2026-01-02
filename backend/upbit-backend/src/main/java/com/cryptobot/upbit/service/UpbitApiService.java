package com.cryptobot.upbit.service;

import com.cryptobot.upbit.config.UpbitApiProperties;
import com.cryptobot.upbit.entity.ApiKey;
import com.cryptobot.upbit.repository.ApiKeyRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpbitApiService {

    private final UpbitApiProperties upbitApiProperties;
    private final WebClient.Builder webClientBuilder;
    private final ApiKeyRepository apiKeyRepository;
    private final EncryptionService encryptionService;

    public Mono<Map<String, Object>> testConnection(String keyName) {
        try {
            UpbitApiProperties.ApiKey apiKey = upbitApiProperties.getKeys().get(keyName);

            if (apiKey == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "API key not found: " + keyName);
                error.put("availableKeys", upbitApiProperties.getKeys().keySet());
                return Mono.just(error);
            }

            String accessKey = apiKey.getAccessKey();
            String secretKey = apiKey.getSecretKey();

            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> claims = new HashMap<>();
            claims.put("access_key", accessKey);
            claims.put("nonce", UUID.randomUUID().toString());

            String jwtToken = Jwts.builder()
                    .claims(claims)
                    .signWith(key)
                    .compact();

            String authenticationToken = "Bearer " + jwtToken;

            WebClient webClient = webClientBuilder
                    .baseUrl(upbitApiProperties.getBaseUrl())
                    .build();

            return webClient.get()
                    .uri("/accounts")
                    .header("Authorization", authenticationToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("keyName", keyName);
                        result.put("response", response);
                        return result;
                    })
                    .onErrorResume(error -> {
                        log.error("API connection test failed for key: {}", keyName, error);
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("keyName", keyName);
                        errorResult.put("error", error.getMessage());
                        return Mono.just(errorResult);
                    });

        } catch (Exception e) {
            log.error("Error creating JWT token", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return Mono.just(errorResult);
        }
    }

    public Mono<Map<String, Object>> getMarketInfo() {
        WebClient webClient = webClientBuilder
                .baseUrl(upbitApiProperties.getBaseUrl())
                .build();

        return webClient.get()
                .uri("/market/all")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("markets", response);
                    return result;
                })
                .onErrorResume(error -> {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    errorResult.put("error", error.getMessage());
                    return Mono.just(errorResult);
                });
    }

    /**
     * 사용자의 활성화된 API 키로 계좌 조회 (인증 필요)
     */
    public Mono<Map<String, Object>> getAccounts(Long userId) {
        try {
            // 사용자의 활성화된 API 키 조회
            ApiKey apiKey = apiKeyRepository.findByUserIdAndIsActiveTrue(userId)
                    .orElseThrow(() -> new RuntimeException("활성화된 API 키를 찾을 수 없습니다"));

            // API 키 복호화
            String accessKey = encryptionService.decrypt(apiKey.getAccessKey());
            String secretKey = encryptionService.decrypt(apiKey.getSecretKey());

            // JWT 토큰 생성
            String authToken = generateUpbitJwtToken(accessKey, secretKey);

            // 업비트 API 호출
            WebClient webClient = webClientBuilder
                    .baseUrl(upbitApiProperties.getBaseUrl())
                    .build();

            return webClient.get()
                    .uri("/accounts")
                    .header("Authorization", authToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("apiKeyName", apiKey.getName());
                        result.put("accounts", response);
                        return result;
                    })
                    .onErrorResume(error -> {
                        log.error("Failed to get accounts for user: {}", userId, error);
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", error.getMessage());
                        return Mono.just(errorResult);
                    });

        } catch (Exception e) {
            log.error("Error getting accounts for user: {}", userId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return Mono.just(errorResult);
        }
    }

    /**
     * 특정 API 키로 계좌 조회 테스트 (인증 필요)
     */
    public Mono<Map<String, Object>> testApiKeyById(Long apiKeyId, Long userId) {
        try {
            // API 키 조회 (사용자 검증 포함)
            ApiKey apiKey = apiKeyRepository.findByIdAndUserId(apiKeyId, userId)
                    .orElseThrow(() -> new RuntimeException("API 키를 찾을 수 없습니다"));

            // API 키 복호화
            String accessKey = encryptionService.decrypt(apiKey.getAccessKey());
            String secretKey = encryptionService.decrypt(apiKey.getSecretKey());

            // JWT 토큰 생성
            String authToken = generateUpbitJwtToken(accessKey, secretKey);

            // 업비트 API 호출
            WebClient webClient = webClientBuilder
                    .baseUrl(upbitApiProperties.getBaseUrl())
                    .build();

            return webClient.get()
                    .uri("/accounts")
                    .header("Authorization", authToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("message", "API 키가 정상적으로 작동합니다");
                        result.put("apiKeyName", apiKey.getName());
                        result.put("accounts", response);
                        return result;
                    })
                    .onErrorResume(error -> {
                        log.error("API key test failed for id: {}", apiKeyId, error);
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("message", "API 키 테스트 실패");
                        errorResult.put("error", error.getMessage());
                        return Mono.just(errorResult);
                    });

        } catch (Exception e) {
            log.error("Error testing API key: {}", apiKeyId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "API 키 테스트 중 오류 발생");
            errorResult.put("error", e.getMessage());
            return Mono.just(errorResult);
        }
    }

    /**
     * 업비트 JWT 토큰 생성
     */
    private String generateUpbitJwtToken(String accessKey, String secretKey) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> claims = new HashMap<>();
        claims.put("access_key", accessKey);
        claims.put("nonce", UUID.randomUUID().toString());

        String jwtToken = Jwts.builder()
                .claims(claims)
                .signWith(key)
                .compact();

        return "Bearer " + jwtToken;
    }
}
