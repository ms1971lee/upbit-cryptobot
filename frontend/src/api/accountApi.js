import axios from 'axios';
import { getToken, removeToken } from '../utils/tokenStorage';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:9090';

// Axios 인스턴스 생성
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터: Authorization 헤더 추가
api.interceptors.request.use(
  (config) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 응답 인터셉터: 에러 처리
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      removeToken();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

/**
 * 계좌 정보 API
 */
const accountApi = {
  /**
   * 계좌 조회
   */
  getAccounts: async () => {
    try {
      const response = await api.get('/api/accounts');
      return response.data;
    } catch (error) {
      console.error('계좌 조회 실패:', error);
      throw error;
    }
  },

  /**
   * 계좌 요약 정보 조회
   */
  getAccountSummary: async () => {
    try {
      const response = await api.get('/api/accounts/summary');
      return response.data;
    } catch (error) {
      console.error('계좌 요약 정보 조회 실패:', error);
      throw error;
    }
  },
};

export default accountApi;
