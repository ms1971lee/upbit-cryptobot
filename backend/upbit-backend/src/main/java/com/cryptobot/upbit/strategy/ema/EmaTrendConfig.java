package com.cryptobot.upbit.strategy.ema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * EMA 추세추종 전략 파라미터 설정
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmaTrendConfig {
    
    /** 눌림 허용 오차 (기본값: 0.2%) */
    @Builder.Default
    private double pullbackTolerance = 0.002;
    
    /** 거래량 배수 (기본값: 1.3배) */
    @Builder.Default
    private double volumeMultiplier = 1.3;
    
    /** EMA 스프레드 최소값 (기본값: 0.001) */
    @Builder.Default
    private double spreadMin = 0.001;
    
    /** EMA 엉킴 판단 최소값 (기본값: 0.0005) */
    @Builder.Default
    private double spreadChopMin = 0.0005;
    
    /** 쿨다운 캔들 수 (기본값: 3) */
    @Builder.Default
    private int cooldownBars = 3;
    
    /** 슬로프 계산 기간 (기본값: 3) */
    @Builder.Default
    private int slopePeriod = 3;
    
    /** EMA 단기 기간 */
    @Builder.Default
    private int emaShortPeriod = 20;
    
    /** EMA 장기 기간 */
    @Builder.Default
    private int emaLongPeriod = 50;
    
    /** 거래량 MA 기간 */
    @Builder.Default
    private int volumeMaPeriod = 20;
    
    /** ATR 기간 */
    @Builder.Default
    private int atrPeriod = 14;
    
    /** 손절 ATR 배수 */
    @Builder.Default
    private double stopAtrMultiplier = 1.5;
    
    /** 1차 익절 R 배수 */
    @Builder.Default
    private double takeProfitRatio = 1.0;
    
    /** 1차 익절 시 청산 비율 (50%) */
    @Builder.Default
    private double partialCloseRatio = 0.5;
    
    /** 일일 손실 제한 (%) */
    @Builder.Default
    private double dailyLossLimitPercent = 3.0;
    
    /** 연속 손실 제한 횟수 */
    @Builder.Default
    private int maxLossStreak = 2;
    
    /**
     * 기본 설정 반환
     */
    public static EmaTrendConfig defaultConfig() {
        return EmaTrendConfig.builder().build();
    }
}
