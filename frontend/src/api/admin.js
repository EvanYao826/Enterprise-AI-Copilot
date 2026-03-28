import axios from 'axios';

const api = axios.create({
  baseURL: '/api/admin',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor - add admin token
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('adminToken');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => Promise.reject(error)
);

// Response interceptor
api.interceptors.response.use(
  response => {
    if (response.data.code === 200) {
      return response.data;
    }
    return Promise.reject(new Error(response.data.message || 'Request failed'));
  },
  error => {
    // Unauthorized - clear token and redirect to login
    if (error.response?.status === 401) {
      localStorage.removeItem('adminToken');
      localStorage.removeItem('adminInfo');
      window.location.href = '/admin/login';
    }
    return Promise.reject(error);
  }
);

export const adminAuthAPI = {
  login: (username, password) => {
    const params = new URLSearchParams();
    params.append('username', username);
    params.append('password', password);
    return api.post('/login', params, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    });
  }
};

export const userManagementAPI = {
  listUsers: (page = 1, size = 10) =>
    api.get(`/users?page=${page}&size=${size}`),
  updateUserStatus: (userId, status) =>
    api.post(`/users/${userId}/status`, null, {
        params: { status }
    })
};

export const knowledgeManagementAPI = {
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

export const qaLogAPI = {
  list: (page = 1, size = 10) =>
    api.get(`/logs?page=${page}&size=${size}`),
};

export default api;
