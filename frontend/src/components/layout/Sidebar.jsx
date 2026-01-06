import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import './Sidebar.css';

const Sidebar = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();

  const menuItems = [
    { path: '/dashboard', label: '메인대시보드' },
    { path: '/trading-scan', label: '매매스캔' },
    { path: '/trade-history', label: '거래히스토리' },
    { path: '/settings', label: '개인설정' }
  ];

  return (
    <div className="sidebar">
      <div className="sidebar-user">
        <div className="user-greeting">
          {user?.username || '이문수님'} 안녕하세요
        </div>
      </div>

      <div className="sidebar-menu">
        {menuItems.map((item) => (
          <button
            key={item.path}
            className={`menu-item ${location.pathname === item.path ? 'active' : ''}`}
            onClick={() => navigate(item.path)}
          >
            {item.label}
          </button>
        ))}
      </div>
    </div>
  );
};

export default Sidebar;
