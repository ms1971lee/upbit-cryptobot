package com.cryptobot.upbit.dto.backtest;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 데이터 동기화 요청 DTO
 */
@Data
public class DataSyncRequest {

    @NotBlank(message = "마켓 코드는 필수입니다")
    private String market;

    @NotBlank(message = "타임프레임은 필수입니다")
    private String timeframe;

    @NotNull(message = "시작일은 필수입니다")
    private String startDate;  // yyyy-MM-dd 형식

    @NotNull(message = "종료일은 필수입니다")
    private String endDate;    // yyyy-MM-dd 형식
}
