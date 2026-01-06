package com.cryptobot.upbit.model.backtest;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 자산 곡선 데이터 포인트
 */
@Data
@Builder
public class EquityPoint {

    private LocalDateTime timestamp;
    private Double portfolioValue;  // 전체 포트폴리오 가치
    private Double cash;            // 현금
    private Double coinValue;       // 코인 평가액
    private Double cumulativeReturn; // 누적 수익률 (%)
    private Double drawdown;        // 낙폭 (%)
}
