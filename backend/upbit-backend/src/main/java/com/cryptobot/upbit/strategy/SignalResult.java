package com.cryptobot.upbit.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalResult {
    private SignalType signalType;
    private List<String> reasonCodes;
    private Map<String, Object> indicatorSnapshot;
    private LocalDateTime candleTime;
    private Double entryPrice;
    private Double stopPrice;
    private Double targetPrice;

    public static SignalResult none() {
        return SignalResult.builder()
                .signalType(SignalType.NONE)
                .reasonCodes(new ArrayList<>())
                .indicatorSnapshot(new HashMap<>())
                .build();
    }

    public static SignalResult buy() {
        return SignalResult.builder()
                .signalType(SignalType.BUY)
                .reasonCodes(new ArrayList<>())
                .indicatorSnapshot(new HashMap<>())
                .build();
    }

    public static SignalResult sell() {
        return SignalResult.builder()
                .signalType(SignalType.SELL)
                .reasonCodes(new ArrayList<>())
                .indicatorSnapshot(new HashMap<>())
                .build();
    }
}
