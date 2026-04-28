import { useState, useEffect } from 'react';
import './AdminDashboard.css';

const statusColors = {
  PENDING: '#d9d9d9',
  RUNNING: '#1890ff',
  COMPLETED: '#52c41a',
  FAILED: '#ff4d4f',
  INTERRUPTED: '#faad14',
  WAITING: '#13c2c2'
};

const statusLabels = {
  PENDING: '待执行',
  RUNNING: '执行中',
  COMPLETED: '已完成',
  FAILED: '失败',
  INTERRUPTED: '已中断',
  WAITING: '等待中'
};

const stepTypeLabels = {
  INTENT_RECOGNITION: '意图识别',
  QUESTION_CLASSIFICATION: '问题分类',
  CLARIFICATION: '澄清判断',
  QUESTION_REWRITING: '问题改写',
  KNOWLEDGE_RETRIEVAL: '知识检索',
  RESULT_EVALUATION: '结果评估',
  ANSWER_GENERATION: '答案生成',
  MEMORY_WRITE: '记忆写入',
  TOOL_CALL: '工具调用'
};

export default function AgentRunManagement() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [visible, setVisible] = useState(false);
  const [selectedRun, setSelectedRun] = useState(null);
  const [steps, setSteps] = useState([]);
  
  const [filters, setFilters] = useState({
    userId: '',
    status: '',
    dateStart: '',
    dateEnd: ''
  });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      let url = '/api/agent-run/list?pageNum=1&pageSize=10';
      if (filters.userId) url += `&userId=${filters.userId}`;
      if (filters.status) url += `&status=${filters.status}`;
      
      const token = localStorage.getItem('adminToken');
      console.log('Fetching URL:', url);
      const response = await fetch(url, {
        method: 'GET',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': token ? `Bearer ${token}` : ''
        }
      });
      console.log('Response status:', response.status);
      const result = await response.json();
      console.log('Response result:', result);
      
      if (result.code === 200) {
        setData(result.data.records || []);
      } else {
        console.error('API returned error:', result.message);
      }
    } catch (error) {
      console.error('获取Agent运行记录失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchRunDetail = async (runId) => {
    try {
      const token = localStorage.getItem('adminToken');
      const headers = { 
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      };
      
      const [runResponse, stepsResponse] = await Promise.all([
        fetch(`/api/agent-run/${runId}`, { headers }),
        fetch(`/api/agent-run/${runId}/steps`, { headers })
      ]);
      const runResult = await runResponse.json();
      const stepsResult = await stepsResponse.json();
      if (runResult.code === 200 && stepsResult.code === 200) {
        setSelectedRun(runResult.data);
        setSteps(stepsResult.data);
        setVisible(true);
      }
    } catch (error) {
      console.error('获取Agent运行详情失败:', error);
    }
  };

  const handleSearch = () => {
    fetchData();
  };

  const handleReset = () => {
    setFilters({ userId: '', status: '', dateStart: '', dateEnd: '' });
    fetchData();
  };

  const formatTime = (time) => {
    if (!time) return '-';
    return new Date(time).toLocaleString('zh-CN');
  };

  return (
    <div className="admin-panel">
      <div className="panel-header">
        <h2>🔄 Agent 执行记录</h2>
      </div>

      <div className="search-bar">
        <div className="search-row">
          <div className="search-item">
            <label>用户ID:</label>
            <input
              type="text"
              value={filters.userId}
              onChange={(e) => setFilters({ ...filters, userId: e.target.value })}
              placeholder="输入用户ID"
            />
          </div>
          <div className="search-item">
            <label>状态:</label>
            <select value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
              <option value="">全部</option>
              {Object.entries(statusLabels).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </div>
          <div className="search-item">
            <label>日期范围:</label>
            <input
              type="date"
              value={filters.dateStart}
              onChange={(e) => setFilters({ ...filters, dateStart: e.target.value })}
            />
            <span>~</span>
            <input
              type="date"
              value={filters.dateEnd}
              onChange={(e) => setFilters({ ...filters, dateEnd: e.target.value })}
            />
          </div>
          <div className="search-actions">
            <button className="btn btn-primary" onClick={handleSearch}>搜索</button>
            <button className="btn btn-default" onClick={handleReset}>重置</button>
          </div>
        </div>
      </div>

      <div className="table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>运行ID</th>
              <th>用户ID</th>
              <th>会话ID</th>
              <th>状态</th>
              <th>目标</th>
              <th>开始时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan="7" className="loading">加载中...</td>
              </tr>
            ) : data.length === 0 ? (
              <tr>
                <td colSpan="7" className="empty">暂无数据</td>
              </tr>
            ) : (
              data.map((item) => (
                <tr key={item.runId}>
                  <td className="text-primary">{item.runId}</td>
                  <td>{item.userId}</td>
                  <td className="ellipsis">{item.conversationId}</td>
                  <td>
                    <span 
                      className="status-badge" 
                      style={{ backgroundColor: statusColors[item.status] + '20', color: statusColors[item.status] }}
                    >
                      {statusLabels[item.status]}
                    </span>
                  </td>
                  <td className="ellipsis" title={item.goal}>{item.goal}</td>
                  <td>{formatTime(item.startTime)}</td>
                  <td>
                    <button className="action-btn edit" onClick={() => fetchRunDetail(item.runId)}>
                      查看详情
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {visible && (
        <div className="modal-overlay" onClick={() => setVisible(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Agent Run 详情 - {selectedRun?.runId}</h3>
              <button className="modal-close" onClick={() => setVisible(false)}>×</button>
            </div>
            <div className="modal-body">
              {selectedRun && (
                <div className="detail-content">
                  <div className="detail-row">
                    <span className="detail-label">Trace ID:</span>
                    <span className="detail-value">{selectedRun.traceId}</span>
                  </div>
                  <div className="detail-row">
                    <span className="detail-label">目标:</span>
                    <span className="detail-value">{selectedRun.goal}</span>
                  </div>
                  <div className="detail-row">
                    <span className="detail-label">错误码:</span>
                    <span className="detail-value">{selectedRun.errorCode || '-'}</span>
                  </div>
                  <div className="detail-row">
                    <span className="detail-label">输入:</span>
                    <span className="detail-value">{selectedRun.input}</span>
                  </div>
                  <div className="detail-row">
                    <span className="detail-label">输出:</span>
                    <span className="detail-value">{selectedRun.output || '-'}</span>
                  </div>

                  <h4>步骤轨迹</h4>
                  <table className="admin-table small">
                    <thead>
                      <tr>
                        <th>步骤名称</th>
                        <th>步骤类型</th>
                        <th>状态</th>
                        <th>耗时(ms)</th>
                        <th>错误信息</th>
                      </tr>
                    </thead>
                    <tbody>
                      {steps.length === 0 ? (
                        <tr>
                          <td colSpan="5" className="empty">暂无步骤数据</td>
                        </tr>
                      ) : (
                        steps.map((step) => (
                          <tr key={step.id}>
                            <td>{step.stepName}</td>
                            <td>{stepTypeLabels[step.stepType] || step.stepType}</td>
                            <td>
                              <span 
                                className="status-badge" 
                                style={{ backgroundColor: statusColors[step.status] + '20', color: statusColors[step.status] }}
                              >
                                {statusLabels[step.status]}
                              </span>
                            </td>
                            <td>{step.durationMs || '-'}</td>
                            <td className={step.errorMessage ? 'text-error' : ''}>
                              {step.errorMessage ? (step.errorMessage.length > 50 ? step.errorMessage.substring(0, 50) + '...' : step.errorMessage) : '-'}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
