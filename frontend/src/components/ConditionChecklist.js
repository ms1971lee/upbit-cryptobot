import React from 'react';
import './ConditionChecklist.css';

/**
 * 조건 체크 현황 컴포넌트
 */
const ConditionChecklist = ({ conditions, direction }) => {
  const {
    trendUp = false,
    trendDown = false,
    pullbackLong = false,
    pullbackShort = false,
    triggerLong = false,
    triggerShort = false,
    filterLong = false,
    filterShort = false,
    maChop = false
  } = conditions || {};

  const isLong = direction === 'LONG' || trendUp;
  
  const checkItems = isLong ? [
    { label: 'TREND_UP', checked: trendUp, desc: '15분봉 상승 추세' },
    { label: 'PULLBACK_LONG', checked: pullbackLong, desc: '5분봉 눌림 감지' },
    { label: 'TRIGGER_LONG', checked: triggerLong, desc: '진입 트리거' },
    { label: 'FILTER_LONG', checked: filterLong, desc: '구조 필터 통과' },
    { label: 'MA_CHOP', checked: !maChop, desc: 'EMA 정배열 (엉킴 없음)', inverse: true },
  ] : [
    { label: 'TREND_DN', checked: trendDown, desc: '15분봉 하락 추세' },
    { label: 'PULLBACK_SHORT', checked: pullbackShort, desc: '5분봉 되돌림 감지' },
    { label: 'TRIGGER_SHORT', checked: triggerShort, desc: '진입 트리거' },
    { label: 'FILTER_SHORT', checked: filterShort, desc: '구조 필터 통과' },
    { label: 'MA_CHOP', checked: !maChop, desc: 'EMA 역배열 (엉킴 없음)', inverse: true },
  ];

  const passedCount = checkItems.filter(item => item.checked).length;
  const totalCount = checkItems.length;
  const percentage = Math.round((passedCount / totalCount) * 100);

  return (
    <div className="condition-checklist">
      <div className="checklist-header">
        <h3>조건 체크 현황</h3>
        <div className="progress-badge" style={{ 
          background: percentage === 100 ? '#00c896' : percentage >= 60 ? '#f0b90b' : '#ea3943'
        }}>
          {passedCount}/{totalCount} ({percentage}%)
        </div>
      </div>
      
      <div className="checklist-items">
        {checkItems.map((item, index) => (
          <div 
            key={index} 
            className={`checklist-item ${item.checked ? 'checked' : ''}`}
          >
            <div className="check-icon">
              {item.checked ? '✅' : '⬜'}
            </div>
            <div className="check-content">
              <div className="check-label">{item.label}</div>
              <div className="check-desc">{item.desc}</div>
            </div>
          </div>
        ))}
      </div>
      
      {percentage === 100 && (
        <div className="ready-signal">
          🚀 모든 조건 충족! 진입 준비 완료
        </div>
      )}
    </div>
  );
};

export default ConditionChecklist;
