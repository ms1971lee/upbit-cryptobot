package com.cryptobot.upbit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // JPA Auditing 활성화 (User 엔티티의 createdAt, updatedAt 자동 관리)
}
