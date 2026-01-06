package com.cryptobot.upbit.strategy;

import com.cryptobot.upbit.domain.candle.Candle;
import com.cryptobot.upbit.indicator.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * V1: Donchian Breakout 전략
 * - Donchian Channel 돌파 시 매수
 * - Volume 확인
 * - ATR 기반 손절/익절
 */
@Slf4j
@Component
public class StrategyV1Donchian implements Strategy {

    private static final int ENTRY_LENGTH = 20;
    private static final int EXIT_LENGTH = 10;
    private static final int VOLUME_LENGTH = 20;
    private static final double VOLUME_MULTIPLIER = 1.2;
    private static final int ATR_LENGTH = 14;
    private static final double ATR_STOP_MULTIPLIER = 1.5;
    private static final double TP_RATIO = 2.0;

    @Override
    public String getCode() {
        return "V1";
    }

    @Override
    public String getName() {
        return "Donchian Breakout";
    }

    @Override
    public SignalResult evaluate(String symbol, List<Candle> candles5m, List<Candle> candles15m) {
        try {
            // 데이터 충분성 확인
            if (candles5m.size() < 100) {
                return SignalResult.none();
            }

            int lastIdx = candles5m.size() - 1;

            // Donchian Channel 계산 (Entry)
            DonchianResult donEntry = DonchianCalculator.calculate(candles5m, ENTRY_LENGTH);
            if (donEntry.getHigh().isEmpty()) {
                return SignalResult.none();
            }

            // Donchian Channel 계산 (Exit)
            DonchianResult donExit = DonchianCalculator.calculate(candles5m, EXIT_LENGTH);
            if (donExit.getLow().isEmpty()) {
                return SignalResult.none();
            }

            // Volume MA 계산
            List<Double> volumes = candles5m.stream()
                    .map(Candle::getVolume)
                    .collect(Collectors.toList());
            List<Double> volumeMa = VolumeMaCalculator.calculate(volumes, VOLUME_LENGTH);
            if (volumeMa.isEmpty()) {
                return SignalResult.none();
            }

            // ATR 계산
            List<Double> atr = AtrCalculator.calculate(candles5m, ATR_LENGTH);
            if (atr.isEmpty()) {
                return SignalResult.none();
            }

            // 현재 인덱스 맞추기
            int donEntryIdx = donEntry.getHigh().size() - 1;
            int donExitIdx = donExit.getLow().size() - 1;
            int volumeMaIdx = volumeMa.size() - 1;
            int atrIdx = atr.size() - 1;

            Candle currentCandle = candles5m.get(lastIdx);
            double currentClose = currentCandle.getClose();
            double currentVolume = currentCandle.getVolume();

            // 이전 Donchian High (t-1)
            double prevDonHigh = donEntryIdx > 0 ? donEntry.getHigh().get(donEntryIdx - 1) : donEntry.getHigh().get(donEntryIdx);
            double donLow = donExit.getLow().get(donExitIdx);
            double currentVolumeMa = volumeMa.get(volumeMaIdx);
            double currentAtr = atr.get(atrIdx);

            // 매수 신호 확인
            SignalResult signal = SignalResult.none();

            // 조건 A: Donchian High 돌파
            boolean breakoutUp = currentClose > prevDonHigh;

            // 조건 B: Volume 확인
            boolean volumeConfirm = currentVolume >= currentVolumeMa * VOLUME_MULTIPLIER;

            if (breakoutUp && volumeConfirm) {
                signal = SignalResult.buy();
                signal.setReasonCodes(new ArrayList<>());
                signal.getReasonCodes().add("DON_BREAK_UP");
                signal.getReasonCodes().add("VOL_CONFIRM");

                // 진입가, 손절가, 목표가 계산
                double entry = currentClose;
                double stop = entry - ATR_STOP_MULTIPLIER * currentAtr;
                double target = entry + TP_RATIO * (entry - stop);

                signal.setEntryPrice(entry);
                signal.setStopPrice(stop);
                signal.setTargetPrice(target);
                signal.setCandleTime(currentCandle.getCandleDateTimeKst());

                // Indicator Snapshot
                Map<String, Object> snapshot = new HashMap<>();
                snapshot.put("close", currentClose);
                snapshot.put("donHigh", prevDonHigh);
                snapshot.put("donLow", donLow);
                snapshot.put("volume", currentVolume);
                snapshot.put("volumeMA", currentVolumeMa);
                snapshot.put("atr", currentAtr);
                signal.setIndicatorSnapshot(snapshot);
            }

            // 매도 신호 확인 (Donchian Low 이탈)
            boolean breakoutDown = currentClose < donLow;
            if (breakoutDown) {
                signal = SignalResult.sell();
                signal.setReasonCodes(new ArrayList<>());
                signal.getReasonCodes().add("DON_BREAK_DOWN");
                signal.setCandleTime(currentCandle.getCandleDateTimeKst());

                Map<String, Object> snapshot = new HashMap<>();
                snapshot.put("close", currentClose);
                snapshot.put("donLow", donLow);
                signal.setIndicatorSnapshot(snapshot);
            }

            return signal;

        } catch (Exception e) {
            log.error("Error in StrategyV1Donchian: {}", e.getMessage(), e);
            return SignalResult.none();
        }
    }
}
