import React, { useState, useEffect } from 'react';
import MainLayout from '../components/layout/MainLayout';
import scanApi from '../api/scanApi';
import accountApi from '../api/accountApi';
import tradingApi from '../api/tradingApi';
import backtestApi from '../api/backtestApi';
import BacktestForm from '../components/backtest/BacktestForm';
import BacktestResult from '../components/backtest/BacktestResult';
import BacktestHistory from '../components/backtest/BacktestHistory';
import BacktestResultModal from '../components/backtest/BacktestResultModal';
import DataManager from '../components/backtest/DataManager';
import './TradingScanPage.css';

const TradingScanPage = () => {
  const [strategy, setStrategy] = useState('V1');
  const [strategies, setStrategies] = useState([]);
  const [timeFrame, setTimeFrame] = useState('5m');
  const [scanResults, setScanResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // 계좌 정보 상태
  const [accountSummary, setAccountSummary] = useState(null);
  const [accountLoading, setAccountLoading] = useState(false);
  const [accountError, setAccountError] = useState('');

  // 매수/매도 모달 상태
  const [orderModal, setOrderModal] = useState({ show: false, type: '', coin: null });
  const [orderAmount, setOrderAmount] = useState('');
  const [orderPrice, setOrderPrice] = useState('');
  const [orderLoading, setOrderLoading] = useState(false);
  const [orderMessage, setOrderMessage] = useState('');

  // 거래 모드 상태
  const [tradingMode, setTradingMode] = useState('TEST');

  // 탭 상태
  const [activeTab, setActiveTab] = useState('scan');
  const [backtestSubTab, setBacktestSubTab] = useState('run'); // 'run' or 'data'

  // 백테스트 상태
  const [backtestId, setBacktestId] = useState(null);
  const [backtestLoading, setBacktestLoading] = useState(false);
  const [backtestError, setBacktestError] = useState('');

  // 백테스트 결과
  const [backtestResult, setBacktestResult] = useState(null);
  const [resultLoading, setResultLoading] = useState(false);
  const [resultError, setResultError] = useState('');

  // 백테스트 이력
  const [backtestHistory, setBacktestHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);

  // 백테스트 결과 모달
  const [resultModal, setResultModal] = useState({ show: false, backtestId: null });

  // 전략 목록 및 계좌 정보 로드
  useEffect(() => {
    loadStrategies();
    loadAccountSummary();
    loadTradingMode();

    // 30초마다 계좌 정보 갱신
    const interval = setInterval(() => {
      loadAccountSummary();
    }, 30000);

    return () => clearInterval(interval);
  }, []);

  const loadTradingMode = async () => {
    try {
      const response = await tradingApi.getTradingMode();
      if (response.success) {
        setTradingMode(response.mode);
      }
    } catch (error) {
      console.error('거래 모드 조회 실패:', error);
    }
  };

  const loadStrategies = async () => {
    try {
      const response = await scanApi.getStrategies();
      if (response.success) {
        setStrategies(response.data);
      }
    } catch (err) {
      console.error('전략 목록 로드 실패:', err);
    }
  };

  const loadAccountSummary = async () => {
    setAccountLoading(true);
    setAccountError('');

    try {
      const response = await accountApi.getAccountSummary();
      if (response.success) {
        setAccountSummary(response);
      } else {
        setAccountError(response.error || 'API 키가 등록되지 않았습니다');
      }
    } catch (err) {
      console.error('계좌 정보 로드 실패:', err);
      setAccountError('계좌 정보를 불러올 수 없습니다');
    } finally {
      setAccountLoading(false);
    }
  };

  // 스캔 실행
  const handleScan = async () => {
    setLoading(true);
    setError('');
    setScanResults([]);

    try {
      const response = await scanApi.getSignals(strategy, timeFrame);
      if (response.success) {
        setScanResults(response.data);
        if (response.data.length === 0) {
          setError('현재 신호가 발생한 종목이 없습니다.');
        }
      } else {
        setError(response.message || '스캔 실패');
      }
    } catch (err) {
      setError('스캔 중 오류가 발생했습니다: ' + err.message);
      console.error('스캔 실패:', err);
    } finally {
      setLoading(false);
    }
  };

  // 매수/매도 모달 열기
  const openOrderModal = (type, coin) => {
    setOrderModal({ show: true, type, coin });
    setOrderAmount('');
    setOrderPrice(coin.currentPrice || '');
    setOrderMessage('');
  };

  // 매수/매도 모달 닫기
  const closeOrderModal = () => {
    setOrderModal({ show: false, type: '', coin: null });
    setOrderAmount('');
    setOrderPrice('');
    setOrderMessage('');
  };

  // 주문 실행
  const handleExecuteOrder = async () => {
    if (!orderAmount || !orderPrice) {
      setOrderMessage('수량과 가격을 입력해주세요');
      return;
    }

    const volume = parseFloat(orderAmount);
    const price = parseFloat(orderPrice);

    if (volume <= 0 || price <= 0) {
      setOrderMessage('수량과 가격은 0보다 커야 합니다');
      return;
    }

    setOrderLoading(true);
    setOrderMessage('');

    try {
      const orderData = {
        market: orderModal.coin.market,
        orderType: orderModal.type,
        price: price,
        volume: volume,
        strategy: strategy,
        memo: `${timeFrame} ${strategy} 전략`
      };

      const response = await tradingApi.executeOrder(orderData);

      if (response.success) {
        setOrderMessage(response.message);

        // 계좌 정보 새로고침
        setTimeout(() => {
          loadAccountSummary();
          closeOrderModal();
        }, 1500);
      } else {
        setOrderMessage(response.message || '주문 실행 실패');
      }
    } catch (error) {
      console.error('주문 실행 실패:', error);
      setOrderMessage('주문 실행 중 오류가 발생했습니다');
    } finally {
      setOrderLoading(false);
    }
  };

  // 코인 아이콘 가져오기
  const getCoinIcon = (market) => {
    const symbol = market ? market.replace('KRW-', '') : '';
    const icons = {
      'BTC': '₿',
      'ETH': 'Ξ',
      'XRP': '✖',
      'DOGE': '🐶',
      'ADA': '₳'
    };
    return icons[symbol] || (symbol ? symbol.charAt(0) : '?');
  };

  // 실시간 보유종목 데이터
  const holdings = accountSummary && accountSummary.holdings ? accountSummary.holdings.map((holding, index) => ({
    id: index + 1,
    coinName: holding.koreanName || holding.currency,
    symbol: holding.currency,
    market: holding.market,
    quantity: holding.balance.toFixed(8),
    buyPrice: holding.avgBuyPrice.toLocaleString(),
    currentPrice: holding.currentPrice.toLocaleString(),
    evalPrice: holding.evalAmount.toLocaleString(),
    profitAmount: holding.profitAmount.toLocaleString(),
    profitRate: holding.profitRate.toFixed(2),
    icon: getCoinIcon(holding.market)
  })) : [];

  // 실시간 통계 데이터
  const stats = accountSummary ? {
    totalBuy: accountSummary.totalBuyAmount || 0,
    totalAssets: accountSummary.totalAssets || 0,
    totalProfit: accountSummary.totalProfit || 0,
    profitRate: accountSummary.totalProfitRate || 0
  } : {
    totalBuy: 0,
    totalAssets: 0,
    totalProfit: 0,
    profitRate: 0
  };

  // ==================== 백테스트 관련 함수 ====================

  // 백테스트 탭 활성화 시 이력 로드
  useEffect(() => {
    if (activeTab === 'backtest') {
      loadBacktestHistory();
    }
  }, [activeTab]);

  // 백테스트 이력 조회
  const loadBacktestHistory = async () => {
    setHistoryLoading(true);

    try {
      const response = await backtestApi.getHistory();
      if (response.success && response.backtests) {
        setBacktestHistory(response.backtests);
      }
    } catch (error) {
      console.error('백테스트 이력 조회 실패:', error);
    } finally {
      setHistoryLoading(false);
    }
  };

  // 백테스트 실행
  const handleRunBacktest = async (formData) => {
    setBacktestLoading(true);
    setBacktestError('');
    setBacktestResult(null);

    try {
      const response = await backtestApi.runBacktest(formData);
      if (response.success && response.backtestId) {
        setBacktestId(response.backtestId);
        // 결과 폴링 시작
        pollBacktestResult(response.backtestId);
      } else {
        setBacktestError(response.message || '백테스트 실행 실패');
      }
    } catch (error) {
      console.error('백테스트 실행 실패:', error);
      setBacktestError(error.response?.data?.message || '백테스트 실행 중 오류가 발생했습니다');
    } finally {
      setBacktestLoading(false);
    }
  };

  // 백테스트 결과 폴링
  const pollBacktestResult = async (id) => {
    setResultLoading(true);
    setResultError('');

    const maxAttempts = 60;  // 최대 60번 (5분)
    const interval = 5000;   // 5초마다

    for (let i = 0; i < maxAttempts; i++) {
      try {
        const response = await backtestApi.getResult(id);

        if (response.success && response.result) {
          const status = response.result.status;

          if (status === 'COMPLETED') {
            setBacktestResult(response.result);
            setResultLoading(false);
            loadBacktestHistory(); // 이력 갱신
            return;
          } else if (status === 'FAILED') {
            setResultError(response.result.errorMessage || '백테스트 실행 실패');
            setResultLoading(false);
            return;
          }
          // RUNNING 상태면 계속 폴링
        }

        // 5초 대기
        await new Promise(resolve => setTimeout(resolve, interval));

      } catch (error) {
        console.error('백테스트 결과 조회 실패:', error);
        setResultError('결과 조회 중 오류가 발생했습니다');
        setResultLoading(false);
        return;
      }
    }

    // 타임아웃
    setResultError('백테스트 실행 시간이 초과되었습니다 (최대 5분)');
    setResultLoading(false);
  };

  // 이력 선택 - 모달 열기
  const handleSelectHistory = (id) => {
    setResultModal({ show: true, backtestId: id });
  };

  // 모달 닫기
  const handleCloseModal = () => {
    setResultModal({ show: false, backtestId: null });
  };

  // 이력 삭제
  const handleDeleteHistory = async (id) => {
    if (!window.confirm('이 백테스트 결과를 삭제하시겠습니까?')) {
      return;
    }

    try {
      const response = await backtestApi.deleteResult(id);
      if (response.success) {
        loadBacktestHistory(); // 이력 갱신
        if (backtestId === id) {
          setBacktestResult(null); // 현재 보고 있던 결과면 초기화
          setBacktestId(null);
        }
      }
    } catch (error) {
      console.error('백테스트 삭제 실패:', error);
      alert('삭제 중 오류가 발생했습니다');
    }
  };

  // 결과 새로고침
  const handleRefreshResult = () => {
    if (backtestId) {
      handleSelectHistory(backtestId);
    }
  };

  return (
    <MainLayout>
      <div className="trading-scan-page">
        {/* 탭 메뉴 */}
        <div className="trading-tabs">
          <button
            className={`tab-btn ${activeTab === 'scan' ? 'active' : ''}`}
            onClick={() => setActiveTab('scan')}
          >
            종목 스캔
          </button>
          <button
            className={`tab-btn ${activeTab === 'backtest' ? 'active' : ''}`}
            onClick={() => setActiveTab('backtest')}
          >
            백테스트
          </button>
        </div>

        {/* 종목 스캔 탭 */}
        {activeTab === 'scan' && (
          <div className="tab-content">
            {/* 메인 컨텐츠 영역 */}
            <div className="trading-main-content">
              {/* 종목 스캔 리스트 */}
              <div className="scan-section">
            <div className="section-header">
              <h2>종목 스캔 리스트</h2>
              <div className="scan-controls">
                <select
                  className="scan-select"
                  value={strategy}
                  onChange={(e) => setStrategy(e.target.value)}
                >
                  {strategies.map((s) => (
                    <option key={s.code} value={s.code}>
                      {s.code}: {s.name}
                    </option>
                  ))}
                  {strategies.length === 0 && (
                    <>
                      <option value="V1">V1: Donchian Breakout</option>
                      <option value="V2">V2: Holy Grail Pullback</option>
                      <option value="V3">V3: Reversal</option>
                    </>
                  )}
                </select>
                <select
                  className="scan-select"
                  value={timeFrame}
                  onChange={(e) => setTimeFrame(e.target.value)}
                >
                  <option value="5m">5분봉</option>
                  <option value="15m">15분봉</option>
                  <option value="30m">30분봉</option>
                  <option value="1h">1시간봉</option>
                  <option value="1d">일봉</option>
                </select>
                <button
                  className="search-btn"
                  onClick={handleScan}
                  disabled={loading}
                >
                  {loading ? '스캔 중...' : '검색'}
                </button>
              </div>
            </div>

            {error && (
              <div className="error-message-box">
                {error}
              </div>
            )}

            {loading && (
              <div className="loading-container">
                <div className="spinner"></div>
                <span className="loading-text">종목 스캔 중입니다. 잠시만 기다려주세요...</span>
              </div>
            )}

            {!loading && scanResults.length > 0 && (
              <div className="scan-table-wrapper">
                <table className="scan-table">
                  <thead>
                    <tr>
                      <th>코인명</th>
                      <th>현재가</th>
                      <th>전일대비</th>
                      <th>신호</th>
                      <th>근거</th>
                      <th>매매</th>
                    </tr>
                  </thead>
                  <tbody>
                    {scanResults.map((item, index) => (
                      <tr key={index}>
                        <td>
                          <div className="coin-info">
                            <span className="coin-icon">{getCoinIcon(item.market)}</span>
                            <div>
                              <div className="coin-name">{item.coinName}</div>
                              <div className="coin-symbol">{item.market}</div>
                            </div>
                          </div>
                        </td>
                        <td className="price">{item.currentPrice.toLocaleString()} KRW</td>
                        <td className={item.changeRate >= 0 ? 'positive' : 'negative'}>
                          {item.changeRate >= 0 ? '+' : ''}{item.changeRate.toFixed(2)}%
                        </td>
                        <td>
                          <span className={`signal-dot ${item.signal.toLowerCase()}`}></span>
                        </td>
                        <td>
                          <div className="reason-codes">
                            {item.reasonCodes && item.reasonCodes.map((code, i) => (
                              <span key={i} className="reason-badge">{code}</span>
                            ))}
                          </div>
                        </td>
                        <td>
                          <button
                            className={`trade-btn ${item.signal === 'BUY' ? 'buy-btn' : 'sell-btn'}`}
                            onClick={() => openOrderModal(item.signal, item)}
                          >
                            {item.signal === 'BUY' ? '매수' : '매도'}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {!loading && !error && scanResults.length === 0 && (
              <div className="empty-state">
                <p>검색 버튼을 클릭하여 종목 스캔을 시작하세요.</p>
              </div>
            )}
          </div>

          {/* 보유종목 리스트 */}
          <div className="holdings-section">
            <div className="section-header">
              <h2>보유종목 리스트</h2>
            </div>

            {accountLoading && (
              <div className="loading-container">
                <div className="spinner"></div>
                <span className="loading-text">계좌 정보를 불러오는 중...</span>
              </div>
            )}

            {accountError && !accountLoading && (
              <div className="error-message-box">
                {accountError}
              </div>
            )}

            {!accountLoading && !accountError && holdings.length === 0 && (
              <div className="empty-state">
                <p>보유 중인 암호화폐가 없습니다.</p>
              </div>
            )}

            {!accountLoading && !accountError && holdings.length > 0 && (
              <div className="holdings-table-wrapper">
                <table className="holdings-table">
                  <thead>
                    <tr>
                      <th>코인명</th>
                      <th>보유수량</th>
                      <th>매수평균가</th>
                      <th>현재가</th>
                      <th>평가금액</th>
                      <th>평가수익</th>
                      <th>수동매도</th>
                    </tr>
                  </thead>
                  <tbody>
                    {holdings.map((item) => (
                      <tr key={item.id}>
                        <td>
                          <div className="coin-info">
                            <span className="coin-icon">{item.icon}</span>
                            <div>
                              <div className="coin-name">{item.coinName}</div>
                              <div className="coin-symbol">{item.market}</div>
                            </div>
                          </div>
                        </td>
                        <td className="price">{item.quantity}</td>
                        <td className="price">{item.buyPrice} KRW</td>
                        <td className="price">{item.currentPrice} KRW</td>
                        <td className="price">{item.evalPrice} KRW</td>
                        <td className={parseFloat(item.profitRate) >= 0 ? 'positive' : 'negative'}>
                          {parseFloat(item.profitRate) >= 0 ? '+' : ''}{item.profitRate}%
                          <div className="profit-amount">
                            ({parseFloat(item.profitAmount) >= 0 ? '+' : ''}{item.profitAmount} KRW)
                          </div>
                        </td>
                        <td>
                          <button
                            className="sell-btn"
                            onClick={() => openOrderModal('SELL', item)}
                          >
                            매도
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>

        {/* 우측 통계 영역 */}
        <div className="trading-stats">
          {accountLoading ? (
            <div className="stat-loading">
              <div className="spinner"></div>
              <span>로딩 중...</span>
            </div>
          ) : accountError ? (
            <div className="stat-error">
              <p>API 키를 등록해주세요</p>
            </div>
          ) : (
            <>
              <div className="stat-card">
                <div className="stat-label">총 매수금액</div>
                <div className="stat-value">{Math.round(stats.totalBuy).toLocaleString()}원</div>
              </div>
              <div className="stat-card">
                <div className="stat-label">보유자산</div>
                <div className="stat-value">{Math.round(stats.totalAssets).toLocaleString()}원</div>
              </div>
              <div className={`stat-card ${stats.totalProfit >= 0 ? 'positive-card' : 'negative-card'}`}>
                <div className="stat-label">총평가수익</div>
                <div className={`stat-value ${stats.totalProfit >= 0 ? 'positive' : 'negative'}`}>
                  {stats.totalProfit >= 0 ? '+' : ''}{Math.round(stats.totalProfit).toLocaleString()}원
                </div>
              </div>
              <div className={`stat-card ${stats.profitRate >= 0 ? 'positive-card' : 'negative-card'}`}>
                <div className="stat-label">총평가수익률</div>
                <div className={`stat-value ${stats.profitRate >= 0 ? 'positive' : 'negative'}`}>
                  {stats.profitRate >= 0 ? '+' : ''}{stats.profitRate.toFixed(2)}%
                </div>
              </div>
            </>
          )}
        </div>

        {/* 매수/매도 모달 */}
        {orderModal.show && (
          <div style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'rgba(0, 0, 0, 0.7)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000
          }}>
            <div style={{
              background: '#1a1f3a',
              borderRadius: '12px',
              padding: '30px',
              width: '450px',
              maxWidth: '90%',
              border: '2px solid #00d4ff'
            }}>
              <h3 style={{ color: '#00d4ff', marginBottom: '20px', fontSize: '20px' }}>
                {orderModal.type === 'BUY' ? '매수 주문' : '매도 주문'}
                {tradingMode === 'TEST' && (
                  <span style={{
                    marginLeft: '10px',
                    fontSize: '14px',
                    background: '#10b981',
                    padding: '4px 8px',
                    borderRadius: '4px',
                    color: '#fff'
                  }}>
                    테스트 모드
                  </span>
                )}
              </h3>

              <div style={{ marginBottom: '20px' }}>
                <div style={{ color: '#e8eaf6', marginBottom: '10px' }}>
                  <strong>{orderModal.coin?.coinName || orderModal.coin?.name}</strong>
                  <span style={{ color: '#b8bfd8', marginLeft: '10px' }}>
                    {orderModal.coin?.market}
                  </span>
                </div>
                {orderModal.coin?.currentPrice && (
                  <div style={{ color: '#b8bfd8', fontSize: '14px' }}>
                    현재가: {orderModal.coin.currentPrice.toLocaleString()} KRW
                  </div>
                )}
              </div>

              <div style={{ marginBottom: '15px' }}>
                <label style={{ display: 'block', color: '#b8bfd8', marginBottom: '5px' }}>
                  가격 (KRW)
                </label>
                <input
                  type="number"
                  value={orderPrice}
                  onChange={(e) => setOrderPrice(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '10px',
                    background: '#0f1429',
                    border: '1px solid #2d3748',
                    borderRadius: '8px',
                    color: '#e8eaf6',
                    fontSize: '16px'
                  }}
                  placeholder="가격을 입력하세요"
                />
              </div>

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', color: '#b8bfd8', marginBottom: '5px' }}>
                  수량
                </label>
                <input
                  type="number"
                  value={orderAmount}
                  onChange={(e) => setOrderAmount(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '10px',
                    background: '#0f1429',
                    border: '1px solid #2d3748',
                    borderRadius: '8px',
                    color: '#e8eaf6',
                    fontSize: '16px'
                  }}
                  placeholder="수량을 입력하세요"
                />
              </div>

              {orderPrice && orderAmount && (
                <div style={{
                  padding: '10px',
                  background: '#0f1429',
                  borderRadius: '8px',
                  marginBottom: '20px',
                  color: '#b8bfd8'
                }}>
                  총 금액: <strong style={{ color: '#00d4ff' }}>
                    {(parseFloat(orderPrice) * parseFloat(orderAmount)).toLocaleString()} KRW
                  </strong>
                </div>
              )}

              {orderMessage && (
                <div style={{
                  padding: '10px',
                  marginBottom: '15px',
                  background: orderMessage.includes('성공') || orderMessage.includes('체결') ? '#10b981' : '#ef4444',
                  color: '#fff',
                  borderRadius: '8px',
                  textAlign: 'center'
                }}>
                  {orderMessage}
                </div>
              )}

              <div style={{ display: 'flex', gap: '10px' }}>
                <button
                  onClick={handleExecuteOrder}
                  disabled={orderLoading}
                  style={{
                    flex: 1,
                    padding: '12px',
                    background: orderModal.type === 'BUY' ? '#10b981' : '#ef4444',
                    color: '#fff',
                    border: 'none',
                    borderRadius: '8px',
                    fontSize: '16px',
                    fontWeight: 'bold',
                    cursor: orderLoading ? 'not-allowed' : 'pointer',
                    opacity: orderLoading ? 0.6 : 1
                  }}
                >
                  {orderLoading ? '처리 중...' : (orderModal.type === 'BUY' ? '매수 실행' : '매도 실행')}
                </button>
                <button
                  onClick={closeOrderModal}
                  disabled={orderLoading}
                  style={{
                    flex: 1,
                    padding: '12px',
                    background: '#2d3748',
                    color: '#fff',
                    border: 'none',
                    borderRadius: '8px',
                    fontSize: '16px',
                    fontWeight: 'bold',
                    cursor: orderLoading ? 'not-allowed' : 'pointer'
                  }}
                >
                  취소
                </button>
              </div>
            </div>
          </div>
        )}
          </div>
        )}

        {/* 백테스트 탭 */}
        {activeTab === 'backtest' && (
          <div className="tab-content">
            {/* 백테스트 서브탭 */}
            <div className="backtest-subtabs">
              <button
                className={`subtab-btn ${backtestSubTab === 'run' ? 'active' : ''}`}
                onClick={() => setBacktestSubTab('run')}
              >
                백테스트 실행
              </button>
              <button
                className={`subtab-btn ${backtestSubTab === 'data' ? 'active' : ''}`}
                onClick={() => setBacktestSubTab('data')}
              >
                데이터 관리
              </button>
            </div>

            {/* 백테스트 실행 서브탭 */}
            {backtestSubTab === 'run' && (
              <div className="backtest-layout">
                {/* 왼쪽: 폼만 */}
                <div className="backtest-left">
                  <BacktestForm
                    onSubmit={handleRunBacktest}
                    loading={backtestLoading}
                    error={backtestError}
                  />
                </div>

                {/* 오른쪽: 결과 + 이력 */}
                <div className="backtest-right">
                  <BacktestResult
                    backtestId={backtestId}
                    result={backtestResult}
                    loading={resultLoading}
                    error={resultError}
                    onRefresh={handleRefreshResult}
                  />

                  <BacktestHistory
                    history={backtestHistory}
                    loading={historyLoading}
                    onSelect={handleSelectHistory}
                    onDelete={handleDeleteHistory}
                    selectedId={backtestId}
                  />
                </div>
              </div>
            )}

            {/* 데이터 관리 서브탭 */}
            {backtestSubTab === 'data' && (
              <DataManager />
            )}
          </div>
        )}
      </div>

      {/* 백테스트 결과 모달 */}
      {resultModal.show && (
        <BacktestResultModal
          backtestId={resultModal.backtestId}
          onClose={handleCloseModal}
        />
      )}
    </MainLayout>
  );
};

export default TradingScanPage;
