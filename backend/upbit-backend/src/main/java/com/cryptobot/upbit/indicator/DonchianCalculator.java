package com.cryptobot.upbit.indicator;

import com.cryptobot.upbit.domain.candle.Candle;

import java.util.ArrayList;
import java.util.List;

/**
 * Donchian Channel 계산기
 * Upper = N일 최고가
 * Lower = N일 최저가
 */
public class DonchianCalculator {

    public static DonchianResult calculate(List<Candle> candles, int length) {
        if (candles == null || candles.size() < length) {
            return DonchianResult.builder()
                    .high(new ArrayList<>())
                    .low(new ArrayList<>())
                    .build();
        }

        List<Double> highs = new ArrayList<>();
        List<Double> lows = new ArrayList<>();

        for (int i = length - 1; i < candles.size(); i++) {
            double maxHigh = Double.MIN_VALUE;
            double minLow = Double.MAX_VALUE;

            for (int j = i - length + 1; j <= i; j++) {
                maxHigh = Math.max(maxHigh, candles.get(j).getHigh());
                minLow = Math.min(minLow, candles.get(j).getLow());
            }

            highs.add(maxHigh);
            lows.add(minLow);
        }

        return DonchianResult.builder()
                .high(highs)
                .low(lows)
                .build();
    }
}
