package com.cryptobot.upbit.model.backtest;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 백테스트 거래 기록 모델
 */
@Data
@Builder
public class Trade {

    private LocalDateTime timestamp;
    private OrderType orderType;
    private Double price;
    private Double volume;
    private Double totalAmount;
    private Double commission;
    private String reason;  // 거래 이유 (신호)

    // 성과 추적
    private Double balanceBefore;
    private Double balanceAfter;
    private Double portfolioValue;
    private Double cumulativeReturn;
    private Double profit;  // 이익/손실

    public enum OrderType {
        BUY, SELL
    }
}
