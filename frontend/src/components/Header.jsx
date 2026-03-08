import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Header.css';

const Header = () => {
  const navigate = useNavigate();
  const [showUserInfo, setShowUserInfo] = useState(false);
  const user = JSON.parse(localStorage.getItem('user') || '{}');

  const handleLogout = () => {
    localStorage.removeItem('user');
    localStorage.removeItem('userId');
    navigate('/login');
  };

  if (!user.username) return null;

  return (
    <header className="app-header">
      <div className="header-left">
        <span className="app-title">AI 知识库系统</span>
      </div>
      <div className="header-right">
        <div className="user-profile-container">
          <span 
            className="user-info clickable" 
            onClick={() => setShowUserInfo(!showUserInfo)}
          >
            欢迎, {user.username}
          </span>
          
          {showUserInfo && (
            <div className="user-popup">
              <div className="popup-header">
                <h3>用户信息</h3>
                <button className="close-btn" onClick={() => setShowUserInfo(false)}>×</button>
              </div>
              <div className="popup-content">
                <div className="info-item">
                  <label>用户名：</label>
                  <span>{user.username}</span>
                </div>
                <div className="info-item">
                  <label>手机号：</label>
                  <span>{user.phone}</span>
                </div>
                <div className="info-item">
                  <label>注册时间：</label>
                  <span>{user.createTime ? new Date(user.createTime).toLocaleString() : '未知'}</span>
                </div>
                <button onClick={handleLogout} className="logout-btn full-width">退出登录</button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

export default Header;
