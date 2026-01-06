import React, { useState, useEffect } from 'react';
import backtestApi from '../../api/backtestApi';
import './Backtest.css';

const DataManager = () => {
  // 데이터 수집 폼 state
  const [syncForm, setSyncForm] = useState({
    market: 'KRW-BTC',
    timeframe: '15m',
    startDate: '2025-12-20',
    endDate: '2026-01-05'
  });

  // 수집 상태
  const [syncing, setSyncing] = useState(false);
  const [syncTaskId, setSyncTaskId] = useState(null);
  const [syncStatus, setSyncStatus] = useState(null);
  const [syncError, setSyncError] = useState('');

  // 데이터 수집 이력
  const [syncHistory, setSyncHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);

  // 컴포넌트 마운트 시 수집 이력 조회
  useEffect(() => {
    loadSyncHistory();
  }, []);

  // 수집 진행 중일 때 폴링
  useEffect(() => {
    if (!syncTaskId || !syncing) return;

    const interval = setInterval(async () => {
      try {
        const status = await backtestApi.getSyncStatus(syncTaskId);
        setSyncStatus(status);

        if (status.status === 'COMPLETED') {
          setSyncing(false);
          setSyncTaskId(null);
          setSyncError('');
          loadSyncHistory(); // 수집 이력 새로고침
        } else if (status.status === 'FAILED') {
          setSyncing(false);
          setSyncTaskId(null);
          setSyncError(status.message || '데이터 수집 실패');
          loadSyncHistory(); // 실패해도 이력 갱신
        }
      } catch (error) {
        console.error('수집 상태 조회 실패:', error);
      }
    }, 2000); // 2초마다 폴링

    return () => clearInterval(interval);
  }, [syncTaskId, syncing]);

  // 데이터 수집 이력 로드
  const loadSyncHistory = async () => {
    setHistoryLoading(true);
    try {
      const response = await backtestApi.getSyncHistory();
      if (response.success) {
        setSyncHistory(response.histories || []);
      }
    } catch (error) {
      console.error('수집 이력 조회 실패:', error);
    } finally {
      setHistoryLoading(false);
    }
  };

  // 폼 입력 핸들러
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setSyncForm(prev => ({ ...prev, [name]: value }));
  };

  // 데이터 수집 시작
  const handleSyncData = async (e) => {
    e.preventDefault();
    setSyncError('');

    // 유효성 검사
    if (new Date(syncForm.startDate) > new Date(syncForm.endDate)) {
      setSyncError('시작일은 종료일보다 이전이어야 합니다');
      return;
    }

    try {
      setSyncing(true);
      const response = await backtestApi.syncData(syncForm);

      if (response.success) {
        setSyncTaskId(response.taskId);
        setSyncStatus({
          taskId: response.taskId,
          status: 'IN_PROGRESS',
          progress: 0,
          recordsProcessed: 0,
          totalRecords: response.estimatedRecords,
          message: '데이터 수집 시작'
        });
      } else {
        setSyncError(response.message || '데이터 수집 시작 실패');
        setSyncing(false);
      }
    } catch (error) {
      console.error('데이터 수집 시작 실패:', error);
      setSyncError(error.response?.data?.message || '데이터 수집 시작 실패');
      setSyncing(false);
    }
  };

  // 날짜 포맷
  const formatDate = (dateString) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="data-manager">
      {/* 데이터 수집 폼 */}
      <div className="data-sync-section">
        <h3>과거 데이터 수집</h3>
        <p className="section-description">
          백테스트를 실행하기 전에 과거 캔들 데이터를 수집해야 합니다.
        </p>

        <form onSubmit={handleSyncData} className="sync-form">
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">마켓</label>
              <select
                name="market"
                value={syncForm.market}
                onChange={handleInputChange}
                className="form-select"
                disabled={syncing}
              >
                <option value="KRW-BTC">비트코인 (BTC)</option>
                <option value="KRW-ETH">이더리움 (ETH)</option>
                <option value="KRW-XRP">리플 (XRP)</option>
                <option value="KRW-ADA">에이다 (ADA)</option>
                <option value="KRW-SOL">솔라나 (SOL)</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">타임프레임</label>
              <select
                name="timeframe"
                value={syncForm.timeframe}
                onChange={handleInputChange}
                className="form-select"
                disabled={syncing}
              >
                <option value="1m">1분봉</option>
                <option value="5m">5분봉</option>
                <option value="15m">15분봉</option>
                <option value="30m">30분봉</option>
                <option value="1h">1시간봉</option>
                <option value="1d">일봉</option>
              </select>
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">시작일</label>
              <input
                type="date"
                name="startDate"
                value={syncForm.startDate}
                onChange={handleInputChange}
                className="form-input"
                disabled={syncing}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">종료일</label>
              <input
                type="date"
                name="endDate"
                value={syncForm.endDate}
                onChange={handleInputChange}
                className="form-input"
                disabled={syncing}
                required
              />
            </div>
          </div>

          {syncError && (
            <div className="form-error">{syncError}</div>
          )}

          <button
            type="submit"
            className="submit-btn"
            disabled={syncing}
          >
            {syncing ? '수집 중...' : '데이터 수집 시작'}
          </button>
        </form>

        {/* 수집 진행 상태 */}
        {syncStatus && syncing && (
          <div className="sync-progress">
            <div className="progress-header">
              <span className="progress-label">진행 상태</span>
              <span className="progress-percentage">
                {syncStatus.progress?.toFixed(1) || 0}%
              </span>
            </div>
            <div className="progress-bar-container">
              <div
                className="progress-bar-fill"
                style={{ width: `${syncStatus.progress || 0}%` }}
              ></div>
            </div>
            <div className="progress-details">
              <span>{syncStatus.message}</span>
              <span>
                {syncStatus.recordsProcessed?.toLocaleString() || 0} / {syncStatus.totalRecords?.toLocaleString() || 0} 건
              </span>
            </div>
          </div>
        )}
      </div>

      {/* 데이터 수집 이력 */}
      <div className="available-data-section">
        <div className="section-header">
          <h3>데이터 수집 이력</h3>
          <button
            className="refresh-btn"
            onClick={loadSyncHistory}
            disabled={historyLoading}
          >
            {historyLoading ? '로딩 중...' : '새로고침'}
          </button>
        </div>

        {historyLoading ? (
          <div className="data-loading">
            <div className="spinner"></div>
            <span>수집 이력 조회 중...</span>
          </div>
        ) : syncHistory.length === 0 ? (
          <div className="data-empty">
            <p>수집 이력이 없습니다.</p>
            <small>위 폼에서 데이터를 수집해주세요.</small>
          </div>
        ) : (
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>마켓</th>
                  <th>타임프레임</th>
                  <th>수집 기간</th>
                  <th>레코드 수</th>
                  <th>상태</th>
                  <th>수집 시각</th>
                </tr>
              </thead>
              <tbody>
                {syncHistory.map((history, index) => (
                  <tr key={index}>
                    <td className="market-cell">{history.market}</td>
                    <td>{history.timeframe}</td>
                    <td className="date-cell">
                      {history.startDate} ~ {history.endDate}
                    </td>
                    <td className="count-cell">{history.recordCount?.toLocaleString() || 0}</td>
                    <td>
                      <span className={`status-badge status-${history.status?.toLowerCase()}`}>
                        {history.status === 'COMPLETED' ? '완료' :
                         history.status === 'IN_PROGRESS' ? '진행중' :
                         history.status === 'FAILED' ? '실패' : history.status}
                      </span>
                    </td>
                    <td className="date-cell">{formatDate(history.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default DataManager;
