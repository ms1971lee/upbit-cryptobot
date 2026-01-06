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
 * V3: Reversal 전략
 * - EMA 이격 확인
 * - RSI 과매도 확인
 * - MACD Histogram 녹색 전환
 */
@Slf4j
@Component
public class StrategyV3Reversal implements Strategy {

    private static final int EMA_LENGTH = 25;
    private static final double GAP_PCT_THRESHOLD = 20.0;
    private static final int RSI_LENGTH = 14;
    private static final double RSI_OVERSOLD = 30.0;
    private static final int MACD_FAST = 12;
    private static final int MACD_SLOW = 26;
    private static final int MACD_SIGNAL = 9;
    private static final int ATR_LENGTH = 14;
    private static final double ATR_STOP_MULTIPLIER = 1.5;
    private static final double TP_RATIO = 3.0;

    @Override
    public String getCode() {
        return "V3";
    }

    @Override
    public String getName() {
        return "Reversal";
    }

    @Override
    public SignalResult evaluate(String symbol, List<Candle> candles5m, List<Candle> candles15m) {
        try {
            if (candles5m.size() < 100) {
                return SignalResult.none();
            }

            int lastIdx = candles5m.size() - 1;

            // EMA 계산
            List<Double> closes = candles5m.stream()
                    .map(Candle::getClose)
                    .collect(Collectors.toList());
            List<Double> ema = EmaCalculator.calculate(closes, EMA_LENGTH);
            if (ema.isEmpty()) {
                return SignalResult.none();
            }

            // RSI 계산
            List<Double> rsi = RsiCalculator.calculate(closes, RSI_LENGTH);
            if (rsi.isEmpty()) {
                return SignalResult.none();
            }

            // MACD 계산
            MacdResult macdResult = MacdCalculator.calculate(closes, MACD_FAST, MACD_SLOW, MACD_SIGNAL);
            if (macdResult.getHistogram().isEmpty()) {
                return SignalResult.none();
            }

            // ATR 계산
            List<Double> atr = AtrCalculator.calculate(candles5m, ATR_LENGTH);
            if (atr.isEmpty()) {
                return SignalResult.none();
            }

            // 현재 값들
            double currentClose = candles5m.get(lastIdx).getClose();
            double currentEma = ema.get(ema.size() - 1);
            double currentRsi = rsi.get(rsi.size() - 1);

            List<Double> histogram = macdResult.getHistogram();
            double currentHist = histogram.get(histogram.size() - 1);
            double prevHist = histogram.size() > 1 ? histogram.get(histogram.size() - 2) : 0.0;

            double currentAtr = atr.get(atr.size() - 1);

            // 조건 A: EMA 이격 (가격이 EMA보다 아래)
            double gapPct = ((currentEma - currentClose) / currentEma) * 100;
            boolean gapOk = gapPct >= GAP_PCT_THRESHOLD;

            // 조건 B: RSI 과매도
            boolean rsiOversold = currentRsi <= RSI_OVERSOLD;

            // 조건 C: MACD Histogram 녹색 전환 (음 → 양)
            boolean histCrossUp = prevHist <= 0 && currentHist > 0;

            // 매수 신호
            if (gapOk && rsiOversold && histCrossUp) {
                SignalResult signal = SignalResult.buy();
                signal.setReasonCodes(new ArrayList<>());
                signal.getReasonCodes().add("GAP_OK");
                signal.getReasonCodes().add("RSI_OVERSOLD");
                signal.getReasonCodes().add("HIST_CROSS_UP");

                double entry = currentClose;
                double stop = entry - ATR_STOP_MULTIPLIER * currentAtr;
                double target = entry + TP_RATIO * (entry - stop);

                signal.setEntryPrice(entry);
                signal.setStopPrice(stop);
                signal.setTargetPrice(target);
                signal.setCandleTime(candles5m.get(lastIdx).getCandleDateTimeKst());

                Map<String, Object> snapshot = new HashMap<>();
                snapshot.put("close", currentClose);
                snapshot.put("ema25", currentEma);
                snapshot.put("gapPct", gapPct);
                snapshot.put("rsi14", currentRsi);
                snapshot.put("histogram", currentHist);
                snapshot.put("histogramPrev", prevHist);
                snapshot.put("atr14", currentAtr);
                snapshot.put("stop", stop);
                snapshot.put("target", target);
                signal.setIndicatorSnapshot(snapshot);

                return signal;
            }

            // 매도 신호: Histogram 빨간색 전환 (양 → 음)
            boolean histCrossDown = prevHist >= 0 && currentHist < 0;
            if (histCrossDown) {
                SignalResult signal = SignalResult.sell();
                signal.setReasonCodes(new ArrayList<>());
                signal.getReasonCodes().add("HIST_CROSS_DOWN_EXIT");
                signal.setCandleTime(candles5m.get(lastIdx).getCandleDateTimeKst());

                Map<String, Object> snapshot = new HashMap<>();
                snapshot.put("histogram", currentHist);
                snapshot.put("histogramPrev", prevHist);
                signal.setIndicatorSnapshot(snapshot);

                return signal;
            }

            return SignalResult.none();

        } catch (Exception e) {
            log.error("Error in StrategyV3Reversal: {}", e.getMessage(), e);
            return SignalResult.none();
        }
    }
}
