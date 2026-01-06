package com.cryptobot.upbit.controller;

import com.cryptobot.upbit.entity.BacktestResult;
import com.cryptobot.upbit.entity.BacktestTrade;
import com.cryptobot.upbit.entity.User;
import com.cryptobot.upbit.repository.BacktestTradeRepository;
import com.cryptobot.upbit.repository.UserRepository;
import com.cryptobot.upbit.service.BacktestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 백테스트 실행 및 결과 조회 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestService backtestService;
    private final BacktestTradeRepository tradeRepository;
    private final UserRepository userRepository;

    /**
     * 백테스트 실행
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runBacktest(
            Authentication authentication,
            @RequestBody Map<String, Object> request) {

        String email = (String) authentication.getPrincipal();
        log.info("Backtest run request from user: {}", email);

        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            // 백테스트 실행 (비동기)
            CompletableFuture<Long> future = backtestService.runBacktest(user.getId(), request);

            // 즉시 응답 반환 (백테스트는 백그라운드에서 실행)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "백테스트가 시작되었습니다");
            response.put("backtestId", future.get()); // 결과 ID 반환

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to run backtest", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "백테스트 실행 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 백테스트 결과 조회
     */
    @GetMapping("/results/{backtestId}")
    public ResponseEntity<Map<String, Object>> getResult(@PathVariable Long backtestId) {
        log.info("Get backtest result: {}", backtestId);

        try {
            BacktestResult result = backtestService.getResult(backtestId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", buildResultResponse(result));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get backtest result", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 백테스트 거래 내역 조회
     */
    @GetMapping("/results/{backtestId}/trades")
    public ResponseEntity<Map<String, Object>> getTrades(@PathVariable Long backtestId) {
        log.info("Get backtest trades: {}", backtestId);

        try {
            List<BacktestTrade> trades = tradeRepository.findByBacktestResultIdOrderByTimestampAsc(backtestId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("trades", trades.stream()
                    .map(this::buildTradeItem)
                    .toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get backtest trades", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 백테스트 이력 조회
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        log.info("Get backtest history for user: {}", email);

        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            List<BacktestResult> results = backtestService.getHistory(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("backtests", results.stream()
                    .map(this::buildHistoryItem)
                    .toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get backtest history", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 백테스트 결과 삭제
     */
    @DeleteMapping("/results/{backtestId}")
    public ResponseEntity<Map<String, Object>> deleteResult(
            Authentication authentication,
            @PathVariable Long backtestId) {

        String email = (String) authentication.getPrincipal();
        log.info("Delete backtest result: {} by user: {}", backtestId, email);

        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            backtestService.deleteResult(backtestId, user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "백테스트 결과가 삭제되었습니다");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to delete backtest result", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 백테스트 결과 응답 생성
     */
    private Map<String, Object> buildResultResponse(BacktestResult result) {
        Map<String, Object> data = new HashMap<>();

        data.put("id", result.getId());
        data.put("status", result.getStatus());

        // Config 정보
        Map<String, Object> config = new HashMap<>();
        config.put("name", result.getConfig().getName());
        config.put("market", result.getConfig().getMarket());
        config.put("timeframe", result.getConfig().getTimeframe());
        config.put("period", result.getConfig().getStartDate() + " ~ " + result.getConfig().getEndDate());
        config.put("initialCapital", result.getConfig().getInitialCapital());
        config.put("strategy", result.getConfig().getStrategyName());
        data.put("config", config);

        // 성과 지표
        Map<String, Object> performance = new HashMap<>();
        performance.put("totalReturn", result.getTotalReturn());
        performance.put("annualReturn", result.getAnnualReturn());
        performance.put("maxDrawdown", result.getMaxDrawdown());
        performance.put("sharpeRatio", result.getSharpeRatio());
        performance.put("winRate", result.getWinRate());
        performance.put("finalCapital", result.getFinalCapital());
        performance.put("peakCapital", result.getPeakCapital());
        data.put("performance", performance);

        // 거래 통계
        Map<String, Object> trades = new HashMap<>();
        trades.put("total", result.getTotalTrades());
        trades.put("winning", result.getWinningTrades());
        trades.put("losing", result.getLosingTrades());
        trades.put("avgProfit", result.getAvgProfit());
        trades.put("avgLoss", result.getAvgLoss());
        data.put("trades", trades);

        // 실행 정보
        data.put("executionTime", result.getExecutionTimeMs());
        data.put("completedAt", result.getCompletedAt());
        data.put("errorMessage", result.getErrorMessage());

        return data;
    }

    /**
     * 이력 항목 생성
     */
    private Map<String, Object> buildHistoryItem(BacktestResult result) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", result.getId());
        item.put("name", result.getConfig().getName());
        item.put("market", result.getConfig().getMarket());
        item.put("strategy", result.getConfig().getStrategyName());
        item.put("period", result.getConfig().getStartDate() + " ~ " + result.getConfig().getEndDate());
        item.put("totalReturn", result.getTotalReturn());
        item.put("maxDrawdown", result.getMaxDrawdown());
        item.put("totalTrades", result.getTotalTrades()); // 체결수량
        item.put("finalCapital", result.getFinalCapital()); // 체결금액
        item.put("status", result.getStatus());
        item.put("createdAt", result.getCreatedAt());
        return item;
    }

    /**
     * 거래 항목 생성
     */
    private Map<String, Object> buildTradeItem(BacktestTrade trade) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", trade.getId());
        item.put("type", trade.getType());
        item.put("timestamp", trade.getTimestamp());
        item.put("price", trade.getPrice());
        item.put("amount", trade.getAmount());
        item.put("total", trade.getTotal());
        item.put("commission", trade.getCommission());
        item.put("profitRate", trade.getProfitRate());
        item.put("profitAmount", trade.getProfitAmount());
        item.put("balanceAfter", trade.getBalanceAfter());
        item.put("positionAfter", trade.getPositionAfter());
        return item;
    }
}
