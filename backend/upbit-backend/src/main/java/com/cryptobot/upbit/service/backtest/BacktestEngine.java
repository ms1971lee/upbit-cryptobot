package com.cryptobot.upbit.service.backtest;

import com.cryptobot.upbit.entity.BacktestConfig;
import com.cryptobot.upbit.entity.BacktestResult;
import com.cryptobot.upbit.entity.MarketCandle;
import com.cryptobot.upbit.model.backtest.BacktestContext;
import com.cryptobot.upbit.model.backtest.Candle;
import com.cryptobot.upbit.model.backtest.EquityPoint;
import com.cryptobot.upbit.repository.MarketCandleRepository;
import com.cryptobot.upbit.service.backtest.strategy.Strategy;
import com.cryptobot.upbit.service.backtest.strategy.StrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 백테스트 엔진
 * 백테스트 실행의 메인 로직을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestEngine {

    private final MarketCandleRepository candleRepository;
    private final OrderExecutor orderExecutor;
    private final PerformanceCalculator performanceCalculator;
    private final StrategyFactory strategyFactory;

    /**
     * 백테스트 실행
     */
    public BacktestResult run(BacktestConfig config) {
        log.info("Starting backtest: {}", config.getName());
        long startTime = System.currentTimeMillis();

        // 1. 초기화
        BacktestContext context = new BacktestContext(config);
        BacktestResult result = BacktestResult.builder()
                .config(config)
                .user(config.getUser())
                .status("RUNNING")
                .build();

        try {
            // 2. 과거 데이터 로드
            List<Candle> candles = loadHistoricalData(config);
            log.info("Loaded {} candles", candles.size());

            if (candles.isEmpty()) {
                throw new RuntimeException("No historical data found for the specified period");
            }

            // 3. 전략 생성
            Strategy strategy = strategyFactory.createStrategy(
                    config.getStrategyName(),
                    config.getStrategyParams()
            );
            log.info("Using strategy: {}", strategy.getName());

            // 4. 전략 기반 백테스트 실행
            executeStrategyBacktest(candles, context, strategy);

            // 5. 성과 지표 계산
            performanceCalculator.calculateMetrics(context, result);

            // 6. 거래 내역 저장 (임시)
            result.setTrades(context.getTrades());

            // 7. 완료 처리
            result.setStatus("COMPLETED");
            result.setCompletedAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Backtest failed", e);
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        result.setExecutionTimeMs(endTime - startTime);

        log.info("Backtest completed: {} in {}ms",
                config.getName(), result.getExecutionTimeMs());

        return result;
    }

    /**
     * 과거 데이터 로드
     */
    private List<Candle> loadHistoricalData(BacktestConfig config) {
        LocalDateTime startDateTime = config.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = config.getEndDate().atTime(23, 59, 59);

        List<MarketCandle> entities = candleRepository
                .findByMarketAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
                        config.getMarket(),
                        config.getTimeframe(),
                        startDateTime,
                        endDateTime
                );

        return entities.stream()
                .map(this::toCandle)
                .collect(Collectors.toList());
    }

    /**
     * 전략 기반 백테스트 실행
     */
    private void executeStrategyBacktest(List<Candle> candles, BacktestContext context, Strategy strategy) {
        // 전략 초기화
        strategy.initialize(context, candles);

        // 각 캔들에 대해 전략 신호 생성 및 실행
        for (int i = 0; i < candles.size(); i++) {
            Candle candle = candles.get(i);

            // 전략 신호 생성
            Strategy.Signal signal = strategy.generateSignal(i, candles, context);

            // 신호에 따라 주문 실행
            if (signal == Strategy.Signal.BUY && context.getCoinBalance() == 0) {
                // 현금의 99%로 매수
                Double buyAmount = context.getCash() * 0.99;
                if (buyAmount > 0) {
                    orderExecutor.executeBuy(candle, context, buyAmount, "Strategy: " + strategy.getName());
                }
            } else if (signal == Strategy.Signal.SELL && context.getCoinBalance() > 0) {
                // 전량 매도
                orderExecutor.executeSell(candle, context, context.getCoinBalance(), "Strategy: " + strategy.getName());
            }

            // 자산 곡선 기록
            recordEquityCurve(candle, context);
        }
    }

    /**
     * 자산 곡선 기록
     */
    private void recordEquityCurve(Candle candle, BacktestContext context) {
        Double currentPrice = candle.getClosingPrice();
        Double portfolioValue = context.getPortfolioValue(currentPrice);
        Double coinValue = context.getCoinBalance() * currentPrice;

        // 최고점 및 낙폭 업데이트
        context.updatePeak(portfolioValue);
        context.updateDrawdown(portfolioValue);

        // 누적 수익률 계산
        Double cumulativeReturn = ((portfolioValue - context.getConfig().getInitialCapital()) /
                context.getConfig().getInitialCapital()) * 100;

        EquityPoint point = EquityPoint.builder()
                .timestamp(candle.getTimestamp())
                .portfolioValue(portfolioValue)
                .cash(context.getCash())
                .coinValue(coinValue)
                .cumulativeReturn(cumulativeReturn)
                .drawdown(context.getCurrentDrawdown())
                .build();

        context.getEquityCurve().add(point);
    }

    /**
     * MarketCandle 엔티티를 Candle 모델로 변환
     */
    private Candle toCandle(MarketCandle entity) {
        return Candle.builder()
                .timestamp(entity.getTimestamp())
                .openingPrice(entity.getOpeningPrice())
                .highPrice(entity.getHighPrice())
                .lowPrice(entity.getLowPrice())
                .closingPrice(entity.getClosingPrice())
                .volume(entity.getVolume())
                .accTradePrice(entity.getAccTradePrice())
                .build();
    }
}
