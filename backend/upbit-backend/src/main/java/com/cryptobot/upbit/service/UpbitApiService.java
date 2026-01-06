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
                        log.info("Upbit API response for API key {}: {}", apiKeyId, response);
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("message", "API 키가 정상적으로 작동합니다");
                        result.put("apiKeyName", apiKey.getName());
                        result.put("accounts", response);
                        return result;
                    })
                    .onErrorResume(error -> {
                        log.error("API key test failed for id: {}, error type: {}, message: {}",
                                  apiKeyId, error.getClass().getName(), error.getMessage());
                        if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                            org.springframework.web.reactive.function.client.WebClientResponseException webError =
                                (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                            log.error("HTTP Status: {}, Response Body: {}", webError.getStatusCode(), webError.getResponseBodyAsString());
                        }
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("message", "API 키 테스트 실패");
                        errorResult.put("error", error.getMessage());
                        errorResult.put("errorType", error.getClass().getSimpleName());
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
     * 계좌 요약 정보 조회 (총매수금액, 보유자산, 총평가금액, 총평가수익률)
     */
    public Mono<Map<String, Object>> getAccountSummary(Long userId) {
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
                    .flatMap(accountsJson -> {
                        try {
                            // JSON 파싱
                            com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                                new com.fasterxml.jackson.databind.ObjectMapper();
                            java.util.List<java.util.Map<String, Object>> accounts =
                                objectMapper.readValue(accountsJson,
                                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>() {});

                            // 보유 종목 정보 수집 및 현재가 조회
                            return calculateAccountSummary(accounts, webClient);

                        } catch (Exception e) {
                            log.error("Failed to parse accounts JSON", e);
                            Map<String, Object> errorResult = new HashMap<>();
                            errorResult.put("success", false);
                            errorResult.put("error", "계좌 정보 파싱 실패: " + e.getMessage());
                            return Mono.just(errorResult);
                        }
                    })
                    .onErrorResume(error -> {
                        log.error("Failed to get account summary for user: {}", userId, error);
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", error.getMessage());
                        return Mono.just(errorResult);
                    });

        } catch (Exception e) {
            log.error("Error getting account summary for user: {}", userId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return Mono.just(errorResult);
        }
    }

    /**
     * 계좌 정보로부터 요약 정보 계산
     */
    private Mono<Map<String, Object>> calculateAccountSummary(
            java.util.List<java.util.Map<String, Object>> accounts,
            WebClient webClient) {

        java.util.List<Mono<Map<String, Object>>> coinMonos = new java.util.ArrayList<>();
        double totalBuyAmount = 0.0;
        double totalKRW = 0.0;

        for (java.util.Map<String, Object> account : accounts) {
            String currency = (String) account.get("currency");
            String balance = (String) account.get("balance");
            String avgBuyPrice = (String) account.get("avg_buy_price");

            double balanceAmount = Double.parseDouble(balance);
            double avgBuyPriceAmount = Double.parseDouble(avgBuyPrice);

            if ("KRW".equals(currency)) {
                totalKRW = balanceAmount;
                continue;
            }

            if (balanceAmount > 0) {
                String market = "KRW-" + currency;
                double buyAmount = balanceAmount * avgBuyPriceAmount;
                totalBuyAmount += buyAmount;

                // 현재가 조회를 위한 Mono 생성
                Mono<Map<String, Object>> coinMono = webClient.get()
                        .uri("/ticker?markets=" + market)
                        .retrieve()
                        .bodyToMono(String.class)
                        .map(tickerJson -> {
                            try {
                                com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                                    new com.fasterxml.jackson.databind.ObjectMapper();
                                java.util.List<java.util.Map<String, Object>> tickers =
                                    objectMapper.readValue(tickerJson,
                                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>() {});

                                if (!tickers.isEmpty()) {
                                    java.util.Map<String, Object> ticker = tickers.get(0);
                                    double currentPrice = ((Number) ticker.get("trade_price")).doubleValue();
                                    double evalAmount = balanceAmount * currentPrice;
                                    double profitAmount = evalAmount - buyAmount;
                                    double profitRate = (profitAmount / buyAmount) * 100;

                                    java.util.Map<String, Object> coinData = new java.util.HashMap<>();
                                    coinData.put("currency", currency);
                                    coinData.put("market", market);
                                    coinData.put("balance", balanceAmount);
                                    coinData.put("avgBuyPrice", avgBuyPriceAmount);
                                    coinData.put("currentPrice", currentPrice);
                                    coinData.put("buyAmount", buyAmount);
                                    coinData.put("evalAmount", evalAmount);
                                    coinData.put("profitAmount", profitAmount);
                                    coinData.put("profitRate", profitRate);
                                    coinData.put("koreanName", ticker.get("korean_name"));

                                    return coinData;
                                }
                            } catch (Exception e) {
                                log.error("Failed to parse ticker for {}", market, e);
                            }
                            return new java.util.HashMap<String, Object>();
                        })
                        .onErrorResume(error -> {
                            log.error("Failed to get ticker for {}", market, error);
                            return Mono.just(new java.util.HashMap<>());
                        });

                coinMonos.add(coinMono);
            }
        }

        final double finalTotalBuyAmount = totalBuyAmount;
        final double finalTotalKRW = totalKRW;

        // 모든 코인의 현재가 조회를 병렬로 실행
        return reactor.core.publisher.Flux.merge(coinMonos)
                .collectList()
                .map(coinDataList -> {
                    // 빈 맵 제거
                    java.util.List<java.util.Map<String, Object>> validCoins = coinDataList.stream()
                            .filter(coin -> !coin.isEmpty())
                            .collect(java.util.stream.Collectors.toList());

                    // 총 평가금액 계산
                    double totalEvalAmount = validCoins.stream()
                            .mapToDouble(coin -> (Double) coin.get("evalAmount"))
                            .sum();

                    // 총 보유자산 = KRW + 총 평가금액
                    double totalAssets = finalTotalKRW + totalEvalAmount;

                    // 총 평가수익
                    double totalProfit = totalEvalAmount - finalTotalBuyAmount;

                    // 총 평가수익률
                    double totalProfitRate = finalTotalBuyAmount > 0
                            ? (totalProfit / finalTotalBuyAmount) * 100
                            : 0.0;

                    java.util.Map<String, Object> summary = new java.util.HashMap<>();
                    summary.put("success", true);
                    summary.put("totalBuyAmount", finalTotalBuyAmount);
                    summary.put("totalKRW", finalTotalKRW);
                    summary.put("totalEvalAmount", totalEvalAmount);
                    summary.put("totalAssets", totalAssets);
                    summary.put("totalProfit", totalProfit);
                    summary.put("totalProfitRate", totalProfitRate);
                    summary.put("holdings", validCoins);

                    return summary;
                });
    }

    /**
     * 특정 마켓의 현재가 조회 (공개 API, 인증 불필요)
     */
    public Mono<Double> getCurrentPrice(String market) {
        WebClient webClient = webClientBuilder
                .baseUrl(upbitApiProperties.getBaseUrl())
                .build();

        return webClient.get()
                .uri("/ticker?markets=" + market)
                .retrieve()
                .bodyToMono(String.class)
                .map(tickerJson -> {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                                new com.fasterxml.jackson.databind.ObjectMapper();
                        java.util.List<java.util.Map<String, Object>> tickers =
                                objectMapper.readValue(tickerJson,
                                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>() {});

                        if (!tickers.isEmpty()) {
                            java.util.Map<String, Object> ticker = tickers.get(0);
                            return ((Number) ticker.get("trade_price")).doubleValue();
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse ticker for {}", market, e);
                    }
                    return 0.0;
                })
                .onErrorResume(error -> {
                    log.error("Failed to get current price for {}", market, error);
                    return Mono.just(0.0);
                });
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
