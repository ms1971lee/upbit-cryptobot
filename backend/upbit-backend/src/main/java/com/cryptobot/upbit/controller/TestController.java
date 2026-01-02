package com.cryptobot.upbit.controller;

import com.cryptobot.upbit.service.UpbitApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TestController {

    private final UpbitApiService upbitApiService;

    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Upbit Cryptobot Backend is running!");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "OK");
        return response;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "upbit-cryptobot");
        return response;
    }

    @GetMapping("/upbit/test/{keyName}")
    public Mono<Map<String, Object>> testUpbitApi(@PathVariable String keyName) {
        return upbitApiService.testConnection(keyName);
    }
}
