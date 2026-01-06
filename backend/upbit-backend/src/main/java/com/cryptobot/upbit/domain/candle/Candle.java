package com.cryptobot.upbit.domain.candle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candle {
    private String market;
    private LocalDateTime candleDateTimeKst;
    private Double openingPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double tradePrice;  // Close price
    private Double candleAccTradeVolume;  // Volume

    // 편의 메서드
    public Double getClose() {
        return tradePrice;
    }

    public Double getVolume() {
        return candleAccTradeVolume;
    }

    public Double getOpen() {
        return openingPrice;
    }

    public Double getHigh() {
        return highPrice;
    }

    public Double getLow() {
        return lowPrice;
    }
}
