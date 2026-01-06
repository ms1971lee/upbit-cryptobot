package com.cryptobot.upbit.scanner.dto;

import com.cryptobot.upbit.strategy.SignalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalScanResult {
    private String market;              // "KRW-BTC"
    private String coinName;            // "비트코인"
    private Double currentPrice;        // 현재가
    private Double changeRate;          // 전일대비 %
    private SignalType signal;          // BUY, SELL, NONE
    private List<String> reasonCodes;   // 신호 근거 코드
    private Map<String, Object> indicators;  // 주요 지표값
    private LocalDateTime scanTime;     // 스캔 시각
}
