package com.cryptobot.upbit.service.backtest.strategy;

import com.cryptobot.upbit.model.backtest.BacktestContext;
import com.cryptobot.upbit.model.backtest.Candle;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 볼린저 밴드 전략 (Bollinger Bands Strategy)
 *
 * 가격이 하단밴드 아래로 내려가면 매수 신호 (과매도)
 * 가격이 상단밴드 위로 올라가면 매도 신호 (과매수)
 */
@Slf4j
public class BollingerBandsStrategy implements Strategy {

    private final int period;            // 이동평균 기간
    private final double stdDevMultiplier; // 표준편차 배수

    private TechnicalIndicators.BollingerBands bands;

    /**
     * 기본 생성자 (20일, 2 표준편차)
     */
    public BollingerBandsStrategy() {
        this(20, 2.0);
    }

    /**
     * 커스텀 설정 생성자
     *
     * @param period 이동평균 기간
     * @param stdDevMultiplier 표준편차 배수
     */
    public BollingerBandsStrategy(int period, double stdDevMultiplier) {
        this.period = period;
        this.stdDevMultiplier = stdDevMultiplier;
    }

    @Override
    public void initialize(BacktestContext context, List<Candle> candles) {
        // 볼린저 밴드 계산
        this.bands = TechnicalIndicators.calculateBollingerBands(candles, period, stdDevMultiplier);
        log.debug("Bollinger Bands Strategy initialized: period={}, stdDev={}",
                period, stdDevMultiplier);
    }

    @Override
    public Signal generateSignal(int currentIndex, List<Candle> candles, BacktestContext context) {
        // 볼린저 밴드가 계산되지 않은 초기 기간
        if (currentIndex < period - 1) {
            return Signal.HOLD;
        }

        Double currentPrice = candles.get(currentIndex).getClosingPrice();
        Double upperBand = bands.getUpperBand(currentIndex);
        Double lowerBand = bands.getLowerBand(currentIndex);

        if (upperBand == null || lowerBand == null) {
            return Signal.HOLD;
        }

        // 가격이 하단밴드 아래로 내려가면 매수 (과매도)
        if (currentPrice < lowerBand && context.getCoinBalance() == 0) {
            log.debug("Price below lower band at index {}: price={}, lower={}",
                    currentIndex, currentPrice, lowerBand);
            return Signal.BUY;
        }

        // 가격이 상단밴드 위로 올라가면 매도 (과매수)
        if (currentPrice > upperBand && context.getCoinBalance() > 0) {
            log.debug("Price above upper band at index {}: price={}, upper={}",
                    currentIndex, currentPrice, upperBand);
            return Signal.SELL;
        }

        return Signal.HOLD;
    }

    @Override
    public String getName() {
        return String.format("Bollinger Bands (%d, %.1f)", period, stdDevMultiplier);
    }
}
