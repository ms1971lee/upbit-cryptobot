package com.cryptobot.upbit.repository;

import com.cryptobot.upbit.entity.TestBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestBalanceRepository extends JpaRepository<TestBalance, Long> {

    List<TestBalance> findByUserId(Long userId);

    Optional<TestBalance> findByUserIdAndCurrency(Long userId, String currency);
}
