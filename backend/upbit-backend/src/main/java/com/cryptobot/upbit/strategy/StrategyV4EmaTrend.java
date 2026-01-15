package com.cryptobot.upbit.strategy;

import com.cryptobot.upbit.domain.candle.Candle;
import com.cryptobot.upbit.indicator.AtrCalculator;
import com.cryptobot.upbit.indicator.EmaCalculator;
import com.cryptobot.upbit.indicator.VolumeMaCalculator;
import com.cryptobot.upbit.strategy.ema.EmaTrendConditions;
import com.cryptobot.upbit.strategy.ema.EmaTrendConfig;
import com.cryptobot.upbit.strategy.ema.EmaTrendState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * V4: EMA + 거래량 기반 추세추종 전략
 * 
 * 핵심 원칙: 방향 예측이 아니라 손실 통제 로직
 * 
 * - 15분봉: 방향 필터 (TREND_UP / TREND_DN)
 * - 5분봉: 진입/청산
 * - 상태 머신으로 중복 신호 방지
 */
@Slf4j
@Component
public class StrategyV4EmaTrend implements Strategy {

    private final EmaTrendConfig config;
    
    // 종목별 상태 관리
    private final Map<String, EmaTrendState> stateMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> cooldownCountMap = new ConcurrentHashMap<>();
    private final Map<String, Double> entryPriceMap = new ConcurrentHashMap<>();
    private final Map<String, Double> stopPriceMap = new ConcurrentHashMap<>();
    
    public StrategyV4EmaTrend() {
        this.config = EmaTrendConfig.defaultConfig();
    }
    
    public StrategyV4EmaTrend(EmaTrendConfig config) {
        this.config = config;
    }

    @Override
    public String getCode() {
        return "V4";
    }

    @Override
    public String getName() {
        return "EMA 추세추종";
    }

