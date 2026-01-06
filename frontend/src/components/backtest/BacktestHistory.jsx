import React from 'react';
import './Backtest.css';

const BacktestHistory = ({ history, loading, onSelect, onDelete, selectedId }) => {
  // 로딩 상태
  if (loading) {
    return (
      <div className="backtest-history">
        <h3>백테스트 이력</h3>
        <div className="history-loading">
          <div className="spinner"></div>
          <span>로딩 중...</span>
        </div>
      </div>
    );
  }

  // 이력 없음
  if (!history || history.length === 0) {
    return (
      <div className="backtest-history">
        <h3>백테스트 이력</h3>
        <div className="history-empty">
          <p>백테스트 이력이 없습니다.</p>
        </div>
      </div>
    );
  }

  // 수익률에 따른 색상 클래스
  const getProfitClass = (value) => {
    if (value > 0) return 'positive';
    if (value < 0) return 'negative';
    return '';
  };

  // 상태 배지
  const getStatusBadge = (status) => {
    const badges = {
      COMPLETED: <span className="status-badge success">완료</span>,
      RUNNING: <span className="status-badge running">실행 중</span>,
      FAILED: <span className="status-badge error">실패</span>
    };
    return badges[status] || <span className="status-badge">{status}</span>;
  };

  return (
    <div className="backtest-history">
      <h3>백테스트 이력</h3>

      <div className="history-table-container">
        <table className="history-table">
          <thead>
            <tr>
              <th>테스트이름</th>
              <th>종목</th>
              <th>투자전략</th>
              <th>체결수량</th>
              <th>체결금액</th>
              <th>수익률</th>
              <th>상태</th>
              <th>생성일</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            {history.map((item) => (
              <tr
                key={item.id}
                className={item.id === selectedId ? 'selected' : ''}
              >
                <td className="name-cell">{item.name}</td>
                <td>{item.market}</td>
                <td>{item.strategy}</td>
                <td>{item.totalTrades || 0}</td>
                <td>
                  {item.finalCapital !== null && item.finalCapital !== undefined
                    ? item.finalCapital.toLocaleString()
                    : '-'}
                </td>
                <td className={getProfitClass(item.totalReturn)}>
                  {item.totalReturn !== null && item.totalReturn !== undefined
                    ? `${item.totalReturn > 0 ? '+' : ''}${item.totalReturn.toFixed(2)}%`
                    : '-'}
                </td>
                <td>{getStatusBadge(item.status)}</td>
                <td className="date-cell">
                  {item.createdAt
                    ? new Date(item.createdAt).toLocaleString()
                    : '-'}
                </td>
                <td className="action-cell">
                  <button
                    className="action-btn view-btn"
                    onClick={() => onSelect(item.id)}
                    title="보기"
                  >
                    보기
                  </button>
                  <button
                    className="action-btn delete-btn"
                    onClick={() => onDelete(item.id)}
                    title="삭제"
                  >
                    삭제
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default BacktestHistory;
