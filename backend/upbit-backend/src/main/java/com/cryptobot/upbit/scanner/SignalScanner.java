package com.cryptobot.upbit.scanner;

import com.cryptobot.upbit.domain.candle.Candle;
import com.cryptobot.upbit.scanner.dto.SignalScanResult;
import com.cryptobot.upbit.strategy.*;
import com.cryptobot.upbit.upbit.UpbitApiClient;
import com.cryptobot.upbit.upbit.dto.UpbitMarketResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SignalScanner {

    private final UpbitApiClient upbitApiClient;
    private final Map<String, Strategy> strategies;

    @Autowired
    public SignalScanner(
            UpbitApiClient upbitApiClient,
            StrategyV1Donchian strategyV1,
            StrategyV2Pullback strategyV2,
            StrategyV3Reversal strategyV3
    ) {
        this.upbitApiClient = upbitApiClient;
        this.strategies = new HashMap<>();
        this.strategies.put("V1", strategyV1);
        this.strategies.put("V2", strategyV2);
        this.strategies.put("V3", strategyV3);
    }

    /**
     * 전체 마켓 스캔
     */
    public List<SignalScanResult> scanMarkets(String strategyCode, String timeframe) {
        log.info("Starting market scan with strategy: {}, timeframe: {}", strategyCode, timeframe);

        // 전략 가져오기
        Strategy strategy = strategies.get(strategyCode);
        if (strategy == null) {
            log.error("Unknown strategy code: {}", strategyCode);
            return new ArrayList<>();
        }

        // 전체 마켓 조회
        List<UpbitMarketResponse> markets = upbitApiClient.getMarketAll();
        log.info("Found {} KRW markets", markets.size());

        List<SignalScanResult> results = new ArrayList<>();

        // 전체 마켓 스캔
        int limit = markets.size();
        log.info("Scanning {} markets...", limit);

        for (int i = 0; i < limit; i++) {
            UpbitMarketResponse market = markets.get(i);
            try {
                SignalScanResult result = scanSingleMarket(market, strategy, timeframe);
                if (result != null && result.getSignal() != SignalType.NONE) {
                    results.add(result);
                    log.info("Signal found for {}: {}", market.getMarket(), result.getSignal());
                }

                // API rate limit 회피 (10 requests/sec)
                if ((i + 1) % 10 == 0 && i < limit - 1) {
                    Thread.sleep(1000);
                    log.debug("Rate limit pause after {} markets", i + 1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Scan interrupted");
                break;
            } catch (Exception e) {
                log.error("Error scanning market {}: {}", market.getMarket(), e.getMessage());
            }
        }

        log.info("Scan completed. Found {} signals", results.size());
        return results;
    }

    private SignalScanResult scanSingleMarket(UpbitMarketResponse market, Strategy strategy, String timeframe) {
        // 캔들 데이터 수집
        List<Candle> candles5m = upbitApiClient.getCandles(market.getMarket(), 5, 200);
        List<Candle> candles15m = upbitApiClient.getCandles(market.getMarket(), 15, 200);

        // 데이터 충분성 확인
        if (!hasEnoughData(candles5m, strategy.getCode())) {
            return null;
        }

        // 전략 평가
        SignalResult signal = strategy.evaluate(market.getMarket(), candles5m, candles15m);

        if (signal.getSignalType() == SignalType.NONE) {
            return null;
        }

        // 현재가 및 변동률
        Candle lastCandle = candles5m.get(candles5m.size() - 1);
        double currentPrice = lastCandle.getClose();

        // 전일 종가 (대략 200개 이전)
        double prevPrice = candles5m.size() > 100 ? candles5m.get(candles5m.size() - 100).getClose() : currentPrice;
        double changeRate = ((currentPrice - prevPrice) / prevPrice) * 100;

        return SignalScanResult.builder()
                .market(market.getMarket())
                .coinName(market.getKoreanName())
                .currentPrice(currentPrice)
                .changeRate(changeRate)
                .signal(signal.getSignalType())
                .reasonCodes(signal.getReasonCodes())
                .indicators(signal.getIndicatorSnapshot())
                .scanTime(LocalDateTime.now())
                .build();
    }

    private boolean hasEnoughData(List<Candle> candles, String strategyCode) {
        int required = switch (strategyCode) {
            case "V1" -> 78;  // MACD warm-up
            case "V2" -> 56;  // ADX warm-up
            case "V3" -> 78;  // MACD warm-up
            default -> 100;
        };
        return candles.size() >= required;
    }
}
