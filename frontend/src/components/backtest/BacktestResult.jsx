import React, { useState, useEffect } from 'react';
import backtestApi from '../../api/backtestApi';
import './Backtest.css';

const BacktestResult = ({ backtestId, result, loading, error, onRefresh }) => {
  const [tradeHistory, setTradeHistory] = useState([]);
  const [tradesLoading, setTradesLoading] = useState(false);
  const [tradesError, setTradesError] = useState('');

  // 거래 내역 조회
  useEffect(() => {
    const fetchTrades = async () => {
      if (!backtestId || !result || result.status !== 'COMPLETED') {
        setTradeHistory([]);
        return;
      }

      try {
        setTradesLoading(true);
        setTradesError('');
        const response = await backtestApi.getTrades(backtestId);

        if (response.success) {
          setTradeHistory(response.trades || []);
        } else {
          setTradesError('거래 내역 조회 실패');
        }
      } catch (err) {
        console.error('거래 내역 조회 오류:', err);
        setTradesError(err.response?.data?.message || '거래 내역 조회 중 오류가 발생했습니다');
      } finally {
        setTradesLoading(false);
      }
    };

    fetchTrades();
  }, [backtestId, result]);
  // 로딩 상태
  if (loading) {
    return (
      <div className="backtest-result">
        <div className="result-loading">
          <div className="spinner"></div>
          <p>백테스트 결과를 생성하는 중입니다...</p>
          <small>약 1-2분 소요됩니다</small>
        </div>
      </div>
    );
  }

  // 에러 상태
  if (error) {
    return (
      <div className="backtest-result">
        <div className="result-error">
          <h3>백테스트 실행 실패</h3>
          <p>{error}</p>
          {backtestId && (
            <button className="refresh-btn" onClick={onRefresh}>
              다시 시도
            </button>
          )}
        </div>
      </div>
    );
  }

  // 결과 없음
  if (!result) {
    return (
      <div className="backtest-result">
        <div className="result-empty">
          <h3>백테스트를 실행하세요</h3>
          <p>왼쪽 폼에서 백테스트 설정을 입력하고 실행 버튼을 클릭하세요.</p>
        </div>
      </div>
    );
  }

  // 결과 데이터
  const { config, performance, trades, executionTime, completedAt } = result;

  // 수익률에 따른 색상 클래스
  const getProfitClass = (value) => {
    if (value > 0) return 'positive';
    if (value < 0) return 'negative';
    return '';
  };

  return (
    <div className="backtest-result">
      {/* 백테스트 정보 */}
      <div className="result-info">
        <h3>{config.name}</h3>
        <div className="info-details">
          <span className="info-item">
            <strong>마켓:</strong> {config.market}
          </span>
          <span className="info-item">
            <strong>전략:</strong> {config.strategy}
          </span>
          <span className="info-item">
            <strong>기간:</strong> {config.period}
          </span>
          <span className="info-item">
            <strong>초기 자본:</strong> {config.initialCapital.toLocaleString()} KRW
          </span>
          <span className="info-item">
            <strong>실행 시간:</strong> {(executionTime / 1000).toFixed(2)}초
          </span>
          {completedAt && (
            <span className="info-item">
              <strong>완료:</strong> {new Date(completedAt).toLocaleString()}
            </span>
          )}
        </div>
      </div>

      {/* 성과 지표 그리드 */}
      <div className="performance-section">
        <h4>성과 지표</h4>
        <div className="performance-grid">
          {/* 총 수익률 */}
          <div className={`performance-card ${getProfitClass(performance.totalReturn)}-card`}>
            <div className="performance-label">총 수익률</div>
            <div className={`performance-value ${getProfitClass(performance.totalReturn)}`}>
              {performance.totalReturn !== null && performance.totalReturn !== undefined
                ? `${performance.totalReturn > 0 ? '+' : ''}${performance.totalReturn.toFixed(2)}%`
                : '-'}
            </div>
            {performance.finalCapital && (
              <div className="performance-subvalue">
                {(performance.finalCapital - config.initialCapital).toLocaleString()} KRW
              </div>
            )}
          </div>

          {/* 연간 수익률 */}
          <div className={`performance-card ${getProfitClass(performance.annualReturn)}-card`}>
            <div className="performance-label">연간 수익률</div>
            <div className={`performance-value ${getProfitClass(performance.annualReturn)}`}>
              {performance.annualReturn !== null && performance.annualReturn !== undefined
                ? `${performance.annualReturn > 0 ? '+' : ''}${performance.annualReturn.toFixed(2)}%`
                : '-'}
            </div>
          </div>

          {/* 최대 낙폭 */}
          <div className="performance-card">
            <div className="performance-label">최대 낙폭</div>
            <div className={`performance-value ${getProfitClass(performance.maxDrawdown)}`}>
              {performance.maxDrawdown !== null && performance.maxDrawdown !== undefined
                ? `${performance.maxDrawdown.toFixed(2)}%`
                : '-'}
            </div>
          </div>

          {/* 샤프 비율 */}
          <div className="performance-card">
            <div className="performance-label">샤프 비율</div>
            <div className="performance-value">
              {performance.sharpeRatio !== null && performance.sharpeRatio !== undefined
                ? performance.sharpeRatio.toFixed(2)
                : '-'}
            </div>
          </div>

          {/* 승률 */}
          <div className="performance-card">
            <div className="performance-label">승률</div>
            <div className="performance-value">
              {performance.winRate !== null && performance.winRate !== undefined
                ? `${performance.winRate.toFixed(1)}%`
                : '-'}
            </div>
          </div>

          {/* 최종 자본 */}
          <div className="performance-card">
            <div className="performance-label">최종 자본</div>
            <div className="performance-value">
              {performance.finalCapital !== null && performance.finalCapital !== undefined
                ? `${performance.finalCapital.toLocaleString()} KRW`
                : '-'}
            </div>
          </div>
        </div>
      </div>

      {/* 거래 통계 */}
      <div className="trades-section">
        <h4>거래 통계</h4>
        <div className="trades-grid">
          <div className="trade-stat">
            <div className="stat-label">총 거래 수</div>
            <div className="stat-value">{trades.total || 0}회</div>
          </div>
          <div className="trade-stat positive">
            <div className="stat-label">승리 거래</div>
            <div className="stat-value">{trades.winning || 0}회</div>
          </div>
          <div className="trade-stat negative">
            <div className="stat-label">손실 거래</div>
            <div className="stat-value">{trades.losing || 0}회</div>
          </div>
          {trades.avgProfit !== null && trades.avgProfit !== undefined && trades.avgProfit !== 'Infinity' && (
            <div className="trade-stat">
              <div className="stat-label">평균 수익</div>
              <div className="stat-value">
                {typeof trades.avgProfit === 'number'
                  ? `${trades.avgProfit.toFixed(2)}%`
                  : '-'}
              </div>
            </div>
          )}
          {trades.avgLoss !== null && trades.avgLoss !== undefined && (
            <div className="trade-stat">
              <div className="stat-label">평균 손실</div>
              <div className="stat-value">
                {typeof trades.avgLoss === 'number'
                  ? `${trades.avgLoss.toFixed(2)}%`
                  : '-'}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 거래 내역 */}
      <div className="trade-history-section">
        <h4>거래 내역</h4>
        {tradesLoading && (
          <div className="trades-loading">
            <div className="spinner-small"></div>
            <p>거래 내역을 불러오는 중...</p>
          </div>
        )}
        {tradesError && (
          <div className="trades-error">
            <p>{tradesError}</p>
          </div>
        )}
        {!tradesLoading && !tradesError && tradeHistory.length === 0 && (
          <div className="trades-empty">
            <p>거래 내역이 없습니다.</p>
          </div>
        )}
        {!tradesLoading && !tradesError && tradeHistory.length > 0 && (
          <div className="trades-table-container">
            <table className="trades-table">
              <thead>
                <tr>
                  <th>No</th>
                  <th>타입</th>
                  <th>시각</th>
                  <th>가격</th>
                  <th>수량</th>
                  <th>거래액</th>
                  <th>수수료</th>
                  <th>수익률</th>
                  <th>현금 잔고</th>
                  <th>코인 보유</th>
                </tr>
              </thead>
              <tbody>
                {tradeHistory.map((trade, index) => (
                  <tr key={trade.id} className={trade.type.toLowerCase()}>
                    <td>{index + 1}</td>
                    <td>
                      <span className={`trade-type ${trade.type.toLowerCase()}`}>
                        {trade.type}
                      </span>
                    </td>
                    <td>{new Date(trade.timestamp).toLocaleString()}</td>
                    <td>{(trade.price || 0).toLocaleString()} KRW</td>
                    <td>{typeof trade.amount === 'number' ? trade.amount.toFixed(8) : '-'}</td>
                    <td>{(trade.total || 0).toLocaleString()} KRW</td>
                    <td>{(trade.commission || 0).toLocaleString()} KRW</td>
                    <td>
                      {trade.profitRate !== null && trade.profitRate !== undefined && typeof trade.profitRate === 'number'
                        ? (
                          <span className={trade.profitRate > 0 ? 'positive' : trade.profitRate < 0 ? 'negative' : ''}>
                            {trade.profitRate > 0 ? '+' : ''}{trade.profitRate.toFixed(2)}%
                          </span>
                        )
                        : '-'}
                    </td>
                    <td>{(trade.balanceAfter || 0).toLocaleString()} KRW</td>
                    <td>{typeof trade.positionAfter === 'number' ? trade.positionAfter.toFixed(8) : '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="trades-summary">
              <p>총 {tradeHistory.length}개의 거래</p>
            </div>
          </div>
        )}
      </div>

      {/* 차트 섹션 (향후 구현) */}
      <div className="chart-section">
        <h4>자산 곡선 차트</h4>
        <div className="chart-placeholder">
          <p>차트 기능은 향후 업데이트 예정입니다.</p>
          <small>백엔드에서 equity curve 데이터를 제공하면 Recharts로 구현됩니다.</small>
        </div>
      </div>

      {/* 새로고침 버튼 */}
      <button className="refresh-btn" onClick={onRefresh}>
        결과 새로고침
      </button>
    </div>
  );
};

export default BacktestResult;
