import axios from 'axios';
import { getToken } from '../utils/tokenStorage';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:9090';

const scanApi = {
  /**
   * 신호 스캔 실행
   */
  getSignals: async (strategy = 'V1', timeframe = '5m') => {
    try {
      const token = getToken();
      const response = await axios.get(`${API_BASE_URL}/api/scan/signals`, {
        params: { strategy, timeframe },
        headers: token ? {
          'Authorization': `Bearer ${token}`
        } : {}
      });
      return response.data;
    } catch (error) {
      console.error('신호 스캔 실패:', error);
      throw error;
    }
  },

  /**
   * 사용 가능한 전략 목록 조회
   */
  getStrategies: async () => {
    try {
      const token = getToken();
      const response = await axios.get(`${API_BASE_URL}/api/scan/strategies`, {
        headers: token ? {
          'Authorization': `Bearer ${token}`
        } : {}
      });
      return response.data;
    } catch (error) {
      console.error('전략 목록 조회 실패:', error);
      throw error;
    }
  }
};

export default scanApi;
