package com.cryptobot.upbit.controller;

import com.cryptobot.upbit.dto.backtest.*;
import com.cryptobot.upbit.service.MarketDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 백테스트 데이터 관리 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/backtest/data")
@RequiredArgsConstructor
public class BacktestDataController {

    private final MarketDataService marketDataService;

    /**
     * 과거 캔들 데이터 동기화 시작
     */
    @PostMapping("/sync")
    public ResponseEntity<DataSyncResponse> syncData(@Valid @RequestBody DataSyncRequest request) {
        log.info("Data sync request: {}", request);

        try {
            // 날짜 파싱
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate startDate = LocalDate.parse(request.getStartDate(), formatter);
            LocalDate endDate = LocalDate.parse(request.getEndDate(), formatter);

            // Task ID 생성
            String taskId = "sync-" + UUID.randomUUID().toString().substring(0, 8);

            // 예상 레코드 수 계산
            int estimatedRecords = estimateRecordCount(request.getTimeframe(), startDate, endDate);

            // 비동기로 데이터 수집 시작
            marketDataService.syncMarketData(taskId, request.getMarket(), request.getTimeframe(),
                    startDate, endDate);

            // 응답 반환
            DataSyncResponse response = DataSyncResponse.builder()
                    .success(true)
                    .message("데이터 동기화 시작")
                    .taskId(taskId)
                    .estimatedRecords(estimatedRecords)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to start data sync", e);
            DataSyncResponse response = DataSyncResponse.builder()
                    .success(false)
                    .message("데이터 동기화 시작 실패: " + e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 데이터 동기화 진행 상태 조회
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<DataSyncStatus> getStatus(@PathVariable String taskId) {
        log.info("Get sync status for task: {}", taskId);

        DataSyncStatus status = marketDataService.getTaskStatus(taskId);
        return ResponseEntity.ok(status);
    }

    /**
     * 사용 가능한 시장 데이터 목록 조회
     */
    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> getAvailableData() {
        log.info("Get available market data request");

        try {
            List<AvailableMarketData> markets = marketDataService.getAvailableMarketData();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("markets", markets);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get available data", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "데이터 조회 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 전체 캔들 데이터 개수 조회 (디버그용)
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getTotalCount() {
        long count = marketDataService.getTotalCandleCount();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("totalCount", count);

        return ResponseEntity.ok(response);
    }

    /**
     * 예상 레코드 수 계산 헬퍼 메서드
     */
    private int estimateRecordCount(String timeframe, LocalDate startDate, LocalDate endDate) {
        long daysBetween = endDate.toEpochDay() - startDate.toEpochDay() + 1;

        switch (timeframe) {
            case "1m": return (int) (daysBetween * 24 * 60);
            case "5m": return (int) (daysBetween * 24 * 12);
            case "15m": return (int) (daysBetween * 24 * 4);
            case "30m": return (int) (daysBetween * 24 * 2);
            case "1h": return (int) (daysBetween * 24);
            case "1d": return (int) daysBetween;
            case "1w": return (int) (daysBetween / 7);
            case "1M": return (int) (daysBetween / 30);
            default: return 0;
        }
    }
}
