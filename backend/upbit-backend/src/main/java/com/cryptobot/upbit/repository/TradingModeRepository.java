package com.cryptobot.upbit.repository;

import com.cryptobot.upbit.entity.TradingMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TradingModeRepository extends JpaRepository<TradingMode, Long> {

    Optional<TradingMode> findByUserId(Long userId);
}
