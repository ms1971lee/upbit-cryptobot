package com.cryptobot.upbit.repository;

import com.cryptobot.upbit.entity.MarketCandle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketCandleRepository extends JpaRepository<MarketCandle, Long> {

    /**
     * 특정 마켓, 타임프레임, 기간의 캔들 데이터 조회
     */
    List<MarketCandle> findByMarketAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
            String market,
            String timeframe,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * 특정 마켓과 타임프레임의 캔들 존재 여부 확인
     */
    boolean existsByMarketAndTimeframeAndTimestamp(
            String market,
            String timeframe,
            LocalDateTime timestamp
    );

    /**
     * 특정 마켓과 타임프레임의 가장 최근 캔들 조회
     */
    Optional<MarketCandle> findTopByMarketAndTimeframeOrderByTimestampDesc(
            String market,
            String timeframe
    );

    /**
     * 특정 마켓과 타임프레임의 가장 오래된 캔들 조회
     */
    Optional<MarketCandle> findTopByMarketAndTimeframeOrderByTimestampAsc(
            String market,
            String timeframe
    );

    /**
     * 특정 마켓과 타임프레임의 캔들 개수 조회
     */
    long countByMarketAndTimeframe(String market, String timeframe);

    /**
     * 특정 마켓과 타임프레임의 데이터 기간 조회
     */
    @Query("SELECT MIN(mc.timestamp), MAX(mc.timestamp) " +
           "FROM MarketCandle mc " +
           "WHERE mc.market = :market AND mc.timeframe = :timeframe")
    Object[] findDateRangeByMarketAndTimeframe(String market, String timeframe);

    /**
     * 사용 가능한 마켓 목록 조회
     */
    @Query("SELECT DISTINCT mc.market FROM MarketCandle mc")
    List<String> findDistinctMarkets();

    /**
     * 특정 마켓의 사용 가능한 타임프레임 목록 조회
     */
    @Query("SELECT DISTINCT mc.timeframe FROM MarketCandle mc WHERE mc.market = :market")
    List<String> findDistinctTimeframesByMarket(String market);
}
