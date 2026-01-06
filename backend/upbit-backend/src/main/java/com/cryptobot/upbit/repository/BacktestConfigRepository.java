package com.cryptobot.upbit.repository;

import com.cryptobot.upbit.entity.BacktestConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BacktestConfigRepository extends JpaRepository<BacktestConfig, Long> {

    /**
     * 사용자의 백테스트 설정 목록 조회
     */
    List<BacktestConfig> findByUserIdOrderByCreatedAtDesc(Long userId);
}
