package com.cryptobot.upbit.strategy.ema;

import com.cryptobot.upbit.domain.candle.Candle;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * EMA 추세추종 전략 조건 체크 유틸리티
 */
public class EmaTrendConditions {
    
    /**
     * 조건 체크 결과
     */
    @Data
    @Builder
    public static class ConditionResult {
        private boolean trendUp;
        private boolean trendDown;
        private boolean trendNone;
        private boolean pullbackLong;
        private boolean pullbackShort;
        private boolean triggerLong;
        private boolean triggerShort;
        private boolean filterLong;
        private boolean filterShort;
        private boolean maChop;
        
        // 현재 지표 값
        private double ema20_15;
        private double ema50_15;
        private double ema20_5;
        private double ema50_5;
        private double spread15;
        private double spread5;
        private double slope20_15;
        private double slope50_15;
        private double currentClose15;
        private double currentClose5;
        private double currentVolume5;
        private double volumeMa5;
    }
    
    /**
     * 슬로프 계산: x[t] - x[t-period]
     */
    public static double calculateSlope(List<Double> values, int period) {
        if (values.size() < period + 1) {
            return 0.0;
        }
        int lastIdx = values.size() - 1;
        return values.get(lastIdx) - values.get(lastIdx - period);
    }
    
    /**
     * 15분봉 상승 추세 체크
     * TREND_UP = EMA20 > EMA50 AND slope(EMA20) > 0 AND slope(EMA50) > 0 AND C > EMA20
     */
    public static boolean checkTrendUp(
            double ema20, double ema50, 
            double slopeEma20, double slopeEma50, 
            double close) {
        return ema20 > ema50 
            && slopeEma20 > 0 
            && slopeEma50 > 0 
            && close > ema20;
    }
    
    /**
     * 15분봉 하락 추세 체크
     * TREND_DN = EMA20 < EMA50 AND slope(EMA20) < 0 AND slope(EMA50) < 0 AND C < EMA20
     */
    public static boolean checkTrendDown(
            double ema20, double ema50, 
            double slopeEma20, double slopeEma50, 
            double close) {
        return ema20 < ema50 
            && slopeEma20 < 0 
            && slopeEma50 < 0 
            && close < ema20;
    }
    
    /**
     * 롱 눌림 감지 (5분봉)
     * PULLBACK_LONG = TREND_UP AND L <= EMA20*(1+tol) AND C >= EMA20*(1-tol) AND VOL < VOL_MA
     */
    public static boolean checkPullbackLong(
            boolean trendUp,
            double low, double close, double ema20,
            double volume, double volumeMa,
            double tolerance) {
        if (!trendUp) return false;
        
        double upperBound = ema20 * (1 + tolerance);
        double lowerBound = ema20 * (1 - tolerance);
        
        return low <= upperBound 
            && close >= lowerBound 
            && volume < volumeMa;
    }
    
    /**
     * 숏 눌림 감지 (5분봉)
     * PULLBACK_SHORT = TREND_DN AND H >= EMA20*(1-tol) AND C <= EMA20*(1+tol) AND VOL < VOL_MA
     */
    public static boolean checkPullbackShort(
            boolean trendDown,
            double high, double close, double ema20,
            double volume, double volumeMa,
            double tolerance) {
        if (!trendDown) return false;
        
        double upperBound = ema20 * (1 + tolerance);
        double lowerBound = ema20 * (1 - tolerance);
        
        return high >= lowerBound 
            && close <= upperBound 
            && volume < volumeMa;
    }
    
    /**
     * 롱 트리거 조건 (5분봉)
     * TRIGGER_LONG = TREND_UP AND C > EMA20 AND C > H_prev AND VOL >= VOL_MA * mult
     */
    public static boolean checkTriggerLong(
            boolean trendUp,
            double close, double ema20, double prevHigh,
            double volume, double volumeMa,
            double volumeMultiplier) {
        if (!trendUp) return false;
        
        return close > ema20 
            && close > prevHigh 
            && volume >= volumeMa * volumeMultiplier;
    }
    
    /**
     * 숏 트리거 조건 (5분봉)
     * TRIGGER_SHORT = TREND_DN AND C < EMA20 AND C < L_prev AND VOL >= VOL_MA * mult
     */
    public static boolean checkTriggerShort(
            boolean trendDown,
            double close, double ema20, double prevLow,
            double volume, double volumeMa,
            double volumeMultiplier) {
        if (!trendDown) return false;
        
        return close < ema20 
            && close < prevLow 
            && volume >= volumeMa * volumeMultiplier;
    }
    
    /**
     * 롱 구조 필터 (5분봉)
     * FILTER_LONG = spread > spread_min AND C > EMA50
     */
    public static boolean checkFilterLong(
            double close, double ema20, double ema50, double spreadMin) {
        double spread = ema20 - ema50;
        return spread > spreadMin && close > ema50;
    }
    
    /**
     * 숏 구조 필터 (5분봉)
     * FILTER_SHORT = spread < -spread_min AND C < EMA50
     */
    public static boolean checkFilterShort(
            double close, double ema20, double ema50, double spreadMin) {
        double spread = ema20 - ema50;
        return spread < -spreadMin && close < ema50;
    }
    
    /**
     * EMA 엉킴 체크
     * MA_CHOP = abs(spread_5) < chop_min OR abs(spread_15) < chop_min
     */
    public static boolean checkMaChop(
            double spread5, double spread15, double chopMin) {
        return Math.abs(spread5) < chopMin || Math.abs(spread15) < chopMin;
    }
    
    /**
     * 롱 손절 체크
     * STOP_LONG = C < stop_price OR C < EMA50
     */
    public static boolean checkStopLong(double close, double stopPrice, double ema50) {
        return close < stopPrice || close < ema50;
    }
    
    /**
     * 숏 손절 체크
     * STOP_SHORT = C > stop_price OR C > EMA50
     */
    public static boolean checkStopShort(double close, double stopPrice, double ema50) {
        return close > stopPrice || close > ema50;
    }
    
    /**
     * 2연속 반대 캔들 체크 (롱용 - 음봉 2개)
     */
    public static boolean checkTwoRedCandles(Candle current, Candle prev) {
        boolean currentRed = current.getClose() < current.getOpen();
        boolean prevRed = prev.getClose() < prev.getOpen();
        return currentRed && prevRed;
    }
    
    /**
     * 2연속 반대 캔들 체크 (숏용 - 양봉 2개)
     */
    public static boolean checkTwoGreenCandles(Candle current, Candle prev) {
        boolean currentGreen = current.getClose() > current.getOpen();
        boolean prevGreen = prev.getClose() > prev.getOpen();
        return currentGreen && prevGreen;
    }
    
    /**
     * 추세 종료 체크 (롱)
     * TREND_EXIT_LONG = C < EMA20
     */
    public static boolean checkTrendExitLong(double close, double ema20) {
        return close < ema20;
    }
    
    /**
     * 추세 종료 체크 (숏)
     * TREND_EXIT_SHORT = C > EMA20
     */
    public static boolean checkTrendExitShort(double close, double ema20) {
        return close > ema20;
    }
}
