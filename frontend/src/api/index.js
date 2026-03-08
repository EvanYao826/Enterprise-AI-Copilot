import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Response interceptor
api.interceptors.response.use(
  response => {
    if (response.data.code === 200) {
      return response.data;
    }
    return Promise.reject(new Error(response.data.message || 'Request failed'));
  },
  error => {
    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  sendCode: (phone) => api.post(`/auth/sendCode?phone=${phone}`),
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  updateUser: (data) => api.post('/auth/update', data),
};

// Chat API
export const chatAPI = {
  createConversation: (userId, title) =>
    api.post(`/chat/conversations?userId=${userId}&title=${title || ''}`),
  getConversations: (userId) =>
    api.get(`/chat/conversations?userId=${userId}`),
  sendMessage: (data) => api.post('/chat/messages', data),
  getMessages: (conversationId) =>
    api.get(`/chat/messages?conversationId=${conversationId}`),
  deleteConversation: (id) => api.delete(`/chat/conversations/${id}`),
};

// Knowledge API
export const knowledgeAPI = {
  upload: (file, categoryId) => {
    const formData = new FormData();
    formData.append('file', file);
    if (categoryId) {
      formData.append('categoryId', categoryId);
    }
    return api.post('/knowledge/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  list: (categoryId) =>
    api.get(`/knowledge/list${categoryId ? `?categoryId=${categoryId}` : ''}`),
  delete: (id) => api.delete(`/knowledge/${id}`),
};

export default api;
