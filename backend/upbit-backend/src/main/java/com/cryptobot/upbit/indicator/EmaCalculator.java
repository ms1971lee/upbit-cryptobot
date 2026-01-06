package com.cryptobot.upbit.indicator;

import java.util.ArrayList;
import java.util.List;

/**
 * EMA (Exponential Moving Average) 계산기
 * EMA = (Close - EMA_prev) * multiplier + EMA_prev
 * multiplier = 2 / (length + 1)
 */
public class EmaCalculator {

    public static List<Double> calculate(List<Double> values, int length) {
        if (values == null || values.isEmpty() || length <= 0) {
            return new ArrayList<>();
        }

        if (values.size() < length) {
            return new ArrayList<>();
        }

        List<Double> ema = new ArrayList<>();
        double multiplier = 2.0 / (length + 1);

        // 첫 EMA는 SMA로 시작
        double sum = 0.0;
        for (int i = 0; i < length; i++) {
            sum += values.get(i);
        }
        double currentEma = sum / length;
        ema.add(currentEma);

        // 이후 EMA 계산
        for (int i = length; i < values.size(); i++) {
            currentEma = (values.get(i) - currentEma) * multiplier + currentEma;
            ema.add(currentEma);
        }

        return ema;
    }
}
