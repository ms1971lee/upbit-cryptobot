package com.cryptobot.upbit.strategy;

import com.cryptobot.upbit.domain.candle.Candle;
import com.cryptobot.upbit.indicator.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * V2: Holy Grail Pullback 전략
 * - 15분봉 ADX 필터
 * - 5분봉 EMA 눌림목 전략
 */
@Slf4j
@Component
public class StrategyV2Pullback implements Strategy {

    private static final int EMA_LENGTH = 20;
    private static final int ADX_LENGTH = 14;
    private static final double ADX_THRESHOLD = 25.0;
    private static final double PULLBACK_TOLERANCE_PCT = 0.2; // 0.2%
    private static final int EXPIRE_BARS = 10;

    // Symbol별 상태 저장
    private final Map<String, PullbackState> stateMap = new ConcurrentHashMap<>();

    static class PullbackState {
        boolean pullbackActive = false;
        double pullbackHigh = 0.0;
        int pullbackStartIndex = 0;
    }

    @Override
    public String getCode() {
        return "V2";
    }

    @Override
    public String getName() {
        return "Holy Grail Pullback";
    }

    @Override
    public SignalResult evaluate(String symbol, List<Candle> candles5m, List<Candle> candles15m) {
        try {
            if (candles5m.size() < 100 || candles15m.size() < 50) {
                return SignalResult.none();
            }

            PullbackState state = stateMap.computeIfAbsent(symbol, k -> new PullbackState());

            int lastIdx5m = candles5m.size() - 1;
            int lastIdx15m = candles15m.size() - 1;

            // Step A: 15분봉 ADX 필터
            List<Double> adx15 = AdxCalculator.calculate(candles15m, ADX_LENGTH);
            if (adx15.isEmpty()) {
                return SignalResult.none();
            }

            double currentAdx15 = adx15.get(adx15.size() - 1);

            // ADX가 25 이상이어야 함
            if (currentAdx15 < ADX_THRESHOLD) {
                return SignalResult.none();
            }

            // 5분봉 EMA 계산
            List<Double> closes5m = candles5m.stream()
                    .map(Candle::getClose)
                    .collect(Collectors.toList());
            List<Double> ema5 = EmaCalculator.calculate(closes5m, EMA_LENGTH);
            if (ema5.isEmpty()) {
                return SignalResult.none();
            }

            double currentClose5m = candles5m.get(lastIdx5m).getClose();
            double currentEma5 = ema5.get(ema5.size() - 1);

            // Step B: 눌림 감지
            double distancePct = Math.abs(currentClose5m - currentEma5) / currentEma5 * 100;

            if (distancePct <= PULLBACK_TOLERANCE_PCT && !state.pullbackActive) {
                // 눌림 시작
                state.pullbackActive = true;
                state.pullbackHigh = candles5m.get(lastIdx5m).getHigh();
                state.pullbackStartIndex = lastIdx5m;

                log.debug("Pullback started for {}: distancePct={}", symbol, distancePct);
            }

            // Step C: 트리거 (진입)
            if (state.pullbackActive && currentClose5m > state.pullbackHigh) {
                SignalResult signal = SignalResult.buy();
                signal.setReasonCodes(new ArrayList<>());
                signal.getReasonCodes().add("ADX_STRONG");
                signal.getReasonCodes().add("PULLBACK_NEAR_EMA");
                signal.getReasonCodes().add("BREAK_PULLBACK_HIGH");

                signal.setEntryPrice(currentClose5m);
                signal.setCandleTime(candles5m.get(lastIdx5m).getCandleDateTimeKst());

                Map<String, Object> snapshot = new HashMap<>();
                snapshot.put("adx15", currentAdx15);
                snapshot.put("ema5", currentEma5);
                snapshot.put("close5", currentClose5m);
                snapshot.put("distancePct", distancePct);
                snapshot.put("pullbackHigh", state.pullbackHigh);
                snapshot.put("pullbackActive", state.pullbackActive);
                signal.setIndicatorSnapshot(snapshot);

                // 상태 초기화
                state.pullbackActive = false;

                return signal;
            }

            // Step D: 만료
            if (state.pullbackActive && (lastIdx5m - state.pullbackStartIndex) > EXPIRE_BARS) {
                state.pullbackActive = false;
                log.debug("Pullback expired for {}", symbol);
            }

            // 매도 신호: Close < EMA
            if (currentClose5m < currentEma5) {
                SignalResult signal = SignalResult.sell();
                signal.setReasonCodes(new ArrayList<>());
                signal.getReasonCodes().add("CLOSE_BELOW_EMA");
                signal.setCandleTime(candles5m.get(lastIdx5m).getCandleDateTimeKst());

                Map<String, Object> snapshot = new HashMap<>();
                snapshot.put("close5", currentClose5m);
                snapshot.put("ema5", currentEma5);
                signal.setIndicatorSnapshot(snapshot);

                return signal;
            }

            return SignalResult.none();

        } catch (Exception e) {
            log.error("Error in StrategyV2Pullback: {}", e.getMessage(), e);
            return SignalResult.none();
        }
    }
}
