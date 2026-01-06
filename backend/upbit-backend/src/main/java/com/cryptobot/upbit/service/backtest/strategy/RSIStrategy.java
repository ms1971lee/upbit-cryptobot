package com.cryptobot.upbit.service.backtest.strategy;

import com.cryptobot.upbit.model.backtest.BacktestContext;
import com.cryptobot.upbit.model.backtest.Candle;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * RSI 전략 (Relative Strength Index Strategy)
 *
 * RSI가 oversoldLevel 이하일 때 매수 신호 (과매도)
 * RSI가 overboughtLevel 이상일 때 매도 신호 (과매수)
 */
@Slf4j
public class RSIStrategy implements Strategy {

    private final int period;           // RSI 계산 기간
    private final double oversoldLevel;  // 과매도 기준 (일반적으로 30)
    private final double overboughtLevel; // 과매수 기준 (일반적으로 70)

    private List<Double> rsi;

    /**
     * 기본 생성자 (14일 RSI, 30/70 기준)
     */
    public RSIStrategy() {
        this(14, 30.0, 70.0);
    }

    /**
     * 커스텀 설정 생성자
     *
     * @param period RSI 계산 기간
     * @param oversoldLevel 과매도 기준
     * @param overboughtLevel 과매수 기준
     */
    public RSIStrategy(int period, double oversoldLevel, double overboughtLevel) {
        if (oversoldLevel >= overboughtLevel) {
            throw new IllegalArgumentException("과매도 기준은 과매수 기준보다 작아야 합니다");
        }
        this.period = period;
        this.oversoldLevel = oversoldLevel;
        this.overboughtLevel = overboughtLevel;
    }

    @Override
    public void initialize(BacktestContext context, List<Candle> candles) {
        // RSI 계산
        this.rsi = TechnicalIndicators.calculateRSI(candles, period);
        log.debug("RSI Strategy initialized: period={}, oversold={}, overbought={}",
                period, oversoldLevel, overboughtLevel);
    }

    @Override
    public Signal generateSignal(int currentIndex, List<Candle> candles, BacktestContext context) {
        // RSI가 계산되지 않은 초기 기간
        if (currentIndex <= period) {
            return Signal.HOLD;
        }

        Double currentRSI = rsi.get(currentIndex);
        if (currentRSI == null) {
            return Signal.HOLD;
        }

        // 과매도 상태에서 매수 (보유하고 있지 않을 때만)
        if (currentRSI < oversoldLevel && context.getCoinBalance() == 0) {
            log.debug("RSI oversold at index {}: RSI={}", currentIndex, currentRSI);
            return Signal.BUY;
        }

        // 과매수 상태에서 매도 (보유하고 있을 때만)
        if (currentRSI > overboughtLevel && context.getCoinBalance() > 0) {
            log.debug("RSI overbought at index {}: RSI={}", currentIndex, currentRSI);
            return Signal.SELL;
        }

        return Signal.HOLD;
    }

    @Override
    public String getName() {
        return String.format("RSI (%d, %.0f/%.0f)", period, oversoldLevel, overboughtLevel);
    }
}
