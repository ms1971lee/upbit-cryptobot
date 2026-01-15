import React from 'react';
import './StateMachineVisualizer.css';

/**
 * 상태 머신 시각화 컴포넌트
 */
const StateMachineVisualizer = ({ currentState, direction }) => {
  const states = [
    { id: 'FLAT', name: '대기', icon: '⏸️' },
    { id: 'WAIT_PULLBACK', name: '눌림 대기', icon: '👀' },
    { id: 'WAIT_TRIGGER', name: '트리거 대기', icon: '🎯' },
    { id: direction === 'LONG' ? 'IN_LONG' : 'IN_SHORT', name: direction === 'LONG' ? '롱 진입' : '숏 진입', icon: direction === 'LONG' ? '📈' : '📉' },
    { id: 'COOLDOWN', name: '쿨다운', icon: '❄️' },
  ];

  const getStateIndex = (stateId) => {
    if (stateId === 'IN_LONG' || stateId === 'IN_SHORT') {
      return 3;
    }
    return states.findIndex(s => s.id === stateId);
  };

  const currentIndex = getStateIndex(currentState);

  return (
    <div className="state-machine-container">
      <h3 className="state-machine-title">상태 머신 현황</h3>
      
      <div className="state-flow">
        {states.map((state, index) => (
          <React.Fragment key={state.id}>
            <div 
              className={`state-node ${currentIndex === index ? 'active' : ''} ${currentIndex > index ? 'completed' : ''}`}
            >
              <div className="state-icon">{state.icon}</div>
              <div className="state-name">{state.name}</div>
              {currentIndex === index && (
                <div className="current-indicator">현재</div>
              )}
            </div>
            {index < states.length - 1 && (
              <div className={`state-arrow ${currentIndex > index ? 'completed' : ''}`}>
                →
              </div>
            )}
          </React.Fragment>
        ))}
      </div>
      
      <div className="state-description">
        {currentState === 'FLAT' && '추세를 확인하고 있습니다.'}
        {currentState === 'WAIT_PULLBACK' && '추세가 확인되었습니다. 눌림목을 기다리고 있습니다.'}
        {currentState === 'WAIT_TRIGGER' && '눌림이 감지되었습니다. 진입 트리거를 기다리고 있습니다.'}
        {currentState === 'IN_LONG' && '롱 포지션에 진입했습니다.'}
        {currentState === 'IN_SHORT' && '숏 포지션에 진입했습니다.'}
        {currentState === 'COOLDOWN' && '쿨다운 중입니다. 잠시 후 다시 시작합니다.'}
      </div>
    </div>
  );
};

export default StateMachineVisualizer;
