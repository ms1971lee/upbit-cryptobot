import axios from 'axios';
import { getToken } from '../utils/tokenStorage';

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
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

/**
 * 트레이딩 API
 */
const tradingApi = {
  /**
   * 현재 거래 모드 조회
   */
  getTradingMode: async () => {
    try {
      const response = await api.get('/api/trading/mode');
      return response.data;
    } catch (error) {
      console.error('거래 모드 조회 실패:', error);
      throw error;
    }
  },

  /**
   * 거래 모드 전환
   */
  switchMode: async (mode) => {
    try {
      const response = await api.post('/api/trading/mode', { mode });
      return response.data;
    } catch (error) {
      console.error('거래 모드 전환 실패:', error);
      throw error;
    }
  },

  /**
   * 테스트 모드 초기화
   */
  resetTestMode: async () => {
    try {
      const response = await api.post('/api/trading/test/reset');
      return response.data;
    } catch (error) {
      console.error('테스트 모드 초기화 실패:', error);
      throw error;
    }
  },

  /**
   * 주문 실행
   */
  executeOrder: async (orderData) => {
    try {
      const response = await api.post('/api/trading/order', orderData);
      return response.data;
    } catch (error) {
      console.error('주문 실행 실패:', error);
      throw error;
    }
  },

  /**
   * 테스트 모드 잔고 조회
   */
  getTestBalances: async () => {
    try {
      const response = await api.get('/api/trading/test/balances');
      return response.data;
    } catch (error) {
      console.error('테스트 잔고 조회 실패:', error);
      throw error;
    }
  },

  /**
   * 테스트 모드 거래 내역 조회
   */
  getTestTrades: async () => {
    try {
      const response = await api.get('/api/trading/test/trades');
      return response.data;
    } catch (error) {
      console.error('테스트 거래 내역 조회 실패:', error);
      throw error;
    }
  },
};

export default tradingApi;
