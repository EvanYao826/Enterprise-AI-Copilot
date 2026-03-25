import React, { useState, useEffect } from 'react';
import { getNoticeList, addNotice, updateNotice, deleteNotice } from '../../api/notice';
import './NoticeManagement.css';

export default function NoticeManagement() {
  const [notices, setNotices] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({ title: '', content: '', isActive: true });
  const [editingId, setEditingId] = useState(null);

  useEffect(() => {
    fetchNotices();
  }, []);

  const fetchNotices = async () => {
    setLoading(true);
    try {
      const res = await getNoticeList({ page: 1, size: 100 });
      if (res.code === 200) {
        setNotices(res.data.records);
      }
    } catch (error) {
      alert('加载通知失败');
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = () => {
    setEditingId(null);
    setFormData({ title: '', content: '', isActive: true });
    setShowModal(true);
  };

  const handleEdit = (notice) => {
    setEditingId(notice.id);
    setFormData({ ...notice });
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('确认删除?')) return;
    try {
      await deleteNotice(id);
      fetchNotices();
    } catch (error) {
      alert('删除失败');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) {
        await updateNotice({ ...formData, id: editingId });
      } else {
        await addNotice(formData);
      }
      setShowModal(false);
      fetchNotices();
    } catch (error) {
      console.error(error);
      alert('操作失败');
    }
  };

  return (
    <div className="notice-container">
      <div className="header-actions">
        <button className="btn btn-primary" onClick={handleAdd}>发布通知</button>
      </div>
      
      <table className="notice-table">
        <thead>
          <tr>
            <th>标题</th>
            <th>内容</th>
            <th>状态</th>
            <th>发布时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          {notices.map(notice => (
            <tr key={notice.id}>
              <td>{notice.title}</td>
              <td className="content-cell">{notice.content}</td>
              <td>{notice.isActive ? '有效' : '失效'}</td>
              <td>{notice.createTime}</td>
              <td>
                <button className="btn btn-sm btn-link" onClick={() => handleEdit(notice)}>编辑</button>
                <button className="btn btn-sm btn-danger" onClick={() => handleDelete(notice.id)}>删除</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {showModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>{editingId ? '编辑通知' : '发布通知'}</h3>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>标题</label>
                <input 
                  type="text" 
                  value={formData.title} 
                  onChange={e => setFormData({...formData, title: e.target.value})}
                  required 
                />
              </div>
              <div className="form-group">
                <label>内容</label>
                <textarea 
                  rows="4" 
                  value={formData.content} 
                  onChange={e => setFormData({...formData, content: e.target.value})}
                  required 
                />
              </div>
              <div className="form-group checkbox">
                <label>
                  <input 
                    type="checkbox" 
                    checked={formData.isActive} 
                    onChange={e => setFormData({...formData, isActive: e.target.checked})} 
                  />
                  有效
                </label>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>取消</button>
                <button type="submit" className="btn btn-primary">保存</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
