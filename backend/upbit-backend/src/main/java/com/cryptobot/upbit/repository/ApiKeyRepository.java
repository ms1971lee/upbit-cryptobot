package com.cryptobot.upbit.repository;

import com.cryptobot.upbit.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    List<ApiKey> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ApiKey> findByIdAndUserId(Long id, Long userId);

    Optional<ApiKey> findByUserIdAndIsActiveTrue(Long userId);

    long countByUserId(Long userId);
}
