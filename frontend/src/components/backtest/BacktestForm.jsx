import React, { useState } from 'react';
import './Backtest.css';

const BacktestForm = ({ onSubmit, loading, error }) => {
  // 전략별 기본 파라미터
  const defaultStrategyParams = {
    BUY_AND_HOLD: {},
    MA_CROSS: {
      shortPeriod: 5,
      longPeriod: 20
    },
    RSI: {
      period: 14,
      oversoldLevel: 30,
      overboughtLevel: 70
    },
    BOLLINGER_BANDS: {
      period: 20,
      stdDevMultiplier: 2.0
    }
  };

  // 폼 데이터 state
  const [formData, setFormData] = useState({
    name: '',
    market: 'KRW-BTC',
    startDate: '',
    endDate: '',
    strategyName: 'BUY_AND_HOLD',
    initialCapital: 10000000,
    timeframe: '1d',
    commissionRate: 0.0005,
    slippageRate: 0.0001,
    strategyParams: {}
  });

  const [showAdvanced, setShowAdvanced] = useState(false);
  const [validationErrors, setValidationErrors] = useState({});

  // 입력 핸들러
  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));

    // 유효성 검사 에러 초기화
    if (validationErrors[field]) {
      setValidationErrors(prev => ({
        ...prev,
        [field]: ''
      }));
    }
  };

  // 전략 변경 핸들러
  const handleStrategyChange = (strategyName) => {
    setFormData(prev => ({
      ...prev,
      strategyName,
      strategyParams: defaultStrategyParams[strategyName] || {}
    }));
  };

  // 전략 파라미터 변경 핸들러
  const handleParamChange = (param, value) => {
    setFormData(prev => ({
      ...prev,
      strategyParams: {
        ...prev.strategyParams,
        [param]: parseFloat(value) || 0
      }
    }));
  };

  // 폼 유효성 검사
  const validateForm = () => {
    const errors = {};

    if (!formData.name.trim()) {
      errors.name = '백테스트 이름을 입력하세요';
    }

    if (!formData.startDate) {
      errors.startDate = '시작일을 선택하세요';
    }

    if (!formData.endDate) {
      errors.endDate = '종료일을 선택하세요';
    }

    if (formData.startDate && formData.endDate && formData.startDate >= formData.endDate) {
      errors.endDate = '종료일은 시작일보다 이후여야 합니다';
    }

    if (formData.initialCapital < 100000) {
      errors.initialCapital = '초기 자본은 최소 100,000원 이상이어야 합니다';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // 폼 제출 핸들러
  const handleSubmit = (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    onSubmit(formData);
  };

  // 전략 파라미터 UI 렌더링
  const renderStrategyParams = () => {
    const { strategyName, strategyParams } = formData;

    if (strategyName === 'BUY_AND_HOLD') {
      return null;
    }

    return (
      <div className="strategy-params">
        <h4 className="params-title">전략 파라미터</h4>

        {strategyName === 'MA_CROSS' && (
          <>
            <div className="form-group">
              <label className="form-label">단기 이평선 기간</label>
              <input
                type="number"
                className="form-input"
                value={strategyParams.shortPeriod || 5}
                onChange={(e) => handleParamChange('shortPeriod', e.target.value)}
                min="1"
                max="50"
              />
            </div>
            <div className="form-group">
              <label className="form-label">장기 이평선 기간</label>
              <input
                type="number"
                className="form-input"
                value={strategyParams.longPeriod || 20}
                onChange={(e) => handleParamChange('longPeriod', e.target.value)}
                min="2"
                max="200"
              />
            </div>
          </>
        )}

        {strategyName === 'RSI' && (
          <>
            <div className="form-group">
              <label className="form-label">RSI 기간</label>
              <input
                type="number"
                className="form-input"
                value={strategyParams.period || 14}
                onChange={(e) => handleParamChange('period', e.target.value)}
                min="1"
                max="50"
              />
            </div>
            <div className="form-group">
              <label className="form-label">과매도 수준</label>
              <input
                type="number"
                className="form-input"
                value={strategyParams.oversoldLevel || 30}
                onChange={(e) => handleParamChange('oversoldLevel', e.target.value)}
                min="0"
                max="100"
                step="1"
              />
            </div>
            <div className="form-group">
              <label className="form-label">과매수 수준</label>
              <input
                type="number"
                className="form-input"
                value={strategyParams.overboughtLevel || 70}
                onChange={(e) => handleParamChange('overboughtLevel', e.target.value)}
                min="0"
                max="100"
                step="1"
              />
            </div>
          </>
        )}

        {strategyName === 'BOLLINGER_BANDS' && (
          <>
            <div className="form-group">
              <label className="form-label">볼린저 밴드 기간</label>
              <input
                type="number"
                className="form-input"
                value={strategyParams.period || 20}
                onChange={(e) => handleParamChange('period', e.target.value)}
                min="1"
                max="50"
              />
            </div>
            <div className="form-group">
              <label className="form-label">표준편차 배수</label>
              <input
                type="number"
                className="form-input"
                value={strategyParams.stdDevMultiplier || 2.0}
                onChange={(e) => handleParamChange('stdDevMultiplier', e.target.value)}
                min="0.5"
                max="5"
                step="0.1"
              />
            </div>
          </>
        )}
      </div>
    );
  };

  return (
    <div className="backtest-form">
      <h3 className="form-title">백테스트 설정</h3>

      <form onSubmit={handleSubmit}>
        {/* 기본 옵션 */}
        <div className="form-section">
          <div className="form-group">
            <label className="form-label">백테스트 이름 *</label>
            <input
              type="text"
              className={`form-input ${validationErrors.name ? 'error' : ''}`}
              value={formData.name}
              onChange={(e) => handleInputChange('name', e.target.value)}
              placeholder="예: BTC 매수 후 보유 테스트"
            />
            {validationErrors.name && (
              <span className="error-message">{validationErrors.name}</span>
            )}
          </div>

          <div className="form-group">
            <label className="form-label">마켓 *</label>
            <select
              className="form-select"
              value={formData.market}
              onChange={(e) => handleInputChange('market', e.target.value)}
            >
              <option value="KRW-BTC">KRW-BTC (비트코인)</option>
              <option value="KRW-ETH">KRW-ETH (이더리움)</option>
              <option value="KRW-XRP">KRW-XRP (리플)</option>
              <option value="KRW-ADA">KRW-ADA (에이다)</option>
              <option value="KRW-SOL">KRW-SOL (솔라나)</option>
            </select>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">시작일 *</label>
              <input
                type="date"
                className={`form-input ${validationErrors.startDate ? 'error' : ''}`}
                value={formData.startDate}
                onChange={(e) => handleInputChange('startDate', e.target.value)}
              />
              {validationErrors.startDate && (
                <span className="error-message">{validationErrors.startDate}</span>
              )}
            </div>

            <div className="form-group">
              <label className="form-label">종료일 *</label>
              <input
                type="date"
                className={`form-input ${validationErrors.endDate ? 'error' : ''}`}
                value={formData.endDate}
                onChange={(e) => handleInputChange('endDate', e.target.value)}
              />
              {validationErrors.endDate && (
                <span className="error-message">{validationErrors.endDate}</span>
              )}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">초기 자본 *</label>
            <input
              type="number"
              className={`form-input ${validationErrors.initialCapital ? 'error' : ''}`}
              value={formData.initialCapital}
              onChange={(e) => handleInputChange('initialCapital', parseInt(e.target.value))}
              min="100000"
              step="100000"
            />
            {validationErrors.initialCapital && (
              <span className="error-message">{validationErrors.initialCapital}</span>
            )}
            <small className="form-hint">
              현재: {formData.initialCapital.toLocaleString()}원
            </small>
          </div>

          <div className="form-group">
            <label className="form-label">전략 *</label>
            <select
              className="form-select"
              value={formData.strategyName}
              onChange={(e) => handleStrategyChange(e.target.value)}
            >
              <option value="BUY_AND_HOLD">매수 후 보유 (Buy & Hold)</option>
              <option value="MA_CROSS">이동평균 교차 (MA Cross)</option>
              <option value="RSI">RSI 전략</option>
              <option value="BOLLINGER_BANDS">볼린저 밴드 전략</option>
            </select>
          </div>

          {renderStrategyParams()}
        </div>

        {/* 고급 옵션 토글 */}
        <button
          type="button"
          className="advanced-toggle"
          onClick={() => setShowAdvanced(!showAdvanced)}
        >
          {showAdvanced ? '▼' : '▶'} 고급 옵션
        </button>

        {/* 고급 옵션 */}
        {showAdvanced && (
          <div className="form-section advanced-section">
            <div className="form-group">
              <label className="form-label">타임프레임</label>
              <select
                className="form-select"
                value={formData.timeframe}
                onChange={(e) => handleInputChange('timeframe', e.target.value)}
              >
                <option value="5m">5분봉</option>
                <option value="15m">15분봉</option>
                <option value="30m">30분봉</option>
                <option value="1h">1시간봉</option>
                <option value="1d">일봉</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">수수료율 (%)</label>
              <input
                type="number"
                className="form-input"
                value={formData.commissionRate * 100}
                onChange={(e) => handleInputChange('commissionRate', parseFloat(e.target.value) / 100)}
                step="0.01"
                min="0"
                max="1"
              />
              <small className="form-hint">
                현재: {(formData.commissionRate * 100).toFixed(2)}%
              </small>
            </div>

            <div className="form-group">
              <label className="form-label">슬리피지 (%)</label>
              <input
                type="number"
                className="form-input"
                value={formData.slippageRate * 100}
                onChange={(e) => handleInputChange('slippageRate', parseFloat(e.target.value) / 100)}
                step="0.01"
                min="0"
                max="1"
              />
              <small className="form-hint">
                현재: {(formData.slippageRate * 100).toFixed(2)}%
              </small>
            </div>
          </div>
        )}

        {/* 에러 메시지 */}
        {error && (
          <div className="form-error">
            {error}
          </div>
        )}

        {/* 제출 버튼 */}
        <button
          type="submit"
          className="submit-btn"
          disabled={loading}
        >
          {loading ? '백테스트 실행 중...' : '백테스트 실행'}
        </button>
      </form>
    </div>
  );
};

export default BacktestForm;
