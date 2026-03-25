import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { chatAPI } from '../api';
import './Chat.css';

export default function Chat() {
  const navigate = useNavigate();
  const userId = parseInt(localStorage.getItem('userId'));
  const [conversations, setConversations] = useState([]);
  const [currentConversation, setCurrentConversation] = useState(null);
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    if (!userId) {
      navigate('/login');
      return;
    }
    loadConversations();
  }, [userId, navigate]);

  useEffect(() => {
    if (currentConversation) {
      loadMessages(currentConversation.id);
    }
  }, [currentConversation]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const getFileInfo = (fullName) => {
    if (!fullName) return { icon: '📄', name: '相关文档' };
    
    // 1. 去除 UUID 前缀 (匹配 8-4-4-4-12_ 格式)
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}_/i;
    const cleanName = fullName.replace(uuidRegex, '');
    
    // 2. 根据后缀匹配图标
    const ext = cleanName.split('.').pop().toLowerCase();
    let icon = '📄';
    switch (ext) {
      case 'pdf': icon = '📕'; break;
      case 'doc':
      case 'docx': icon = '📘'; break;
      case 'txt': icon = '📄'; break;
      case 'md': icon = '📝'; break;
      case 'xlsx':
      case 'xls': icon = '📗'; break;
      case 'ppt':
      case 'pptx': icon = '📙'; break;
      case 'zip':
      case 'rar': icon = '📦'; break;
      default: icon = '📄';
    }
    
    return { icon, name: cleanName };
  };

  const renderSources = (sourcesJson) => {
    if (!sourcesJson) return null;
    try {
      const sources = JSON.parse(sourcesJson);
      if (!Array.isArray(sources) || sources.length === 0) return null;
      return (
        <div className="message-sources">
          <h4>参考来源:</h4>
          <ul>
            {sources.map((s, i) => {
              const { icon, name } = getFileInfo(s.doc || s.doc_name);
              return (
                <li key={i}>
                  {/* 如果 source 字段是 URL，直接打开；否则跳转到内部知识库页面 */}
                  {s.source && (s.source.startsWith('http://') || s.source.startsWith('https://')) ? (
                      <span className="source-link" onClick={() => window.open(s.source, '_blank')}>
                        {icon} {name}
                      </span>
                  ) : (
                      <span className="source-link" onClick={() => window.open(`/knowledge?docId=${s.doc_id || s.docId}&page=${s.page}`, '_blank')}>
                        {icon} {name}
                      </span>
                  )}
                </li>
              );
            })}
          </ul>
        </div>
      );
    } catch (e) {
      console.error("Parse sources failed", e);
      return null;
    }
  };

  const [menuOpenId, setMenuOpenId] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [editTitle, setEditTitle] = useState('');
  const menuRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setMenuOpenId(null);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleMenuClick = (id, e) => {
    e.stopPropagation();
    setMenuOpenId(menuOpenId === id ? null : id);
  };

  const handlePin = async (conv, e) => {
    e.stopPropagation();
    setMenuOpenId(null);
    try {
      await chatAPI.updateConversation(conv.id, { isPinned: !conv.isPinned });
      loadConversations();
    } catch (err) {
      console.error('更新置顶失败', err);
    }
  };

  const handleRenameStart = (conv, e) => {
    e.stopPropagation();
    setMenuOpenId(null);
    setEditingId(conv.id);
    setEditTitle(conv.title);
  };

  const handleRenameSave = async (id) => {
    try {
      await chatAPI.updateConversation(id, { title: editTitle });
      setEditingId(null);
      loadConversations();
    } catch (err) {
      console.error('重命名失败', err);
    }
  };

  const handleRenameKeyDown = (e, id) => {
    if (e.key === 'Enter') {
      handleRenameSave(id);
    } else if (e.key === 'Escape') {
      setEditingId(null);
    }
  };

  const handleDelete = async (id, e) => {
    e.stopPropagation();
    setMenuOpenId(null);
    if (!window.confirm('确定删除此对话吗？')) return;

    try {
      await chatAPI.deleteConversation(id);
      setConversations(conversations.filter(c => c.id !== id));
      if (currentConversation?.id === id) {
        setCurrentConversation(null);
        setMessages([]);
      }
    } catch (err) {
      console.error('删除会话失败:', err);
    }
  };

  const loadConversations = async () => {
    try {
      const response = await chatAPI.getConversations(userId);
      setConversations(response.data || []);
    } catch (err) {
      console.error('加载会话失败:', err);
    }
  };

  const loadMessages = async (conversationId) => {
    try {
      const response = await chatAPI.getMessages(conversationId);
      setMessages(response.data || []);
    } catch (err) {
      console.error('加载消息失败:', err);
    }
  };

  const handleCreateConversation = async () => {
    try {
      const response = await chatAPI.createConversation(userId, '新对话');
      setConversations([response.data, ...conversations]);
      setCurrentConversation(response.data);
    } catch (err) {
      console.error('创建会话失败:', err);
    }
  };

  const handleDeleteConversation = async (id, e) => {
    e.stopPropagation();
    if (!confirm('确定删除此对话吗？')) return;

    try {
      await chatAPI.deleteConversation(id);
      setConversations(conversations.filter(c => c.id !== id));
      if (currentConversation?.id === id) {
        setCurrentConversation(null);
        setMessages([]);
      }
    } catch (err) {
      console.error('删除会话失败:', err);
    }
  };

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!inputMessage.trim() || !currentConversation) return;

    const userMessage = {
      id: Date.now(),
      conversationId: currentConversation.id,
      role: 'user',
      content: inputMessage,
      createTime: new Date().toISOString()
    };

    setMessages(prev => [...prev, userMessage]);
    setInputMessage('');
    setLoading(true);

    try {
      const response = await chatAPI.sendMessage({
        userId,
        conversationId: currentConversation.id,
        content: inputMessage
      });
      setMessages(prev => [...prev, response.data]);
      
      // 如果是第一条消息，刷新会话列表以获取生成的标题
      if (messages.length === 0) {
        // 给一点时间让后台异步生成标题
        setTimeout(async () => {
          const res = await chatAPI.getConversations(userId);
          const updatedList = res.data || [];
          setConversations(updatedList);
          
          const updatedConv = updatedList.find(c => c.id === currentConversation.id);
          if (updatedConv) {
            setCurrentConversation(updatedConv);
          }
        }, 2000);
      }
    } catch (err) {
      console.error('发送消息失败:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('user');
    localStorage.removeItem('userId');
    navigate('/login');
  };

  return (
    <div className="chat-layout">
      {/* Sidebar */}
      <div className="chat-sidebar">
        <div className="sidebar-logo" onClick={() => navigate('/')}>
          <h2>🤖 AI Knowledge</h2>
        </div>
        
        <div className="sidebar-header">
          <button className="new-chat-btn" onClick={handleCreateConversation}>
            <span className="new-chat-icon">+</span> 新建对话
          </button>
        </div>

        <div className="conversation-list">
          {conversations.map(conv => (
            <div
              key={conv.id}
              className={`conversation-item ${currentConversation?.id === conv.id ? 'active' : ''} ${conv.isPinned ? 'pinned' : ''}`}
              onClick={() => setCurrentConversation(conv)}
            >
              {editingId === conv.id ? (
                <input
                  type="text"
                  value={editTitle}
                  onChange={(e) => setEditTitle(e.target.value)}
                  onBlur={() => handleRenameSave(conv.id)}
                  onKeyDown={(e) => handleRenameKeyDown(e, conv.id)}
                  autoFocus
                  className="rename-input"
                  onClick={(e) => e.stopPropagation()}
                />
              ) : (
                <span className="conversation-title">
                  {conv.isPinned && <span className="pin-icon">📌</span>}
                  {conv.title || '新对话'}
                </span>
              )}
              
              <button
                className="menu-btn"
                onClick={(e) => handleMenuClick(conv.id, e)}
              >
                •••
              </button>

              {menuOpenId === conv.id && (
                <div className="context-menu" ref={menuRef}>
                  <div className="menu-item" onClick={(e) => handleRenameStart(conv, e)}>
                    ✏️ 重命名
                  </div>
                  <div className="menu-item" onClick={(e) => handlePin(conv, e)}>
                    {conv.isPinned ? '🚫 取消置顶' : '📌 置顶'}
                  </div>
                  <div className="menu-item delete" onClick={(e) => handleDelete(conv.id, e)}>
                    🗑️ 删除
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>

        <div className="sidebar-footer">
          <div className="user-profile" onClick={handleLogout} title="点击退出登录">
            <div className="user-avatar">👤</div>
            <span>退出登录</span>
          </div>
        </div>
      </div>

      {/* Main chat area */}
      <div className="chat-main">
        {currentConversation ? (
          <>
            <div className="chat-header">
              {currentConversation.title || '新对话'}
            </div>

            <div className="messages-container">
              {messages.length === 0 ? (
                <div className="welcome-screen">
                  <div className="message-avatar" style={{width: 60, height: 60, fontSize: 32, marginBottom: 20}}>🤖</div>
                  <h1>有什么我可以帮你的吗？</h1>
                </div>
              ) : (
                messages.map((msg, index) => (
                  <div key={msg.id || index} className={`message-row ${msg.role}`}>
                    <div className="message-content-wrapper">
                      <div className="message-avatar">
                        {msg.role === 'user' ? '👤' : '🤖'}
                      </div>
                      <div className="message-body">
                        {msg.content}
                        {msg.role === 'assistant' && msg.sources && renderSources(msg.sources)}
                      </div>
                    </div>
                  </div>
                ))
              )}
              {loading && (
                <div className="message-row assistant">
                  <div className="message-content-wrapper">
                    <div className="message-avatar">🤖</div>
                    <div className="message-body">
                      <span className="typing">正在思考...</span>
                    </div>
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            <div className="input-container">
              <div className="input-wrapper">
                <form className="chat-input-area" onSubmit={handleSendMessage}>
                  <textarea
                    value={inputMessage}
                    onChange={(e) => setInputMessage(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && !e.shiftKey) {
                        e.preventDefault();
                        handleSendMessage(e);
                      }
                    }}
                    placeholder="给 AI 发送消息..."
                    disabled={loading}
                    rows={1}
                  />
                  <button type="submit" className="send-btn" disabled={loading || !inputMessage.trim()}>
                    ➤
                  </button>
                </form>
              </div>
            </div>
          </>
        ) : (
          <div className="welcome-screen">
            <h1>欢迎使用 AI 知识系统</h1>
            <p>基于 RAG 技术，为您提供精准的企业知识问答服务</p>
            <button className="start-btn" onClick={handleCreateConversation}>
              开始新对话
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
