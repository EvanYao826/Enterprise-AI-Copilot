import { useState, useEffect } from 'react';
import { qaLogAPI } from '../../api/admin';
import './AdminDashboard.css';

export default function QaLogManagement() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [selectedLog, setSelectedLog] = useState(null);
  const [showDetail, setShowDetail] = useState(false);

  useEffect(() => {
    loadLogs();
  }, [currentPage]);

  const loadLogs = async () => {
    setLoading(true);
    try {
      const response = await qaLogAPI.list(currentPage, pageSize);
      setLogs(response.data.records || []);
      setTotal(response.data.total || 0);
    } catch (err) {
      console.error('加载问答日志失败:', err);
      // alert('加载问答日志失败');
    } finally {
      setLoading(false);
    }
  };

  const handleViewDetail = (log) => {
    setSelectedLog(log);
    setShowDetail(true);
  };

  const truncateText = (text, maxLength = 50) => {
    if (!text) return '-';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  };

  return (
    <div>
      <div className="page-header">
        <h1>问答日志</h1>
        <p>查看系统中的所有问答记录</p>
      </div>

      <div className="admin-card">
        <div className="card-header">
          <h2>日志列表</h2>
          <span className="total-info">共 {total} 条记录</span>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
            加载中...
          </div>
        ) : logs.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
            暂无日志记录
          </div>
        ) : (
          <>
            <table className="admin-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>用户ID</th>
                  <th>问题</th>
                  <th>回答</th>
                  <th>反馈</th>
                  <th>时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.id}>
                    <td>{log.id}</td>
                    <td>{log.userId || '-'}</td>
                    <td style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {truncateText(log.question)}
                    </td>
                    <td style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {truncateText(log.answer)}
                    </td>
                    <td>
                      {log.feedbackType === 'like' ? (
                        <span className="feedback-badge like">👍 点赞</span>
                      ) : log.feedbackType === 'dislike' ? (
                        <span className="feedback-badge dislike">👎 点踩</span>
                      ) : (
                        <span className="feedback-badge none">-</span>
                      )}
                    </td>
                    <td style={{ minWidth: '150px' }}>
                      {log.createTime
                        ? new Date(log.createTime).toLocaleString('zh-CN')
                        : '-'}
                    </td>
                    <td>
                      <button
                        className="action-btn edit"
                        onClick={() => handleViewDetail(log)}
                      >
                        查看详情
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

      {/* Detail Modal */}
      {showDetail && selectedLog && (
        <div
          className="modal-overlay"
          onClick={() => setShowDetail(false)}
        >
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>问答详情</h3>
              <button className="close-btn" onClick={() => setShowDetail(false)}>
                ×
              </button>
            </div>
            <div className="modal-body">
              <div className="detail-item">
                <label>ID:</label>
                <span>{selectedLog.id}</span>
              </div>
              <div className="detail-item">
                <label>用户ID:</label>
                <span>{selectedLog.userId || '-'}</span>
              </div>
              <div className="detail-item">
                <label>时间:</label>
                <span>
                  {selectedLog.createTime
                    ? new Date(selectedLog.createTime).toLocaleString('zh-CN')
                    : '-'}
                </span>
              </div>
              <div className="detail-item full-width">
                <label>问题:</label>
                <div className="detail-text">{selectedLog.question || '-'}</div>
              </div>
              <div className="detail-item full-width">
                <label>回答:</label>
                <div className="detail-text">{selectedLog.answer || '-'}</div>
              </div>
              <div className="detail-item">
                <label>反馈:</label>
                <span>
                  {selectedLog.feedbackType === 'like' ? (
                    <span className="feedback-badge like">👍 点赞</span>
                  ) : selectedLog.feedbackType === 'dislike' ? (
                    <span className="feedback-badge dislike">👎 点踩</span>
                  ) : (
                    '- 无反馈'
                  )}
                </span>
              </div>
              {selectedLog.feedbackTime && (
                <div className="detail-item">
                  <label>反馈时间:</label>
                  <span>
                    {new Date(selectedLog.feedbackTime).toLocaleString('zh-CN')}
                  </span>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
