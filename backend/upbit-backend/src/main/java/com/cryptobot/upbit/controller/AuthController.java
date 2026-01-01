package com.cryptobot.upbit.controller;

import com.cryptobot.upbit.dto.auth.AuthResponse;
import com.cryptobot.upbit.dto.auth.LoginRequest;
import com.cryptobot.upbit.dto.auth.SignupRequest;
import com.cryptobot.upbit.dto.auth.UserDto;
import com.cryptobot.upbit.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("Signup request received for email: {}", request.getEmail());

        AuthResponse response = authService.signup(request);

        return ResponseEntity.ok(response);
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.getEmail());

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    /**
     * 현재 사용자 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        String email = (String) authentication.getPrincipal();

        log.info("Get current user request for email: {}", email);

        UserDto user = authService.getCurrentUser(email);

        return ResponseEntity.ok(user);
    }
}
