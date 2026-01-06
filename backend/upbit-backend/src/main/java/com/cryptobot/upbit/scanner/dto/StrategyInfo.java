package com.cryptobot.upbit.scanner.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StrategyInfo {
    private String code;
    private String name;
    private String description;
}
