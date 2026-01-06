package com.cryptobot.upbit.entity;

import com.cryptobot.upbit.model.backtest.Trade;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 백테스트 결과 엔티티
 */
@Entity
@Table(name = "backtest_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private BacktestConfig config;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 실행 상태 (RUNNING, COMPLETED, FAILED)
     */
    @Column(nullable = false, length = 20)
    private String status;

    // ========== 성과 지표 ==========

    /**
     * 총 수익률 (%)
     */
    @Column(name = "total_return")
    private Double totalReturn;

    /**
     * 연간 수익률 (%)
     */
    @Column(name = "annual_return")
    private Double annualReturn;

    /**
     * 최대 낙폭 (%)
     */
    @Column(name = "max_drawdown")
    private Double maxDrawdown;

    /**
     * 샤프 비율
     */
    @Column(name = "sharpe_ratio")
    private Double sharpeRatio;

    /**
     * 승률 (%)
     */
    @Column(name = "win_rate")
    private Double winRate;

    // ========== 거래 통계 ==========

    /**
     * 총 거래 횟수
     */
    @Column(name = "total_trades")
    private Integer totalTrades;

    /**
     * 수익 거래 횟수
     */
    @Column(name = "winning_trades")
    private Integer winningTrades;

    /**
     * 손실 거래 횟수
     */
    @Column(name = "losing_trades")
    private Integer losingTrades;

    /**
     * 평균 수익 (%)
     */
    @Column(name = "avg_profit")
    private Double avgProfit;

    /**
     * 평균 손실 (%)
     */
    @Column(name = "avg_loss")
    private Double avgLoss;

    // ========== 자금 정보 ==========

    /**
     * 최종 자금
     */
    @Column(name = "final_capital")
    private Double finalCapital;

    /**
     * 최고 자금
     */
    @Column(name = "peak_capital")
    private Double peakCapital;

    // ========== 실행 정보 ==========

    /**
     * 실행 시간 (ms)
     */
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    /**
     * 에러 메시지
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 생성 시간
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 완료 시간
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 거래 내역 (임시 저장용, DB에 저장되지 않음)
     */
    @Transient
    private List<Trade> trades;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
