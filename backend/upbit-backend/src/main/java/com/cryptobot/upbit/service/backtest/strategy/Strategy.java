package com.cryptobot.upbit.service.backtest.strategy;

import com.cryptobot.upbit.model.backtest.BacktestContext;
import com.cryptobot.upbit.model.backtest.Candle;

import java.util.List;

/**
 * 백테스트 전략 인터페이스
 * 모든 트레이딩 전략은 이 인터페이스를 구현해야 합니다.
 */
public interface Strategy {

    /**
     * 전략 초기화
     * 백테스트 시작 전에 호출되며, 필요한 초기 설정을 수행합니다.
     *
     * @param context 백테스트 컨텍스트
     * @param candles 전체 캔들 데이터
     */
    void initialize(BacktestContext context, List<Candle> candles);

    /**
     * 매 캔들마다 호출되어 매매 신호를 생성합니다.
     *
     * @param currentIndex 현재 캔들 인덱스
     * @param candles 전체 캔들 데이터
     * @param context 백테스트 컨텍스트
     * @return 매매 신호 (BUY, SELL, HOLD)
     */
    Signal generateSignal(int currentIndex, List<Candle> candles, BacktestContext context);

    /**
     * 전략 이름 반환
     */
    String getName();

    /**
     * 매매 신호
     */
    enum Signal {
        BUY,    // 매수 신호
        SELL,   // 매도 신호
        HOLD    // 홀드 (아무것도 하지 않음)
    }
}
