package com.cryptobot.upbit.upbit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpbitCandleResponse {
    private String market;

    @JsonProperty("candle_date_time_kst")
    private String candleDateTimeKst;

    @JsonProperty("opening_price")
    private Double openingPrice;

    @JsonProperty("high_price")
    private Double highPrice;

    @JsonProperty("low_price")
    private Double lowPrice;

    @JsonProperty("trade_price")
    private Double tradePrice;

    @JsonProperty("candle_acc_trade_volume")
    private Double candleAccTradeVolume;

    @JsonProperty("prev_closing_price")
    private Double prevClosingPrice;

    @JsonProperty("change_rate")
    private Double changeRate;
}
