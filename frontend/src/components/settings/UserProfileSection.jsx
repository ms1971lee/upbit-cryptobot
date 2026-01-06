import React, { useState, useEffect } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { authAPI } from '../../api/authApi';
import './UserProfileSection.css';

const UserProfileSection = () => {
  const { user, refreshUser } = useAuth();

  // 프로필 정보 상태
  const [profileForm, setProfileForm] = useState({
    username: '',
    phoneNumber: ''
  });

  // 비밀번호 변경 상태
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });

  // UI 상태
  const [profileLoading, setProfileLoading] = useState(false);
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [profileError, setProfileError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [profileSuccess, setProfileSuccess] = useState('');
  const [passwordSuccess, setPasswordSuccess] = useState('');

  // 사용자 정보로 폼 초기화
  useEffect(() => {
    if (user) {
      setProfileForm({
        username: user.username || '',
        phoneNumber: user.phoneNumber || ''
      });
    }
  }, [user]);

  // 프로필 폼 변경 핸들러
  const handleProfileChange = (e) => {
    const { name, value } = e.target;
    setProfileForm(prev => ({
      ...prev,
      [name]: value
    }));
  };

  // 비밀번호 폼 변경 핸들러
  const handlePasswordChange = (e) => {
    const { name, value } = e.target;
    setPasswordForm(prev => ({
      ...prev,
      [name]: value
    }));
  };

  // 프로필 업데이트 제출
  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    setProfileError('');
    setProfileSuccess('');
    setProfileLoading(true);

    try {
      await authAPI.updateProfile({
        username: profileForm.username || null,
        phoneNumber: profileForm.phoneNumber || null
      });

      // 사용자 정보 갱신
      await refreshUser();

      setProfileSuccess('프로필이 성공적으로 업데이트되었습니다.');

      // 3초 후 성공 메시지 제거
      setTimeout(() => setProfileSuccess(''), 3000);
    } catch (error) {
      setProfileError(
        error.response?.data?.message || '프로필 업데이트에 실패했습니다.'
      );
    } finally {
      setProfileLoading(false);
    }
  };

  // 비밀번호 변경 제출
  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    setPasswordError('');
    setPasswordSuccess('');

    // 새 비밀번호 확인
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordError('새 비밀번호가 일치하지 않습니다.');
      return;
    }

    // 비밀번호 길이 검증
    if (passwordForm.newPassword.length < 8) {
      setPasswordError('비밀번호는 최소 8자 이상이어야 합니다.');
      return;
    }

    setPasswordLoading(true);

    try {
      await authAPI.changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword
      });

      setPasswordSuccess('비밀번호가 성공적으로 변경되었습니다.');

      // 폼 초기화
      setPasswordForm({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      });

      // 3초 후 성공 메시지 제거
      setTimeout(() => setPasswordSuccess(''), 3000);
    } catch (error) {
      setPasswordError(
        error.response?.data?.message || '비밀번호 변경에 실패했습니다.'
      );
    } finally {
      setPasswordLoading(false);
    }
  };

  return (
    <div className="user-profile-content">
      {/* 프로필 정보 수정 */}
      <div className="profile-section">
        <h3>프로필 정보</h3>
        <form onSubmit={handleProfileSubmit}>
          <div className="form-group">
            <label>이메일</label>
            <input
              type="email"
              value={user?.email || ''}
              disabled
              className="disabled-input"
            />
            <span className="input-hint">이메일은 변경할 수 없습니다</span>
          </div>

          <div className="form-group">
            <label htmlFor="username">사용자명</label>
            <input
              type="text"
              id="username"
              name="username"
              value={profileForm.username}
              onChange={handleProfileChange}
              placeholder="사용자명을 입력하세요"
              maxLength={50}
            />
          </div>

          <div className="form-group">
            <label htmlFor="phoneNumber">전화번호</label>
            <input
              type="tel"
              id="phoneNumber"
              name="phoneNumber"
              value={profileForm.phoneNumber}
              onChange={handleProfileChange}
              placeholder="전화번호를 입력하세요"
              maxLength={20}
            />
          </div>

          {profileError && (
            <div className="error-message">{profileError}</div>
          )}

          {profileSuccess && (
            <div className="success-message">{profileSuccess}</div>
          )}

          <button
            type="submit"
            className="profile-button"
            disabled={profileLoading}
          >
            {profileLoading ? '업데이트 중...' : '프로필 업데이트'}
          </button>
        </form>
      </div>

      {/* 비밀번호 변경 */}
      <div className="profile-section">
        <h3>비밀번호 변경</h3>
        <form onSubmit={handlePasswordSubmit}>
          <div className="form-group">
            <label htmlFor="currentPassword">현재 비밀번호</label>
            <input
              type="password"
              id="currentPassword"
              name="currentPassword"
              value={passwordForm.currentPassword}
              onChange={handlePasswordChange}
              placeholder="현재 비밀번호를 입력하세요"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="newPassword">새 비밀번호</label>
            <input
              type="password"
              id="newPassword"
              name="newPassword"
              value={passwordForm.newPassword}
              onChange={handlePasswordChange}
              placeholder="새 비밀번호를 입력하세요 (최소 8자)"
              required
              minLength={8}
            />
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">새 비밀번호 확인</label>
            <input
              type="password"
              id="confirmPassword"
              name="confirmPassword"
              value={passwordForm.confirmPassword}
              onChange={handlePasswordChange}
              placeholder="새 비밀번호를 다시 입력하세요"
              required
              minLength={8}
            />
          </div>

          {passwordError && (
            <div className="error-message">{passwordError}</div>
          )}

          {passwordSuccess && (
            <div className="success-message">{passwordSuccess}</div>
          )}

          <button
            type="submit"
            className="profile-button"
            disabled={passwordLoading}
          >
            {passwordLoading ? '변경 중...' : '비밀번호 변경'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default UserProfileSection;
