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
public class DonchianResult {
    private List<Double> high;  // N일 최고가
    private List<Double> low;   // N일 최저가
}
