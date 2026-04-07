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
  const [menuOpenId, setMenuOpenId] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [editTitle, setEditTitle] = useState('');
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteConversationId, setDeleteConversationId] = useState(null);
  const [abortController, setAbortController] = useState(null);
  const [abortedRequests, setAbortedRequests] = useState(new Set());
  const messagesEndRef = useRef(null);
  const menuRef = useRef(null);

  useEffect(() => {
    document.title = 'AI 知识系统-智能对话';
    return () => {
      document.title = 'AI 知识系统';
    };
  }, []);

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

  const handleDelete = (id, e) => {
    e.stopPropagation();
    setMenuOpenId(null);
    setDeleteConversationId(id);
    setShowDeleteConfirm(true);
  };

  const handleDeleteConfirm = async () => {
    if (!deleteConversationId) return;
    
    try {
      await chatAPI.deleteConversation(deleteConversationId);
      setConversations(conversations.filter(c => c.id !== deleteConversationId));
      if (currentConversation?.id === deleteConversationId) {
        setCurrentConversation(null);
        setMessages([]);
      }
    } catch (err) {
      console.error('删除会话失败:', err);
    } finally {
      setShowDeleteConfirm(false);
      setDeleteConversationId(null);
    }
  };

  const handleDeleteCancel = () => {
    setShowDeleteConfirm(false);
    setDeleteConversationId(null);
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

  const handleDeleteConversation = (id, e) => {
    e.stopPropagation();
    setDeleteConversationId(id);
    setShowDeleteConfirm(true);
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

    // 创建AI思考中的消息
    const thinkingMessageId = Date.now() + 1;
    const thinkingMessage = {
      id: thinkingMessageId,
      conversationId: currentConversation.id,
      role: 'assistant',
      content: '',
      isStreaming: true,
      createTime: new Date().toISOString()
    };

    // 生成唯一的请求ID
    const requestId = Date.now() + Math.random();

    setMessages(prev => [...prev, userMessage, thinkingMessage]);
    setInputMessage('');
    setLoading(true);

    // 创建AbortController用于中断
    const controller = new AbortController();
    setAbortController(controller);

    try {
      // 使用非流式API
      const response = await chatAPI.sendMessage({
        userId,
        conversationId: currentConversation.id,
        content: inputMessage
      }, {
        signal: controller.signal
      });

      // 检查请求是否已被中断
      if (abortedRequests.has(requestId)) {
        console.log('Request was aborted, skipping response processing');
        return;
      }

      // 替换思考消息为完整响应
      const aiMessage = {
        id: thinkingMessageId,
        conversationId: currentConversation.id,
        role: 'assistant',
        content: response.data.content,
        sources: response.data.sources,
        createTime: new Date().toISOString()
      };

      setMessages(prev => prev.map(msg => msg.id === thinkingMessageId ? aiMessage : msg));
      setLoading(false);
      setAbortController(null);

      // 刷新会话列表
      loadConversations();
    } catch (err) {
      if (err.name === 'AbortError') {
        // 用户手动中断
        const abortedMessage = {
          id: thinkingMessageId,
          conversationId: currentConversation.id,
          role: 'assistant',
          content: '已终止回答',
          createTime: new Date().toISOString()
        };
        setMessages(prev => prev.map(msg => msg.id === thinkingMessageId ? abortedMessage : msg));
        // 将请求ID添加到已中断集合
        setAbortedRequests(prev => new Set(prev).add(requestId));
      } else {
        console.error('发送消息失败:', err);
      }
      setLoading(false);
      setAbortController(null);
    }
  };

  const handleStopGeneration = () => {
    if (abortController) {
      abortController.abort();
      // 立即更新UI，显示已终止回答
      setMessages(prev => {
        // 找到最后一条AI消息（思考中状态）
        const lastAssistantMessageIndex = prev.lastIndexOf(
          prev.find(msg => msg.role === 'assistant' && msg.isStreaming)
        );
        if (lastAssistantMessageIndex !== -1) {
          const updatedMessages = [...prev];
          updatedMessages[lastAssistantMessageIndex] = {
            ...updatedMessages[lastAssistantMessageIndex],
            content: '已终止回答',
            isStreaming: false
          };
          return updatedMessages;
        }
        return prev;
      });
      setLoading(false);
      setAbortController(null);
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
        <div className="sidebar-header">
          <div className="sidebar-logo" onClick={() => navigate('/')}>
            <h2>🤖 AI Knowledge</h2>
          </div>
          <button className="new-chat-btn" onClick={handleCreateConversation}>
            <span className="new-chat-icon">+</span> 开启新对话
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
              <div className="chat-header-left">
                <h3>{currentConversation.title || '新对话'}</h3>
              </div>
            </div>

            <div className="messages-container">
              {messages.length === 0 ? (
                <div className="welcome-screen">
                  <div className="welcome-avatar">🤖</div>
                  <h1>今天有什么可以帮到你？</h1>
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
                        {msg.isStreaming && <span className="typing-cursor">▋</span>}
                        {msg.role === 'assistant' && msg.sources && renderSources(msg.sources)}
                      </div>
                    </div>
                  </div>
                ))
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
                    rows={1}
                  />
                  <div className="input-buttons">
                    {loading && (
                      <button 
                        type="button" 
                        className="stop-btn" 
                        onClick={handleStopGeneration}
                        title="停止生成"
                      >
                        ⏹️
                      </button>
                    )}
                    <button type="submit" className="send-btn" disabled={!inputMessage.trim()}>
                      ➤
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </>
        ) : (
          <div className="welcome-screen">
            <div className="welcome-avatar">🤖</div>
            <h1>欢迎使用 AI 知识系统</h1>
            <p>基于 RAG 技术，为您提供精准的企业知识问答服务</p>
            <button className="start-btn" onClick={handleCreateConversation}>
              开始新对话
            </button>
          </div>
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
              <p>确定删除此对话吗？</p>
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
    </div>
  );
}
