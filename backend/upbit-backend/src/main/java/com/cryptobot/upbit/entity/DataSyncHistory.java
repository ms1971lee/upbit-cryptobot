package com.cryptobot.upbit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 데이터 수집 이력 엔티티
 */
@Entity
@Table(name = "data_sync_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class DataSyncHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 마켓 코드 (예: KRW-BTC)
     */
    @Column(nullable = false, length = 20)
    private String market;

    /**
     * 타임프레임 (예: 1m, 5m, 15m, 1h, 1d)
     */
    @Column(nullable = false, length = 10)
    private String timeframe;

    /**
     * 수집 시작일
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * 수집 종료일
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * 수집 상태 (IN_PROGRESS, COMPLETED, FAILED)
     */
    @Column(nullable = false, length = 20)
    private String status;

    /**
     * 수집된 레코드 수
     */
    @Column(name = "record_count")
    private Integer recordCount;

    /**
     * 진행률 (0-100)
     */
    @Column
    private Double progress;

    /**
     * 에러 메시지
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Task ID (비동기 처리용)
     */
    @Column(name = "task_id", length = 100)
    private String taskId;

    /**
     * 생성 일시 (수집 시작 시각)
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 완료 일시
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
