package com.cryptobot.upbit.model.backtest;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 캔들 데이터 모델 (백테스트용)
 */
@Data
@Builder
public class Candle {

    private LocalDateTime timestamp;
    private Double openingPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double closingPrice;
    private Double volume;
    private Double accTradePrice;
}
