package com.cryptobot.upbit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name; // API 키 이름 (예: "메인 계좌", "테스트 계좌")

    @Column(name = "access_key", nullable = false, length = 500)
    private String accessKey; // 암호화된 access key

    @Column(name = "secret_key", nullable = false, length = 500)
    private String secretKey; // 암호화된 secret key

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false; // 활성화 여부

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
