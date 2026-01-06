package com.cryptobot.upbit.service.backtest.strategy;

import com.cryptobot.upbit.model.backtest.BacktestContext;
import com.cryptobot.upbit.model.backtest.Candle;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 전략 팩토리
 * 전략 이름과 파라미터로 전략 인스턴스를 생성합니다.
 */
@Slf4j
@Component
public class StrategyFactory {

    private final ObjectMapper objectMapper;

    public StrategyFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 전략 생성
     *
     * @param strategyName 전략 이름
     * @param paramsJson 전략 파라미터 (JSON 문자열)
     * @return 전략 인스턴스
     */
    public Strategy createStrategy(String strategyName, String paramsJson) {
        try {
            Map<String, Object> params = null;
            if (paramsJson != null && !paramsJson.isEmpty()) {
                params = objectMapper.readValue(paramsJson, Map.class);
            }

            return createStrategy(strategyName, params);

        } catch (Exception e) {
            log.error("Failed to parse strategy params: {}", paramsJson, e);
            throw new RuntimeException("전략 파라미터 파싱 실패: " + e.getMessage());
        }
    }

    /**
     * 전략 생성
     *
     * @param strategyName 전략 이름
     * @param params 전략 파라미터
     * @return 전략 인스턴스
     */
    public Strategy createStrategy(String strategyName, Map<String, Object> params) {
        if (strategyName == null || strategyName.isEmpty()) {
            strategyName = "BUY_AND_HOLD";
        }

        switch (strategyName.toUpperCase()) {
            case "BUY_AND_HOLD":
                return new BuyAndHoldStrategy();

            case "MA_CROSS":
                return createMACrossStrategy(params);

            case "RSI":
                return createRSIStrategy(params);

            case "BOLLINGER_BANDS":
                return createBollingerBandsStrategy(params);

            default:
                throw new IllegalArgumentException("Unknown strategy: " + strategyName);
        }
    }

    /**
     * MA Cross 전략 생성
     */
    private Strategy createMACrossStrategy(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return new MACrossStrategy();
        }

        Integer shortPeriod = getIntParam(params, "shortPeriod", 5);
        Integer longPeriod = getIntParam(params, "longPeriod", 20);

        return new MACrossStrategy(shortPeriod, longPeriod);
    }

    /**
     * RSI 전략 생성
     */
    private Strategy createRSIStrategy(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return new RSIStrategy();
        }

        Integer period = getIntParam(params, "period", 14);
        Double oversoldLevel = getDoubleParam(params, "oversoldLevel", 30.0);
        Double overboughtLevel = getDoubleParam(params, "overboughtLevel", 70.0);

        return new RSIStrategy(period, oversoldLevel, overboughtLevel);
    }

    /**
     * Bollinger Bands 전략 생성
     */
    private Strategy createBollingerBandsStrategy(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return new BollingerBandsStrategy();
        }

        Integer period = getIntParam(params, "period", 20);
        Double stdDevMultiplier = getDoubleParam(params, "stdDevMultiplier", 2.0);

        return new BollingerBandsStrategy(period, stdDevMultiplier);
    }

    /**
     * 파라미터에서 정수 값 추출
     */
    private Integer getIntParam(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    /**
     * 파라미터에서 실수 값 추출
     */
    private Double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    /**
     * Buy and Hold 전략 (내부 클래스)
     */
    private static class BuyAndHoldStrategy implements Strategy {
        private boolean bought = false;

        @Override
        public void initialize(BacktestContext context, List<Candle> candles) {
            // No initialization needed
        }

        @Override
        public Signal generateSignal(int currentIndex, List<Candle> candles, BacktestContext context) {
            // 첫 캔들에서 매수
            if (currentIndex == 0 && !bought) {
                bought = true;
                return Signal.BUY;
            }

            // 마지막 캔들에서 매도
            if (currentIndex == candles.size() - 1 && bought) {
                return Signal.SELL;
            }

            return Signal.HOLD;
        }

        @Override
        public String getName() {
            return "Buy and Hold";
        }
    }
}