    @Override
    public SignalResult evaluate(String symbol, List<Candle> candles5m, List<Candle> candles15m) {
        try {
            // 데이터 충분성 확인
            if (candles5m.size() < 100 || candles15m.size() < 60) {
                return SignalResult.none();
            }

            // 현재 상태 조회
            EmaTrendState currentState = stateMap.getOrDefault(symbol, EmaTrendState.FLAT);
            
            // ============ 지표 계산 ============
            
            // 15분봉 EMA
            List<Double> closes15 = candles15m.stream().map(Candle::getClose).collect(Collectors.toList());
            List<Double> ema20_15 = EmaCalculator.calculate(closes15, config.getEmaShortPeriod());
            List<Double> ema50_15 = EmaCalculator.calculate(closes15, config.getEmaLongPeriod());
            
            if (ema20_15.isEmpty() || ema50_15.isEmpty()) {
                return SignalResult.none();
            }
            
            // 5분봉 EMA
            List<Double> closes5 = candles5m.stream().map(Candle::getClose).collect(Collectors.toList());
            List<Double> ema20_5 = EmaCalculator.calculate(closes5, config.getEmaShortPeriod());
            List<Double> ema50_5 = EmaCalculator.calculate(closes5, config.getEmaLongPeriod());
            
            if (ema20_5.isEmpty() || ema50_5.isEmpty()) {
                return SignalResult.none();
            }
            
            // 거래량 MA (5분봉)
            List<Double> volumes5 = candles5m.stream().map(Candle::getVolume).collect(Collectors.toList());
            List<Double> volumeMa5 = VolumeMaCalculator.calculate(volumes5, config.getVolumeMaPeriod());
            
            if (volumeMa5.isEmpty()) {
                return SignalResult.none();
            }
            
            // ATR (5분봉)
            List<Double> atr5 = AtrCalculator.calculate(candles5m, config.getAtrPeriod());
            if (atr5.isEmpty()) {
                return SignalResult.none();
            }

            // ============ 현재 값 추출 ============
            
            int idx15 = ema20_15.size() - 1;
            int idx5 = ema20_5.size() - 1;
            int volIdx = volumeMa5.size() - 1;
            int atrIdx = atr5.size() - 1;
            int candleIdx5 = candles5m.size() - 1;
            int candleIdx15 = candles15m.size() - 1;
            
            double currentEma20_15 = ema20_15.get(idx15);
            double currentEma50_15 = ema50_15.get(Math.min(idx15, ema50_15.size() - 1));
            double currentEma20_5 = ema20_5.get(idx5);
            double currentEma50_5 = ema50_5.get(Math.min(idx5, ema50_5.size() - 1));
            double currentVolumeMa5 = volumeMa5.get(volIdx);
            double currentAtr = atr5.get(atrIdx);
            
            Candle currentCandle5 = candles5m.get(candleIdx5);
            Candle prevCandle5 = candles5m.get(candleIdx5 - 1);
            Candle currentCandle15 = candles15m.get(candleIdx15);
            
            double close5 = currentCandle5.getClose();
            double high5 = currentCandle5.getHigh();
            double low5 = currentCandle5.getLow();
            double volume5 = currentCandle5.getVolume();
            double close15 = currentCandle15.getClose();
            double prevHigh5 = prevCandle5.getHigh();
            double prevLow5 = prevCandle5.getLow();
            
            // 슬로프 계산
            double slopeEma20_15 = EmaTrendConditions.calculateSlope(ema20_15, config.getSlopePeriod());
            double slopeEma50_15 = EmaTrendConditions.calculateSlope(ema50_15, config.getSlopePeriod());
            
            // 스프레드 계산
            double spread15 = currentEma20_15 - currentEma50_15;
            double spread5 = currentEma20_5 - currentEma50_5;

            // ============ 조건 체크 ============
            
            // 15분봉 추세 판단
            boolean trendUp = EmaTrendConditions.checkTrendUp(
                currentEma20_15, currentEma50_15, slopeEma20_15, slopeEma50_15, close15);
            boolean trendDown = EmaTrendConditions.checkTrendDown(
                currentEma20_15, currentEma50_15, slopeEma20_15, slopeEma50_15, close15);
            boolean trendNone = !trendUp && !trendDown;
            
            // EMA 엉킴 체크
            boolean maChop = EmaTrendConditions.checkMaChop(spread5, spread15, config.getSpreadChopMin());
            
            // 5분봉 조건
            boolean pullbackLong = EmaTrendConditions.checkPullbackLong(
                trendUp, low5, close5, currentEma20_5, volume5, currentVolumeMa5, config.getPullbackTolerance());
            boolean pullbackShort = EmaTrendConditions.checkPullbackShort(
                trendDown, high5, close5, currentEma20_5, volume5, currentVolumeMa5, config.getPullbackTolerance());
            boolean triggerLong = EmaTrendConditions.checkTriggerLong(
                trendUp, close5, currentEma20_5, prevHigh5, volume5, currentVolumeMa5, config.getVolumeMultiplier());
            boolean triggerShort = EmaTrendConditions.checkTriggerShort(
                trendDown, close5, currentEma20_5, prevLow5, volume5, currentVolumeMa5, config.getVolumeMultiplier());
            boolean filterLong = EmaTrendConditions.checkFilterLong(
                close5, currentEma20_5, currentEma50_5, config.getSpreadMin());
            boolean filterShort = EmaTrendConditions.checkFilterShort(
                close5, currentEma20_5, currentEma50_5, config.getSpreadMin());

            // ============ 상태 전이 및 신호 생성 ============
            
            SignalResult signal = SignalResult.none();
            EmaTrendState nextState = currentState;
            
            // 쿨다운 처리
            if (currentState == EmaTrendState.COOLDOWN) {
                int cooldownCount = cooldownCountMap.getOrDefault(symbol, 0);
                if (cooldownCount >= config.getCooldownBars()) {
                    nextState = EmaTrendState.FLAT;
                    cooldownCountMap.put(symbol, 0);
                } else {
                    cooldownCountMap.put(symbol, cooldownCount + 1);
                }
            }
            
            // 추세 없음 또는 EMA 엉킴 → FLAT
            if (trendNone || maChop) {
                nextState = EmaTrendState.FLAT;
            }
            
            // FLAT → WAIT_PULLBACK
            if (currentState == EmaTrendState.FLAT && (trendUp || trendDown) && !maChop) {
                nextState = EmaTrendState.WAIT_PULLBACK;
                log.info("[{}] 추세 감지: {}", symbol, trendUp ? "상승" : "하락");
            }
            
            // WAIT_PULLBACK → WAIT_TRIGGER
            if (currentState == EmaTrendState.WAIT_PULLBACK) {
                if (pullbackLong || pullbackShort) {
                    nextState = EmaTrendState.WAIT_TRIGGER;
                    log.info("[{}] 눌림 감지: {}", symbol, pullbackLong ? "롱" : "숏");
                }
            }
            
            // WAIT_TRIGGER → IN_LONG / IN_SHORT (진입!)
            if (currentState == EmaTrendState.WAIT_TRIGGER) {
                if (triggerLong && filterLong && trendUp) {
                    // 롱 진입
                    nextState = EmaTrendState.IN_LONG;
                    
                    double entry = close5;
                    double stop = entry - config.getStopAtrMultiplier() * currentAtr;
                    double rValue = entry - stop;
                    double target = entry + config.getTakeProfitRatio() * rValue;
                    
                    entryPriceMap.put(symbol, entry);
                    stopPriceMap.put(symbol, stop);
                    
                    signal = SignalResult.buy();
                    signal.setEntryPrice(entry);
                    signal.setStopPrice(stop);
                    signal.setTargetPrice(target);
                    signal.setCandleTime(currentCandle5.getCandleDateTimeKst());
                    signal.setReasonCodes(new ArrayList<>(Arrays.asList(
                        "TREND_UP", "PULLBACK_DONE", "TRIGGER_LONG", "FILTER_PASS"
                    )));
                    
                    log.info("[{}] 롱 진입 신호! Entry: {}, Stop: {}, Target: {}", 
                        symbol, entry, stop, target);
                }
                else if (triggerShort && filterShort && trendDown) {
                    // 숏 진입
                    nextState = EmaTrendState.IN_SHORT;
                    
                    double entry = close5;
                    double stop = entry + config.getStopAtrMultiplier() * currentAtr;
                    double rValue = stop - entry;
                    double target = entry - config.getTakeProfitRatio() * rValue;
                    
                    entryPriceMap.put(symbol, entry);
                    stopPriceMap.put(symbol, stop);
                    
                    signal = SignalResult.sell();
                    signal.setEntryPrice(entry);
                    signal.setStopPrice(stop);
                    signal.setTargetPrice(target);
                    signal.setCandleTime(currentCandle5.getCandleDateTimeKst());
                    signal.setReasonCodes(new ArrayList<>(Arrays.asList(
                        "TREND_DN", "PULLBACK_DONE", "TRIGGER_SHORT", "FILTER_PASS"
                    )));
                    
                    log.info("[{}] 숏 진입 신호! Entry: {}, Stop: {}, Target: {}", 
                        symbol, entry, stop, target);
                }
            }
            
            // IN_LONG → 청산 체크
            if (currentState == EmaTrendState.IN_LONG) {
                Double entryPrice = entryPriceMap.get(symbol);
                Double stopPrice = stopPriceMap.get(symbol);
                
                if (entryPrice != null && stopPrice != null) {
                    boolean stopHit = EmaTrendConditions.checkStopLong(close5, stopPrice, currentEma50_5);
                    boolean trendExit = EmaTrendConditions.checkTrendExitLong(close5, currentEma20_5);
                    boolean twoRed = EmaTrendConditions.checkTwoRedCandles(currentCandle5, prevCandle5);
                    
                    if (stopHit || (twoRed && close5 < currentEma20_5)) {
                        nextState = EmaTrendState.COOLDOWN;
                        signal = SignalResult.sell();
                        signal.setReasonCodes(new ArrayList<>(Arrays.asList(
                            stopHit ? "STOP_LOSS" : "TWO_RED_EXIT"
                        )));
                        signal.setCandleTime(currentCandle5.getCandleDateTimeKst());
                        log.info("[{}] 롱 손절/청산", symbol);
                    }
                    else if (trendExit) {
                        nextState = EmaTrendState.COOLDOWN;
                        signal = SignalResult.sell();
                        signal.setReasonCodes(new ArrayList<>(Collections.singletonList("TREND_EXIT")));
                        signal.setCandleTime(currentCandle5.getCandleDateTimeKst());
                        log.info("[{}] 롱 추세 종료 청산", symbol);
                    }
                }
            }
            
            // IN_SHORT → 청산 체크
            if (currentState == EmaTrendState.IN_SHORT) {
                Double entryPrice = entryPriceMap.get(symbol);
                Double stopPrice = stopPriceMap.get(symbol);
                
                if (entryPrice != null && stopPrice != null) {
                    boolean stopHit = EmaTrendConditions.checkStopShort(close5, stopPrice, currentEma50_5);
                    boolean trendExit = EmaTrendConditions.checkTrendExitShort(close5, currentEma20_5);
                    boolean twoGreen = EmaTrendConditions.checkTwoGreenCandles(currentCandle5, prevCandle5);
                    
                    if (stopHit || (twoGreen && close5 > currentEma20_5)) {
                        nextState = EmaTrendState.COOLDOWN;
                        signal = SignalResult.buy();  // 숏 청산 = 매수
                        signal.setReasonCodes(new ArrayList<>(Arrays.asList(
                            stopHit ? "STOP_LOSS" : "TWO_GREEN_EXIT"
                        )));
                        signal.setCandleTime(currentCandle5.getCandleDateTimeKst());
                        log.info("[{}] 숏 손절/청산", symbol);
                    }
                    else if (trendExit) {
                        nextState = EmaTrendState.COOLDOWN;
                        signal = SignalResult.buy();  // 숏 청산 = 매수
                        signal.setReasonCodes(new ArrayList<>(Collections.singletonList("TREND_EXIT")));
                        signal.setCandleTime(currentCandle5.getCandleDateTimeKst());
                        log.info("[{}] 숏 추세 종료 청산", symbol);
                    }
                }
            }
            
            // 상태 업데이트
            stateMap.put(symbol, nextState);
            
            // Indicator Snapshot
            if (signal.getSignalType() != SignalType.NONE) {
                Map<String, Object> snapshot = new HashMap<>();
                snapshot.put("state", nextState.name());
                snapshot.put("prevState", currentState.name());
                snapshot.put("ema20_15", currentEma20_15);
                snapshot.put("ema50_15", currentEma50_15);
                snapshot.put("ema20_5", currentEma20_5);
                snapshot.put("ema50_5", currentEma50_5);
                snapshot.put("spread15", spread15);
                snapshot.put("spread5", spread5);
                snapshot.put("slopeEma20", slopeEma20_15);
                snapshot.put("slopeEma50", slopeEma50_15);
                snapshot.put("close5", close5);
                snapshot.put("volume5", volume5);
                snapshot.put("volumeMa5", currentVolumeMa5);
                snapshot.put("atr", currentAtr);
                snapshot.put("trendUp", trendUp);
                snapshot.put("trendDown", trendDown);
                signal.setIndicatorSnapshot(snapshot);
            }
            
            return signal;

        } catch (Exception e) {
            log.error("Error in StrategyV4EmaTrend: {}", e.getMessage(), e);
            return SignalResult.none();
        }
    }
    
    /**
     * 특정 종목의 현재 상태 조회
     */
    public EmaTrendState getState(String symbol) {
        return stateMap.getOrDefault(symbol, EmaTrendState.FLAT);
    }
    
    /**
     * 모든 종목 상태 조회
     */
    public Map<String, EmaTrendState> getAllStates() {
        return new HashMap<>(stateMap);
    }
    
    /**
     * 상태 초기화
     */
    public void resetState(String symbol) {
        stateMap.put(symbol, EmaTrendState.FLAT);
        cooldownCountMap.remove(symbol);
        entryPriceMap.remove(symbol);
        stopPriceMap.remove(symbol);
    }
    
    /**
     * 전체 초기화
     */
    public void resetAll() {
        stateMap.clear();
        cooldownCountMap.clear();
        entryPriceMap.clear();
        stopPriceMap.clear();
    }
}
