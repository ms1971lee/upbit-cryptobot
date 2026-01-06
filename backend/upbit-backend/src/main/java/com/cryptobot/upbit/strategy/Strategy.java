package com.cryptobot.upbit.strategy;

import com.cryptobot.upbit.domain.candle.Candle;

import java.util.List;

public interface Strategy {
    String getCode();
    String getName();
    SignalResult evaluate(String symbol, List<Candle> candles5m, List<Candle> candles15m);
}
