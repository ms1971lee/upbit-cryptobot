import React, { useState, useEffect } from 'react';
import MainLayout from '../components/layout/MainLayout';
import scanApi from '../api/scanApi';
import './TradingScanPage.css';

const TradingScanPage = () => {
  const [strategy, setStrategy] = useState('V1');
  const [strategies, setStrategies] = useState([]);
  const [timeFrame, setTimeFrame] = useState('5m');
  const [scanResults, setScanResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // 전략 목록 로드
  useEffect(() => {
    loadStrategies();
  }, []);

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

  // 코인 아이콘 가져오기
  const getCoinIcon = (market) => {
    const symbol = market.replace('KRW-', '');
    const icons = {
      'BTC': '₿',
      'ETH': 'Ξ',
      'XRP': '✖',
      'DOGE': '🐶',
      'ADA': '₳'
    };
    return icons[symbol] || symbol.charAt(0);
  };

  const holdings = [
    {
      id: 1,
      coinName: '비트코인',
      symbol: 'BTC',
      quantity: '123,1254,215',
      buyPrice: '+1.56%',
      currentPrice: '123,1254,215',
      evalPrice: '123,1254,215',
      profitRate: '+1.56%',
      icon: '₿'
    },
    {
      id: 2,
      coinName: '이더리움',
      symbol: 'ETH',
      quantity: '4,500,000',
      buyPrice: '-0.32%',
      currentPrice: '4,500,000',
      evalPrice: '4,500,000',
      profitRate: '-0.32%',
      icon: 'Ξ'
    }
  ];

  const stats = {
    totalBuy: 3548000,
    totalAssets: 2548000,
    totalProfit: 1000000,
    profitRate: 1.56
  };

  return (
    <MainLayout>
      <div className="trading-scan-page">
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
                          <button className={`trade-btn ${item.signal === 'BUY' ? 'buy-btn' : 'sell-btn'}`}>
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

            <div className="holdings-table-wrapper">
              <table className="holdings-table">
                <thead>
                  <tr>
                    <th>코인명</th>
                    <th>보유수량</th>
                    <th>매수금액</th>
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
                          <span className="coin-name">{item.coinName}</span>
                        </div>
                      </td>
                      <td className="price">{item.quantity}</td>
                      <td className={item.buyPrice.startsWith('+') ? 'positive' : 'negative'}>
                        {item.buyPrice}
                      </td>
                      <td className="price">{item.currentPrice}</td>
                      <td className="price">{item.evalPrice}</td>
                      <td className={item.profitRate.startsWith('+') ? 'positive' : 'negative'}>
                        {item.profitRate}
                      </td>
                      <td>
                        <button className="sell-btn">매도</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* 우측 통계 영역 */}
        <div className="trading-stats">
          <div className="stat-card">
            <div className="stat-label">총 매수금액</div>
            <div className="stat-value">{stats.totalBuy.toLocaleString()}원</div>
          </div>
          <div className="stat-card">
            <div className="stat-label">보유자산</div>
            <div className="stat-value">{stats.totalAssets.toLocaleString()}원</div>
          </div>
          <div className="stat-card positive-card">
            <div className="stat-label">총평가수익</div>
            <div className="stat-value positive">+{stats.totalProfit.toLocaleString()}원</div>
          </div>
          <div className="stat-card positive-card">
            <div className="stat-label">총평가수익률</div>
            <div className="stat-value positive">+{stats.profitRate}%</div>
          </div>
        </div>
      </div>
    </MainLayout>
  );
};

export default TradingScanPage;
