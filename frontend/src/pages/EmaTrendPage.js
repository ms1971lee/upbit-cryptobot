import React, { useState, useEffect, useCallback } from 'react';
import StateMachineVisualizer from '../components/StateMachineVisualizer';
import ConditionChecklist from '../components/ConditionChecklist';
import emaTrendApi from '../api/emaTrendApi';
import './EmaTrendPage.css';

/**
 * EMA 추세추종 자동매매 페이지
 */
const EmaTrendPage = () => {
  const [isAutoTrading, setIsAutoTrading] = useState(false);
  const [selectedSymbol, setSelectedSymbol] = useState('KRW-BTC');
  const [currentState, setCurrentState] = useState('FLAT');
  const [conditions, setConditions] = useState({});
  const [position, setPosition] = useState(null);
  const [trades, setTrades] = useState([]);
  const [config, setConfig] = useState(null);
  const [allStates, setAllStates] = useState({});
  const [loading, setLoading] = useState(false);

  // 인기 종목 목록
  const popularSymbols = [
    'KRW-BTC', 'KRW-ETH', 'KRW-XRP', 'KRW-SOL', 
    'KRW-DOGE', 'KRW-ADA', 'KRW-AVAX', 'KRW-DOT'
  ];

  // 상태 조회
  const fetchState = useCallback(async () => {
    try {
      const response = await emaTrendApi.getState(selectedSymbol);
      if (response.success) {
        setCurrentState(response.data.state);
      }
    } catch (error) {
      console.error('상태 조회 실패:', error);
    }
  }, [selectedSymbol]);

  // 전체 상태 조회
  const fetchAllStates = useCallback(async () => {
    try {
      const response = await emaTrendApi.getAllStates();
      if (response.success) {
        setAllStates(response.data);
      }
    } catch (error) {
      console.error('전체 상태 조회 실패:', error);
    }
  }, []);

  // 설정 조회
  const fetchConfig = useCallback(async () => {
    try {
      const response = await emaTrendApi.getConfig();
      if (response.success) {
        setConfig(response.data);
      }
    } catch (error) {
      console.error('설정 조회 실패:', error);
    }
  }, []);

  // 초기 로드
  useEffect(() => {
    fetchConfig();
  }, [fetchConfig]);

  // 자동 갱신
  useEffect(() => {
    if (isAutoTrading) {
      fetchState();
      fetchAllStates();
      const interval = setInterval(() => {
        fetchState();
        fetchAllStates();
      }, 5000); // 5초마다 갱신
      return () => clearInterval(interval);
    }
  }, [isAutoTrading, fetchState, fetchAllStates]);

  // 상태 초기화
  const handleReset = async () => {
    setLoading(true);
    try {
      await emaTrendApi.resetState(selectedSymbol);
      await fetchState();
      alert('상태가 초기화되었습니다.');
    } catch (error) {
      alert('초기화 실패: ' + error.message);
    }
    setLoading(false);
  };

  // 전체 초기화
  const handleResetAll = async () => {
    if (!window.confirm('모든 종목의 상태를 초기화하시겠습니까?')) return;
    setLoading(true);
    try {
      await emaTrendApi.resetAll();
      await fetchAllStates();
      setCurrentState('FLAT');
      alert('전체 상태가 초기화되었습니다.');
    } catch (error) {
      alert('전체 초기화 실패: ' + error.message);
    }
    setLoading(false);
  };

  // 방향 결정
  const direction = conditions.trendUp ? 'LONG' : conditions.trendDown ? 'SHORT' : 'NONE';

  return (
    <div className="ema-trend-page">
      {/* 헤더 */}
      <header className="page-header">
        <div className="header-left">
          <h1>📊 EMA 추세추종 자동매매</h1>
          <span className="subtitle">V4 Strategy - 상태 머신 기반</span>
        </div>
        <div className="header-right">
          <button 
            className={`auto-trade-btn ${isAutoTrading ? 'active' : ''}`}
            onClick={() => setIsAutoTrading(!isAutoTrading)}
          >
            {isAutoTrading ? '🔴 자동매매 중지' : '🟢 자동매매 시작'}
          </button>
        </div>
      </header>

      {/* 종목 선택 */}
      <section className="symbol-section">
        <div className="symbol-selector">
          <label>종목 선택</label>
          <select 
            value={selectedSymbol} 
            onChange={(e) => setSelectedSymbol(e.target.value)}
          >
            {popularSymbols.map(symbol => (
              <option key={symbol} value={symbol}>{symbol}</option>
            ))}
          </select>
        </div>
        
        <div className="quick-symbols">
          {popularSymbols.slice(0, 4).map(symbol => (
            <button 
              key={symbol}
              className={`quick-btn ${selectedSymbol === symbol ? 'active' : ''}`}
              onClick={() => setSelectedSymbol(symbol)}
            >
              {symbol.replace('KRW-', '')}
            </button>
          ))}
        </div>

        <div className="action-buttons">
          <button className="reset-btn" onClick={handleReset} disabled={loading}>
            🔄 상태 초기화
          </button>
          <button className="reset-all-btn" onClick={handleResetAll} disabled={loading}>
            ⚠️ 전체 초기화
          </button>
        </div>
      </section>

      {/* 메인 컨텐츠 */}
      <div className="main-content">
        {/* 상태 머신 */}
        <section className="state-section">
          <StateMachineVisualizer 
            currentState={currentState} 
            direction={direction}
          />
        </section>

        {/* 2열 레이아웃 */}
        <div className="two-column">
          {/* 왼쪽: 조건 체크 */}
          <section className="conditions-section">
            <ConditionChecklist 
              conditions={conditions} 
              direction={direction}
            />
          </section>

          {/* 오른쪽: 포지션 정보 */}
          <section className="position-section">
            <div className="position-card">
              <h3>📈 포지션 정보</h3>
              {position ? (
                <div className="position-details">
                  <div className="position-row">
                    <span>방향</span>
                    <span className={position.side === 'LONG' ? 'long' : 'short'}>
                      {position.side === 'LONG' ? '🟢 롱' : '🔴 숏'}
                    </span>
                  </div>
                  <div className="position-row">
                    <span>진입가</span>
                    <span>{position.entryPrice?.toLocaleString()}</span>
                  </div>
                  <div className="position-row">
                    <span>손절가</span>
                    <span className="stop">{position.stopPrice?.toLocaleString()}</span>
                  </div>
                  <div className="position-row">
                    <span>목표가 (1R)</span>
                    <span className="target">{position.targetPrice?.toLocaleString()}</span>
                  </div>
                  <div className="position-row">
                    <span>현재 손익</span>
                    <span className={position.pnl >= 0 ? 'profit' : 'loss'}>
                      {position.pnl >= 0 ? '+' : ''}{position.pnl?.toFixed(2)}%
                    </span>
                  </div>
                </div>
              ) : (
                <div className="no-position">
                  포지션 없음
                </div>
              )}
            </div>
          </section>
        </div>

        {/* 파라미터 설정 */}
        {config && (
          <section className="config-section">
            <h3>⚙️ 전략 파라미터</h3>
            <div className="config-grid">
              <div className="config-item">
                <span className="config-label">눌림 허용</span>
                <span className="config-value">{(config.pullbackTolerance * 100).toFixed(1)}%</span>
              </div>
              <div className="config-item">
                <span className="config-label">거래량 배수</span>
                <span className="config-value">{config.volumeMultiplier}x</span>
              </div>
              <div className="config-item">
                <span className="config-label">손절 ATR</span>
                <span className="config-value">{config.stopAtrMultiplier}x</span>
              </div>
              <div className="config-item">
                <span className="config-label">익절 R배수</span>
                <span className="config-value">{config.takeProfitRatio}R</span>
              </div>
              <div className="config-item">
                <span className="config-label">쿨다운</span>
                <span className="config-value">{config.cooldownBars}캔들</span>
              </div>
              <div className="config-item">
                <span className="config-label">일일 손실한도</span>
                <span className="config-value">{config.dailyLossLimitPercent}%</span>
              </div>
            </div>
          </section>
        )}

        {/* 거래 내역 */}
        <section className="trades-section">
          <h3>📋 최근 거래 내역</h3>
          <div className="trades-table">
            <table>
              <thead>
                <tr>
                  <th>시간</th>
                  <th>종목</th>
                  <th>방향</th>
                  <th>진입가</th>
                  <th>청산가</th>
                  <th>손익</th>
                  <th>사유</th>
                </tr>
              </thead>
              <tbody>
                {trades.length > 0 ? (
                  trades.map((trade, idx) => (
                    <tr key={idx}>
                      <td>{trade.time}</td>
                      <td>{trade.symbol}</td>
                      <td className={trade.side === 'LONG' ? 'long' : 'short'}>
                        {trade.side}
                      </td>
                      <td>{trade.entryPrice?.toLocaleString()}</td>
                      <td>{trade.exitPrice?.toLocaleString()}</td>
                      <td className={trade.pnl >= 0 ? 'profit' : 'loss'}>
                        {trade.pnl >= 0 ? '+' : ''}{trade.pnl?.toFixed(2)}%
                      </td>
                      <td>{trade.reason}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="7" className="no-data">거래 내역이 없습니다</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        {/* 활성 종목 현황 */}
        {Object.keys(allStates).length > 0 && (
          <section className="active-symbols-section">
            <h3>🔍 활성 종목 현황</h3>
            <div className="active-symbols-grid">
              {Object.entries(allStates).map(([symbol, stateInfo]) => (
                <div 
                  key={symbol} 
                  className={`active-symbol-card ${stateInfo.state}`}
                  onClick={() => setSelectedSymbol(symbol)}
                >
                  <div className="symbol-name">{symbol.replace('KRW-', '')}</div>
                  <div className="symbol-state">{stateInfo.displayName}</div>
                </div>
              ))}
            </div>
          </section>
        )}
      </div>

      {/* 핵심 원칙 */}
      <footer className="page-footer">
        <div className="principle">
          💡 <strong>핵심 원칙:</strong> 선물 자동매매는 방향 예측이 아니라 손실 통제 로직이다.
        </div>
      </footer>
    </div>
  );
};

export default EmaTrendPage;
