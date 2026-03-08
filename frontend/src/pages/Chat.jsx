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
        <div className="sidebar-header">
          <h2>AI 助手</h2>
          <button className="btn btn-primary" onClick={handleCreateConversation}>
            新建对话
          </button>
        </div>

        <div className="conversation-list">
          {conversations.map(conv => (
            <div
              key={conv.id}
              className={`conversation-item ${currentConversation?.id === conv.id ? 'active' : ''}`}
              onClick={() => setCurrentConversation(conv)}
            >
              <div className="conversation-info">
                <span className="conversation-title">{conv.title || '新对话'}</span>
                <span className="conversation-time">
                  {new Date(conv.createTime).toLocaleDateString()}
                </span>
              </div>
              <button
                className="delete-btn"
                onClick={(e) => handleDeleteConversation(conv.id, e)}
              >
                ×
              </button>
            </div>
          ))}
        </div>

        <div className="sidebar-footer">
          <button className="btn btn-default" onClick={handleLogout}>
            退出登录
          </button>
        </div>
      </div>

      {/* Main chat area */}
      <div className="chat-main">
        {currentConversation ? (
          <>
            <div className="chat-header">
              <h3>{currentConversation.title || '新对话'}</h3>
            </div>

            <div className="messages-container">
              {messages.length === 0 ? (
                <div className="empty-messages">
                  <p>开始对话吧！</p>
                </div>
              ) : (
                messages.map((msg, index) => (
                  <div key={msg.id || index} className={`message ${msg.role}`}>
                    <div className="message-avatar">
                      {msg.role === 'user' ? '👤' : '🤖'}
                    </div>
                    <div className="message-content">
                      {msg.content}
                    </div>
                  </div>
                ))
              )}
              {loading && (
                <div className="message assistant">
                  <div className="message-avatar">🤖</div>
                  <div className="message-content">
                    <span className="typing">正在思考...</span>
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            <form className="chat-input-area" onSubmit={handleSendMessage}>
              <input
                type="text"
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                placeholder="输入消息..."
                disabled={loading}
              />
              <button type="submit" className="btn btn-primary" disabled={loading || !inputMessage.trim()}>
                发送
              </button>
            </form>
          </>
        ) : (
          <div className="no-conversation">
            <div className="welcome-content">
              <h2>欢迎使用 AI 知识系统</h2>
              <p>选择一个对话或创建新对话开始</p>
              <button className="btn btn-primary" onClick={handleCreateConversation}>
                创建新对话
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
