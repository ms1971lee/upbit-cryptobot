package com.cryptobot.upbit.dto.backtest;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 사용 가능한 시장 데이터 DTO
 */
@Data
@Builder
public class AvailableMarketData {

    private String market;
    private String timeframe;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long recordCount;
}
