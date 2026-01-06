package com.cryptobot.upbit.dto.backtest;

import lombok.Builder;
import lombok.Data;

/**
 * 데이터 동기화 상태 DTO
 */
@Data
@Builder
public class DataSyncStatus {

    private String taskId;
    private String status;  // IN_PROGRESS, COMPLETED, FAILED
    private Double progress;  // 0.0 ~ 100.0
    private Integer recordsProcessed;
    private Integer totalRecords;
    private String message;
}
