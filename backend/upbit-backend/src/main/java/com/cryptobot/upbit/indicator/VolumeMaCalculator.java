package com.cryptobot.upbit.indicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Volume MA (Simple Moving Average) 계산기
 */
public class VolumeMaCalculator {

    public static List<Double> calculate(List<Double> volumes, int length) {
        if (volumes == null || volumes.size() < length) {
            return new ArrayList<>();
        }

        List<Double> volumeMa = new ArrayList<>();

        for (int i = length - 1; i < volumes.size(); i++) {
            double sum = 0.0;
            for (int j = i - length + 1; j <= i; j++) {
                sum += volumes.get(j);
            }
            volumeMa.add(sum / length);
        }

        return volumeMa;
    }
}
