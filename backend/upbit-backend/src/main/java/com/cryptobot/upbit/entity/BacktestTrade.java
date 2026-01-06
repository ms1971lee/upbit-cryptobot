package com.cryptobot.upbit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 백테스트 거래 내역 엔티티
 */
@Entity
@Table(name = "backtest_trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 백테스트 결과 ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backtest_result_id", nullable = false)
    private BacktestResult backtestResult;

    /**
     * 거래 타입 (BUY, SELL)
     */
    @Column(nullable = false, length = 10)
    private String type;

    /**
     * 거래 시각
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * 거래 가격
     */
    @Column(nullable = false)
    private Double price;

    /**
     * 거래 수량
     */
    @Column(nullable = false)
    private Double amount;

    /**
     * 거래 총액
     */
    @Column(nullable = false)
    private Double total;

    /**
     * 수수료
     */
    @Column(nullable = false)
    private Double commission;

    /**
     * 수익률 (SELL인 경우만)
     */
    @Column
    private Double profitRate;

    /**
     * 수익 금액 (SELL인 경우만)
     */
    @Column
    private Double profitAmount;

    /**
     * 거래 후 잔고
     */
    @Column(nullable = false)
    private Double balanceAfter;

    /**
     * 거래 후 보유 수량
     */
    @Column(nullable = false)
    private Double positionAfter;

    /**
     * 생성 일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
