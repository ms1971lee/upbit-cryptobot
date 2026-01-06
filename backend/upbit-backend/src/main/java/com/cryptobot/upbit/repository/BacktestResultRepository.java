package com.cryptobot.upbit.repository;

import com.cryptobot.upbit.entity.BacktestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BacktestResultRepository extends JpaRepository<BacktestResult, Long> {

    /**
     * 사용자의 백테스트 결과 목록 조회
     */
    List<BacktestResult> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Config ID로 결과 조회
     */
    Optional<BacktestResult> findByConfigId(Long configId);

    /**
     * 상태별 결과 조회
     */
    List<BacktestResult> findByStatus(String status);
}
