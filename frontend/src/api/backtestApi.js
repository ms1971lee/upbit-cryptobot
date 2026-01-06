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

// 응답 인터셉터: 401 에러 처리
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      removeToken();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

const backtestApi = {
  /**
   * 백테스트 실행
   * POST /api/backtest/run
   *
   * @param {Object} formData - 백테스트 설정 데이터
   * @returns {Promise} 백테스트 ID 반환
   */
  runBacktest: async (formData) => {
    try {
      const requestBody = {
        name: formData.name,
        market: formData.market,
        timeframe: formData.timeframe,
        startDate: formData.startDate,
        endDate: formData.endDate,
        initialCapital: formData.initialCapital,
        strategyName: formData.strategyName,
        strategyParams: formData.strategyParams, // JSON 객체로 전송
        commissionRate: formData.commissionRate,
        slippageRate: formData.slippageRate
      };

      const response = await api.post('/api/backtest/run', requestBody);
      return response.data;
    } catch (error) {
      console.error('백테스트 실행 실패:', error);
      throw error;
    }
  },

  /**
   * 백테스트 결과 조회
   * GET /api/backtest/results/{backtestId}
   *
   * @param {number} backtestId - 백테스트 ID
   * @returns {Promise} 백테스트 결과
   */
  getResult: async (backtestId) => {
    try {
      const response = await api.get(`/api/backtest/results/${backtestId}`);
      return response.data;
    } catch (error) {
      console.error('백테스트 결과 조회 실패:', error);
      throw error;
    }
  },

  /**
   * 백테스트 이력 조회
   * GET /api/backtest/history
   *
   * @returns {Promise} 백테스트 이력 목록
   */
  getHistory: async () => {
    try {
      const response = await api.get('/api/backtest/history');
      return response.data;
    } catch (error) {
      console.error('백테스트 이력 조회 실패:', error);
      throw error;
    }
  },

  /**
   * 백테스트 결과 삭제
   * DELETE /api/backtest/results/{backtestId}
   *
   * @param {number} backtestId - 백테스트 ID
   * @returns {Promise} 삭제 결과
   */
  deleteResult: async (backtestId) => {
    try {
      const response = await api.delete(`/api/backtest/results/${backtestId}`);
      return response.data;
    } catch (error) {
      console.error('백테스트 결과 삭제 실패:', error);
      throw error;
    }
  },

  /**
   * 백테스트 거래 내역 조회
   * GET /api/backtest/results/{backtestId}/trades
   *
   * @param {number} backtestId - 백테스트 ID
   * @returns {Promise} 거래 내역 목록
   */
  getTrades: async (backtestId) => {
    try {
      const response = await api.get(`/api/backtest/results/${backtestId}/trades`);
      return response.data;
    } catch (error) {
      console.error('거래 내역 조회 실패:', error);
      throw error;
    }
  },

  /**
   * 과거 데이터 수집 시작
   * POST /api/backtest/data/sync
   *
   * @param {Object} syncData - 수집 설정 데이터
   * @returns {Promise} Task ID 반환
   */
  syncData: async (syncData) => {
    try {
      const requestBody = {
        market: syncData.market,
        timeframe: syncData.timeframe,
        startDate: syncData.startDate,
        endDate: syncData.endDate
      };

      const response = await api.post('/api/backtest/data/sync', requestBody);
      return response.data;
    } catch (error) {
      console.error('데이터 수집 시작 실패:', error);
      throw error;
    }
  },

  /**
   * 데이터 수집 진행 상태 조회
   * GET /api/backtest/data/status/{taskId}
   *
   * @param {string} taskId - Task ID
   * @returns {Promise} 수집 진행 상태
   */
  getSyncStatus: async (taskId) => {
    try {
      const response = await api.get(`/api/backtest/data/status/${taskId}`);
      return response.data;
    } catch (error) {
      console.error('수집 상태 조회 실패:', error);
      throw error;
    }
  },

  /**
   * 사용 가능한 데이터 목록 조회
   * GET /api/backtest/data/available
   *
   * @returns {Promise} 사용 가능한 마켓 데이터 목록
   */
  getAvailableData: async () => {
    try {
      const response = await api.get('/api/backtest/data/available');
      return response.data;
    } catch (error) {
      console.error('사용 가능한 데이터 조회 실패:', error);
      throw error;
    }
  },

  /**
   * 전체 데이터 개수 조회
   * GET /api/backtest/data/count
   *
   * @returns {Promise} 전체 캔들 데이터 개수
   */
  getTotalCount: async () => {
    try {
      const response = await api.get('/api/backtest/data/count');
      return response.data;
    } catch (error) {
      console.error('전체 데이터 개수 조회 실패:', error);
      throw error;
    }
  }
};

export default backtestApi;
