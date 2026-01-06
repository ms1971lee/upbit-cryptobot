package com.cryptobot.upbit.scanner.controller;

import com.cryptobot.upbit.common.dto.ApiResponse;
import com.cryptobot.upbit.scanner.SignalScanner;
import com.cryptobot.upbit.scanner.dto.SignalScanResult;
import com.cryptobot.upbit.scanner.dto.StrategyInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/scan")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ScanController {

    private final SignalScanner signalScanner;

    /**
     * 신호 스캔 실행
     */
    @GetMapping("/signals")
    public ResponseEntity<ApiResponse<List<SignalScanResult>>> scanSignals(
            @RequestParam(defaultValue = "V1") String strategy,
            @RequestParam(defaultValue = "5m") String timeframe
    ) {
        try {
            log.info("Scan request received - strategy: {}, timeframe: {}", strategy, timeframe);
            List<SignalScanResult> results = signalScanner.scanMarkets(strategy, timeframe);
            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (Exception e) {
            log.error("Scan failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("스캔 실패: " + e.getMessage()));
        }
    }

    /**
     * 사용 가능한 전략 목록 조회
     */
    @GetMapping("/strategies")
    public ResponseEntity<ApiResponse<List<StrategyInfo>>> getStrategies() {
        try {
            List<StrategyInfo> strategies = List.of(
                    new StrategyInfo("V1", "Donchian Breakout", "돌파형 전략"),
                    new StrategyInfo("V2", "Holy Grail Pullback", "눌림목 전략"),
                    new StrategyInfo("V3", "Reversal", "반전 전략")
            );
            return ResponseEntity.ok(ApiResponse.success(strategies));
        } catch (Exception e) {
            log.error("Failed to get strategies: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("전략 목록 조회 실패"));
        }
    }
}
