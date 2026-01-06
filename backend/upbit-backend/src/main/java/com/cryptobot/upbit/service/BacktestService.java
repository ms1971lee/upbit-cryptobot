package com.cryptobot.upbit.service;

import com.cryptobot.upbit.entity.BacktestConfig;
import com.cryptobot.upbit.entity.BacktestResult;
import com.cryptobot.upbit.entity.BacktestTrade;
import com.cryptobot.upbit.entity.User;
import com.cryptobot.upbit.model.backtest.Trade;
import com.cryptobot.upbit.repository.BacktestConfigRepository;
import com.cryptobot.upbit.repository.BacktestResultRepository;
import com.cryptobot.upbit.repository.BacktestTradeRepository;
import com.cryptobot.upbit.repository.UserRepository;
import com.cryptobot.upbit.service.backtest.BacktestEngine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestService {

    private final BacktestEngine backtestEngine;
    private final BacktestConfigRepository configRepository;
    private final BacktestResultRepository resultRepository;
    private final BacktestTradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 백테스트 실행 (비동기)
     */
    @Async
    @Transactional
    public CompletableFuture<Long> runBacktest(Long userId, Map<String, Object> request) {
        try {
            // 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            // Config 생성
            BacktestConfig config = createConfig(user, request);
            config = configRepository.save(config);

            // 백테스트 실행
            BacktestResult result = backtestEngine.run(config);
            result.setConfig(config);
            result.setUser(user);

            // 결과 저장
            result = resultRepository.save(result);

            // 거래 내역 저장
            saveTrades(result);

            log.info("Backtest completed: ID={}, Return={}%, Trades={}",
                    result.getId(), result.getTotalReturn(), result.getTrades() != null ? result.getTrades().size() : 0);

            return CompletableFuture.completedFuture(result.getId());

        } catch (Exception e) {
            log.error("Backtest execution failed", e);
            throw new RuntimeException("백테스트 실행 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 백테스트 결과 조회
     */
    @Transactional(readOnly = true)
    public BacktestResult getResult(Long resultId) {
        BacktestResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("백테스트 결과를 찾을 수 없습니다"));

        // Lazy loading 초기화
        result.getConfig().getName();
        result.getUser().getEmail();

        return result;
    }

    /**
     * 사용자의 백테스트 이력 조회
     */
    @Transactional(readOnly = true)
    public List<BacktestResult> getHistory(Long userId) {
        List<BacktestResult> results = resultRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // Lazy loading 초기화
        results.forEach(result -> {
            result.getConfig().getName();
            result.getUser().getEmail();
        });

        return results;
    }

    /**
     * 백테스트 결과 삭제
     */
    @Transactional
    public void deleteResult(Long resultId, Long userId) {
        BacktestResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("백테스트 결과를 찾을 수 없습니다"));

        // 권한 확인
        if (!result.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다");
        }

        resultRepository.delete(result);
        log.info("Backtest result deleted: {}", resultId);
    }

    /**
     * Config 생성
     */
    private BacktestConfig createConfig(User user, Map<String, Object> request) {
        try {
            String strategyParamsJson = null;
            if (request.containsKey("strategyParams")) {
                strategyParamsJson = objectMapper.writeValueAsString(request.get("strategyParams"));
            }

            return BacktestConfig.builder()
                    .user(user)
                    .name((String) request.get("name"))
                    .market((String) request.get("market"))
                    .timeframe((String) request.get("timeframe"))
                    .startDate(java.time.LocalDate.parse((String) request.get("startDate")))
                    .endDate(java.time.LocalDate.parse((String) request.get("endDate")))
                    .initialCapital(((Number) request.get("initialCapital")).doubleValue())
                    .strategyName((String) request.get("strategyName"))
                    .strategyParams(strategyParamsJson)
                    .commissionRate(
                            request.containsKey("commissionRate") ?
                                    ((Number) request.get("commissionRate")).doubleValue() : 0.0005
                    )
                    .slippageRate(
                            request.containsKey("slippageRate") ?
                                    ((Number) request.get("slippageRate")).doubleValue() : 0.0001
                    )
                    .build();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("전략 파라미터 변환 실패", e);
        }
    }

    /**
     * 거래 내역 저장
     */
    private void saveTrades(BacktestResult result) {
        if (result.getTrades() == null || result.getTrades().isEmpty()) {
            log.debug("No trades to save for backtest {}", result.getId());
            return;
        }

        List<Trade> trades = result.getTrades();
        log.info("Saving {} trades for backtest {}", trades.size(), result.getId());

        // 코인 보유량 추적 (거래 시뮬레이션)
        Double currentPosition = 0.0;

        for (Trade trade : trades) {
            // 거래 후 포지션 계산
            if (trade.getOrderType() == Trade.OrderType.BUY) {
                currentPosition += trade.getVolume();
            } else if (trade.getOrderType() == Trade.OrderType.SELL) {
                currentPosition -= trade.getVolume();
                // 부동소수점 오차 보정
                if (currentPosition < 0.00000001) {
                    currentPosition = 0.0;
                }
            }

            BacktestTrade backtestTrade = BacktestTrade.builder()
                    .backtestResult(result)
                    .type(trade.getOrderType().name())
                    .timestamp(trade.getTimestamp())
                    .price(trade.getPrice())
                    .amount(trade.getVolume())
                    .total(trade.getTotalAmount())
                    .commission(trade.getCommission())
                    .profitRate(trade.getProfit())
                    .profitAmount(trade.getProfit() != null && trade.getVolume() != null ?
                            (trade.getPrice() * trade.getVolume() * trade.getProfit() / 100) : null)
                    .balanceAfter(trade.getBalanceAfter())
                    .positionAfter(currentPosition)
                    .build();

            tradeRepository.save(backtestTrade);
        }

        log.info("Successfully saved {} trades", trades.size());
    }
}
