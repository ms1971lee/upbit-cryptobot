import axios from 'axios';

const API_BASE = '/api/strategy/ema-trend';

/**
 * EMA 추세추종 전략 API
 */
const emaTrendApi = {
  /**
   * 특정 종목 상태 조회
   */
  getState: async (symbol) => {
    const response = await axios.get(`${API_BASE}/state/${symbol}`);
    return response.data;
  },

  /**
   * 모든 종목 상태 조회
   */
  getAllStates: async () => {
    const response = await axios.get(`${API_BASE}/states`);
    return response.data;
  },

  /**
   * 상태 초기화
   */
  resetState: async (symbol) => {
    const response = await axios.post(`${API_BASE}/reset/${symbol}`);
    return response.data;
  },

  /**
   * 전체 초기화
   */
  resetAll: async () => {
    const response = await axios.post(`${API_BASE}/reset-all`);
    return response.data;
  },

  /**
   * 설정 조회
   */
  getConfig: async () => {
    const response = await axios.get(`${API_BASE}/config`);
    return response.data;
  },

  /**
   * 상태 흐름 정보
   */
  getStateFlow: async () => {
    const response = await axios.get(`${API_BASE}/state-flow`);
    return response.data;
  }
};

export default emaTrendApi;
