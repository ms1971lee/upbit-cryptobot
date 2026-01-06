import React, { useState, useEffect } from 'react';
import backtestApi from '../../api/backtestApi';
import './Backtest.css';

const BacktestResultModal = ({ backtestId, onClose }) => {
  const [result, setResult] = useState(null);
  const [tradeHistory, setTradeHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [tradesLoading, setTradesLoading] = useState(false);

  useEffect(() => {
    const loadResult = async () => {
      setLoading(true);
      setError('');

      try {
        const response = await backtestApi.getResult(backtestId);
        if (response.success) {
          setResult(response.result);
        } else {
          setError('결과를 불러올 수 없습니다');
        }
      } catch (err) {
        console.error('백테스트 결과 조회 실패:', err);
        setError(err.response?.data?.message || '결과 조회 중 오류가 발생했습니다');
      } finally {
        setLoading(false);
      }
    };

    const loadTrades = async () => {
      setTradesLoading(true);

      try {
        const response = await backtestApi.getTrades(backtestId);
        if (response.success) {
          setTradeHistory(response.trades || []);
        }
      } catch (err) {
        console.error('거래 내역 조회 실패:', err);
      } finally {
        setTradesLoading(false);
      }
    };

    if (backtestId) {
      loadResult();
      loadTrades();
    }
  }, [backtestId]);

  const getProfitClass = (value) => {
    if (value > 0) return 'positive';
    if (value < 0) return 'negative';
    return '';
  };

  // ESC 키로 닫기
  useEffect(() => {
    const handleEsc = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleEsc);
    return () => window.removeEventListener('keydown', handleEsc);
  }, [onClose]);

  // 배경 클릭 시 닫기
  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div className="modal-backdrop" onClick={handleBackdropClick}>
      <div className="modal-container backtest-result-modal">
        <div className="modal-header">
          <h2>백테스트 결과</h2>
          <button className="modal-close-btn" onClick={onClose}>
            ✕
          </button>
        </div>

        <div className="modal-body">
          {loading && (
            <div className="modal-loading">
              <div className="spinner"></div>
              <p>결과를 불러오는 중...</p>
            </div>
          )}

          {error && (
            <div className="modal-error">
              <p>{error}</p>
            </div>
          )}

          {!loading && !error && result && (
            <>
              {/* 백테스트 정보 */}
              <div className="result-info">
                <h3>{result.config.name}</h3>
                <div className="info-details">
                  <span className="info-item">
                    <strong>마켓:</strong> {result.config.market}
                  </span>
                  <span className="info-item">
                    <strong>전략:</strong> {result.config.strategy}
                  </span>
                  <span className="info-item">
                    <strong>기간:</strong> {result.config.period}
                  </span>
                  <span className="info-item">
                    <strong>초기 자본:</strong> {result.config.initialCapital.toLocaleString()} KRW
                  </span>
                </div>
              </div>

              {/* 성과 지표 그리드 */}
              <div className="performance-section">
                <h4>성과 지표</h4>
                <div className="performance-grid">
                  {/* 총 수익률 */}
                  <div className={`performance-card ${getProfitClass(result.performance.totalReturn)}-card`}>
                    <div className="performance-label">총 수익률</div>
                    <div className={`performance-value ${getProfitClass(result.performance.totalReturn)}`}>
                      {result.performance.totalReturn !== null && result.performance.totalReturn !== undefined
                        ? `${result.performance.totalReturn > 0 ? '+' : ''}${result.performance.totalReturn.toFixed(2)}%`
                        : '-'}
                    </div>
                  </div>

                  {/* 최대 낙폭 */}
                  <div className="performance-card">
                    <div className="performance-label">최대 낙폭</div>
                    <div className={`performance-value ${getProfitClass(result.performance.maxDrawdown)}`}>
                      {result.performance.maxDrawdown !== null && result.performance.maxDrawdown !== undefined
                        ? `${result.performance.maxDrawdown.toFixed(2)}%`
                        : '-'}
                    </div>
                  </div>

                  {/* 샤프 비율 */}
                  <div className="performance-card">
                    <div className="performance-label">샤프 비율</div>
                    <div className="performance-value">
                      {result.performance.sharpeRatio !== null && result.performance.sharpeRatio !== undefined
                        ? result.performance.sharpeRatio.toFixed(2)
                        : '-'}
                    </div>
                  </div>

                  {/* 승률 */}
                  <div className="performance-card">
                    <div className="performance-label">승률</div>
                    <div className="performance-value">
                      {result.performance.winRate !== null && result.performance.winRate !== undefined
                        ? `${result.performance.winRate.toFixed(1)}%`
                        : '-'}
                    </div>
                  </div>

                  {/* 최종 자본 */}
                  <div className="performance-card">
                    <div className="performance-label">최종 자본</div>
                    <div className="performance-value">
                      {result.performance.finalCapital !== null && result.performance.finalCapital !== undefined
                        ? `${result.performance.finalCapital.toLocaleString()} KRW`
                        : '-'}
                    </div>
                  </div>

                  {/* 총 거래 수 */}
                  <div className="performance-card">
                    <div className="performance-label">총 거래 수</div>
                    <div className="performance-value">
                      {result.trades.total || 0}회
                    </div>
                  </div>
                </div>
              </div>

              {/* 거래 내역 */}
              {!tradesLoading && tradeHistory.length > 0 && (
                <div className="trade-history-section">
                  <h4>거래 내역</h4>
                  <div className="trades-table-container modal-trades-table">
                    <table className="trades-table">
                      <thead>
                        <tr>
                          <th>No</th>
                          <th>타입</th>
                          <th>시각</th>
                          <th>가격</th>
                          <th>수량</th>
                          <th>거래액</th>
                          <th>수익률</th>
                        </tr>
                      </thead>
                      <tbody>
                        {tradeHistory.slice(0, 10).map((trade, index) => (
                          <tr key={trade.id} className={trade.type.toLowerCase()}>
                            <td>{index + 1}</td>
                            <td>
                              <span className={`trade-type ${trade.type.toLowerCase()}`}>
                                {trade.type}
                              </span>
                            </td>
                            <td>{new Date(trade.timestamp).toLocaleString()}</td>
                            <td>{trade.price.toLocaleString()} KRW</td>
                            <td>{trade.amount.toFixed(8)}</td>
                            <td>{trade.total.toLocaleString()} KRW</td>
                            <td>
                              {trade.profitRate !== null && trade.profitRate !== undefined
                                ? (
                                  <span className={trade.profitRate > 0 ? 'positive' : trade.profitRate < 0 ? 'negative' : ''}>
                                    {trade.profitRate > 0 ? '+' : ''}{trade.profitRate.toFixed(2)}%
                                  </span>
                                )
                                : '-'}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                    {tradeHistory.length > 10 && (
                      <div className="trades-summary">
                        <p>총 {tradeHistory.length}개의 거래 (최근 10개만 표시)</p>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        <div className="modal-footer">
          <button className="modal-btn modal-btn-secondary" onClick={onClose}>
            닫기
          </button>
        </div>
      </div>
    </div>
  );
};

export default BacktestResultModal;
