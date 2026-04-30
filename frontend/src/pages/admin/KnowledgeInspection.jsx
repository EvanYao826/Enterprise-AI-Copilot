import { useState, useEffect } from 'react';
import './AdminDashboard.css';

const priorityColors = {
  '高': '#ff4d4f',
  '中': '#faad14',
  '低': '#52c41a'
};

const suggestionTypeLabels = {
  '紧急补库': '紧急',
  '建议补库': '建议',
  '观察': '观察'
};

export default function KnowledgeInspection() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState({
    startDate: '',
    endDate: '',
    minCount: 1,
    clusterThreshold: 3
  });
  const [expandedCluster, setExpandedCluster] = useState(null);

  useEffect(() => {
    fetchAnalysis();
  }, []);

  const fetchAnalysis = async () => {
    setLoading(true);
    try {
      let url = `/api/admin/knowledge-inspection/unanswered/analyze?minCount=${filters.minCount}&clusterThreshold=${filters.clusterThreshold}`;
      if (filters.startDate) url += `&startDate=${filters.startDate}`;
      if (filters.endDate) url += `&endDate=${filters.endDate}`;

      const token = localStorage.getItem('adminToken');
      const response = await fetch(url, {
        headers: {
          'Authorization': token ? `Bearer ${token}` : ''
        }
      });
      const result = await response.json();
      
      if (result.code === 200) {
        setData(result.data);
      }
    } catch (error) {
      console.error('获取分析数据失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    fetchAnalysis();
  };

  const handleExport = () => {
    if (!data?.exportData || data.exportData.length === 0) {
      alert('没有可导出的数据');
      return;
    }

    const headers = ['问题', '出现次数', '首次出现', '最后出现'];
    const rows = data.exportData.map(item => [
      item.question,
      item.count,
      item.firstOccurrence ? new Date(item.firstOccurrence).toLocaleString('zh-CN') : '-',
      item.lastOccurrence ? new Date(item.lastOccurrence).toLocaleString('zh-CN') : '-'
    ]);

    const csvContent = [headers.join(','), ...rows.map(row => row.join(','))].join('\n');
    const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `未命中问题分析_${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
  };

  const formatDate = (date) => {
    if (!date) return '-';
    return new Date(date).toLocaleDateString('zh-CN');
  };

  return (
    <div className="admin-panel">
      <div className="panel-header">
        <h2>🔍 未命中问题分析</h2>
        <p>分析用户未得到满意回答的问题，识别知识缺口并给出补库建议</p>
      </div>

      <div className="search-bar">
        <div className="search-row">
          <div className="search-item">
            <label>开始日期:</label>
            <input
              type="date"
              value={filters.startDate}
              onChange={(e) => setFilters({ ...filters, startDate: e.target.value })}
            />
          </div>
          <div className="search-item">
            <label>结束日期:</label>
            <input
              type="date"
              value={filters.endDate}
              onChange={(e) => setFilters({ ...filters, endDate: e.target.value })}
            />
          </div>
          <div className="search-item">
            <label>最小出现次数:</label>
            <input
              type="number"
              min="1"
              value={filters.minCount}
              onChange={(e) => setFilters({ ...filters, minCount: parseInt(e.target.value) || 1 })}
            />
          </div>
          <div className="search-item">
            <label>聚类阈值:</label>
            <input
              type="number"
              min="1"
              max="10"
              value={filters.clusterThreshold}
              onChange={(e) => setFilters({ ...filters, clusterThreshold: parseInt(e.target.value) || 3 })}
            />
          </div>
          <div className="search-actions">
            <button className="btn btn-primary" onClick={handleSearch}>分析</button>
            <button className="btn btn-default" onClick={handleExport}>导出</button>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="admin-card">
          <div className="loading" style={{ textAlign: 'center', padding: '40px' }}>
            分析中...
          </div>
        </div>
      ) : data ? (
        <>
          <div className="admin-card">
            <div className="card-header">
              <h2>📊 分析概览</h2>
            </div>
            <div className="stats-grid">
              <div className="stat-item">
                <div className="stat-value">{data.totalUnansweredCount}</div>
                <div className="stat-label">未命中问题总数</div>
              </div>
              <div className="stat-item">
                <div className="stat-value">{data.totalUniqueQuestions}</div>
                <div className="stat-label">独立问题数</div>
              </div>
              <div className="stat-item">
                <div className="stat-value">{data.clusterCount}</div>
                <div className="stat-label">主题聚类数</div>
              </div>
            </div>
          </div>

          <div className="admin-card">
            <div className="card-header">
              <h2>🏷️ 高频未命中主题</h2>
            </div>
            {data.clusters && data.clusters.length > 0 ? (
              <div className="cluster-list">
                {data.clusters.map((cluster, index) => (
                  <div 
                    key={index} 
                    className="cluster-item"
                    onClick={() => setExpandedCluster(expandedCluster === index ? null : index)}
                  >
                    <div className="cluster-header">
                      <div className="cluster-info">
                        <span className="cluster-topic">{cluster.topic}</span>
                        <span className="cluster-count">出现 {cluster.totalCount} 次</span>
                      </div>
                      <div className="cluster-arrow">
                        {expandedCluster === index ? '▲' : '▼'}
                      </div>
                    </div>
                    {expandedCluster === index && (
                      <div className="cluster-details">
                        <div className="cluster-summary">
                          <strong>主题摘要:</strong> {cluster.topicSummary}
                        </div>
                        <div className="cluster-keywords">
                          <strong>关键词:</strong> 
                          {cluster.suggestedKeywords?.map((kw, i) => (
                            <span key={i} className="keyword-tag">{kw}</span>
                          ))}
                        </div>
                        <div className="cluster-questions">
                          <strong>相关问题:</strong>
                          <ul>
                            {cluster.questions?.slice(0, 5).map((q, i) => (
                              <li key={i}>{q}</li>
                            ))}
                            {cluster.questions?.length > 5 && (
                              <li className="more-questions">... 还有 {cluster.questions.length - 5} 个问题</li>
                            )}
                          </ul>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="empty" style={{ textAlign: 'center', padding: '40px' }}>
                暂无聚类数据
              </div>
            )}
          </div>

          <div className="admin-card">
            <div className="card-header">
              <h2>💡 补库建议</h2>
            </div>
            {data.suggestions && data.suggestions.length > 0 ? (
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>优先级</th>
                    <th>主题</th>
                    <th>建议类型</th>
                    <th>问题数量</th>
                    <th>相关分类</th>
                    <th>建议内容</th>
                  </tr>
                </thead>
                <tbody>
                  {data.suggestions.map((suggestion, index) => (
                    <tr key={index}>
                      <td>
                        <span 
                          className="status-badge"
                          style={{ 
                            backgroundColor: priorityColors[suggestion.priority] + '20', 
                            color: priorityColors[suggestion.priority] 
                          }}
                        >
                          {suggestion.priority}
                        </span>
                      </td>
                      <td>{suggestion.topic}</td>
                      <td>{suggestion.suggestionType}</td>
                      <td>{suggestion.questionCount}</td>
                      <td>{suggestion.relatedCategory}</td>
                      <td className="ellipsis" title={suggestion.suggestion}>
                        {suggestion.suggestion}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div className="empty" style={{ textAlign: 'center', padding: '40px' }}>
                暂无补库建议
              </div>
            )}
          </div>
        </>
      ) : (
        <div className="admin-card">
          <div className="empty" style={{ textAlign: 'center', padding: '40px' }}>
            暂无数据
          </div>
        </div>
      )}

      <style>{`
        .stats-grid {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 20px;
        }
        
        .stat-item {
          background: white;
          border: 1px solid #e8e8e8;
          border-radius: 12px;
          padding: 28px 24px;
          color: #333;
          text-align: center;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
          transition: all 0.3s;
        }
        
        .stat-item:hover {
          box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
          transform: translateY(-2px);
        }
        
        .stat-value {
          font-size: 42px;
          font-weight: 800;
          margin-bottom: 12px;
          color: #1890ff;
          letter-spacing: -0.5px;
        }
        
        .stat-label {
          font-size: 15px;
          color: #666;
          font-weight: 500;
        }
        
        .cluster-list {
          display: flex;
          flex-direction: column;
          gap: 12px;
        }
        
        .cluster-item {
          border: 1px solid #e8e8e8;
          border-radius: 8px;
          overflow: hidden;
          cursor: pointer;
          transition: all 0.3s;
        }
        
        .cluster-item:hover {
          border-color: #40a9ff;
          box-shadow: 0 2px 8px rgba(64, 169, 255, 0.15);
        }
        
        .cluster-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 16px;
          background-color: #fafafa;
        }
        
        .cluster-info {
          display: flex;
          align-items: center;
          gap: 12px;
        }
        
        .cluster-topic {
          font-weight: 600;
          color: #333;
          font-size: 15px;
        }
        
        .cluster-count {
          font-size: 13px;
          color: #999;
          background-color: #fff;
          padding: 4px 10px;
          border-radius: 12px;
        }
        
        .cluster-arrow {
          font-size: 12px;
          color: #999;
          transition: transform 0.3s;
        }
        
        .cluster-details {
          padding: 16px;
          background-color: #fff;
          border-top: 1px solid #e8e8e8;
        }
        
        .cluster-summary, .cluster-keywords, .cluster-questions {
          margin-bottom: 12px;
          font-size: 14px;
          color: #666;
        }
        
        .cluster-questions ul {
          margin: 8px 0 0 20px;
          padding: 0;
        }
        
        .cluster-questions li {
          margin-bottom: 6px;
          color: #333;
        }
        
        .more-questions {
          color: #999 !important;
          font-style: italic;
        }
        
        .keyword-tag {
          display: inline-block;
          background-color: #e6f7ff;
          color: #1890ff;
          padding: 4px 10px;
          border-radius: 4px;
          font-size: 12px;
          margin-right: 8px;
          margin-top: 4px;
        }
        
        .ellipsis {
          max-width: 200px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      `}</style>
    </div>
  );
}