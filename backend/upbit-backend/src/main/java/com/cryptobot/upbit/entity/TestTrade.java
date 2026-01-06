package com.cryptobot.upbit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_trades")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TestTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String market; // 예: KRW-BTC

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType; // BUY or SELL

    @Column(nullable = false)
    private Double price; // 체결 가격

    @Column(nullable = false)
    private Double volume; // 수량

    @Column(nullable = false)
    private Double totalAmount; // 총 거래금액 (price * volume)

    @Column(nullable = false)
    private Double fee; // 수수료 (0.05%)

    @Column(nullable = false)
    private String strategy; // 사용된 전략 (V1, V2, V3)

    @Column(length = 1000)
    private String memo; // 메모

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum OrderType {
        BUY,
        SELL
    }

    // 수수료 계산 헬퍼 메서드
    public void calculateFee() {
        this.fee = this.totalAmount * 0.0005; // 0.05% 수수료
    }
}
