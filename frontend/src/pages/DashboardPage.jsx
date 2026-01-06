import React, { useState, useEffect } from 'react';
import MainLayout from '../components/layout/MainLayout';
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts';
import accountApi from '../api/accountApi';
import './DashboardPage.css';

const DashboardPage = () => {
  const [activeTab, setActiveTab] = useState('assets'); // assets, profit, transactions, pending, deposit

  // 실시간 계좌 데이터 상태
  const [accountSummary, setAccountSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [activeIndex, setActiveIndex] = useState(null);

  // 계좌 정보 로드
  useEffect(() => {
    loadAccountSummary();

    // 30초마다 자동 갱신
    const interval = setInterval(() => {
      loadAccountSummary();
    }, 30000);

    return () => clearInterval(interval);
  }, []);

  const loadAccountSummary = async () => {
    setLoading(true);
    setError('');

    try {
      const response = await accountApi.getAccountSummary();
      if (response.success) {
        setAccountSummary(response);
      } else {
        setError(response.error || 'API 키가 등록되지 않았습니다');
      }
    } catch (err) {
      console.error('계좌 정보 로드 실패:', err);
      setError('계좌 정보를 불러올 수 없습니다');
    } finally {
      setLoading(false);
    }
  };

  // 실시간 데이터로부터 계산
  const assetData = accountSummary ? {
    krw: accountSummary.totalKRW || 0,
    totalBuy: accountSummary.totalBuyAmount || 0,
    totalEval: accountSummary.totalEvalAmount || 0,
    orderAvailable: accountSummary.totalKRW || 0
  } : {
    krw: 0,
    totalBuy: 0,
    totalEval: 0,
    orderAvailable: 0
  };

  const profitData = accountSummary ? {
    totalAssets: accountSummary.totalAssets || 0,
    totalProfit: accountSummary.totalProfit || 0,
    profitRate: accountSummary.totalProfitRate || 0
  } : {
    totalAssets: 0,
    totalProfit: 0,
    profitRate: 0
  };

  // 코인 아이콘 가져오기
  const getCoinIcon = (market) => {
    const symbol = market ? market.replace('KRW-', '') : '';
    const icons = {
      'BTC': '₿',
      'ETH': 'Ξ',
      'XRP': '✖',
      'DOGE': '🐶',
      'SHIB': '🐕',
      'ADA': '₳',
      'TRX': '🔺'
    };
    return icons[symbol] || (symbol ? symbol.charAt(0) : '?');
  };

  const holdings = accountSummary && accountSummary.holdings ? accountSummary.holdings.map((holding, index) => ({
    id: index + 1,
    name: holding.koreanName || holding.currency,
    symbol: holding.currency,
    market: holding.market,
    icon: getCoinIcon(holding.market),
    quantity: holding.balance.toFixed(8),
    avgPrice: holding.avgBuyPrice.toLocaleString(),
    buyAmount: Math.round(holding.buyAmount).toLocaleString(),
    evalAmount: Math.round(holding.evalAmount).toLocaleString(),
    profitRate: holding.profitRate.toFixed(2),
    profitAmount: Math.round(holding.profitAmount)
  })) : [];

  // 파이 차트 데이터 계산
  const colors = ['#00d4ff', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#06b6d4'];
  const assetRatio = accountSummary && accountSummary.holdings ? [
    // KRW 먼저 추가
    ...(accountSummary.totalKRW > 0 ? [{
      name: 'KRW',
      value: (accountSummary.totalKRW / accountSummary.totalAssets * 100),
      color: '#10b981',
      glow: 'rgba(16, 185, 129, 0.5)'
    }] : []),
    // 보유 코인들
    ...accountSummary.holdings.map((holding, index) => ({
      name: holding.currency,
      value: (holding.evalAmount / accountSummary.totalAssets * 100),
      color: colors[index % colors.length],
      glow: `${colors[index % colors.length]}80`
    }))
  ].filter(item => item.value > 0.01) : []; // 0.01% 이상만 표시

  // 활성화된 섹터 렌더러 (호버 시 확대 효과)
  const renderActiveShape = (props) => {
    const { cx, cy, innerRadius, outerRadius, startAngle, endAngle, fill, payload, percent } = props;

    return (
      <g>
        <text x={cx} y={cy - 20} textAnchor="middle" fill="#e8eaf6" style={{ fontSize: '20px', fontWeight: 'bold' }}>
          {payload.name}
        </text>
        <text x={cx} y={cy + 10} textAnchor="middle" fill="#00d4ff" style={{ fontSize: '28px', fontWeight: 'bold' }}>
          {`${(percent * 100).toFixed(1)}%`}
        </text>
        <text x={cx} y={cy + 35} textAnchor="middle" fill="#b8bfd8" style={{ fontSize: '14px' }}>
          비중
        </text>
        <Pie
          cx={cx}
          cy={cy}
          innerRadius={innerRadius}
          outerRadius={outerRadius + 10}
          startAngle={startAngle}
          endAngle={endAngle}
          fill={fill}
          style={{
            filter: `drop-shadow(0 0 20px ${payload.glow})`
          }}
        />
      </g>
    );
  };

  const onPieEnter = (_, index) => {
    setActiveIndex(index);
  };

  const onPieLeave = () => {
    setActiveIndex(null);
  };

  const tabs = [
    { id: 'assets', label: '보유자산' },
    { id: 'profit', label: '투자순익' },
    { id: 'transactions', label: '거래내역' },
    { id: 'pending', label: '미체결' },
    { id: 'deposit', label: '입출금내역' }
  ];

  return (
    <MainLayout>
      <div className="dashboard-page">
        {/* 탭 메뉴 */}
        <div className="dashboard-tabs">
          {tabs.map(tab => (
            <button
              key={tab.id}
              className={`tab-btn ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* 보유자산 탭 */}
        {activeTab === 'assets' && (
          <div className="tab-content">
            <div className="dashboard-grid">
              {/* 왼쪽: 보유자산 & 투자수익 */}
              <div className="left-section">
                {/* 보유자산 */}
                <div className="info-card">
                  <div className="info-row">
                    <span className="info-label">보유 KRW</span>
                    <span className="info-value">
                      {assetData.krw.toLocaleString()} <span className="unit">KRW</span>
                    </span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">총 매수</span>
                    <span className="info-value">
                      {assetData.totalBuy.toLocaleString()} <span className="unit">KRW</span>
                    </span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">총 평가</span>
                    <span className="info-value">
                      {assetData.totalEval.toLocaleString()} <span className="unit">KRW</span>
                    </span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">주문가능</span>
                    <span className="info-value">
                      {assetData.orderAvailable.toLocaleString()} <span className="unit">KRW</span>
                    </span>
                  </div>
                </div>

                {/* 투자수익 */}
                <div className="profit-card">
                  <div className="profit-row">
                    <span className="profit-label">총 보유자산</span>
                    <span className="profit-value">
                      {profitData.totalAssets.toLocaleString()} <span className="unit">KRW</span>
                    </span>
                  </div>
                  <div className="profit-row">
                    <span className="profit-label">총평가손익</span>
                    <span className={`profit-value ${profitData.totalProfit < 0 ? 'negative' : 'positive'}`}>
                      {profitData.totalProfit.toLocaleString()} <span className="unit">KRW</span>
                    </span>
                  </div>
                  <div className="profit-row">
                    <span className="profit-label">총평가수익률</span>
                    <span className={`profit-value ${profitData.profitRate < 0 ? 'negative' : 'positive'}`}>
                      {profitData.profitRate} <span className="unit">%</span>
                    </span>
                  </div>
                </div>
              </div>

              {/* 오른쪽: 파이 차트 */}
              <div className="right-section">
                <div className="chart-card">
                  <div className="chart-header">
                    <span>보유 비중 (%)</span>
                    <button
                      className="refresh-btn"
                      onClick={loadAccountSummary}
                      disabled={loading}
                    >
                      ⟳
                    </button>
                  </div>
                  {loading && (
                    <div style={{ textAlign: 'center', padding: '100px 0', color: '#b8bfd8' }}>
                      <div className="spinner" style={{ margin: '0 auto 20px' }}></div>
                      로딩 중...
                    </div>
                  )}
                  {error && !loading && (
                    <div style={{ textAlign: 'center', padding: '100px 20px', color: '#ef4444' }}>
                      {error}
                    </div>
                  )}
                  {!loading && !error && assetRatio.length === 0 && (
                    <div style={{ textAlign: 'center', padding: '100px 20px', color: '#b8bfd8' }}>
                      보유 중인 자산이 없습니다
                    </div>
                  )}
                  {!loading && !error && assetRatio.length > 0 && (
                    <div className="pie-chart-container">
                    <ResponsiveContainer width="100%" height={350}>
                      <PieChart>
                        <defs>
                          {assetRatio.map((entry, index) => (
                            <radialGradient key={`gradient-${index}`} id={`gradient-${entry.name}`}>
                              <stop offset="0%" stopColor={entry.color} stopOpacity={0.9} />
                              <stop offset="100%" stopColor={entry.color} stopOpacity={0.6} />
                            </radialGradient>
                          ))}
                        </defs>
                        <Pie
                          data={assetRatio}
                          cx="50%"
                          cy="45%"
                          innerRadius={60}
                          outerRadius={90}
                          fill="#8884d8"
                          dataKey="value"
                          paddingAngle={3}
                          onMouseEnter={onPieEnter}
                          onMouseLeave={onPieLeave}
                          activeIndex={activeIndex}
                          activeShape={renderActiveShape}
                          animationBegin={0}
                          animationDuration={800}
                          animationEasing="ease-out"
                        >
                          {assetRatio.map((entry, index) => (
                            <Cell
                              key={`cell-${index}`}
                              fill={`url(#gradient-${entry.name})`}
                              stroke={entry.color}
                              strokeWidth={2}
                              style={{
                                filter: activeIndex === index
                                  ? `drop-shadow(0 0 15px ${entry.glow})`
                                  : `drop-shadow(0 0 8px ${entry.glow})`,
                                transition: 'all 0.3s ease',
                                cursor: 'pointer'
                              }}
                            />
                          ))}
                        </Pie>
                        {activeIndex === null && (
                          <text x="50%" y="45%" textAnchor="middle" dominantBaseline="middle">
                            <tspan x="50%" dy="-10" style={{ fontSize: '16px', fill: '#b8bfd8', fontWeight: '500' }}>
                              총 보유
                            </tspan>
                            <tspan x="50%" dy="30" style={{ fontSize: '24px', fill: '#00d4ff', fontWeight: 'bold' }}>
                              100%
                            </tspan>
                          </text>
                        )}
                        <Tooltip
                          contentStyle={{
                            background: 'rgba(15, 20, 41, 0.98)',
                            border: '2px solid rgba(0, 212, 255, 0.5)',
                            borderRadius: '12px',
                            color: '#e8eaf6',
                            padding: '12px 16px',
                            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.4)',
                            backdropFilter: 'blur(10px)'
                          }}
                          itemStyle={{
                            color: '#00d4ff',
                            fontWeight: '600',
                            fontSize: '14px'
                          }}
                          formatter={(value, name) => [`${value.toFixed(1)}%`, name]}
                        />
                        <Legend
                          verticalAlign="bottom"
                          height={50}
                          iconType="circle"
                          iconSize={12}
                          formatter={(value, entry) => (
                            <span style={{
                              color: '#e8eaf6',
                              fontSize: '14px',
                              fontWeight: '500',
                              marginLeft: '8px'
                            }}>
                              {value}
                            </span>
                          )}
                          wrapperStyle={{
                            paddingTop: '20px'
                          }}
                        />
                      </PieChart>
                    </ResponsiveContainer>
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* 보유자산 목록 */}
            <div className="assets-table-section">
              <div className="table-header">
                <h3>보유자산 목록</h3>
                <button className="deposit-btn">+ KRW입금</button>
              </div>

              {loading && (
                <div style={{ textAlign: 'center', padding: '60px 0', color: '#b8bfd8' }}>
                  <div className="spinner" style={{ margin: '0 auto 20px' }}></div>
                  로딩 중...
                </div>
              )}

              {error && !loading && (
                <div style={{ textAlign: 'center', padding: '60px 20px', color: '#ef4444' }}>
                  {error}
                  <div style={{ marginTop: '20px' }}>
                    <button
                      onClick={loadAccountSummary}
                      style={{
                        padding: '10px 20px',
                        background: '#00d4ff',
                        color: '#0f1429',
                        border: 'none',
                        borderRadius: '8px',
                        cursor: 'pointer',
                        fontWeight: 'bold'
                      }}
                    >
                      다시 시도
                    </button>
                  </div>
                </div>
              )}

              {!loading && !error && holdings.length === 0 && (
                <div style={{ textAlign: 'center', padding: '60px 20px', color: '#b8bfd8' }}>
                  보유 중인 암호화폐가 없습니다
                </div>
              )}

              {!loading && !error && holdings.length > 0 && (
                <div className="assets-table">
                  <table>
                    <thead>
                      <tr>
                        <th>보유자산</th>
                        <th>보유수량</th>
                        <th>매수평균가 ▼</th>
                        <th>매수금액 ▼</th>
                        <th>평가금액 ▼</th>
                        <th>평가손익(%) ▼</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {holdings.map(holding => (
                        <tr key={holding.id}>
                          <td>
                            <div className="coin-info">
                              <span className="coin-icon">{holding.icon}</span>
                              <div>
                                <div className="coin-name">{holding.name}</div>
                                <div className="coin-symbol">{holding.market}</div>
                              </div>
                            </div>
                          </td>
                          <td>
                            {holding.quantity}
                            <div className="sub-text">{holding.symbol}</div>
                          </td>
                          <td>
                            {holding.avgPrice} <span className="unit-small">KRW</span>
                            <div className="sub-text">수량</div>
                          </td>
                          <td>
                            {holding.buyAmount} <span className="unit-small">KRW</span>
                          </td>
                          <td>
                            {holding.evalAmount} <span className="unit-small">KRW</span>
                          </td>
                          <td>
                            <div className={parseFloat(holding.profitRate) < 0 ? 'negative' : 'positive'}>
                              {parseFloat(holding.profitRate) > 0 ? '+' : ''}{holding.profitRate} %
                            </div>
                            <div className={`sub-text ${holding.profitAmount < 0 ? 'negative' : 'positive'}`}>
                              {holding.profitAmount.toLocaleString()} KRW
                            </div>
                          </td>
                          <td>
                            <button className="order-btn">주문 ▼</button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}

        {/* 다른 탭들 (임시 메시지) */}
        {activeTab !== 'assets' && (
          <div className="tab-content">
            <div className="empty-state">
              <p>{tabs.find(t => t.id === activeTab)?.label} 페이지는 준비 중입니다.</p>
            </div>
          </div>
        )}
      </div>
    </MainLayout>
  );
};

export default DashboardPage;
