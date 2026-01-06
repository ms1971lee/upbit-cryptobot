package com.cryptobot.upbit.controller;

import com.cryptobot.upbit.entity.TradingMode;
import com.cryptobot.upbit.entity.User;
import com.cryptobot.upbit.repository.UserRepository;
import com.cryptobot.upbit.service.TradingService;
import com.cryptobot.upbit.service.UpbitApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final UpbitApiService upbitApiService;
    private final UserRepository userRepository;
    private final TradingService tradingService;

    /**
     * 업비트 계좌 조회 (활성화된 API 키 사용)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAccounts(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        log.info("Get accounts request for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        Map<String, Object> result = upbitApiService.getAccounts(user.getId())
                .block();

        return ResponseEntity.ok(result);
    }

    /**
     * 업비트 계좌 요약 정보 조회
     * 테스트 모드일 경우 테스트 잔고 데이터 반환
     * 실거래 모드일 경우 실제 업비트 API 데이터 반환
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getAccountSummary(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        log.info("Get account summary request for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 거래 모드 확인
        TradingMode tradingMode = tradingService.getTradingMode(user.getId());

        Map<String, Object> result;
        if (tradingMode.getMode() == TradingMode.Mode.TEST) {
            // 테스트 모드: 테스트 잔고 데이터 반환
            log.info("Using test mode data for user: {}", email);
            result = tradingService.getTestAccountSummary(user.getId());
        } else {
            // 실거래 모드: 실제 업비트 API 데이터 반환
            log.info("Using live mode data for user: {}", email);
            result = upbitApiService.getAccountSummary(user.getId()).block();
        }

        return ResponseEntity.ok(result);
    }
}
