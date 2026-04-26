import { useState, useEffect } from 'react';
import { knowledgeManagementAPI } from '../../api/admin';
import './AdminDashboard.css';

export default function KnowledgeManagement() {
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteDocumentId, setDeleteDocumentId] = useState(null);
  const [showMessageModal, setShowMessageModal] = useState(false);
  const [messageModalContent, setMessageModalContent] = useState('');
  const [messageModalTitle, setMessageModalTitle] = useState('提示');

  useEffect(() => {
    loadDocuments();
  }, []);

  const loadDocuments = async () => {
    setLoading(true);
    try {
      // 知识库列表目前后端返回的是 List<KnowledgeDoc>，不是分页对象
      // 如果后端改为了分页，这里需要调整。目前假设是 List。
      const response = await knowledgeManagementAPI.list();
      const list = response.data || [];
      setDocuments(list);
      // 统计数量直接取 list.length
    } catch (err) {
      console.error('加载文档列表失败:', err);
      // alert('加载文档列表失败');
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
    if (!selectedFile) {
      setMessageModalTitle('提示');
      setMessageModalContent('请先选择文件');
      setShowMessageModal(true);
      return;
    }

    setUploading(true);
    try {
      await knowledgeManagementAPI.upload(selectedFile);
      setSelectedFile(null);
      document.getElementById('admin-file-input').value = '';
      loadDocuments();
      setMessageModalTitle('成功');
      setMessageModalContent('上传成功');
      setShowMessageModal(true);
    } catch (err) {
      console.error('上传文档失败:', err);
      setMessageModalTitle('错误');
      setMessageModalContent('上传失败: ' + (err.message || '未知错误'));
      setShowMessageModal(true);
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = (id) => {
    setDeleteDocumentId(id);
    setShowDeleteConfirm(true);
  };

  const handleDeleteConfirm = async () => {
    if (!deleteDocumentId) return;
    
    try {
      await knowledgeManagementAPI.delete(deleteDocumentId);
      setDocuments(documents.filter(doc => doc.id !== deleteDocumentId));
      setMessageModalTitle('成功');
      setMessageModalContent('删除成功');
      setShowMessageModal(true);
    } catch (err) {
      console.error('删除文档失败:', err);
      setMessageModalTitle('错误');
      setMessageModalContent('删除失败');
      setShowMessageModal(true);
    } finally {
      setShowDeleteConfirm(false);
      setDeleteDocumentId(null);
    }
  };

  const handleDeleteCancel = () => {
    setShowDeleteConfirm(false);
    setDeleteDocumentId(null);
  };

  const handleMessageModalClose = () => {
    setShowMessageModal(false);
  };

  const getStatusBadge = (status) => {
    const statusMap = {
      'PENDING': { text: '待处理', className: 'pending' },
      'PROCESSING': { text: '处理中', className: 'processing' },
      'COMPLETED': { text: '已完成', className: 'completed' },
      'FAILED': { text: '失败', className: 'failed' }
    };
    const info = statusMap[status] || { text: status, className: '' };
    return <span className={`status-badge ${info.className}`}>{info.text}</span>;
  };

  const handleRetry = async (id, filePath) => {
    try {
      await knowledgeManagementAPI.retryParse(id, filePath);
      loadDocuments();
      setMessageModalTitle('成功');
      setMessageModalContent('重试解析成功');
      setShowMessageModal(true);
    } catch (err) {
      console.error('重试解析失败:', err);
      setMessageModalTitle('错误');
      setMessageModalContent('重试失败: ' + (err.message || '未知错误'));
      setShowMessageModal(true);
    }
  };

  return (
    <div>
      <div className="page-header">
        <h1>知识库管理</h1>
        <p>管理系统中的所有知识文档</p>
      </div>

      {/* Upload Section */}
      <div className="admin-card">
        <div className="card-header">
          <h2>上传文档</h2>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label>选择文件</label>
            <input
              id="admin-file-input"
              type="file"
              onChange={handleFileSelect}
              accept=".txt,.pdf,.doc,.docx,.md"
            />
          </div>
          <div className="form-group" style={{ display: 'flex', alignItems: 'flex-end' }}>
            <button
              className="btn btn-primary"
              onClick={handleUpload}
              disabled={!selectedFile || uploading}
              style={{ padding: '10px 20px' }}
            >
              {uploading ? '上传中...' : '上传'}
            </button>
          </div>
        </div>

        <p style={{ fontSize: '12px', color: '#999', margin: '8px 0 0 0' }}>
          支持格式: .txt, .pdf, .doc, .docx, .md
        </p>
      </div>

      {/* Document List */}
      <div className="admin-card">
        <div className="card-header">
          <h2>文档列表</h2>
          <span className="total-info">共 {documents.length} 个文档</span>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
            加载中...
          </div>
        ) : documents.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
            暂无文档，请先上传
          </div>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>文档名称</th>
                <th>状态</th>
                <th>上传时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {documents.map((doc) => (
                <tr key={doc.id}>
                  <td>{doc.id}</td>
                  <td style={{ maxWidth: '300px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {doc.docName}
                  </td>
                  <td>
                    {getStatusBadge(doc.status)}
                    {doc.status === 'FAILED' && doc.errorMessage && (
                      <div style={{ fontSize: '12px', color: '#f44336', marginTop: '4px' }}>
                        {doc.errorMessage.length > 50 
                          ? doc.errorMessage.substring(0, 50) + '...' 
                          : doc.errorMessage
                        }
                      </div>
                    )}
                  </td>
                  <td>
                    {doc.createTime
                      ? new Date(doc.createTime).toLocaleString('zh-CN')
                      : '-'
                    }
                  </td>
                  <td>
                    <button
                      className="action-btn delete"
                      onClick={() => handleDelete(doc.id)}
                    >
                      删除
                    </button>
                    {doc.status === 'FAILED' && (
                      <button
                        className="action-btn retry"
                        onClick={() => handleRetry(doc.id, doc.filePath)}
                      >
                        重试
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>确认删除</h3>
            </div>
            <div className="modal-body">
              <p>确定删除此文档吗？</p>
            </div>
            <div className="modal-footer">
              <button className="modal-btn cancel" onClick={handleDeleteCancel}>
                取消
              </button>
              <button className="modal-btn confirm" onClick={handleDeleteConfirm}>
                确定
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Message Modal */}
      {showMessageModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>{messageModalTitle}</h3>
            </div>
            <div className="modal-body">
              <p>{messageModalContent}</p>
            </div>
            <div className="modal-footer">
              <button className="modal-btn confirm" onClick={handleMessageModalClose}>
                确定
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
