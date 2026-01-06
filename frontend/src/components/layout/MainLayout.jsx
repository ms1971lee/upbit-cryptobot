import React from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import Sidebar from './Sidebar';
import './MainLayout.css';

const MainLayout = ({ children }) => {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="main-layout">
      <Sidebar />

      <div className="main-content-wrapper">
        <div className="main-header">
          <h1 className="main-title">Upbit Cryptobot Dashboard</h1>
          <button onClick={handleLogout} className="logout-btn">
            로그아웃
          </button>
        </div>

        <div className="main-content">
          {children}
        </div>
      </div>
    </div>
  );
};

export default MainLayout;
