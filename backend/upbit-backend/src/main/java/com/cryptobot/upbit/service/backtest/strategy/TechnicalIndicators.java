package com.cryptobot.upbit.service.backtest.strategy;

import com.cryptobot.upbit.model.backtest.Candle;

import java.util.ArrayList;
import java.util.List;

/**
 * 기술적 지표 계산 유틸리티
 */
public class TechnicalIndicators {

    /**
     * 단순 이동평균(SMA) 계산
     *
     * @param candles 캔들 데이터
     * @param period 기간
     * @return 각 시점의 SMA 값 (period 이전 시점은 null)
     */
    public static List<Double> calculateSMA(List<Candle> candles, int period) {
        List<Double> sma = new ArrayList<>();

        for (int i = 0; i < candles.size(); i++) {
            if (i < period - 1) {
                sma.add(null);
                continue;
            }

            double sum = 0.0;
            for (int j = 0; j < period; j++) {
                sum += candles.get(i - j).getClosingPrice();
            }
            sma.add(sum / period);
        }

        return sma;
    }

    /**
     * RSI (Relative Strength Index) 계산
     *
     * @param candles 캔들 데이터
     * @param period 기간 (일반적으로 14)
     * @return 각 시점의 RSI 값 (period 이전 시점은 null)
     */
    public static List<Double> calculateRSI(List<Candle> candles, int period) {
        List<Double> rsi = new ArrayList<>();

        if (candles.size() < period + 1) {
            for (int i = 0; i < candles.size(); i++) {
                rsi.add(null);
            }
            return rsi;
        }

        // 첫 번째 평균 상승/하락 계산
        double avgGain = 0.0;
        double avgLoss = 0.0;

        for (int i = 1; i <= period; i++) {
            double change = candles.get(i).getClosingPrice() - candles.get(i - 1).getClosingPrice();
            if (change > 0) {
                avgGain += change;
            } else {
                avgLoss += Math.abs(change);
            }
        }

        avgGain /= period;
        avgLoss /= period;

        // 첫 period개는 null
        for (int i = 0; i < period; i++) {
            rsi.add(null);
        }

        // 첫 RSI 계산
        if (avgLoss == 0) {
            rsi.add(100.0);
        } else {
            double rs = avgGain / avgLoss;
            rsi.add(100.0 - (100.0 / (1.0 + rs)));
        }

        // 나머지 RSI 계산 (Wilder's smoothing)
        for (int i = period + 1; i < candles.size(); i++) {
            double change = candles.get(i).getClosingPrice() - candles.get(i - 1).getClosingPrice();
            double gain = change > 0 ? change : 0;
            double loss = change < 0 ? Math.abs(change) : 0;

            avgGain = ((avgGain * (period - 1)) + gain) / period;
            avgLoss = ((avgLoss * (period - 1)) + loss) / period;

            if (avgLoss == 0) {
                rsi.add(100.0);
            } else {
                double rs = avgGain / avgLoss;
                rsi.add(100.0 - (100.0 / (1.0 + rs)));
            }
        }

        return rsi;
    }

    /**
     * 볼린저 밴드 계산
     *
     * @param candles 캔들 데이터
     * @param period 기간 (일반적으로 20)
     * @param stdDevMultiplier 표준편차 배수 (일반적으로 2)
     * @return [상단밴드, 중간밴드(SMA), 하단밴드]의 리스트
     */
    public static BollingerBands calculateBollingerBands(List<Candle> candles, int period, double stdDevMultiplier) {
        List<Double> upperBand = new ArrayList<>();
        List<Double> middleBand = new ArrayList<>();
        List<Double> lowerBand = new ArrayList<>();

        for (int i = 0; i < candles.size(); i++) {
            if (i < period - 1) {
                upperBand.add(null);
                middleBand.add(null);
                lowerBand.add(null);
                continue;
            }

            // SMA 계산
            double sum = 0.0;
            for (int j = 0; j < period; j++) {
                sum += candles.get(i - j).getClosingPrice();
            }
            double sma = sum / period;
            middleBand.add(sma);

            // 표준편차 계산
            double variance = 0.0;
            for (int j = 0; j < period; j++) {
                double diff = candles.get(i - j).getClosingPrice() - sma;
                variance += diff * diff;
            }
            double stdDev = Math.sqrt(variance / period);

            // 상단/하단 밴드 계산
            upperBand.add(sma + (stdDevMultiplier * stdDev));
            lowerBand.add(sma - (stdDevMultiplier * stdDev));
        }

        return new BollingerBands(upperBand, middleBand, lowerBand);
    }

    /**
     * 볼린저 밴드 데이터 클래스
     */
    public static class BollingerBands {
        private final List<Double> upperBand;
        private final List<Double> middleBand;
        private final List<Double> lowerBand;

        public BollingerBands(List<Double> upperBand, List<Double> middleBand, List<Double> lowerBand) {
            this.upperBand = upperBand;
            this.middleBand = middleBand;
            this.lowerBand = lowerBand;
        }

        public List<Double> getUpperBand() {
            return upperBand;
        }

        public List<Double> getMiddleBand() {
            return middleBand;
        }

        public List<Double> getLowerBand() {
            return lowerBand;
        }

        public Double getUpperBand(int index) {
            return upperBand.get(index);
        }

        public Double getMiddleBand(int index) {
            return middleBand.get(index);
        }

        public Double getLowerBand(int index) {
            return lowerBand.get(index);
        }
    }
}
