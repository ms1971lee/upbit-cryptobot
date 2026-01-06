package com.cryptobot.upbit.repository;

import com.cryptobot.upbit.entity.TestTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestTradeRepository extends JpaRepository<TestTrade, Long> {

    List<TestTrade> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<TestTrade> findByUserIdAndMarketOrderByCreatedAtDesc(Long userId, String market);
}
