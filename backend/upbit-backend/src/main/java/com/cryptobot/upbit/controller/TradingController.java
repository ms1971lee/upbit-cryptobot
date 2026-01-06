package com.cryptobot.upbit.controller;

import com.cryptobot.upbit.dto.trading.OrderRequest;
import com.cryptobot.upbit.entity.TradingMode;
import com.cryptobot.upbit.entity.User;
import com.cryptobot.upbit.repository.UserRepository;
import com.cryptobot.upbit.service.TradingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/trading")
@RequiredArgsConstructor
public class TradingController {

    private final TradingService tradingService;
    private final UserRepository userRepository;

    /**
     * 현재 거래 모드 조회
     */
    @GetMapping("/mode")
    public ResponseEntity<Map<String, Object>> getTradingMode(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        log.info("Get trading mode request for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        TradingMode mode = tradingService.getTradingMode(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("mode", mode.getMode().name());
        result.put("testInitialBalance", mode.getTestInitialBalance());

        return ResponseEntity.ok(result);
    }

    /**
     * 거래 모드 전환
     */
    @PostMapping("/mode")
    public ResponseEntity<Map<String, Object>> switchMode(
            Authentication authentication,
            @RequestBody Map<String, String> request) {

        String email = (String) authentication.getPrincipal();
        String modeStr = request.get("mode");
        log.info("Switch trading mode request for user: {}, mode: {}", email, modeStr);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        TradingMode.Mode mode = TradingMode.Mode.valueOf(modeStr);
        TradingMode tradingMode = tradingService.switchMode(user.getId(), mode);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("mode", tradingMode.getMode().name());
        result.put("message", mode == TradingMode.Mode.TEST ? "테스트 모드로 전환되었습니다" : "실거래 모드로 전환되었습니다");

        return ResponseEntity.ok(result);
    }

    /**
     * 테스트 모드 초기화
     */
    @PostMapping("/test/reset")
    public ResponseEntity<Map<String, Object>> resetTestMode(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        log.info("Reset test mode request for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        tradingService.resetTestMode(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "테스트 모드가 초기화되었습니다");

        return ResponseEntity.ok(result);
    }

    /**
     * 주문 실행 (테스트 모드만 지원)
     */
    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> executeOrder(
            Authentication authentication,
            @Valid @RequestBody OrderRequest request) {

        String email = (String) authentication.getPrincipal();
        log.info("Execute order request for user: {}, market: {}, type: {}", email, request.getMarket(), request.getOrderType());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        TradingMode tradingMode = tradingService.getTradingMode(user.getId());

        if (tradingMode.getMode() == TradingMode.Mode.LIVE) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "실거래 모드는 아직 지원하지 않습니다");
            return ResponseEntity.ok(result);
        }

        Map<String, Object> result = tradingService.executeTestOrder(user.getId(), request);
        return ResponseEntity.ok(result);
    }

    /**
     * 테스트 모드 잔고 조회
     */
    @GetMapping("/test/balances")
    public ResponseEntity<Map<String, Object>> getTestBalances(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        log.info("Get test balances request for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        Map<String, Object> result = tradingService.getTestBalances(user.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * 테스트 모드 거래 내역 조회
     */
    @GetMapping("/test/trades")
    public ResponseEntity<Map<String, Object>> getTestTrades(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        log.info("Get test trades request for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        Map<String, Object> result = tradingService.getTestTrades(user.getId());
        return ResponseEntity.ok(result);
    }
}
