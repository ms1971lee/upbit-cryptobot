package com.cryptobot.upbit.indicator;

import com.cryptobot.upbit.domain.candle.Candle;

import java.util.ArrayList;
import java.util.List;

/**
 * ADX (Average Directional Index) 계산기
 * Wilder's smoothing 방식 사용
 */
public class AdxCalculator {

    public static List<Double> calculate(List<Candle> candles, int length) {
        if (candles == null || candles.size() < length * 2 + 1) {
            return new ArrayList<>();
        }

        List<Double> adx = new ArrayList<>();

        // True Range, +DM, -DM 계산
        List<Double> trList = new ArrayList<>();
        List<Double> plusDmList = new ArrayList<>();
        List<Double> minusDmList = new ArrayList<>();

        for (int i = 1; i < candles.size(); i++) {
            Candle current = candles.get(i);
            Candle previous = candles.get(i - 1);

            // True Range
            double tr1 = current.getHigh() - current.getLow();
            double tr2 = Math.abs(current.getHigh() - previous.getClose());
            double tr3 = Math.abs(current.getLow() - previous.getClose());
            double tr = Math.max(tr1, Math.max(tr2, tr3));

            // Directional Movement
            double upMove = current.getHigh() - previous.getHigh();
            double downMove = previous.getLow() - current.getLow();

            double plusDm = (upMove > downMove && upMove > 0) ? upMove : 0;
            double minusDm = (downMove > upMove && downMove > 0) ? downMove : 0;

            trList.add(tr);
            plusDmList.add(plusDm);
            minusDmList.add(minusDm);
        }

        if (trList.size() < length) {
            return new ArrayList<>();
        }

        // 첫 번째 평균 계산
        double atr = 0.0;
        double plusDi = 0.0;
        double minusDi = 0.0;

        for (int i = 0; i < length; i++) {
            atr += trList.get(i);
            plusDi += plusDmList.get(i);
            minusDi += minusDmList.get(i);
        }

        atr /= length;
        plusDi /= length;
        minusDi /= length;

        // DI+ 와 DI- 계산
        List<Double> plusDiList = new ArrayList<>();
        List<Double> minusDiList = new ArrayList<>();

        if (atr > 0) {
            plusDiList.add((plusDi / atr) * 100);
            minusDiList.add((minusDi / atr) * 100);
        } else {
            plusDiList.add(0.0);
            minusDiList.add(0.0);
        }

        // Wilder's smoothing으로 계속 계산
        for (int i = length; i < trList.size(); i++) {
            atr = (atr * (length - 1) + trList.get(i)) / length;
            plusDi = (plusDi * (length - 1) + plusDmList.get(i)) / length;
            minusDi = (minusDi * (length - 1) + minusDmList.get(i)) / length;

            if (atr > 0) {
                plusDiList.add((plusDi / atr) * 100);
                minusDiList.add((minusDi / atr) * 100);
            } else {
                plusDiList.add(0.0);
                minusDiList.add(0.0);
            }
        }

        // DX 계산
        List<Double> dxList = new ArrayList<>();
        for (int i = 0; i < plusDiList.size(); i++) {
            double pdi = plusDiList.get(i);
            double mdi = minusDiList.get(i);
            double sum = pdi + mdi;

            if (sum > 0) {
                double dx = (Math.abs(pdi - mdi) / sum) * 100;
                dxList.add(dx);
            } else {
                dxList.add(0.0);
            }
        }

        if (dxList.size() < length) {
            return new ArrayList<>();
        }

        // 첫 ADX는 DX의 평균
        double currentAdx = 0.0;
        for (int i = 0; i < length; i++) {
            currentAdx += dxList.get(i);
        }
        currentAdx /= length;
        adx.add(currentAdx);

        // Wilder's smoothing으로 ADX 계산
        for (int i = length; i < dxList.size(); i++) {
            currentAdx = (currentAdx * (length - 1) + dxList.get(i)) / length;
            adx.add(currentAdx);
        }

        return adx;
    }
}
