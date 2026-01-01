import React from 'react';
import { useAuth } from '../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import './DashboardPage.css';

const DashboardPage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="dashboard-container">
      <div className="dashboard-header">
        <h1>Upbit Cryptobot Dashboard</h1>
        <button onClick={handleLogout} className="logout-button">
          로그아웃
        </button>
      </div>

      <div className="dashboard-content">
        <div className="user-info-card">
          <h2>사용자 정보</h2>
          <div className="info-row">
            <span className="label">이메일:</span>
            <span className="value">{user?.email}</span>
          </div>
          <div className="info-row">
            <span className="label">사용자명:</span>
            <span className="value">{user?.username || '미설정'}</span>
          </div>
          <div className="info-row">
            <span className="label">전화번호:</span>
            <span className="value">{user?.phoneNumber || '미설정'}</span>
          </div>
          <div className="info-row">
            <span className="label">업비트 API 키:</span>
            <span className="value">
              {user?.hasUpbitApiKey ? '✅ 설정됨' : '❌ 미설정'}
            </span>
          </div>
        </div>

        <div className="welcome-card">
          <h2>환영합니다!</h2>
          <p>Upbit Cryptobot에 로그인하셨습니다.</p>
          <p>여기에서 암호화폐 자동매매를 관리할 수 있습니다.</p>
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
