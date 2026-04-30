import {useEffect, useState} from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authAPI } from '../api';
import './Auth.css';


export default function Login() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    phone: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  useEffect(() => {
    document.title = 'AI 知识系统-用户登录';
    return () => {
      document.title = 'AI 知识系统';
    };
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await authAPI.login(formData);
      // 检查返回的数据是否包含 data 字段
      const userData = response.data;
      if (userData) {
        // token 已通过响应拦截器存储到 cookie
        // 只存储用户信息到 localStorage（非敏感信息）
        const { accessToken, refreshToken, ...userInfo } = userData;
        localStorage.setItem('user', JSON.stringify(userInfo.user));
        localStorage.setItem('userId', userInfo.user.id);
        
        // 使用replace选项，避免登录页面留在历史堆栈中
        navigate('/chat', { replace: true });
        
        // 替换整个历史记录，使后退按钮不可用
        setTimeout(() => {
          window.history.replaceState(null, null, window.location.href);
        }, 100);
      } else {
        setError('登录失败，服务器返回数据异常');
      }
    } catch (err) {
      setError(err.message || '登录失败，请检查手机号和密码');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="particle"></div>
      <div className="particle"></div>
      <div className="particle"></div>
      <div className="particle"></div>
      <div className="particle"></div>
      <div className="particle"></div>
      <div className="particle"></div>
      <div className="particle"></div>
      <div className="particle-line"></div>
      <div className="particle-line"></div>
      <div className="particle-line"></div>
      <div className="particle-line"></div>
      <div className="auth-card">
        <h1 className="auth-title">AI 知识系统</h1>
        <h2 className="auth-subtitle">登录</h2>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>手机号</label>
            <input
              type="tel"
              name="phone"
              value={formData.phone}
              onChange={handleChange}
              placeholder="请输入手机号"
              required
            />
          </div>

          <div className="form-group">
            <label>密码</label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="请输入密码"
              required
            />
          </div>

          {error && <div className="error">{error}</div>}

          <button type="submit" className="btn btn-primary auth-btn" disabled={loading}>
            {loading ? '登录中...' : '登录'}
          </button>
        </form>

        <div className="auth-footer">
          还没有账号？<Link to="/register">立即注册</Link>
        </div>
        <div className="auth-footer">
          <Link to="/admin/login" style={{ textDecoration: 'none', color: '#666', fontSize: '12px' }}>
            管理员入口
          </Link>
        </div>
      </div>
    </div>
  );
}
