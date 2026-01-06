package com.cryptobot.upbit.indicator;

import java.util.ArrayList;
import java.util.List;

/**
 * MACD (Moving Average Convergence Divergence) 계산기
 * MACD Line = EMA(fast) - EMA(slow)
 * Signal Line = EMA(MACD, signal)
 * Histogram = MACD Line - Signal Line
 */
public class MacdCalculator {

    public static MacdResult calculate(List<Double> closes, int fast, int slow, int signal) {
        if (closes == null || closes.size() < slow) {
            return MacdResult.builder()
                    .macdLine(new ArrayList<>())
                    .signalLine(new ArrayList<>())
                    .histogram(new ArrayList<>())
                    .build();
        }

        // 1. Fast EMA 계산
        List<Double> emaFast = EmaCalculator.calculate(closes, fast);

        // 2. Slow EMA 계산
        List<Double> emaSlow = EmaCalculator.calculate(closes, slow);

        if (emaFast.isEmpty() || emaSlow.isEmpty()) {
            return MacdResult.builder()
                    .macdLine(new ArrayList<>())
                    .signalLine(new ArrayList<>())
                    .histogram(new ArrayList<>())
                    .build();
        }

        // 3. MACD Line = Fast EMA - Slow EMA
        // emaSlow가 더 늦게 시작하므로, emaFast를 맞춰줘야 함
        int offset = slow - fast;
        List<Double> macdLine = new ArrayList<>();

        for (int i = 0; i < emaSlow.size(); i++) {
            macdLine.add(emaFast.get(i + offset) - emaSlow.get(i));
        }

        // 4. Signal Line = EMA(MACD, signal)
        List<Double> signalLine = EmaCalculator.calculate(macdLine, signal);

        // 5. Histogram = MACD Line - Signal Line
        List<Double> histogram = new ArrayList<>();
        int signalOffset = signal - 1;

        for (int i = 0; i < signalLine.size(); i++) {
            histogram.add(macdLine.get(i + signalOffset) - signalLine.get(i));
        }

        return MacdResult.builder()
                .macdLine(macdLine)
                .signalLine(signalLine)
                .histogram(histogram)
                .build();
    }
}
