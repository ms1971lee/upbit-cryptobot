package com.cryptobot.upbit.service;

import com.cryptobot.upbit.config.UpbitApiProperties;
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
}
