package com.cryptobot.upbit.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 백테스트 설정 엔티티
 */
@Entity
@Table(name = "backtest_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 백테스트 이름
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 마켓 (예: KRW-BTC)
     */
    @Column(nullable = false, length = 20)
    private String market;

    /**
     * 타임프레임 (예: 1h, 1d)
     */
    @Column(nullable = false, length = 10)
    private String timeframe;

    /**
     * 백테스트 시작일
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * 백테스트 종료일
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * 초기 자금
     */
    @Column(name = "initial_capital", nullable = false)
    private Double initialCapital;

    /**
     * 전략 이름
     */
    @Column(name = "strategy_name", nullable = false, length = 50)
    private String strategyName;

    /**
     * 전략 파라미터 (JSON)
     */
    @Column(name = "strategy_params", columnDefinition = "TEXT")
    private String strategyParams;

    /**
     * 수수료율 (기본: 0.0005 = 0.05%)
     */
    @Column(name = "commission_rate")
    private Double commissionRate = 0.0005;

    /**
     * 슬리피지율 (기본: 0.0001 = 0.01%)
     */
    @Column(name = "slippage_rate")
    private Double slippageRate = 0.0001;

    /**
     * 생성 시간
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
