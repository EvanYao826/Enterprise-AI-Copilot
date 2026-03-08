import { useState, useEffect } from 'react';
import { userManagementAPI } from '../../api/admin';
import './AdminDashboard.css';

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(10);
  const [total, setTotal] = useState(0);

  useEffect(() => {
    loadUsers();
  }, [currentPage]);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const response = await userManagementAPI.listUsers(currentPage, pageSize);
      // 分页接口返回的数据结构通常是 data.records 和 data.total
      setUsers(response.data.records || []);
      setTotal(response.data.total || 0);
    } catch (err) {
      console.error('加载用户列表失败:', err);
      // alert('加载用户列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleStatusChange = async (userId, currentStatus) => {
    const newStatus = currentStatus === 1 ? 0 : 1;
    const action = newStatus === 1 ? '解封' : '封禁';

    if (!confirm(`确定要${action}此用户吗？`)) return;

    try {
      await userManagementAPI.updateUserStatus(userId, newStatus);
      loadUsers();
      alert(`${action}成功`);
    } catch (err) {
      console.error('更新用户状态失败:', err);
      alert(`${action}失败`);
    }
  };

  const getStatusBadge = (status) => {
    if (status === 1) {
      return <span className="status-badge normal">正常</span>;
    }
    return <span className="status-badge banned">已封禁</span>;
  };

  return (
    <div>
      <div className="page-header">
        <h1>用户管理</h1>
        <p>管理系统中的所有用户</p>
      </div>

      <div className="admin-card">
        <div className="card-header">
          <h2>用户列表</h2>
          <span className="total-info">共 {total} 个用户</span>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
            加载中...
          </div>
        ) : users.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
            暂无用户数据
          </div>
        ) : (
          <>
            <table className="admin-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>用户名</th>
                  <th>手机号</th>
                  <th>状态</th>
                  <th>注册时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => (
                  <tr key={user.id}>
                    <td>{user.id}</td>
                    <td>{user.username || '-'}</td>
                    <td>{user.phone || '-'}</td>
                    <td>{getStatusBadge(user.status)}</td>
                    <td>
                      {user.createTime
                        ? new Date(user.createTime).toLocaleString('zh-CN')
                        : '-'}
                    </td>
                    <td>
                      <button
                        className="action-btn edit"
                        onClick={() => handleStatusChange(user.id, user.status)}
                      >
                        {user.status === 1 ? '封禁' : '解封'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="pagination">
              <button
                onClick={() => setCurrentPage((p) => p - 1)}
                disabled={currentPage === 1}
              >
                上一页
              </button>
              <span>
                第 {currentPage} 页 / 共 {Math.ceil(total / pageSize)} 页
              </span>
              <button
                onClick={() => setCurrentPage((p) => p + 1)}
                disabled={currentPage >= Math.ceil(total / pageSize)}
              >
                下一页
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
