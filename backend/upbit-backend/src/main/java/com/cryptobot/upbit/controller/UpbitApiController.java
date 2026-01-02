package com.cryptobot.upbit.controller;

import com.cryptobot.upbit.entity.User;
import com.cryptobot.upbit.repository.UserRepository;
import com.cryptobot.upbit.service.UpbitApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/upbit")
@RequiredArgsConstructor
public class UpbitApiController {

    private final UpbitApiService upbitApiService;
    private final UserRepository userRepository;

    /**
     * 마켓 정보 조회 (인증 불필요)
     */
    @GetMapping("/markets")
    public Mono<ResponseEntity<Map<String, Object>>> getMarkets() {
        log.info("Get markets request");
        return upbitApiService.getMarketInfo()
                .map(ResponseEntity::ok);
    }

    /**
     * 계좌 조회 (인증 필요 - 사용자의 활성화된 API 키 사용)
     */
    @GetMapping("/accounts")
    public Mono<ResponseEntity<Map<String, Object>>> getAccounts(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        log.info("Get accounts request for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        return upbitApiService.getAccounts(user.getId())
                .map(ResponseEntity::ok);
    }
}
