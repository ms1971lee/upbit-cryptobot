package com.cryptobot.upbit.repository;

import com.cryptobot.upbit.entity.BacktestTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BacktestTradeRepository extends JpaRepository<BacktestTrade, Long> {

    /**
     * 특정 백테스트의 모든 거래 내역 조회 (시간 순)
     */
    List<BacktestTrade> findByBacktestResultIdOrderByTimestampAsc(Long backtestResultId);

    /**
     * 특정 백테스트의 거래 개수 조회
     */
    long countByBacktestResultId(Long backtestResultId);
}
