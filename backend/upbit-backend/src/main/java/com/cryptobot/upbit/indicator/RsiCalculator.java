package com.cryptobot.upbit.indicator;

import java.util.ArrayList;
import java.util.List;

/**
 * RSI (Relative Strength Index) 계산기
 * Wilder's smoothing 방식 사용
 * RSI = 100 - (100 / (1 + RS))
 * RS = Average Gain / Average Loss
 */
public class RsiCalculator {

    public static List<Double> calculate(List<Double> closes, int length) {
        if (closes == null || closes.size() < length + 1) {
            return new ArrayList<>();
        }

        List<Double> rsi = new ArrayList<>();

        // 첫 번째 평균 계산
        double sumGain = 0.0;
        double sumLoss = 0.0;

        for (int i = 1; i <= length; i++) {
            double change = closes.get(i) - closes.get(i - 1);
            if (change > 0) {
                sumGain += change;
            } else {
                sumLoss += Math.abs(change);
            }
        }

        double avgGain = sumGain / length;
        double avgLoss = sumLoss / length;

        // 첫 RSI 계산
        double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
        rsi.add(100.0 - (100.0 / (1.0 + rs)));

        // Wilder's smoothing으로 나머지 RSI 계산
        for (int i = length + 1; i < closes.size(); i++) {
            double change = closes.get(i) - closes.get(i - 1);
            double gain = change > 0 ? change : 0;
            double loss = change < 0 ? Math.abs(change) : 0;

            avgGain = (avgGain * (length - 1) + gain) / length;
            avgLoss = (avgLoss * (length - 1) + loss) / length;

            rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
            rsi.add(100.0 - (100.0 / (1.0 + rs)));
        }

        return rsi;
    }
}
