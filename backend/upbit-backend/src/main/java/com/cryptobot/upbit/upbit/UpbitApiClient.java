package com.cryptobot.upbit.upbit;

import com.cryptobot.upbit.domain.candle.Candle;
import com.cryptobot.upbit.upbit.dto.UpbitCandleResponse;
import com.cryptobot.upbit.upbit.dto.UpbitMarketResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UpbitApiClient {

    private final WebClient webClient;
    private static final String BASE_URL = "https://api.upbit.com/v1";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public UpbitApiClient() {
        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    /**
     * 전체 마켓 코드 조회
     */
    public List<UpbitMarketResponse> getMarketAll() {
        try {
            List<UpbitMarketResponse> markets = webClient.get()
                    .uri("/market/all?isDetails=false")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<UpbitMarketResponse>>() {})
                    .block();

            if (markets == null) {
                return Collections.emptyList();
            }

            // KRW 마켓만 필터링
            return markets.stream()
                    .filter(m -> m.getMarket().startsWith("KRW-"))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to fetch market list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 분봉 캔들 조회
     * @param market 마켓 코드 (예: KRW-BTC)
     * @param minutes 분 단위 (1, 3, 5, 15, 10, 30, 60, 240)
     * @param count 캔들 개수 (최대 200)
     */
    public List<Candle> getCandles(String market, int minutes, int count) {
        try {
            Thread.sleep(100); // Rate limiting: 초당 10회

            List<UpbitCandleResponse> responses = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/candles/minutes/" + minutes)
                            .queryParam("market", market)
                            .queryParam("count", Math.min(count, 200))
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<UpbitCandleResponse>>() {})
                    .block();

            if (responses == null || responses.isEmpty()) {
                return Collections.emptyList();
            }

            // Candle 객체로 변환 (최신 데이터가 앞에 오므로 역순으로 정렬)
            List<Candle> candles = responses.stream()
                    .map(this::convertToCandle)
                    .collect(Collectors.toList());

            Collections.reverse(candles); // 오래된 데이터가 앞에 오도록

            return candles;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while fetching candles: {}", e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch candles for {}: {}", market, e.getMessage());
            return Collections.emptyList();
        }
    }

    private Candle convertToCandle(UpbitCandleResponse response) {
        return Candle.builder()
                .market(response.getMarket())
                .candleDateTimeKst(LocalDateTime.parse(response.getCandleDateTimeKst(), DATETIME_FORMATTER))
                .openingPrice(response.getOpeningPrice())
                .highPrice(response.getHighPrice())
                .lowPrice(response.getLowPrice())
                .tradePrice(response.getTradePrice())
                .candleAccTradeVolume(response.getCandleAccTradeVolume())
                .build();
    }
}
