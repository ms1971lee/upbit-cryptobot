import axios from 'axios';
import { getToken, removeToken } from '../utils/tokenStorage';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:9090';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 요청 인터셉터: 토큰 자동 추가
apiClient.interceptors.request.use(
  (config) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 응답 인터셉터: 401 에러 시 로그아웃
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      removeToken();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const authAPI = {
  signup: (data) => apiClient.post('/api/auth/signup', data),
  login: (data) => apiClient.post('/api/auth/login', data),
  getCurrentUser: () => apiClient.get('/api/auth/me'),
  updateApiKeys: (data) => apiClient.put('/api/auth/api-keys', data), // deprecated

  // 프로필 관리
  updateProfile: (data) => apiClient.put('/api/auth/profile', data),
  changePassword: (data) => apiClient.put('/api/auth/password', data),

  // API Key 관리
  getAllApiKeys: () => apiClient.get('/api/api-keys'),
  getActiveApiKey: () => apiClient.get('/api/api-keys/active'),
  createApiKey: (data) => apiClient.post('/api/api-keys', data),
  updateApiKey: (id, data) => apiClient.put(`/api/api-keys/${id}`, data),
  deleteApiKey: (id) => apiClient.delete(`/api/api-keys/${id}`),
  activateApiKey: (id) => apiClient.post(`/api/api-keys/${id}/activate`),
  testApiKey: (id) => apiClient.post(`/api/api-keys/${id}/test`)
};

export default apiClient;
