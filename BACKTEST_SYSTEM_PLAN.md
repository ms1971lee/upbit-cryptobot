# 백테스트 시스템 구현 계획

## 목차
1. [시스템 개요](#1-시스템-개요)
2. [기술 아키텍처](#2-기술-아키텍처)
3. [데이터베이스 설계](#3-데이터베이스-설계)
4. [백엔드 API 설계](#4-백엔드-api-설계)
5. [백테스트 엔진 설계](#5-백테스트-엔진-설계)
6. [트레이딩 전략 시스템](#6-트레이딩-전략-시스템)
7. [성과 지표](#7-성과-지표)
8. [프론트엔드 UI 설계](#8-프론트엔드-ui-설계)
9. [구현 순서](#9-구현-순서)
10. [기술적 고려사항](#10-기술적-고려사항)

---

## 1. 시스템 개요

### 1.1 백테스트란?
과거의 시장 데이터를 사용하여 트레이딩 전략의 성과를 시뮬레이션하는 시스템입니다. 실제 자금을 투입하기 전에 전략의 유효성을 검증할 수 있습니다.

### 1.2 주요 기능
- **과거 데이터 수집**: 업비트 API를 통한 과거 캔들 데이터 수집 및 저장
- **전략 정의**: 다양한 트레이딩 전략을 코드로 정의
- **백테스트 실행**: 과거 데이터로 전략 시뮬레이션
- **성과 분석**: 수익률, MDD, 샤프 비율 등 다양한 지표 계산
- **결과 시각화**: 차트와 리포트로 결과 표시
- **전략 비교**: 여러 전략의 성과 비교

### 1.3 사용 시나리오
1. 사용자가 백테스트 설정 (기간, 초기 자금, 전략 선택)
2. 시스템이 과거 데이터로 전략 실행
3. 매수/매도 신호에 따라 가상 거래 수행
4. 성과 지표 계산 및 결과 표시
5. 결과 분석 후 전략 조정 또는 실전 적용

---

## 2. 기술 아키텍처

### 2.1 시스템 구조
```
┌─────────────────────────────────────────────────────────────┐
│                      Frontend (React)                        │
│  - Backtest Configuration UI                                 │
│  - Strategy Selection                                        │
│  - Results Visualization (Charts, Tables)                    │
│  - Performance Metrics Dashboard                             │
└──────────────────┬──────────────────────────────────────────┘
                   │ REST API
┌──────────────────┴──────────────────────────────────────────┐
│                   Backend (Spring Boot)                      │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           Backtest Engine (Core)                     │  │
│  │  - Historical Data Loader                            │  │
│  │  - Strategy Executor                                 │  │
│  │  - Order Simulator                                   │  │
│  │  - Portfolio Manager                                 │  │
│  │  - Performance Calculator                            │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           Strategy Framework                         │  │
│  │  - Strategy Interface                                │  │
│  │  - Built-in Strategies (MA, RSI, Bollinger, etc)    │  │
│  │  - Custom Strategy Support                           │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           Data Management                            │  │
│  │  - Market Data Service                               │  │
│  │  - Upbit API Integration                             │  │
│  │  - Data Cache/Storage                                │  │
│  └──────────────────────────────────────────────────────┘  │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────┴──────────────────────────────────────────┐
│                   Database (H2/PostgreSQL)                   │
│  - Historical Market Data (Candles)                          │
│  - Backtest Configurations                                   │
│  - Backtest Results                                          │
│  - Trading Strategies                                        │
└──────────────────────────────────────────────────────────────┘
```

### 2.2 기술 스택
- **Backend**: Spring Boot 3.5.9, Java 17
- **Frontend**: React 18.2.0
- **Database**: H2 (개발), PostgreSQL (운영 권장)
- **Data Processing**: Java Streams, CompletableFuture
- **Charting**: Recharts, ApexCharts (프론트엔드)
- **API Integration**: WebClient (Upbit API)

---

## 3. 데이터베이스 설계

### 3.1 market_candles (시장 캔들 데이터)
과거 시세 데이터를 저장합니다.

```sql
CREATE TABLE market_candles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    market VARCHAR(20) NOT NULL,              -- KRW-BTC, KRW-ETH
    timeframe VARCHAR(10) NOT NULL,           -- 1m, 5m, 15m, 1h, 1d
    timestamp TIMESTAMP NOT NULL,             -- 캔들 시작 시간
    opening_price DECIMAL(20, 8) NOT NULL,    -- 시가
    high_price DECIMAL(20, 8) NOT NULL,       -- 고가
    low_price DECIMAL(20, 8) NOT NULL,        -- 저가
    closing_price DECIMAL(20, 8) NOT NULL,    -- 종가
    volume DECIMAL(20, 8) NOT NULL,           -- 거래량
    acc_trade_price DECIMAL(20, 8),           -- 누적 거래대금
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE INDEX idx_market_timeframe_timestamp (market, timeframe, timestamp),
    INDEX idx_market_timestamp (market, timestamp)
);
```

**설명**:
- 업비트 API로 수집한 과거 캔들 데이터
- timeframe: 1분봉, 5분봉, 1시간봉, 일봉 등
- 중복 방지를 위한 unique index

### 3.2 backtest_configs (백테스트 설정)
백테스트 실행 시 사용한 설정을 저장합니다.

```sql
CREATE TABLE backtest_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,               -- 백테스트 이름
    market VARCHAR(20) NOT NULL,              -- KRW-BTC
    timeframe VARCHAR(10) NOT NULL,           -- 1h, 1d
    start_date DATE NOT NULL,                 -- 백테스트 시작일
    end_date DATE NOT NULL,                   -- 백테스트 종료일
    initial_capital DECIMAL(15, 2) NOT NULL,  -- 초기 자금
    strategy_name VARCHAR(50) NOT NULL,       -- 전략 이름
    strategy_params JSON,                     -- 전략 파라미터 (JSON)
    commission_rate DECIMAL(5, 4),            -- 수수료율 (0.0005 = 0.05%)
    slippage_rate DECIMAL(5, 4),              -- 슬리피지율
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_id (user_id)
);
```

**strategy_params 예시**:
```json
{
  "shortPeriod": 5,
  "longPeriod": 20,
  "rsiPeriod": 14,
  "rsiOverbought": 70,
  "rsiOversold": 30
}
```

### 3.3 backtest_results (백테스트 결과)
백테스트 실행 결과를 저장합니다.

```sql
CREATE TABLE backtest_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_id BIGINT NOT NULL,                -- 백테스트 설정 ID
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,              -- RUNNING, COMPLETED, FAILED

    -- 성과 지표
    total_return DECIMAL(10, 4),              -- 총 수익률 (%)
    annual_return DECIMAL(10, 4),             -- 연간 수익률 (%)
    max_drawdown DECIMAL(10, 4),              -- 최대 낙폭 (%)
    sharpe_ratio DECIMAL(10, 4),              -- 샤프 비율
    win_rate DECIMAL(5, 2),                   -- 승률 (%)

    -- 거래 통계
    total_trades INT,                         -- 총 거래 횟수
    winning_trades INT,                       -- 수익 거래 횟수
    losing_trades INT,                        -- 손실 거래 횟수
    avg_profit DECIMAL(10, 4),                -- 평균 수익 (%)
    avg_loss DECIMAL(10, 4),                  -- 평균 손실 (%)

    -- 자금 정보
    final_capital DECIMAL(15, 2),             -- 최종 자금
    peak_capital DECIMAL(15, 2),              -- 최고 자금

    -- 실행 정보
    execution_time_ms BIGINT,                 -- 실행 시간 (ms)
    error_message TEXT,                       -- 에러 메시지
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,

    FOREIGN KEY (config_id) REFERENCES backtest_configs(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_config_id (config_id),
    INDEX idx_user_id (user_id)
);
```

### 3.4 backtest_trades (백테스트 거래 내역)
백테스트 중 발생한 모든 거래를 저장합니다.

```sql
CREATE TABLE backtest_trades (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    result_id BIGINT NOT NULL,                -- 백테스트 결과 ID
    market VARCHAR(20) NOT NULL,
    order_type VARCHAR(10) NOT NULL,          -- BUY, SELL
    price DECIMAL(20, 8) NOT NULL,            -- 체결 가격
    volume DECIMAL(20, 8) NOT NULL,           -- 체결 수량
    total_amount DECIMAL(15, 2) NOT NULL,     -- 총 금액
    commission DECIMAL(15, 2),                -- 수수료
    timestamp TIMESTAMP NOT NULL,             -- 거래 시간
    reason VARCHAR(100),                      -- 거래 이유 (신호)

    -- 성과 추적
    balance_before DECIMAL(15, 2),            -- 거래 전 잔고
    balance_after DECIMAL(15, 2),             -- 거래 후 잔고
    portfolio_value DECIMAL(15, 2),           -- 포트폴리오 가치
    cumulative_return DECIMAL(10, 4),         -- 누적 수익률

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (result_id) REFERENCES backtest_results(id),
    INDEX idx_result_id (result_id),
    INDEX idx_timestamp (timestamp)
);
```

### 3.5 backtest_equity_curves (자산 곡선 데이터)
시간에 따른 포트폴리오 가치 변화를 저장합니다 (차트용).

```sql
CREATE TABLE backtest_equity_curves (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    result_id BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,             -- 시점
    portfolio_value DECIMAL(15, 2) NOT NULL,  -- 포트폴리오 가치
    cash DECIMAL(15, 2) NOT NULL,             -- 현금
    coin_value DECIMAL(15, 2) NOT NULL,       -- 코인 평가액
    cumulative_return DECIMAL(10, 4),         -- 누적 수익률
    drawdown DECIMAL(10, 4),                  -- 낙폭

    FOREIGN KEY (result_id) REFERENCES backtest_results(id),
    INDEX idx_result_id_timestamp (result_id, timestamp)
);
```

### 3.6 trading_strategies (저장된 전략)
사용자가 생성한 커스텀 전략을 저장합니다.

```sql
CREATE TABLE trading_strategies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    strategy_type VARCHAR(50) NOT NULL,       -- MA_CROSS, RSI, BOLLINGER, CUSTOM
    parameters JSON NOT NULL,                 -- 전략 파라미터
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_id (user_id)
);
```

---

## 4. 백엔드 API 설계

### 4.1 Market Data API (시장 데이터 관리)

#### 1) POST /api/backtest/data/sync
과거 캔들 데이터를 업비트에서 수집하여 저장합니다.

**Request**:
```json
{
  "market": "KRW-BTC",
  "timeframe": "1h",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31"
}
```

**Response**:
```json
{
  "success": true,
  "message": "데이터 동기화 시작",
  "taskId": "sync-task-12345",
  "estimatedRecords": 8760
}
```

#### 2) GET /api/backtest/data/status/{taskId}
데이터 동기화 진행 상태를 조회합니다.

**Response**:
```json
{
  "taskId": "sync-task-12345",
  "status": "IN_PROGRESS",
  "progress": 65.5,
  "recordsProcessed": 5740,
  "totalRecords": 8760,
  "message": "데이터 수집 중..."
}
```

#### 3) GET /api/backtest/data/available
사용 가능한 시장 데이터 목록을 조회합니다.

**Response**:
```json
{
  "success": true,
  "markets": [
    {
      "market": "KRW-BTC",
      "timeframe": "1h",
      "startDate": "2024-01-01",
      "endDate": "2024-12-31",
      "recordCount": 8760
    },
    {
      "market": "KRW-ETH",
      "timeframe": "1d",
      "startDate": "2024-01-01",
      "endDate": "2024-12-31",
      "recordCount": 365
    }
  ]
}
```

### 4.2 Strategy API (전략 관리)

#### 1) GET /api/backtest/strategies
사용 가능한 전략 목록을 조회합니다.

**Response**:
```json
{
  "success": true,
  "strategies": [
    {
      "name": "MA_CROSS",
      "displayName": "이동평균 교차 전략",
      "description": "단기/장기 이동평균선 교차로 매매",
      "parameters": [
        {
          "name": "shortPeriod",
          "type": "number",
          "default": 5,
          "min": 2,
          "max": 50,
          "description": "단기 이동평균 기간"
        },
        {
          "name": "longPeriod",
          "type": "number",
          "default": 20,
          "min": 10,
          "max": 200,
          "description": "장기 이동평균 기간"
        }
      ]
    },
    {
      "name": "RSI",
      "displayName": "RSI 전략",
      "description": "RSI 과매수/과매도 구간에서 매매",
      "parameters": [
        {
          "name": "period",
          "type": "number",
          "default": 14,
          "min": 5,
          "max": 30
        },
        {
          "name": "overbought",
          "type": "number",
          "default": 70,
          "min": 60,
          "max": 90
        },
        {
          "name": "oversold",
          "type": "number",
          "default": 30,
          "min": 10,
          "max": 40
        }
      ]
    }
  ]
}
```

#### 2) POST /api/backtest/strategies
새 전략을 생성합니다.

**Request**:
```json
{
  "name": "My MA Strategy",
  "strategyType": "MA_CROSS",
  "parameters": {
    "shortPeriod": 5,
    "longPeriod": 20
  }
}
```

**Response**:
```json
{
  "success": true,
  "strategyId": 15,
  "message": "전략이 저장되었습니다"
}
```

### 4.3 Backtest Execution API (백테스트 실행)

#### 1) POST /api/backtest/run
백테스트를 실행합니다.

**Request**:
```json
{
  "name": "BTC 1시간봉 MA 백테스트",
  "market": "KRW-BTC",
  "timeframe": "1h",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "initialCapital": 10000000,
  "strategyName": "MA_CROSS",
  "strategyParams": {
    "shortPeriod": 5,
    "longPeriod": 20
  },
  "commissionRate": 0.0005,
  "slippageRate": 0.0001
}
```

**Response**:
```json
{
  "success": true,
  "message": "백테스트 시작",
  "backtestId": 42,
  "configId": 15
}
```

#### 2) GET /api/backtest/status/{backtestId}
백테스트 실행 상태를 조회합니다.

**Response**:
```json
{
  "backtestId": 42,
  "status": "RUNNING",
  "progress": 45.2,
  "currentDate": "2024-06-15",
  "message": "백테스트 실행 중..."
}
```

#### 3) GET /api/backtest/results/{backtestId}
백테스트 결과를 조회합니다.

**Response**:
```json
{
  "success": true,
  "result": {
    "id": 42,
    "name": "BTC 1시간봉 MA 백테스트",
    "status": "COMPLETED",
    "config": {
      "market": "KRW-BTC",
      "timeframe": "1h",
      "period": "2024-01-01 ~ 2024-12-31",
      "initialCapital": 10000000,
      "strategy": "MA_CROSS (5/20)"
    },
    "performance": {
      "totalReturn": 45.2,
      "annualReturn": 45.2,
      "maxDrawdown": -15.3,
      "sharpeRatio": 1.85,
      "winRate": 62.5,
      "finalCapital": 14520000,
      "peakCapital": 15200000
    },
    "trades": {
      "total": 120,
      "winning": 75,
      "losing": 45,
      "avgProfit": 8.5,
      "avgLoss": -4.2
    },
    "executionTime": 3250,
    "completedAt": "2025-01-05T14:30:00"
  }
}
```

#### 4) GET /api/backtest/results/{backtestId}/trades
백테스트 거래 내역을 조회합니다.

**Query Parameters**:
- page: 페이지 번호 (default: 0)
- size: 페이지 크기 (default: 50)

**Response**:
```json
{
  "success": true,
  "trades": [
    {
      "id": 1,
      "timestamp": "2024-01-05T09:00:00",
      "orderType": "BUY",
      "price": 55000000,
      "volume": 0.1,
      "totalAmount": 5500000,
      "commission": 2750,
      "reason": "골든크로스 발생",
      "portfolioValue": 10000000,
      "cumulativeReturn": 0.0
    },
    {
      "id": 2,
      "timestamp": "2024-01-12T14:00:00",
      "orderType": "SELL",
      "price": 58000000,
      "volume": 0.1,
      "totalAmount": 5800000,
      "commission": 2900,
      "reason": "데드크로스 발생",
      "portfolioValue": 10297100,
      "cumulativeReturn": 2.97
    }
  ],
  "pagination": {
    "page": 0,
    "size": 50,
    "totalElements": 120,
    "totalPages": 3
  }
}
```

#### 5) GET /api/backtest/results/{backtestId}/equity-curve
자산 곡선 데이터를 조회합니다 (차트용).

**Response**:
```json
{
  "success": true,
  "equityCurve": [
    {
      "timestamp": "2024-01-01T00:00:00",
      "portfolioValue": 10000000,
      "cash": 10000000,
      "coinValue": 0,
      "cumulativeReturn": 0.0,
      "drawdown": 0.0
    },
    {
      "timestamp": "2024-01-05T09:00:00",
      "portfolioValue": 9997250,
      "cash": 4497250,
      "coinValue": 5500000,
      "cumulativeReturn": -0.03,
      "drawdown": -0.03
    },
    {
      "timestamp": "2024-01-12T14:00:00",
      "portfolioValue": 10297100,
      "cash": 10297100,
      "coinValue": 0,
      "cumulativeReturn": 2.97,
      "drawdown": 0.0
    }
  ]
}
```

#### 6) GET /api/backtest/history
사용자의 백테스트 이력을 조회합니다.

**Query Parameters**:
- page: 페이지 번호
- size: 페이지 크기
- sort: 정렬 기준 (createdAt, totalReturn)

**Response**:
```json
{
  "success": true,
  "backtests": [
    {
      "id": 42,
      "name": "BTC 1시간봉 MA 백테스트",
      "market": "KRW-BTC",
      "strategy": "MA_CROSS",
      "period": "2024-01-01 ~ 2024-12-31",
      "totalReturn": 45.2,
      "maxDrawdown": -15.3,
      "status": "COMPLETED",
      "createdAt": "2025-01-05T14:00:00"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1
  }
}
```

#### 7) DELETE /api/backtest/results/{backtestId}
백테스트 결과를 삭제합니다.

**Response**:
```json
{
  "success": true,
  "message": "백테스트 결과가 삭제되었습니다"
}
```

### 4.4 Comparison API (전략 비교)

#### POST /api/backtest/compare
여러 백테스트 결과를 비교합니다.

**Request**:
```json
{
  "backtestIds": [42, 43, 44]
}
```

**Response**:
```json
{
  "success": true,
  "comparison": [
    {
      "id": 42,
      "name": "MA 5/20",
      "totalReturn": 45.2,
      "maxDrawdown": -15.3,
      "sharpeRatio": 1.85,
      "winRate": 62.5
    },
    {
      "id": 43,
      "name": "MA 10/30",
      "totalReturn": 38.5,
      "maxDrawdown": -12.1,
      "sharpeRatio": 1.92,
      "winRate": 65.0
    },
    {
      "id": 44,
      "name": "RSI 14",
      "totalReturn": 52.3,
      "maxDrawdown": -18.5,
      "sharpeRatio": 1.75,
      "winRate": 58.5
    }
  ]
}
```

---

## 5. 백테스트 엔진 설계

### 5.1 핵심 컴포넌트

#### BacktestEngine.java
백테스트 실행의 메인 엔진입니다.

```java
@Service
@Slf4j
public class BacktestEngine {

    /**
     * 백테스트 실행 메인 메서드
     */
    public BacktestResult run(BacktestConfig config) {
        // 1. 초기화
        BacktestContext context = initializeContext(config);

        // 2. 과거 데이터 로드
        List<Candle> candles = loadHistoricalData(config);

        // 3. 전략 인스턴스 생성
        TradingStrategy strategy = strategyFactory.createStrategy(
            config.getStrategyName(),
            config.getStrategyParams()
        );

        // 4. 캔들 데이터를 순회하며 전략 실행
        for (int i = 0; i < candles.size(); i++) {
            Candle currentCandle = candles.get(i);

            // 전략에 충분한 데이터가 모였는지 확인
            if (i < strategy.getMinimumDataPoints()) {
                continue;
            }

            // 현재까지의 캔들 데이터로 전략 실행
            List<Candle> historicalData = candles.subList(0, i + 1);
            TradeSignal signal = strategy.analyze(historicalData, context);

            // 신호에 따라 주문 실행
            if (signal != null && signal.getAction() != Action.HOLD) {
                executeOrder(signal, currentCandle, context);
            }

            // 포트폴리오 가치 갱신
            updatePortfolio(currentCandle, context);

            // 자산 곡선 기록
            recordEquityCurve(currentCandle.getTimestamp(), context);
        }

        // 5. 성과 지표 계산
        PerformanceMetrics metrics = calculatePerformance(context);

        // 6. 결과 저장 및 반환
        return saveResults(config, context, metrics);
    }
}
```

#### BacktestContext.java
백테스트 실행 중 상태를 관리합니다.

```java
@Data
public class BacktestContext {
    // 설정
    private BacktestConfig config;

    // 포트폴리오 상태
    private Double cash;                    // 현금
    private Double coinBalance;             // 코인 보유량
    private Double avgBuyPrice;             // 평균 매수가

    // 거래 내역
    private List<Trade> trades;

    // 자산 곡선
    private List<EquityPoint> equityCurve;

    // 통계
    private Double peakCapital;             // 최고 자산
    private Double currentDrawdown;         // 현재 낙폭
    private Double maxDrawdown;             // 최대 낙폭

    public Double getPortfolioValue(Double currentPrice) {
        return cash + (coinBalance * currentPrice);
    }

    public void updatePeak(Double currentValue) {
        if (currentValue > peakCapital) {
            peakCapital = currentValue;
        }
    }

    public void updateDrawdown(Double currentValue) {
        currentDrawdown = (currentValue - peakCapital) / peakCapital * 100;
        if (currentDrawdown < maxDrawdown) {
            maxDrawdown = currentDrawdown;
        }
    }
}
```

#### OrderExecutor.java
주문 실행을 시뮬레이션합니다.

```java
@Component
public class OrderExecutor {

    public void executeBuy(Candle candle, BacktestContext context, Double amount) {
        Double price = candle.getClosingPrice();
        Double commission = amount * context.getConfig().getCommissionRate();
        Double slippage = amount * context.getConfig().getSlippageRate();
        Double totalCost = amount + commission + slippage;

        // 자금 확인
        if (context.getCash() < totalCost) {
            log.warn("Insufficient cash for buy order");
            return;
        }

        // 매수 실행
        Double volume = amount / price;
        context.setCash(context.getCash() - totalCost);

        // 평균 매수가 갱신
        Double totalValue = (context.getCoinBalance() * context.getAvgBuyPrice()) + amount;
        context.setCoinBalance(context.getCoinBalance() + volume);
        context.setAvgBuyPrice(totalValue / context.getCoinBalance());

        // 거래 기록
        Trade trade = Trade.builder()
            .timestamp(candle.getTimestamp())
            .orderType(OrderType.BUY)
            .price(price)
            .volume(volume)
            .totalAmount(amount)
            .commission(commission + slippage)
            .portfolioValue(context.getPortfolioValue(price))
            .build();
        context.getTrades().add(trade);
    }

    public void executeSell(Candle candle, BacktestContext context, Double volume) {
        // 매도 로직...
    }
}
```

#### PerformanceCalculator.java
성과 지표를 계산합니다.

```java
@Component
public class PerformanceCalculator {

    public PerformanceMetrics calculate(BacktestContext context) {
        PerformanceMetrics metrics = new PerformanceMetrics();

        // 총 수익률
        Double initialCapital = context.getConfig().getInitialCapital();
        Double finalCapital = context.getEquityCurve().get(
            context.getEquityCurve().size() - 1
        ).getPortfolioValue();
        metrics.setTotalReturn((finalCapital - initialCapital) / initialCapital * 100);

        // 연간 수익률
        long days = ChronoUnit.DAYS.between(
            context.getConfig().getStartDate(),
            context.getConfig().getEndDate()
        );
        Double years = days / 365.0;
        metrics.setAnnualReturn(
            Math.pow(finalCapital / initialCapital, 1.0 / years) - 1.0) * 100
        );

        // 최대 낙폭
        metrics.setMaxDrawdown(context.getMaxDrawdown());

        // 샤프 비율
        metrics.setSharpeRatio(calculateSharpeRatio(context));

        // 승률
        long winningTrades = context.getTrades().stream()
            .filter(t -> t.getProfit() > 0)
            .count();
        metrics.setWinRate((double) winningTrades / context.getTrades().size() * 100);

        // 거래 통계
        metrics.setTotalTrades(context.getTrades().size());
        metrics.setWinningTrades((int) winningTrades);
        metrics.setLosingTrades(context.getTrades().size() - (int) winningTrades);

        return metrics;
    }

    private Double calculateSharpeRatio(BacktestContext context) {
        // 일별 수익률 계산
        List<Double> dailyReturns = new ArrayList<>();
        for (int i = 1; i < context.getEquityCurve().size(); i++) {
            Double prevValue = context.getEquityCurve().get(i - 1).getPortfolioValue();
            Double currValue = context.getEquityCurve().get(i).getPortfolioValue();
            dailyReturns.add((currValue - prevValue) / prevValue);
        }

        // 평균 및 표준편차
        Double avgReturn = dailyReturns.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        Double variance = dailyReturns.stream()
            .mapToDouble(r -> Math.pow(r - avgReturn, 2))
            .average()
            .orElse(0.0);

        Double stdDev = Math.sqrt(variance);

        // 샤프 비율 (무위험 수익률 0 가정)
        return stdDev > 0 ? (avgReturn / stdDev) * Math.sqrt(252) : 0.0;
    }
}
```

### 5.2 데이터 로더

#### MarketDataLoader.java
과거 캔들 데이터를 효율적으로 로드합니다.

```java
@Service
public class MarketDataLoader {

    private final MarketCandleRepository candleRepository;

    public List<Candle> loadData(String market, String timeframe,
                                  LocalDate startDate, LocalDate endDate) {
        // 데이터베이스에서 캔들 데이터 조회
        List<MarketCandle> entities = candleRepository.findByMarketAndTimeframeAndTimestampBetween(
            market, timeframe,
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59)
        );

        // Entity -> DTO 변환
        return entities.stream()
            .map(this::toCandle)
            .collect(Collectors.toList());
    }

    /**
     * 데이터가 없으면 업비트 API에서 가져오기
     */
    @Async
    public CompletableFuture<Void> syncData(String market, String timeframe,
                                             LocalDate startDate, LocalDate endDate) {
        // 업비트 API로 데이터 수집
        // 페이징 처리하며 배치로 저장
        // ...
        return CompletableFuture.completedFuture(null);
    }
}
```

---

## 6. 트레이딩 전략 시스템

### 6.1 전략 인터페이스

#### TradingStrategy.java
모든 전략이 구현해야 하는 인터페이스입니다.

```java
public interface TradingStrategy {

    /**
     * 전략 분석 및 신호 생성
     * @param candles 현재까지의 캔들 데이터
     * @param context 백테스트 컨텍스트
     * @return 매매 신호
     */
    TradeSignal analyze(List<Candle> candles, BacktestContext context);

    /**
     * 전략 실행에 필요한 최소 데이터 개수
     */
    int getMinimumDataPoints();

    /**
     * 전략 이름
     */
    String getName();

    /**
     * 전략 설명
     */
    String getDescription();
}
```

#### TradeSignal.java
전략이 생성하는 매매 신호입니다.

```java
@Data
@Builder
public class TradeSignal {
    private Action action;          // BUY, SELL, HOLD
    private Double confidence;      // 신뢰도 (0.0 ~ 1.0)
    private String reason;          // 신호 발생 이유
    private Double suggestedAmount; // 권장 거래 금액/수량

    public enum Action {
        BUY, SELL, HOLD
    }
}
```

### 6.2 내장 전략 구현

#### MovingAverageCrossStrategy.java
이동평균 교차 전략입니다.

```java
@Component
public class MovingAverageCrossStrategy implements TradingStrategy {

    private int shortPeriod = 5;
    private int longPeriod = 20;

    @Override
    public TradeSignal analyze(List<Candle> candles, BacktestContext context) {
        // 단기/장기 이동평균 계산
        Double shortMA = calculateMA(candles, shortPeriod);
        Double longMA = calculateMA(candles, longPeriod);

        // 이전 캔들의 MA
        Double prevShortMA = calculateMA(
            candles.subList(0, candles.size() - 1),
            shortPeriod
        );
        Double prevLongMA = calculateMA(
            candles.subList(0, candles.size() - 1),
            longPeriod
        );

        // 골든크로스 (매수 신호)
        if (prevShortMA <= prevLongMA && shortMA > longMA) {
            if (context.getCoinBalance() == 0) {  // 포지션이 없을 때만
                return TradeSignal.builder()
                    .action(Action.BUY)
                    .confidence(0.8)
                    .reason("골든크로스 발생")
                    .suggestedAmount(context.getCash() * 0.95)  // 현금의 95% 투자
                    .build();
            }
        }

        // 데드크로스 (매도 신호)
        if (prevShortMA >= prevLongMA && shortMA < longMA) {
            if (context.getCoinBalance() > 0) {  // 포지션이 있을 때만
                return TradeSignal.builder()
                    .action(Action.SELL)
                    .confidence(0.8)
                    .reason("데드크로스 발생")
                    .suggestedAmount(context.getCoinBalance())  // 전량 매도
                    .build();
            }
        }

        return TradeSignal.builder()
            .action(Action.HOLD)
            .build();
    }

    private Double calculateMA(List<Candle> candles, int period) {
        if (candles.size() < period) {
            return 0.0;
        }

        return candles.subList(candles.size() - period, candles.size())
            .stream()
            .mapToDouble(Candle::getClosingPrice)
            .average()
            .orElse(0.0);
    }

    @Override
    public int getMinimumDataPoints() {
        return longPeriod + 1;  // 장기 MA + 이전 값 비교
    }

    @Override
    public String getName() {
        return "MA_CROSS";
    }

    @Override
    public String getDescription() {
        return String.format("이동평균 교차 전략 (단기: %d, 장기: %d)",
            shortPeriod, longPeriod);
    }
}
```

#### RSIStrategy.java
RSI 과매수/과매도 전략입니다.

```java
@Component
public class RSIStrategy implements TradingStrategy {

    private int period = 14;
    private double overbought = 70.0;
    private double oversold = 30.0;

    @Override
    public TradeSignal analyze(List<Candle> candles, BacktestContext context) {
        Double rsi = calculateRSI(candles, period);

        // 과매도 구간 (매수 신호)
        if (rsi < oversold && context.getCoinBalance() == 0) {
            return TradeSignal.builder()
                .action(Action.BUY)
                .confidence(0.7)
                .reason(String.format("RSI 과매도 (%.2f)", rsi))
                .suggestedAmount(context.getCash() * 0.95)
                .build();
        }

        // 과매수 구간 (매도 신호)
        if (rsi > overbought && context.getCoinBalance() > 0) {
            return TradeSignal.builder()
                .action(Action.SELL)
                .confidence(0.7)
                .reason(String.format("RSI 과매수 (%.2f)", rsi))
                .suggestedAmount(context.getCoinBalance())
                .build();
        }

        return TradeSignal.builder()
            .action(Action.HOLD)
            .build();
    }

    private Double calculateRSI(List<Candle> candles, int period) {
        if (candles.size() < period + 1) {
            return 50.0;  // 중립
        }

        // 가격 변화 계산
        List<Double> gains = new ArrayList<>();
        List<Double> losses = new ArrayList<>();

        for (int i = candles.size() - period; i < candles.size(); i++) {
            Double change = candles.get(i).getClosingPrice() -
                           candles.get(i - 1).getClosingPrice();
            if (change > 0) {
                gains.add(change);
                losses.add(0.0);
            } else {
                gains.add(0.0);
                losses.add(Math.abs(change));
            }
        }

        // 평균 상승/하락
        Double avgGain = gains.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        Double avgLoss = losses.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        if (avgLoss == 0) {
            return 100.0;
        }

        Double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    @Override
    public int getMinimumDataPoints() {
        return period + 1;
    }

    @Override
    public String getName() {
        return "RSI";
    }

    @Override
    public String getDescription() {
        return String.format("RSI 전략 (기간: %d, 과매수: %.0f, 과매도: %.0f)",
            period, overbought, oversold);
    }
}
```

#### BollingerBandStrategy.java
볼린저 밴드 전략입니다.

```java
@Component
public class BollingerBandStrategy implements TradingStrategy {

    private int period = 20;
    private double stdDevMultiplier = 2.0;

    @Override
    public TradeSignal analyze(List<Candle> candles, BacktestContext context) {
        BollingerBands bands = calculateBollingerBands(candles, period, stdDevMultiplier);
        Double currentPrice = candles.get(candles.size() - 1).getClosingPrice();

        // 하단 밴드 돌파 (매수 신호)
        if (currentPrice < bands.getLowerBand() && context.getCoinBalance() == 0) {
            return TradeSignal.builder()
                .action(Action.BUY)
                .confidence(0.75)
                .reason("볼린저 밴드 하단 돌파")
                .suggestedAmount(context.getCash() * 0.95)
                .build();
        }

        // 상단 밴드 돌파 (매도 신호)
        if (currentPrice > bands.getUpperBand() && context.getCoinBalance() > 0) {
            return TradeSignal.builder()
                .action(Action.SELL)
                .confidence(0.75)
                .reason("볼린저 밴드 상단 돌파")
                .suggestedAmount(context.getCoinBalance())
                .build();
        }

        return TradeSignal.builder()
            .action(Action.HOLD)
            .build();
    }

    @Data
    static class BollingerBands {
        private Double middleBand;
        private Double upperBand;
        private Double lowerBand;
    }

    private BollingerBands calculateBollingerBands(List<Candle> candles,
                                                    int period,
                                                    double stdDevMultiplier) {
        // 이동평균 (중간 밴드)
        Double ma = calculateMA(candles, period);

        // 표준편차
        Double stdDev = calculateStdDev(candles, period, ma);

        BollingerBands bands = new BollingerBands();
        bands.setMiddleBand(ma);
        bands.setUpperBand(ma + (stdDev * stdDevMultiplier));
        bands.setLowerBand(ma - (stdDev * stdDevMultiplier));

        return bands;
    }

    // calculateMA, calculateStdDev 메서드...

    @Override
    public int getMinimumDataPoints() {
        return period;
    }
}
```

### 6.3 전략 팩토리

#### StrategyFactory.java
전략 인스턴스를 생성합니다.

```java
@Component
public class StrategyFactory {

    @Autowired
    private ApplicationContext applicationContext;

    public TradingStrategy createStrategy(String strategyName,
                                         Map<String, Object> params) {
        TradingStrategy strategy;

        switch (strategyName) {
            case "MA_CROSS":
                MovingAverageCrossStrategy maStrategy =
                    applicationContext.getBean(MovingAverageCrossStrategy.class);
                if (params.containsKey("shortPeriod")) {
                    maStrategy.setShortPeriod((Integer) params.get("shortPeriod"));
                }
                if (params.containsKey("longPeriod")) {
                    maStrategy.setLongPeriod((Integer) params.get("longPeriod"));
                }
                strategy = maStrategy;
                break;

            case "RSI":
                RSIStrategy rsiStrategy =
                    applicationContext.getBean(RSIStrategy.class);
                if (params.containsKey("period")) {
                    rsiStrategy.setPeriod((Integer) params.get("period"));
                }
                if (params.containsKey("overbought")) {
                    rsiStrategy.setOverbought((Double) params.get("overbought"));
                }
                if (params.containsKey("oversold")) {
                    rsiStrategy.setOversold((Double) params.get("oversold"));
                }
                strategy = rsiStrategy;
                break;

            case "BOLLINGER":
                strategy = applicationContext.getBean(BollingerBandStrategy.class);
                // 파라미터 설정...
                break;

            default:
                throw new IllegalArgumentException("Unknown strategy: " + strategyName);
        }

        return strategy;
    }
}
```

---

## 7. 성과 지표

### 7.1 기본 지표

#### 총 수익률 (Total Return)
```
총 수익률 = (최종 자산 - 초기 자산) / 초기 자산 × 100
```

#### 연간 수익률 (Annual Return)
```
연간 수익률 = ((최종 자산 / 초기 자산)^(1/년수) - 1) × 100
```

#### 최대 낙폭 (Maximum Drawdown, MDD)
```
낙폭 = (현재 자산 - 최고점 자산) / 최고점 자산 × 100
MDD = 기간 중 최소 낙폭값
```

#### 샤프 비율 (Sharpe Ratio)
```
샤프 비율 = (평균 수익률 - 무위험 수익률) / 수익률 표준편차

무위험 수익률 = 0으로 가정
일별 수익률로 계산 후 연간화: × √252
```

#### 승률 (Win Rate)
```
승률 = 수익 거래 횟수 / 전체 거래 횟수 × 100
```

### 7.2 추가 지표

#### 손익비 (Profit Factor)
```
손익비 = 총 수익 거래 금액 / 총 손실 거래 금액
```

#### 최대 연속 손실 (Maximum Consecutive Losses)
가장 긴 연속 손실 거래 횟수

#### 평균 보유 기간
각 포지션의 평균 보유 시간

#### 칼마 비율 (Calmar Ratio)
```
칼마 비율 = 연간 수익률 / |MDD|
```

---

## 8. 프론트엔드 UI 설계

### 8.1 화면 구성

#### 1) Backtest Configuration Page (`/backtest/new`)
새 백테스트를 설정하는 페이지입니다.

**UI 컴포넌트**:
```
┌────────────────────────────────────────────────────────┐
│  백테스트 설정                                           │
├────────────────────────────────────────────────────────┤
│                                                        │
│  백테스트 이름: [____________________________]         │
│                                                        │
│  시장 선택:     [KRW-BTC ▼]                           │
│  타임프레임:    [1시간 ▼]                              │
│                                                        │
│  기간 설정:                                            │
│    시작일: [2024-01-01]  종료일: [2024-12-31]        │
│                                                        │
│  초기 자금:     [10,000,000] KRW                      │
│                                                        │
│  전략 선택:     [이동평균 교차 전략 ▼]                 │
│                                                        │
│  전략 파라미터:                                         │
│    단기 기간:   [5____]                               │
│    장기 기간:   [20___]                               │
│                                                        │
│  고급 설정:                                            │
│    수수료율:    [0.05] %                              │
│    슬리피지:    [0.01] %                              │
│                                                        │
│         [취소]              [백테스트 실행]            │
└────────────────────────────────────────────────────────┘
```

#### 2) Backtest Results Page (`/backtest/results/:id`)
백테스트 결과를 표시하는 페이지입니다.

**UI 컴포넌트**:
```
┌────────────────────────────────────────────────────────┐
│  백테스트 결과: BTC 1시간봉 MA 백테스트                  │
├────────────────────────────────────────────────────────┤
│                                                        │
│  ┌──────────── 성과 요약 ────────────┐                │
│  │ 총 수익률:    +45.2%              │                │
│  │ 연간 수익률:  +45.2%              │                │
│  │ 최대 낙폭:    -15.3%              │                │
│  │ 샤프 비율:    1.85                │                │
│  │ 승률:         62.5%               │                │
│  │                                   │                │
│  │ 최종 자산: 14,520,000 KRW         │                │
│  └───────────────────────────────────┘                │
│                                                        │
│  ┌──────────── 자산 곡선 ────────────┐                │
│  │                                   │                │
│  │   15M ┤         ╱╲                │                │
│  │       │        ╱  ╲    ╱╲         │                │
│  │   12M ┤   ╱╲  ╱    ╲  ╱  ╲        │                │
│  │       │  ╱  ╲╱      ╲╱    ╲       │                │
│  │   10M ┼─────────────────────       │                │
│  │       └─────────────────────       │                │
│  │        Jan   Mar   Jun   Dec       │                │
│  └───────────────────────────────────┘                │
│                                                        │
│  ┌──────────── 거래 내역 ────────────┐                │
│  │ 시간              타입   가격     수익률│            │
│  │ 2024-01-05 09:00  매수  55,000,000   │            │
│  │ 2024-01-12 14:00  매도  58,000,000  +5.5%│        │
│  │ 2024-01-25 11:00  매수  56,000,000   │            │
│  │ ...                                  │            │
│  │                         [더보기...] │            │
│  └───────────────────────────────────┘                │
│                                                        │
│  [전략 수정 후 재실행]  [결과 다운로드]  [삭제]       │
└────────────────────────────────────────────────────────┘
```

#### 3) Backtest History Page (`/backtest/history`)
백테스트 이력을 관리하는 페이지입니다.

**UI 컴포넌트**:
```
┌────────────────────────────────────────────────────────┐
│  백테스트 이력                        [+ 새 백테스트]  │
├────────────────────────────────────────────────────────┤
│                                                        │
│  필터: [전체 ▼] [KRW-BTC ▼]   정렬: [최신순 ▼]       │
│                                                        │
│  ┌────────────────────────────────────────────────┐  │
│  │ BTC 1시간봉 MA 백테스트                         │  │
│  │ KRW-BTC | MA_CROSS | 2024-01-01 ~ 2024-12-31  │  │
│  │ 수익률: +45.2% | MDD: -15.3% | 2025-01-05    │  │
│  │                          [보기] [삭제]         │  │
│  └────────────────────────────────────────────────┘  │
│                                                        │
│  ┌────────────────────────────────────────────────┐  │
│  │ ETH 일봉 RSI 백테스트                           │  │
│  │ KRW-ETH | RSI | 2024-01-01 ~ 2024-12-31       │  │
│  │ 수익률: +38.5% | MDD: -12.1% | 2025-01-04    │  │
│  │                          [보기] [삭제]         │  │
│  └────────────────────────────────────────────────┘  │
│                                                        │
└────────────────────────────────────────────────────────┘
```

#### 4) Strategy Comparison Page (`/backtest/compare`)
여러 전략의 성과를 비교하는 페이지입니다.

**UI 컴포넌트**:
```
┌────────────────────────────────────────────────────────┐
│  전략 비교                                              │
├────────────────────────────────────────────────────────┤
│                                                        │
│  비교할 백테스트 선택:                                  │
│    ☑ MA 5/20 전략                                     │
│    ☑ MA 10/30 전략                                    │
│    ☑ RSI 14 전략                                      │
│    ☐ 볼린저 밴드 전략                                  │
│                                                        │
│  ┌──────────── 성과 비교 ────────────┐                │
│  │ 지표          MA 5/20  MA 10/30  RSI 14│           │
│  │ 총 수익률     45.2%    38.5%     52.3% │           │
│  │ 최대 낙폭    -15.3%   -12.1%    -18.5% │           │
│  │ 샤프 비율     1.85     1.92      1.75  │           │
│  │ 승률         62.5%    65.0%     58.5%  │           │
│  └───────────────────────────────────────┘            │
│                                                        │
│  ┌──────────── 수익률 비교 차트 ──────┐               │
│  │                                     │               │
│  │   60% ┤           ╱RSI              │               │
│  │       │      ╱MA5/20                │               │
│  │   40% ┤    ╱ ╱MA10/30               │               │
│  │       │  ╱  ╱                       │               │
│  │   20% ┤─────                        │               │
│  │       └──────────────               │               │
│  │        Jan  Jun  Dec                │               │
│  └─────────────────────────────────────┘               │
│                                                        │
└────────────────────────────────────────────────────────┘
```

### 8.2 React 컴포넌트 구조

```
src/
├── pages/
│   └── backtest/
│       ├── BacktestConfigPage.jsx      # 백테스트 설정
│       ├── BacktestResultPage.jsx      # 결과 상세
│       ├── BacktestHistoryPage.jsx     # 이력 관리
│       └── BacktestComparePage.jsx     # 전략 비교
│
├── components/
│   └── backtest/
│       ├── ConfigForm.jsx              # 설정 폼
│       ├── StrategySelector.jsx        # 전략 선택
│       ├── PerformanceMetrics.jsx      # 성과 지표 카드
│       ├── EquityCurveChart.jsx        # 자산 곡선 차트
│       ├── TradeList.jsx               # 거래 내역 테이블
│       ├── DrawdownChart.jsx           # 낙폭 차트
│       ├── ComparisonTable.jsx         # 비교 테이블
│       └── ProgressBar.jsx             # 진행 상태 표시
│
├── api/
│   └── backtestApi.js                  # API 클라이언트
│
└── utils/
    └── backtestUtils.js                # 계산 유틸리티
```

### 8.3 주요 컴포넌트 예시

#### BacktestConfigPage.jsx
```javascript
import React, { useState, useEffect } from 'react';
import { backtestApi } from '../../api/backtestApi';
import { useNavigate } from 'react-router-dom';

const BacktestConfigPage = () => {
  const navigate = useNavigate();
  const [config, setConfig] = useState({
    name: '',
    market: 'KRW-BTC',
    timeframe: '1h',
    startDate: '2024-01-01',
    endDate: '2024-12-31',
    initialCapital: 10000000,
    strategyName: 'MA_CROSS',
    strategyParams: {
      shortPeriod: 5,
      longPeriod: 20
    },
    commissionRate: 0.0005,
    slippageRate: 0.0001
  });

  const [strategies, setStrategies] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadStrategies();
  }, []);

  const loadStrategies = async () => {
    const response = await backtestApi.getStrategies();
    setStrategies(response.strategies);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await backtestApi.runBacktest(config);
      navigate(`/backtest/results/${response.backtestId}`);
    } catch (error) {
      alert('백테스트 실행 실패: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="backtest-config-page">
      <h1>백테스트 설정</h1>
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>백테스트 이름</label>
          <input
            type="text"
            value={config.name}
            onChange={(e) => setConfig({...config, name: e.target.value})}
            required
          />
        </div>

        <div className="form-group">
          <label>시장</label>
          <select
            value={config.market}
            onChange={(e) => setConfig({...config, market: e.target.value})}
          >
            <option value="KRW-BTC">KRW-BTC</option>
            <option value="KRW-ETH">KRW-ETH</option>
            {/* ... */}
          </select>
        </div>

        {/* 기타 필드들... */}

        <button type="submit" disabled={loading}>
          {loading ? '실행 중...' : '백테스트 실행'}
        </button>
      </form>
    </div>
  );
};

export default BacktestConfigPage;
```

#### EquityCurveChart.jsx
```javascript
import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend } from 'recharts';

const EquityCurveChart = ({ data }) => {
  const chartData = data.map(point => ({
    date: new Date(point.timestamp).toLocaleDateString(),
    portfolio: point.portfolioValue,
    drawdown: point.drawdown
  }));

  return (
    <div className="equity-curve-chart">
      <h3>자산 곡선</h3>
      <LineChart width={800} height={400} data={chartData}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="date" />
        <YAxis yAxisId="left" />
        <YAxis yAxisId="right" orientation="right" />
        <Tooltip />
        <Legend />
        <Line
          yAxisId="left"
          type="monotone"
          dataKey="portfolio"
          stroke="#8884d8"
          name="포트폴리오 가치"
        />
        <Line
          yAxisId="right"
          type="monotone"
          dataKey="drawdown"
          stroke="#ff0000"
          name="낙폭 (%)"
        />
      </LineChart>
    </div>
  );
};

export default EquityCurveChart;
```

---

## 9. 구현 순서

### Phase 1: 데이터 수집 및 저장 (1주)
1. MarketCandle 엔티티 및 리포지토리 생성
2. MarketDataLoader 서비스 구현
3. 업비트 API 연동 (과거 캔들 데이터 수집)
4. 데이터 동기화 API 구현
5. 프론트엔드: 데이터 관리 UI

**테스트**: 1년치 BTC 1시간봉 데이터 수집

### Phase 2: 백테스트 엔진 핵심 (2주)
1. BacktestConfig, BacktestResult 엔티티 생성
2. BacktestContext 구현
3. BacktestEngine 메인 로직 구현
4. OrderExecutor 구현
5. PerformanceCalculator 구현 (기본 지표)

**테스트**: 단순 Buy & Hold 전략으로 엔진 검증

### Phase 3: 전략 시스템 (1주)
1. TradingStrategy 인터페이스 정의
2. MovingAverageCrossStrategy 구현
3. RSIStrategy 구현
4. StrategyFactory 구현
5. 전략 관리 API 구현

**테스트**: MA 교차 전략으로 실제 백테스트

### Phase 4: 거래 내역 및 자산 곡선 (1주)
1. BacktestTrade, BacktestEquityCurve 엔티티 생성
2. 백테스트 중 상세 데이터 기록
3. 거래 내역 조회 API
4. 자산 곡선 조회 API

**테스트**: 100회 이상 거래 발생하는 백테스트로 검증

### Phase 5: 프론트엔드 UI (2주)
1. 백테스트 설정 페이지 구현
2. 결과 페이지 구현 (성과 지표)
3. 자산 곡선 차트 구현
4. 거래 내역 테이블 구현
5. 백테스트 이력 페이지 구현

**테스트**: 전체 플로우 E2E 테스트

### Phase 6: 고급 기능 (1주)
1. 여러 전략 비교 기능
2. BollingerBandStrategy 추가
3. 추가 성과 지표 (칼마 비율, 손익비 등)
4. 결과 내보내기 (CSV, PDF)
5. 성능 최적화

**테스트**: 10개 백테스트 동시 실행 성능 테스트

---

## 10. 기술적 고려사항

### 10.1 성능 최적화

#### 데이터 캐싱
```java
@Cacheable(value = "marketData", key = "#market + '_' + #timeframe")
public List<Candle> loadData(String market, String timeframe,
                              LocalDate startDate, LocalDate endDate) {
    // ...
}
```

#### 비동기 처리
```java
@Async
public CompletableFuture<BacktestResult> runBacktestAsync(BacktestConfig config) {
    BacktestResult result = backtestEngine.run(config);
    return CompletableFuture.completedFuture(result);
}
```

#### 배치 저장
```java
// 거래 내역과 자산 곡선을 배치로 저장
@Transactional
public void saveResults(BacktestContext context) {
    tradeRepository.saveAllAndFlush(context.getTrades());
    equityCurveRepository.saveAllAndFlush(context.getEquityCurve());
}
```

### 10.2 데이터 관리

#### 데이터 압축
시간이 지난 과거 데이터는 압축하여 저장 공간 절약

#### 데이터 정합성
- 중복 캔들 방지 (unique index)
- 누락된 캔들 감지 및 재수집

### 10.3 확장성

#### 멀티 마켓 지원
여러 시장을 동시에 백테스트할 수 있도록 설계

#### 커스텀 전략 지원
사용자가 코드 없이 전략을 조합할 수 있는 빌더 제공 (향후)

#### 분산 처리
많은 백테스트를 병렬로 실행할 수 있도록 태스크 큐 사용 (향후)

### 10.4 보안

#### 백테스트 결과 권한
- 사용자는 자신의 백테스트만 조회/삭제 가능
- API에서 userId 검증

#### 리소스 제한
- 백테스트 동시 실행 개수 제한
- 데이터 조회 기간 제한 (최대 5년)

---

## 부록: 백테스트 실행 예시

### 시나리오: MA 5/20 전략으로 BTC 백테스트

**설정**:
- 시장: KRW-BTC
- 기간: 2024-01-01 ~ 2024-12-31
- 초기 자금: 10,000,000 KRW
- 전략: MA 교차 (단기 5, 장기 20)
- 타임프레임: 1시간

**실행 과정**:
1. 사용자가 설정 입력
2. 백엔드가 8,760개 캔들 로드 (1년 × 365일 × 24시간)
3. 각 캔들마다 MA 계산
4. 골든크로스/데드크로스 감지
5. 신호 발생 시 매수/매도 시뮬레이션
6. 120회 거래 발생
7. 성과 계산: 총 수익률 +45.2%, MDD -15.3%
8. 결과 저장 및 사용자에게 표시

**예상 소요 시간**: 3~5초

---

## 결론

이 백테스트 시스템은 다음과 같은 가치를 제공합니다:

1. **전략 검증**: 실제 자금 투입 전에 전략의 유효성 확인
2. **리스크 관리**: MDD, 샤프 비율 등으로 리스크 평가
3. **성과 비교**: 여러 전략의 성과를 객관적으로 비교
4. **최적화**: 파라미터를 조정하며 최적의 설정 탐색
5. **교육**: 트레이딩 전략과 시장 동작에 대한 이해 증진

구현 후에는 실제 트레이딩 시스템과 연계하여 백테스트에서 검증된 전략을 실전에 적용할 수 있습니다.
