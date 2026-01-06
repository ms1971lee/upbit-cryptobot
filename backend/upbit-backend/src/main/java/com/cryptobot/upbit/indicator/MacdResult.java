package com.cryptobot.upbit.indicator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MacdResult {
    private List<Double> macdLine;
    private List<Double> signalLine;
    private List<Double> histogram;
}
