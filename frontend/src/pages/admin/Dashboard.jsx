import React, { useEffect, useState, useRef } from 'react';
import { getDashboardStats } from '../../api/dashboard';
import * as echarts from 'echarts';
import './Dashboard.css';

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const pieChartRef = useRef(null);
  const trendChartRef = useRef(null);
  const chartInstances = useRef([]);

  useEffect(() => {
    fetchStats();
    
    const handleResize = () => {
      chartInstances.current.forEach(chart => chart.resize());
    };
    window.addEventListener('resize', handleResize);
    
    return () => {
      window.removeEventListener('resize', handleResize);
      chartInstances.current.forEach(chart => chart.dispose());
    };
  }, []);

  useEffect(() => {
    if (stats) {
      // Small timeout to ensure DOM is ready
      setTimeout(initCharts, 100);
    }
  }, [stats]);

  const fetchStats = async () => {
    try {
      const res = await getDashboardStats();
      if (res.code === 200) {
        setStats(res.data);
      }
    } catch (error) {
      console.error('Fetch stats failed', error);
    } finally {
      setLoading(false);
    }
  };

  const initCharts = () => {
    // Clean up old instances
    chartInstances.current.forEach(chart => chart.dispose());
    chartInstances.current = [];

    // 1. 3D Pie Chart (Simulated)
    if (pieChartRef.current) {
        const pieChart = echarts.init(pieChartRef.current);
        const optionPie = {
            title: {
                text: '核心指标分布',
                left: 'center',
                textStyle: { fontSize: 16 }
            },
            tooltip: {
                trigger: 'item',
                formatter: '{b}: {c} ({d}%)'
            },
            legend: {
                bottom: '0%',
                left: 'center'
            },
            series: [
                {
                    name: '统计',
                    type: 'pie',
                    radius: ['30%', '60%'], // Donut shape for better look
                    center: ['50%', '50%'],
                    roseType: 'area', // Rose chart looks more "3D" and handles scale differences better
                    itemStyle: {
                        borderRadius: 8,
                        shadowBlur: 20,
                        shadowColor: 'rgba(0, 0, 0, 0.3)'
                    },
                    data: [
                        { value: stats.userCount || 0, name: '用户数' },
                        { value: stats.docCount || 0, name: '文档数' },
                        { value: stats.qaCount || 0, name: '提问数' },
                        // For Hit Rate, we use the value directly. 
                        // Note: If hitRate is 95%, value is 95. If count is 1000, this slice is small.
                        // But using 'roseType: area' helps visualize small values better.
                        { value: stats.hitRate ? Math.round(stats.hitRate) : 0, name: 'AI命中率(%)' }
                    ]
                }
            ]
        };
        pieChart.setOption(optionPie);
        chartInstances.current.push(pieChart);
    }

    // 2. Trend Chart
    if (trendChartRef.current) {
        const trendChart = echarts.init(trendChartRef.current);
        
        // Prepare data
        const trends = stats.questionTrends || [];
        const dates = trends.map(item => item.date);
        const counts = trends.map(item => item.count);

        const optionTrend = {
            title: {
                text: '近7日提问趋势',
                left: 'center',
                textStyle: { fontSize: 16 }
            },
            tooltip: {
                trigger: 'axis'
            },
            grid: {
                left: '3%',
                right: '4%',
                bottom: '10%',
                containLabel: true
            },
            xAxis: {
                type: 'category',
                boundaryGap: false,
                data: dates.length > 0 ? dates : ['无数据']
            },
            yAxis: {
                type: 'value'
            },
            series: [
                {
                    name: '提问数',
                    type: 'line',
                    stack: 'Total',
                    smooth: true,
                    areaStyle: {
                        opacity: 0.3
                    },
                    emphasis: {
                        focus: 'series'
                    },
                    data: dates.length > 0 ? counts : [0]
                }
            ]
        };
        trendChart.setOption(optionTrend);
        chartInstances.current.push(trendChart);
    }
  };

  if (loading) return <div className="loading">加载中...</div>;
  if (!stats) return <div className="error">暂无数据</div>;

  return (
    <div className="dashboard-container">
      <h2 className="page-title">仪表盘</h2>
      
      {/* 核心指标卡片 */}
      <div className="stats-cards">
        <div className="stat-card">
          <div className="stat-value">{stats.userCount}</div>
          <div className="stat-label">用户总数</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats.docCount}</div>
          <div className="stat-label">文档总数</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats.qaCount}</div>
          <div className="stat-label">提问总数</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{(stats.hitRate || 0).toFixed(1)}%</div>
          <div className="stat-label">AI命中率</div>
        </div>
      </div>

      {/* 图表区域 */}
      <div className="charts-row" style={{ display: 'flex', gap: '20px', margin: '20px 0', height: '350px' }}>
          <div className="chart-container" style={{ flex: 1, background: '#fff', padding: '15px', borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}>
              <div ref={pieChartRef} style={{ width: '100%', height: '100%' }}></div>
          </div>
          <div className="chart-container" style={{ flex: 1, background: '#fff', padding: '15px', borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}>
              <div ref={trendChartRef} style={{ width: '100%', height: '100%' }}></div>
          </div>
      </div>

      <div className="dashboard-grid">
        {/* 热门文档 */}
        <div className="dashboard-section">
          <h3>🔥 热门文档</h3>
          <table className="data-table">
            <thead>
              <tr>
                <th>文档名称</th>
                <th>查看次数</th>
              </tr>
            </thead>
            <tbody>
              {stats.hotDocs && stats.hotDocs.length > 0 ? (
                stats.hotDocs.map((doc, index) => (
                  <tr key={index}>
                    <td>{doc.title || `文档ID:${doc.doc_id}`}</td>
                    <td>{doc.view_count}</td>
                  </tr>
                ))
              ) : (
                <tr><td colSpan="2" className="empty-text">暂无数据</td></tr>
              )}
            </tbody>
          </table>
        </div>

        {/* 热门问题 */}
        <div className="dashboard-section">
          <h3>💡 热门提问</h3>
          <table className="data-table">
             <thead>
                <tr>
                    <th>问题内容</th>
                    <th>提问次数</th>
                </tr>
             </thead>
             <tbody>
                {stats.topQuestions && stats.topQuestions.length > 0 ? (
                  stats.topQuestions.map((q, index) => (
                    <tr key={index}>
                      <td className="text-truncate" title={q.question} style={{maxWidth: '200px'}}>{q.question}</td>
                      <td>{q.count}</td>
                    </tr>
                  ))
                ) : (
                  <tr><td colSpan="2" className="empty-text">暂无数据</td></tr>
                )}
             </tbody>
          </table>
        </div>

        {/* 未命中问题 */}
        <div className="dashboard-section">
          <h3>❓ 未命中问题 (需优化)</h3>
          <table className="data-table">
             <thead>
                <tr>
                    <th>问题内容</th>
                    <th>频次</th>
                </tr>
             </thead>
             <tbody>
                {stats.unansweredQuestions && stats.unansweredQuestions.length > 0 ? (
                  stats.unansweredQuestions.map((q, index) => (
                    <tr key={index}>
                      <td className="text-truncate" title={q.question} style={{maxWidth: '200px', color: '#ff4d4f'}}>{q.question}</td>
                      <td>{q.count}</td>
                    </tr>
                  ))
                ) : (
                  <tr><td colSpan="2" className="empty-text">表现良好，暂无未命中</td></tr>
                )}
             </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
