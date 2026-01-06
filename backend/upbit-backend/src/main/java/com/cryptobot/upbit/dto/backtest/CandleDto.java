package com.cryptobot.upbit.dto.backtest;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 캔들 데이터 DTO
 */
@Data
@Builder
public class CandleDto {

    private String market;
    private String timeframe;
    private LocalDateTime timestamp;
    private Double openingPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double closingPrice;
    private Double volume;
    private Double accTradePrice;
}
