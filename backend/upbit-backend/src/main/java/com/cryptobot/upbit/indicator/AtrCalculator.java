package com.cryptobot.upbit.indicator;

import com.cryptobot.upbit.domain.candle.Candle;

import java.util.ArrayList;
import java.util.List;

/**
 * ATR (Average True Range) 계산기
 * True Range = max(high - low, abs(high - prev_close), abs(low - prev_close))
 * ATR = Wilder's smoothing of True Range
 */
public class AtrCalculator {

    public static List<Double> calculate(List<Candle> candles, int length) {
        if (candles == null || candles.size() < length + 1) {
            return new ArrayList<>();
        }

        List<Double> atr = new ArrayList<>();

        // True Range 계산
        List<Double> trueRanges = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            Candle current = candles.get(i);
            Candle previous = candles.get(i - 1);

            double tr1 = current.getHigh() - current.getLow();
            double tr2 = Math.abs(current.getHigh() - previous.getClose());
            double tr3 = Math.abs(current.getLow() - previous.getClose());

            double trueRange = Math.max(tr1, Math.max(tr2, tr3));
            trueRanges.add(trueRange);
        }

        if (trueRanges.size() < length) {
            return new ArrayList<>();
        }

        // 첫 ATR은 True Range의 평균
        double sum = 0.0;
        for (int i = 0; i < length; i++) {
            sum += trueRanges.get(i);
        }
        double currentAtr = sum / length;
        atr.add(currentAtr);

        // Wilder's smoothing으로 ATR 계산
        for (int i = length; i < trueRanges.size(); i++) {
            currentAtr = (currentAtr * (length - 1) + trueRanges.get(i)) / length;
            atr.add(currentAtr);
        }

        return atr;
    }
}
