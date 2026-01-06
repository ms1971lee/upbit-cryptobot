package com.cryptobot.upbit.service.backtest;

import com.cryptobot.upbit.entity.BacktestResult;
import com.cryptobot.upbit.model.backtest.BacktestContext;
import com.cryptobot.upbit.model.backtest.EquityPoint;
import com.cryptobot.upbit.model.backtest.Trade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 성과 지표 계산기
 */
@Slf4j
@Component
public class PerformanceCalculator {

    /**
     * 백테스트 결과의 성과 지표 계산
     */
    public void calculateMetrics(BacktestContext context, BacktestResult result) {
        Double initialCapital = context.getConfig().getInitialCapital();
        Double finalCapital = context.getEquityCurve().isEmpty() ?
                initialCapital :
                context.getEquityCurve().get(context.getEquityCurve().size() - 1).getPortfolioValue();

        // 총 수익률
        Double totalReturn = ((finalCapital - initialCapital) / initialCapital) * 100;
        result.setTotalReturn(totalReturn);

        // 연간 수익률
        long days = ChronoUnit.DAYS.between(
                context.getConfig().getStartDate(),
                context.getConfig().getEndDate()
        );
        Double years = days / 365.0;
        if (years > 0) {
            Double annualReturn = (Math.pow(finalCapital / initialCapital, 1.0 / years) - 1.0) * 100;
            result.setAnnualReturn(annualReturn);
        } else {
            result.setAnnualReturn(totalReturn);
        }

        // 최대 낙폭
        result.setMaxDrawdown(context.getMaxDrawdown());

        // 샤프 비율
        Double sharpeRatio = calculateSharpeRatio(context);
        result.setSharpeRatio(sharpeRatio);

        // 거래 통계
        calculateTradeStatistics(context, result);

        // 자금 정보
        result.setFinalCapital(finalCapital);
        result.setPeakCapital(context.getPeakCapital());

        log.info("Performance metrics calculated: Return {}%, MDD {}%, Sharpe {}",
                String.format("%.2f", totalReturn),
                String.format("%.2f", context.getMaxDrawdown()),
                String.format("%.2f", sharpeRatio));
    }

    /**
     * 거래 통계 계산
     */
    private void calculateTradeStatistics(BacktestContext context, BacktestResult result) {
        List<Trade> trades = context.getTrades();

        if (trades.isEmpty()) {
            result.setTotalTrades(0);
            result.setWinningTrades(0);
            result.setLosingTrades(0);
            result.setWinRate(0.0);
            result.setAvgProfit(0.0);
            result.setAvgLoss(0.0);
            return;
        }

        // 매수/매도 쌍을 찾아서 수익 계산
        int winCount = 0;
        int lossCount = 0;
        double totalProfit = 0.0;
        double totalLoss = 0.0;

        for (Trade trade : trades) {
            if (trade.getOrderType() == Trade.OrderType.SELL && trade.getProfit() != null) {
                if (trade.getProfit() > 0) {
                    winCount++;
                    totalProfit += trade.getProfit();
                } else if (trade.getProfit() < 0) {
                    lossCount++;
                    totalLoss += Math.abs(trade.getProfit());
                }
            }
        }

        int totalPairs = winCount + lossCount;
        result.setTotalTrades(totalPairs);
        result.setWinningTrades(winCount);
        result.setLosingTrades(lossCount);

        if (totalPairs > 0) {
            result.setWinRate((double) winCount / totalPairs * 100);
        } else {
            result.setWinRate(0.0);
        }

        if (winCount > 0) {
            result.setAvgProfit(totalProfit / winCount);
        } else {
            result.setAvgProfit(0.0);
        }

        if (lossCount > 0) {
            result.setAvgLoss(totalLoss / lossCount);
        } else {
            result.setAvgLoss(0.0);
        }
    }

    /**
     * 샤프 비율 계산
     */
    private Double calculateSharpeRatio(BacktestContext context) {
        List<EquityPoint> equityCurve = context.getEquityCurve();

        if (equityCurve.size() < 2) {
            return 0.0;
        }

        // 일별 수익률 계산
        double sumReturns = 0.0;
        double sumSquaredReturns = 0.0;
        int count = 0;

        for (int i = 1; i < equityCurve.size(); i++) {
            Double prevValue = equityCurve.get(i - 1).getPortfolioValue();
            Double currValue = equityCurve.get(i).getPortfolioValue();

            if (prevValue > 0) {
                double dailyReturn = (currValue - prevValue) / prevValue;
                sumReturns += dailyReturn;
                sumSquaredReturns += dailyReturn * dailyReturn;
                count++;
            }
        }

        if (count == 0) {
            return 0.0;
        }

        // 평균 및 표준편차
        double avgReturn = sumReturns / count;
        double variance = (sumSquaredReturns / count) - (avgReturn * avgReturn);
        double stdDev = Math.sqrt(variance);

        // 샤프 비율 (무위험 수익률 0 가정, 연간화)
        if (stdDev > 0) {
            return (avgReturn / stdDev) * Math.sqrt(252);  // 252 trading days
        } else {
            return 0.0;
        }
    }
}
