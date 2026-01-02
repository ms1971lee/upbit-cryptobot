import React, { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import { authAPI } from '../api/authApi';
import './DashboardPage.css';
import './ApiSettingsPage.css';

const ApiSettingsPage = () => {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [apiKeys, setApiKeys] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editingKey, setEditingKey] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    accessKey: '',
    secretKey: '',
    isActive: false
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadApiKeys();
  }, []);

  useEffect(() => {
    console.log('=== formData changed ===');
    console.log('formData.accessKey:', formData.accessKey);
    console.log('formData:', formData);
  }, [formData]);

  const loadApiKeys = async () => {
    try {
      const response = await authAPI.getAllApiKeys();
      console.log('Loaded API keys:', response.data);
      setApiKeys(response.data);
    } catch (err) {
      console.error('Failed to load API keys:', err);
    }
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({
      ...formData,
      [name]: type === 'checkbox' ? checked : value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      if (editingKey) {
        // 수정 시: 빈 문자열 제거 (입력된 필드만 전송)
        const updateData = {
          name: formData.name || undefined,
          accessKey: formData.accessKey || undefined,
          secretKey: formData.secretKey || undefined,
          isActive: formData.isActive
        };
        console.log('=== API Key Update Request ===');
        console.log('Editing Key ID:', editingKey.id);
        console.log('Form Data:', {
          name: formData.name,
          accessKey: formData.accessKey ? 'PROVIDED' : 'empty',
          secretKey: formData.secretKey ? 'PROVIDED' : 'empty',
          isActive: formData.isActive
        });
        console.log('Update Data (after undefined conversion):', {
          name: updateData.name,
          accessKey: updateData.accessKey ? 'PROVIDED' : 'undefined',
          secretKey: updateData.secretKey ? 'PROVIDED' : 'undefined',
          isActive: updateData.isActive
        });

        const response = await authAPI.updateApiKey(editingKey.id, updateData);
        console.log('Update Response:', response.data);
        setSuccess('API 키가 성공적으로 수정되었습니다!');
      } else {
        console.log('=== API Key Create Request ===');
        console.log('Form Data:', formData);
        await authAPI.createApiKey(formData);
        setSuccess('API 키가 성공적으로 추가되었습니다!');
      }

      setFormData({ name: '', accessKey: '', secretKey: '', isActive: false });
      setShowForm(false);
      setEditingKey(null);
      loadApiKeys();
    } catch (err) {
      console.error('API Key Save Error:', err);
      const errorMessage = err.response?.data?.message || 'API 키 저장에 실패했습니다';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (apiKey) => {
    console.log('=== handleEdit called ===');
    console.log('apiKey object:', apiKey);
    console.log('apiKey fields:', Object.keys(apiKey));
    console.log('apiKey.name:', apiKey.name);
    console.log('apiKey.accessKeyMasked:', apiKey.accessKeyMasked);

    setEditingKey(apiKey);
    const newFormData = {
      name: apiKey.name,
      accessKey: '',
      secretKey: '',
      isActive: apiKey.isActive
    };
    console.log('Setting formData to:', newFormData);
    setFormData(newFormData);
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('정말 이 API 키를 삭제하시겠습니까?')) {
      return;
    }

    try {
      await authAPI.deleteApiKey(id);
      setSuccess('API 키가 삭제되었습니다');
      loadApiKeys();
    } catch (err) {
      setError('API 키 삭제에 실패했습니다');
    }
  };

  const handleActivate = async (id) => {
    try {
      await authAPI.activateApiKey(id);
      setSuccess('API 키가 활성화되었습니다');
      loadApiKeys();
    } catch (err) {
      setError('API 키 활성화에 실패했습니다');
    }
  };

  const handleTest = async (id) => {
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const response = await authAPI.testApiKey(id);
      if (response.data.success) {
        setSuccess(`✅ ${response.data.message}\n계좌 정보가 정상적으로 조회되었습니다.`);
      } else {
        setError(`❌ ${response.data.message}: ${response.data.error}`);
      }
    } catch (err) {
      setError('API 키 테스트에 실패했습니다: ' + (err.response?.data?.error || err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingKey(null);
    setFormData({ name: '', accessKey: '', secretKey: '', isActive: false });
    setError('');
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
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '30px' }}>
            <h2>API 키 관리</h2>
            {!showForm && (
              <button
                onClick={() => setShowForm(true)}
                className="auth-button"
                style={{ width: 'auto', padding: '10px 20px', fontSize: '14px' }}
              >
                + API 키 추가
              </button>
            )}
          </div>

          {error && <div className="error-message">{error}</div>}
          {success && <div className="success-message">{success}</div>}

          {showForm && (
            <div className="api-form-section">
              <h3>{editingKey ? 'API 키 수정' : 'API 키 추가'}</h3>
              <form onSubmit={handleSubmit} autoComplete="off">
                {/* 브라우저 자동완성 방지용 더미 필드 */}
                <input type="text" style={{display: 'none'}} />
                <input type="password" style={{display: 'none'}} />

                <div className="form-group">
                  <label htmlFor="name">API 키 이름 *</label>
                  <input
                    type="text"
                    id="name"
                    name="name"
                    value={formData.name}
                    onChange={handleChange}
                    required
                    placeholder="예: 메인 계좌, 테스트 계좌"
                    autoComplete="off"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="accessKey">Access Key</label>
                  {editingKey && (
                    <div style={{
                      padding: '10px',
                      backgroundColor: 'rgba(255, 255, 255, 0.05)',
                      borderRadius: '4px',
                      marginBottom: '8px',
                      color: '#888'
                    }}>
                      현재: {editingKey.accessKeyMasked}
                    </div>
                  )}
                  <input
                    type="text"
                    id="accessKey"
                    name="accessKey"
                    value={formData.accessKey}
                    onChange={handleChange}
                    required={!editingKey}
                    placeholder={editingKey ? "변경하려면 새 Access Key 입력" : "업비트 API Access Key"}
                    autoComplete="off"
                    data-lpignore="true"
                    data-form-type="other"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="secretKey">Secret Key</label>
                  {editingKey && (
                    <div style={{
                      padding: '10px',
                      backgroundColor: 'rgba(255, 255, 255, 0.05)',
                      borderRadius: '4px',
                      marginBottom: '8px',
                      color: '#888'
                    }}>
                      현재: ••••••••••••
                    </div>
                  )}
                  <input
                    type="password"
                    id="secretKey"
                    name="secretKey"
                    value={formData.secretKey}
                    onChange={handleChange}
                    required={!editingKey}
                    placeholder={editingKey ? "변경하려면 새 Secret Key 입력" : "업비트 API Secret Key"}
                    autoComplete="new-password"
                    data-lpignore="true"
                    data-form-type="other"
                  />
                </div>

                <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <input
                    type="checkbox"
                    id="isActive"
                    name="isActive"
                    checked={formData.isActive}
                    onChange={handleChange}
                    style={{ width: 'auto', margin: 0 }}
                  />
                  <label htmlFor="isActive" style={{ margin: 0 }}>이 API 키를 활성화 (다른 키는 비활성화됨)</label>
                </div>

                <div style={{ display: 'flex', gap: '10px', marginTop: '20px' }}>
                  <button type="submit" className="auth-button" disabled={loading} style={{ flex: 1 }}>
                    {loading ? '저장 중...' : editingKey ? 'API 키 수정' : 'API 키 추가'}
                  </button>
                  <button
                    type="button"
                    onClick={handleCancel}
                    className="logout-button"
                    style={{ flex: 1 }}
                  >
                    취소
                  </button>
                </div>
              </form>
            </div>
          )}

          {!showForm && (
            <>
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
                  <li>발급받은 Access Key와 Secret Key를 아래에 추가합니다</li>
                </ol>
              </div>

              <div className="api-keys-list">
                <h3>등록된 API 키 ({apiKeys.length}개)</h3>
                {apiKeys.length === 0 ? (
                  <div className="no-api-keys">
                    <p>등록된 API 키가 없습니다.</p>
                    <p>위 '+ API 키 추가' 버튼을 눌러 API 키를 추가해주세요.</p>
                  </div>
                ) : (
                  <div className="api-keys-grid">
                    {apiKeys.map((apiKey) => (
                      <div key={apiKey.id} className={`api-key-card ${apiKey.isActive ? 'active' : ''}`}>
                        <div className="api-key-header">
                          <h4>{apiKey.name}</h4>
                          {apiKey.isActive && <span className="active-badge">활성</span>}
                        </div>
                        <div className="api-key-info">
                          <div className="info-row">
                            <span className="label">Access Key:</span>
                            <span className="value">{apiKey.accessKeyMasked}</span>
                          </div>
                          <div className="info-row">
                            <span className="label">생성일:</span>
                            <span className="value">{new Date(apiKey.createdAt).toLocaleString('ko-KR')}</span>
                          </div>
                        </div>
                        <div className="api-key-actions">
                          {!apiKey.isActive && (
                            <button
                              onClick={() => handleActivate(apiKey.id)}
                              className="activate-button"
                            >
                              활성화
                            </button>
                          )}
                          <button
                            onClick={() => handleTest(apiKey.id)}
                            className="test-button"
                            disabled={loading}
                          >
                            테스트
                          </button>
                          <button
                            onClick={() => handleEdit(apiKey)}
                            className="edit-button"
                          >
                            수정
                          </button>
                          <button
                            onClick={() => handleDelete(apiKey.id)}
                            className="delete-button"
                          >
                            삭제
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </>
          )}

          <div className="auth-link" style={{ marginTop: '30px' }}>
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
