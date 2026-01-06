package com.cryptobot.upbit.service.backtest.strategy;

import com.cryptobot.upbit.model.backtest.BacktestContext;
import com.cryptobot.upbit.model.backtest.Candle;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 이동평균 교차 전략 (MA Cross Strategy)
 *
 * 단기 이동평균이 장기 이동평균을 상향 돌파하면 매수 신호
 * 단기 이동평균이 장기 이동평균을 하향 돌파하면 매도 신호
 */
@Slf4j
public class MACrossStrategy implements Strategy {

    private final int shortPeriod;   // 단기 MA 기간
    private final int longPeriod;    // 장기 MA 기간

    private List<Double> shortMA;
    private List<Double> longMA;

    /**
     * 기본 생성자 (5일, 20일 MA)
     */
    public MACrossStrategy() {
        this(5, 20);
    }

    /**
     * 커스텀 기간 생성자
     *
     * @param shortPeriod 단기 MA 기간
     * @param longPeriod 장기 MA 기간
     */
    public MACrossStrategy(int shortPeriod, int longPeriod) {
        if (shortPeriod >= longPeriod) {
            throw new IllegalArgumentException("단기 기간은 장기 기간보다 작아야 합니다");
        }
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
    }

    @Override
    public void initialize(BacktestContext context, List<Candle> candles) {
        // 이동평균 계산
        this.shortMA = TechnicalIndicators.calculateSMA(candles, shortPeriod);
        this.longMA = TechnicalIndicators.calculateSMA(candles, longPeriod);

        log.debug("MA Cross Strategy initialized: short={}, long={}", shortPeriod, longPeriod);
    }

    @Override
    public Signal generateSignal(int currentIndex, List<Candle> candles, BacktestContext context) {
        // 장기 MA가 계산되지 않은 초기 기간
        if (currentIndex < longPeriod) {
            return Signal.HOLD;
        }

        // 이전 시점이 없으면 HOLD
        if (currentIndex == 0) {
            return Signal.HOLD;
        }

        Double currentShortMA = shortMA.get(currentIndex);
        Double currentLongMA = longMA.get(currentIndex);
        Double prevShortMA = shortMA.get(currentIndex - 1);
        Double prevLongMA = longMA.get(currentIndex - 1);

        if (currentShortMA == null || currentLongMA == null ||
            prevShortMA == null || prevLongMA == null) {
            return Signal.HOLD;
        }

        // Golden Cross: 단기 MA가 장기 MA를 상향 돌파
        boolean goldenCross = prevShortMA <= prevLongMA && currentShortMA > currentLongMA;

        // Dead Cross: 단기 MA가 장기 MA를 하향 돌파
        boolean deadCross = prevShortMA >= prevLongMA && currentShortMA < currentLongMA;

        if (goldenCross && context.getCoinBalance() == 0) {
            log.debug("Golden Cross detected at index {}: shortMA={}, longMA={}",
                    currentIndex, currentShortMA, currentLongMA);
            return Signal.BUY;
        } else if (deadCross && context.getCoinBalance() > 0) {
            log.debug("Dead Cross detected at index {}: shortMA={}, longMA={}",
                    currentIndex, currentShortMA, currentLongMA);
            return Signal.SELL;
        }

        return Signal.HOLD;
    }

    @Override
    public String getName() {
        return String.format("MA Cross (%d/%d)", shortPeriod, longPeriod);
    }
}
