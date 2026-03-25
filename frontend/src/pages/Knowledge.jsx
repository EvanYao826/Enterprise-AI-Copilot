import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { knowledgeAPI } from '../api/index';
import './Knowledge.css';

export default function Knowledge() {
  const navigate = useNavigate();
  const location = useLocation();
  const userId = localStorage.getItem('userId');
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);

  useEffect(() => {
    if (!userId) {
      navigate('/login');
      return;
    }
    loadDocuments();
  }, [userId, navigate]);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const docId = params.get('docId');
    if (docId && userId) {
      viewDocument(docId);
    }
  }, [location, userId]);

  const viewDocument = async (id) => {
    try {
      const res = await knowledgeAPI.view(id, userId);
      // Here we could open a modal or redirect to a viewer
      // For now, just show a message that we recorded the view
      // and maybe highlight the document if it's in the list
      console.log('Viewed doc:', res.data);
    } catch (e) {
      console.error('View doc failed', e);
    }
  };

  const loadDocuments = async () => {
    setLoading(true);
    try {
      const response = await knowledgeAPI.list();
      setDocuments(response.data || []);
    } catch (err) {
      console.error('加载文档失败:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    if (file) {
      setSelectedFile(file);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    setUploading(true);
    try {
      await knowledgeAPI.upload(selectedFile);
      setSelectedFile(null);
      document.getElementById('file-input').value = '';
      loadDocuments();
      alert('上传成功');
    } catch (err) {
      alert(err.message || '上传失败');
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('确定删除此文档吗？')) return;

    try {
      await knowledgeAPI.delete(id);
      setDocuments(documents.filter(doc => doc.id !== id));
    } catch (err) {
      alert(err.message || '删除失败');
    }
  };

  const getStatusBadge = (status) => {
    const statusMap = {
      'PENDING': { text: '处理中', className: 'pending' },
      'COMPLETED': { text: '已完成', className: 'completed' },
      'FAILED': { text: '失败', className: 'failed' }
    };
    const info = statusMap[status] || { text: status, className: '' };
    return <span className={`status-badge ${info.className}`}>{info.text}</span>;
  };

  return (
    <div className="knowledge-container">
      <div className="knowledge-header">
        <h1>知识库管理</h1>
        <button className="btn btn-default" onClick={() => navigate('/chat')}>
          返回聊天
        </button>
      </div>

      {/* Upload section */}
      <div className="upload-section card">
        <h3>上传文档</h3>
        <div className="upload-area">
          <input
            id="file-input"
            type="file"
            onChange={handleFileSelect}
            accept=".txt,.pdf,.doc,.docx,.md"
          />
          <button
            className="btn btn-primary"
            onClick={handleUpload}
            disabled={!selectedFile || uploading}
          >
            {uploading ? '上传中...' : '上传'}
          </button>
        </div>
        <p className="upload-hint">支持 .txt, .pdf, .doc, .docx, .md 格式</p>
      </div>

      {/* Document list */}
      <div className="document-section card">
        <h3>文档列表</h3>

        {loading ? (
          <div className="loading">加载中...</div>
        ) : documents.length === 0 ? (
          <div className="empty-list">暂无文档，请先上传</div>
        ) : (
          <table className="document-table">
            <thead>
              <tr>
                <th>文档名称</th>
                <th>状态</th>
                <th>上传时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {documents.map(doc => (
                <tr key={doc.id}>
                  <td className="doc-name">{doc.docName}</td>
                  <td>{getStatusBadge(doc.status)}</td>
                  <td>{new Date(doc.createTime).toLocaleString()}</td>
                  <td>
                    <button
                      className="btn btn-danger btn-sm"
                      onClick={() => handleDelete(doc.id)}
                    >
                      删除
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
