package com.cryptobot.upbit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_balances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "currency"})
})
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TestBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String currency; // KRW, BTC, ETH 등

    @Column(nullable = false)
    private Double balance; // 보유 수량

    @Column(name = "avg_buy_price", nullable = false)
    private Double avgBuyPrice; // 평균 매수가

    @Column(nullable = false)
    private Double locked = 0.0; // 주문 중 잠긴 수량

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 평균 매수가 재계산 헬퍼 메서드
    public void updateAvgBuyPrice(Double newPrice, Double newVolume) {
        if (this.balance == 0) {
            this.avgBuyPrice = newPrice;
        } else {
            Double totalValue = (this.balance * this.avgBuyPrice) + (newVolume * newPrice);
            this.balance += newVolume;
            this.avgBuyPrice = totalValue / this.balance;
        }
    }

    // 매도 시 잔고 감소
    public void decreaseBalance(Double volume) {
        this.balance -= volume;
        if (this.balance <= 0.00000001) { // 아주 작은 값은 0으로
            this.balance = 0.0;
            this.avgBuyPrice = 0.0;
        }
    }
}
