package com.cryptobot.upbit.model.backtest;

import com.cryptobot.upbit.entity.BacktestConfig;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 백테스트 실행 컨텍스트
 * 백테스트 실행 중 상태를 관리합니다.
 */
@Data
public class BacktestContext {

    // 설정
    private BacktestConfig config;

    // 포트폴리오 상태
    private Double cash;                    // 현금
    private Double coinBalance;             // 코인 보유량
    private Double avgBuyPrice;             // 평균 매수가

    // 거래 내역
    private List<Trade> trades;

    // 자산 곡선
    private List<EquityPoint> equityCurve;

    // 통계
    private Double peakCapital;             // 최고 자산
    private Double currentDrawdown;         // 현재 낙폭
    private Double maxDrawdown;             // 최대 낙폭

    public BacktestContext(BacktestConfig config) {
        this.config = config;
        this.cash = config.getInitialCapital();
        this.coinBalance = 0.0;
        this.avgBuyPrice = 0.0;
        this.trades = new ArrayList<>();
        this.equityCurve = new ArrayList<>();
        this.peakCapital = config.getInitialCapital();
        this.currentDrawdown = 0.0;
        this.maxDrawdown = 0.0;
    }

    /**
     * 현재 포트폴리오 가치 계산
     */
    public Double getPortfolioValue(Double currentPrice) {
        return cash + (coinBalance * currentPrice);
    }

    /**
     * 최고 자산 업데이트
     */
    public void updatePeak(Double currentValue) {
        if (currentValue > peakCapital) {
            peakCapital = currentValue;
        }
    }

    /**
     * 낙폭 업데이트
     */
    public void updateDrawdown(Double currentValue) {
        if (peakCapital > 0) {
            currentDrawdown = ((currentValue - peakCapital) / peakCapital) * 100;
            if (currentDrawdown < maxDrawdown) {
                maxDrawdown = currentDrawdown;
            }
        }
    }

    /**
     * 평균 매수가 업데이트
     */
    public void updateAvgBuyPrice(Double newPrice, Double newVolume) {
        if (coinBalance > 0) {
            Double totalValue = (coinBalance * avgBuyPrice) + (newVolume * newPrice);
            coinBalance += newVolume;
            avgBuyPrice = totalValue / coinBalance;
        } else {
            coinBalance = newVolume;
            avgBuyPrice = newPrice;
        }
    }

    /**
     * 코인 잔고 감소
     */
    public void decreaseCoinBalance(Double amount) {
        coinBalance -= amount;
        if (coinBalance < 0.00000001) {
            coinBalance = 0.0;
            avgBuyPrice = 0.0;
        }
    }
}
