package com.cryptobot.upbit.dto.trading;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequest {

    @NotBlank(message = "마켓 정보는 필수입니다")
    private String market; // 예: KRW-BTC

    @NotBlank(message = "주문 타입은 필수입니다")
    private String orderType; // BUY or SELL

    @NotNull(message = "가격은 필수입니다")
    @Positive(message = "가격은 양수여야 합니다")
    private Double price; // 주문 가격

    @NotNull(message = "수량은 필수입니다")
    @Positive(message = "수량은 양수여야 합니다")
    private Double volume; // 주문 수량

    private String strategy; // 사용된 전략 (V1, V2, V3)

    private String memo; // 메모
}
