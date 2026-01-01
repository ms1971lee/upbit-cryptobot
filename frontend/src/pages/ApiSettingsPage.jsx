import React, { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import authAPI from '../api/authApi';
import './DashboardPage.css';
import './ApiSettingsPage.css';

const ApiSettingsPage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    upbitAccessKey: '',
    upbitSecretKey: ''
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      await authAPI.updateApiKeys(formData);
      setSuccess('API 키가 성공적으로 저장되었습니다!');
      setFormData({
        upbitAccessKey: '',
        upbitSecretKey: ''
      });
    } catch (err) {
      const errorMessage = err.response?.data?.message || 'API 키 저장에 실패했습니다';
      const errors = err.response?.data?.errors;

      if (errors) {
        const errorTexts = Object.values(errors).join(', ');
        setError(`${errorMessage}: ${errorTexts}`);
      } else {
        setError(errorMessage);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="api-settings-container">
      <div className="dashboard-header">
        <h1>Upbit Crypto Bot</h1>
        <button onClick={handleLogout} className="logout-button">
          로그아웃
        </button>
      </div>

      <div className="api-settings-content">
        <div className="api-settings-card">
          <h2>API 설정</h2>

          <div className={`api-status ${user?.hasUpbitApiKey ? 'configured' : 'not-configured'}`}>
            <span className="api-status-icon">
              {user?.hasUpbitApiKey ? '✓' : '⚠'}
            </span>
            <span className="api-status-text">
              {user?.hasUpbitApiKey
                ? 'API 키가 등록되어 있습니다'
                : 'API 키가 등록되지 않았습니다'}
            </span>
          </div>

          <div className="api-guide">
            <h3>업비트 API 키 발급 방법</h3>
            <ol>
              <li>
                <a href="https://upbit.com/mypage/open_api_management" target="_blank" rel="noopener noreferrer">
                  업비트 Open API 관리 페이지
                </a>에 접속합니다
              </li>
              <li>로그인 후 'Open API 키 발급' 버튼을 클릭합니다</li>
              <li>필요한 권한을 선택합니다 (자산조회, 주문조회, 주문하기 등)</li>
              <li>발급받은 Access Key와 Secret Key를 아래에 입력합니다</li>
            </ol>
          </div>

          <form onSubmit={handleSubmit}>
            {error && <div className="error-message">{error}</div>}
            {success && <div className="success-message">{success}</div>}

            <div className="form-group">
              <label htmlFor="upbitAccessKey">Access Key *</label>
              <input
                type="text"
                id="upbitAccessKey"
                name="upbitAccessKey"
                value={formData.upbitAccessKey}
                onChange={handleChange}
                required
                placeholder="업비트 API Access Key"
              />
            </div>

            <div className="form-group">
              <label htmlFor="upbitSecretKey">Secret Key *</label>
              <input
                type="password"
                id="upbitSecretKey"
                name="upbitSecretKey"
                value={formData.upbitSecretKey}
                onChange={handleChange}
                required
                placeholder="업비트 API Secret Key"
              />
            </div>

            <button type="submit" className="auth-button" disabled={loading}>
              {loading ? '저장 중...' : 'API 키 저장'}
            </button>
          </form>

          <div className="auth-link" style={{ marginTop: '20px' }}>
            <a href="/dashboard" onClick={(e) => { e.preventDefault(); navigate('/dashboard'); }}>
              대시보드로 돌아가기
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ApiSettingsPage;
