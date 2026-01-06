package com.cryptobot.upbit.repository;

import com.cryptobot.upbit.entity.DataSyncHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataSyncHistoryRepository extends JpaRepository<DataSyncHistory, Long> {

    /**
     * 사용자의 데이터 수집 이력 목록 조회 (최신순)
     */
    List<DataSyncHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Task ID로 이력 조회
     */
    Optional<DataSyncHistory> findByTaskId(String taskId);

    /**
     * 사용자의 특정 마켓 + 타임프레임 이력 조회
     */
    List<DataSyncHistory> findByUserIdAndMarketAndTimeframeOrderByCreatedAtDesc(Long userId, String market, String timeframe);
}
