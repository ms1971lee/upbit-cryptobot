package com.cryptobot.upbit.dto.backtest;

import lombok.Builder;
import lombok.Data;

/**
 * 데이터 동기화 응답 DTO
 */
@Data
@Builder
public class DataSyncResponse {

    private boolean success;
    private String message;
    private String taskId;
    private Integer estimatedRecords;
}
