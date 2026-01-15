package com.cryptobot.upbit.controller;

import com.cryptobot.upbit.common.dto.ApiResponse;
import com.cryptobot.upbit.strategy.StrategyV4EmaTrend;
import com.cryptobot.upbit.strategy.ema.EmaTrendConfig;
import com.cryptobot.upbit.strategy.ema.EmaTrendState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * EMA 추세추종 전략 전용 API
 */
@Slf4j
@RestController
@RequestMapping("/api/strategy/ema-trend")
@RequiredArgsConstructor
public class EmaTrendController {

    private final StrategyV4EmaTrend strategy;
    
    /**
     * 특정 종목 상태 조회
     */
    @GetMapping("/state/{symbol}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getState(@PathVariable String symbol) {
        EmaTrendState state = strategy.getState(symbol);
        
        Map<String, Object> result = new HashMap<>();
        result.put("symbol", symbol);
        result.put("state", state.name());
        result.put("displayName", state.getDisplayName());
        result.put("description", state.getDescription());
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * 모든 종목 상태 조회
     */
    @GetMapping("/states")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllStates() {
        Map<String, EmaTrendState> states = strategy.getAllStates();
        
        Map<String, Object> result = new HashMap<>();
        states.forEach((symbol, state) -> {
            Map<String, String> stateInfo = new HashMap<>();
            stateInfo.put("state", state.name());
            stateInfo.put("displayName", state.getDisplayName());
            result.put(symbol, stateInfo);
        });
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * 상태 초기화
     */
    @PostMapping("/reset/{symbol}")
    public ResponseEntity<ApiResponse<String>> resetState(@PathVariable String symbol) {
        strategy.resetState(symbol);
        log.info("Reset state for symbol: {}", symbol);
        return ResponseEntity.ok(ApiResponse.success("상태 초기화 완료: " + symbol));
    }
    
    /**
     * 전체 초기화
     */
    @PostMapping("/reset-all")
    public ResponseEntity<ApiResponse<String>> resetAll() {
        strategy.resetAll();
        log.info("Reset all states");
        return ResponseEntity.ok(ApiResponse.success("전체 상태 초기화 완료"));
    }
    
    /**
     * 현재 설정 조회
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<EmaTrendConfig>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success(EmaTrendConfig.defaultConfig()));
    }
    
    /**
     * 상태 머신 흐름 정보
     */
    @GetMapping("/state-flow")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStateFlow() {
        Map<String, Object> flow = new HashMap<>();
        
        // 상태 목록
        Map<String, Map<String, String>> states = new HashMap<>();
        for (EmaTrendState state : EmaTrendState.values()) {
            Map<String, String> info = new HashMap<>();
            info.put("displayName", state.getDisplayName());
            info.put("description", state.getDescription());
            states.put(state.name(), info);
        }
        flow.put("states", states);
        
        // 전이 규칙
        Map<String, String[]> transitions = new HashMap<>();
        transitions.put("FLAT", new String[]{"WAIT_PULLBACK"});
        transitions.put("WAIT_PULLBACK", new String[]{"WAIT_TRIGGER", "FLAT"});
        transitions.put("WAIT_TRIGGER", new String[]{"IN_LONG", "IN_SHORT", "FLAT"});
        transitions.put("IN_LONG", new String[]{"COOLDOWN"});
        transitions.put("IN_SHORT", new String[]{"COOLDOWN"});
        transitions.put("COOLDOWN", new String[]{"FLAT"});
        flow.put("transitions", transitions);
        
        return ResponseEntity.ok(ApiResponse.success(flow));
    }
}
