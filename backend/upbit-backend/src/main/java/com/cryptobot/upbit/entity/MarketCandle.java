package com.cryptobot.upbit.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 시장 캔들 데이터 엔티티
 * 업비트 API로부터 수집한 과거 시세 데이터를 저장
 */
@Entity
@Table(name = "market_candles",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_market_timeframe_timestamp",
            columnNames = {"market", "timeframe", "timestamp"}
        )
    },
    indexes = {
        @Index(name = "idx_market_timestamp", columnList = "market, timestamp")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketCandle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 마켓 코드 (예: KRW-BTC, KRW-ETH)
     */
    @Column(nullable = false, length = 20)
    private String market;

    /**
     * 타임프레임 (예: 1m, 5m, 15m, 1h, 1d)
     */
    @Column(nullable = false, length = 10)
    private String timeframe;

    /**
     * 캔들 시작 시간
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * 시가
     */
    @Column(name = "opening_price", nullable = false)
    private Double openingPrice;

    /**
     * 고가
     */
    @Column(name = "high_price", nullable = false)
    private Double highPrice;

    /**
     * 저가
     */
    @Column(name = "low_price", nullable = false)
    private Double lowPrice;

    /**
     * 종가
     */
    @Column(name = "closing_price", nullable = false)
    private Double closingPrice;

    /**
     * 거래량 (코인 기준)
     */
    @Column(nullable = false)
    private Double volume;

    /**
     * 누적 거래대금 (KRW)
     */
    @Column(name = "acc_trade_price")
    private Double accTradePrice;

    /**
     * 데이터 생성 시간
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
