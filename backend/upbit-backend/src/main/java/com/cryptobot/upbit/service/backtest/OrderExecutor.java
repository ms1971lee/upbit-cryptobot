package com.cryptobot.upbit.service.backtest;

import com.cryptobot.upbit.model.backtest.BacktestContext;
import com.cryptobot.upbit.model.backtest.Candle;
import com.cryptobot.upbit.model.backtest.Trade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 주문 실행 시뮬레이터
 */
@Slf4j
@Component
public class OrderExecutor {

    /**
     * 매수 주문 실행
     */
    public void executeBuy(Candle candle, BacktestContext context, Double amount, String reason) {
        Double price = candle.getClosingPrice();
        Double commission = amount * context.getConfig().getCommissionRate();
        Double slippage = amount * context.getConfig().getSlippageRate();
        Double totalCost = amount + commission + slippage;

        // 자금 확인
        if (context.getCash() < totalCost) {
            log.warn("Insufficient cash for buy order at {}: need {}, have {}",
                    candle.getTimestamp(), totalCost, context.getCash());
            return;
        }

        // 매수 실행
        Double volume = amount / price;
        Double balanceBefore = context.getCash();
        context.setCash(context.getCash() - totalCost);

        // 평균 매수가 갱신
        context.updateAvgBuyPrice(price, volume);

        // 포트폴리오 가치 계산
        Double portfolioValue = context.getPortfolioValue(price);

        // 거래 기록
        Trade trade = Trade.builder()
                .timestamp(candle.getTimestamp())
                .orderType(Trade.OrderType.BUY)
                .price(price)
                .volume(volume)
                .totalAmount(amount)
                .commission(commission + slippage)
                .reason(reason)
                .balanceBefore(balanceBefore)
                .balanceAfter(context.getCash())
                .portfolioValue(portfolioValue)
                .build();

        context.getTrades().add(trade);

        log.debug("BUY executed at {}: {} @ {} (cost: {})",
                candle.getTimestamp(), volume, price, totalCost);
    }

    /**
     * 매도 주문 실행
     */
    public void executeSell(Candle candle, BacktestContext context, Double volume, String reason) {
        Double price = candle.getClosingPrice();

        // 수량 확인
        if (context.getCoinBalance() < volume) {
            log.warn("Insufficient coin balance for sell order at {}: need {}, have {}",
                    candle.getTimestamp(), volume, context.getCoinBalance());
            return;
        }

        Double amount = volume * price;
        Double commission = amount * context.getConfig().getCommissionRate();
        Double slippage = amount * context.getConfig().getSlippageRate();
        Double receivedAmount = amount - commission - slippage;

        // 매도 실행
        Double balanceBefore = context.getCash();
        context.setCash(context.getCash() + receivedAmount);

        // 코인 잔고 감소
        context.decreaseCoinBalance(volume);

        // 포트폴리오 가치 계산
        Double portfolioValue = context.getPortfolioValue(price);

        // 수익 계산
        Double profit = (price - context.getAvgBuyPrice()) / context.getAvgBuyPrice() * 100;

        // 거래 기록
        Trade trade = Trade.builder()
                .timestamp(candle.getTimestamp())
                .orderType(Trade.OrderType.SELL)
                .price(price)
                .volume(volume)
                .totalAmount(amount)
                .commission(commission + slippage)
                .reason(reason)
                .balanceBefore(balanceBefore)
                .balanceAfter(context.getCash())
                .portfolioValue(portfolioValue)
                .profit(profit)
                .build();

        context.getTrades().add(trade);

        log.debug("SELL executed at {}: {} @ {} (received: {}, profit: {}%)",
                candle.getTimestamp(), volume, price, receivedAmount, profit);
    }
}
