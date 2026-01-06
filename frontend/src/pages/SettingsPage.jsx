import React, { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../components/layout/MainLayout';
import tradingApi from '../api/tradingApi';
import './SettingsPage.css';

const SettingsPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [tradingMode, setTradingMode] = useState('TEST');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    loadTradingMode();
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

  const handleModeSwitch = async (mode) => {
    if (mode === 'LIVE') {
      if (!window.confirm('실거래 모드로 전환하시겠습니까?\n\n⚠️ 주의: 실제 자금이 사용됩니다!')) {
        return;
      }
    }

    setLoading(true);
    setMessage('');

    try {
      const response = await tradingApi.switchMode(mode);
      if (response.success) {
        setTradingMode(mode);
        setMessage(response.message);
        setTimeout(() => setMessage(''), 3000);
      }
    } catch (error) {
      console.error('모드 전환 실패:', error);
      setMessage('모드 전환에 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  const handleResetTestMode = async () => {
    if (!window.confirm('테스트 모드를 초기화하시겠습니까?\n\n모든 거래 내역과 잔고가 삭제됩니다.')) {
      return;
    }

    setLoading(true);
    try {
      const response = await tradingApi.resetTestMode();
      if (response.success) {
        setMessage(response.message);
        setTimeout(() => setMessage(''), 3000);
      }
    } catch (error) {
      console.error('테스트 모드 초기화 실패:', error);
      setMessage('초기화에 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  return (
    <MainLayout>
      <div className="settings-page">
        <div className="settings-grid">
          {/* 사용자 정보 카드 */}
          <div className="settings-card user-info-card">
            <h2 className="card-title">사용자 정보</h2>

            <div className="info-group">
              <div className="info-label">이메일:</div>
              <div className="info-value">{user?.email || '-'}</div>
            </div>

            <div className="info-group">
              <div className="info-label">사용자명:</div>
              <div className="info-value">{user?.username || '-'}</div>
            </div>

            <div className="info-group">
              <div className="info-label">전화번호:</div>
              <div className="info-value">{user?.phoneNumber || '미등록'}</div>
            </div>

            <div className="info-group">
              <div className="info-label">업비트 API 키:</div>
              <div className="info-value api-status">
                {user?.apiKeyCount > 0 ? (
                  <>
                    <span className="status-icon">✓</span> {user.apiKeyCount}개 등록됨
                  </>
                ) : (
                  <>
                    <span className="status-icon">✗</span> 등록된 키 없음
                  </>
                )}
              </div>
            </div>

            <button className="settings-action-btn" onClick={() => navigate('/profile-edit')}>
              개인설정
            </button>
          </div>

          {/* 거래 모드 설정 카드 */}
          <div className="settings-card trading-mode-card">
            <h2 className="card-title">거래 모드 설정</h2>

            {message && (
              <div style={{
                padding: '10px',
                marginBottom: '15px',
                background: '#10b981',
                color: '#fff',
                borderRadius: '8px',
                textAlign: 'center'
              }}>
                {message}
              </div>
            )}

            <div className="mode-toggle-container" style={{ marginBottom: '20px' }}>
              <div className="mode-info" style={{ marginBottom: '15px', color: '#b8bfd8' }}>
                <strong>현재 모드:</strong>{' '}
                <span style={{
                  color: tradingMode === 'TEST' ? '#10b981' : '#ef4444',
                  fontWeight: 'bold'
                }}>
                  {tradingMode === 'TEST' ? '테스트 모드' : '실거래 모드'}
                </span>
              </div>

              <div style={{ display: 'flex', gap: '10px', marginBottom: '15px' }}>
                <button
                  onClick={() => handleModeSwitch('TEST')}
                  disabled={loading || tradingMode === 'TEST'}
                  style={{
                    flex: 1,
                    padding: '12px',
                    background: tradingMode === 'TEST' ? '#10b981' : '#2d3748',
                    color: '#fff',
                    border: 'none',
                    borderRadius: '8px',
                    cursor: tradingMode === 'TEST' ? 'default' : 'pointer',
                    fontWeight: 'bold',
                    opacity: tradingMode === 'TEST' ? 1 : 0.7
                  }}
                >
                  테스트 모드
                </button>

                <button
                  onClick={() => handleModeSwitch('LIVE')}
                  disabled={loading || tradingMode === 'LIVE'}
                  style={{
                    flex: 1,
                    padding: '12px',
                    background: tradingMode === 'LIVE' ? '#ef4444' : '#2d3748',
                    color: '#fff',
                    border: 'none',
                    borderRadius: '8px',
                    cursor: tradingMode === 'LIVE' ? 'default' : 'pointer',
                    fontWeight: 'bold',
                    opacity: tradingMode === 'LIVE' ? 1 : 0.7
                  }}
                >
                  실거래 모드
                </button>
              </div>

              <div style={{ fontSize: '13px', color: '#9ca3af', lineHeight: '1.5' }}>
                {tradingMode === 'TEST' ? (
                  <>
                    • 가상 자금으로 매매를 연습할 수 있습니다<br />
                    • 실제 자금은 사용되지 않습니다<br />
                    • 초기 자금: 10,000,000 KRW
                  </>
                ) : (
                  <>
                    ⚠️ 실제 업비트 API를 통해 거래됩니다<br />
                    ⚠️ 실제 자금이 사용됩니다<br />
                    ⚠️ 신중하게 사용하세요
                  </>
                )}
              </div>
            </div>

            {tradingMode === 'TEST' && (
              <button
                onClick={handleResetTestMode}
                disabled={loading}
                style={{
                  width: '100%',
                  padding: '12px',
                  background: '#ef4444',
                  color: '#fff',
                  border: 'none',
                  borderRadius: '8px',
                  cursor: 'pointer',
                  fontWeight: 'bold'
                }}
              >
                테스트 모드 초기화
              </button>
            )}
          </div>

          {/* 환영 메시지 카드 */}
          <div className="settings-card welcome-card">
            <h2 className="card-title welcome-title">환영합니다!</h2>

            <p className="welcome-text">
              Upbit Cryptobot에 로그인하셨습니다.
            </p>

            <p className="welcome-text">
              여기에서 암호화폐 자동매매를 관리할 수 있습니다.
            </p>

            <button className="api-manage-btn" onClick={() => navigate('/api-keys')}>
              API 키 관리
            </button>
          </div>
        </div>
      </div>
    </MainLayout>
  );
};

export default SettingsPage;
