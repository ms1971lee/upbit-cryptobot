# 백테스트 시스템 플로우차트

## 📋 목차
- [개요](#개요)
- [백테스트 실행 흐름](#백테스트-실행-흐름)
- [전략 종류](#전략-종류)
- [상세 플로우](#상세-플로우)
- [주요 컴포넌트](#주요-컴포넌트)

---

## 개요

업비트 자동매매 프로그램의 백테스트 시스템은 과거 데이터를 기반으로 다양한 매매 전략의 성과를 시뮬레이션합니다.

**현재 버전**: V1 (단일 버전)
- V1, V2, V3 같은 버전 구분은 없습니다
- 대신 4가지 전략을 선택하여 백테스트를 실행합니다

---

## 백테스트 실행 흐름

```
[사용자 요청]
    ↓
[BacktestController]
    ↓
[BacktestService]
    ↓
    ├─ 1. 사용자 조회
    ├─ 2. Config 생성 및 저장
    ├─ 3. BacktestEngine 실행 ────┐
    ├─ 4. 결과 저장                │
    └─ 5. 거래 내역 저장           │
                                   │
                    ┌──────────────┘
                    ↓
            [BacktestEngine]
                    ↓
            ┌───────┴───────┐
            ↓               ↓
    [데이터 로드]    [전략 생성]
            ↓               ↓
    [MarketCandle]  [StrategyFactory]
            └───────┬───────┘
                    ↓
            [전략 실행 루프]
                    ↓
            ┌───────┴───────┐
            ↓               ↓
    [신호 생성]      [주문 실행]
    (Strategy)     (OrderExecutor)
            └───────┬───────┘
                    ↓
            [성과 계산]
        (PerformanceCalculator)
                    ↓
            [BacktestResult]
```

---

## 전략 종류

현재 시스템은 4가지 전략을 지원합니다:

### 1. **BUY_AND_HOLD** (매수 후 보유)
```
매수: 백테스트 시작 시점
매도: 백테스트 종료 시점
```
- 파라미터: 없음
- 설명: 단순히 처음에 매수하고 끝까지 보유cl

### 2. **MA_CROSS** (이동평균 교차)
```
매수: Golden Cross (단기 MA > 장기 MA)
매도: Dead Cross (단기 MA < 장기 MA)
```
- 파라미터:
  - `shortPeriod`: 단기 이동평균 기간 (기본값: 5)
  - `longPeriod`: 장기 이동평균 기간 (기본값: 20)
- 설명: 단기 이동평균이 장기 이동평균을 돌파할 때 매매

### 3. **RSI** (상대강도지수)
```
매수: RSI <= 과매도 수준
매도: RSI >= 과매수 수준
```
- 파라미터:
  - `period`: RSI 계산 기간 (기본값: 14)
  - `oversoldLevel`: 과매도 임계값 (기본값: 30)
  - `overboughtLevel`: 과매수 임계값 (기본값: 70)
- 설명: RSI 지표를 이용한 과매수/과매도 구간 매매

### 4. **BOLLINGER_BANDS** (볼린저밴드)
```
매수: 가격이 하단 밴드 아래로 떨어질 때
매도: 가격이 상단 밴드 위로 올라갈 때
```
- 파라미터:
  - `period`: 이동평균 기간 (기본값: 20)
  - `stdDevMultiplier`: 표준편차 배수 (기본값: 2.0)
- 설명: 볼린저밴드 돌파를 이용한 매매

---

## 상세 플로우

### 1️⃣ 백테스트 요청 단계

```
[API 요청] POST /api/backtest/run
{
  "name": "백테스트 이름",
  "market": "KRW-BTC",
  "timeframe": "15m",
  "startDate": "2025-12-20",
  "endDate": "2026-01-05",
  "initialCapital": 10000000,
  "strategyName": "MA_CROSS",           ← 전략 선택
  "strategyParams": {                   ← 전략 파라미터
    "shortPeriod": 5,
    "longPeriod": 20
  },
  "commissionRate": 0.0005,
  "slippageRate": 0.0001
}
```

### 2️⃣ 초기화 단계

```
BacktestService.runBacktest()
    ↓
1. 사용자 조회 (User 엔티티)
2. BacktestConfig 생성
   - 전략 이름 저장
   - 전략 파라미터 JSON 문자열로 저장
3. Config DB 저장
```

### 3️⃣ 엔진 실행 단계

```
BacktestEngine.run(config)
    ↓
┌─────────────────────────────────────┐
│ 1. Context 초기화                    │
│    - initialCapital 설정             │
│    - cash = initialCapital          │
│    - coinBalance = 0                │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 2. 과거 데이터 로드                  │
│    MarketCandleRepository에서        │
│    시작일~종료일 캔들 데이터 조회    │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 3. 전략 생성                         │
│    StrategyFactory.createStrategy()  │
│    - 전략 이름으로 전략 인스턴스 생성│
│    - 전략 파라미터 적용              │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 4. 전략 초기화                       │
│    strategy.initialize()             │
│    - 기술적 지표 계산 (MA, RSI 등)  │
└─────────────────────────────────────┘
```

### 4️⃣ 전략 실행 루프

```
for (각 캔들에 대해) {
    ┌─────────────────────────────────┐
    │ 1. 신호 생성                     │
    │    signal = strategy.generateSignal() │
    │    - BUY / SELL / HOLD          │
    └─────────────────────────────────┘
        ↓
    ┌─────────────────────────────────┐
    │ 2. 신호 실행                     │
    │    if (BUY && 코인 미보유) {    │
    │        현금의 99%로 매수         │
    │        OrderExecutor.executeBuy()│
    │    }                             │
    │    if (SELL && 코인 보유) {     │
    │        전량 매도                 │
    │        OrderExecutor.executeSell()│
    │    }                             │
    └─────────────────────────────────┘
        ↓
    ┌─────────────────────────────────┐
    │ 3. 자산 곡선 기록                │
    │    - portfolioValue 계산         │
    │    - 수익률 계산                 │
    │    - 낙폭(Drawdown) 업데이트    │
    │    - EquityCurve에 추가         │
    └─────────────────────────────────┘
}
```

### 5️⃣ 성과 계산 단계

```
PerformanceCalculator.calculateMetrics()
    ↓
┌─────────────────────────────────────┐
│ 계산 항목:                           │
│ - 총 수익률 (totalReturn)            │
│ - 연간 수익률 (annualReturn)         │
│ - 최대 낙폭 (maxDrawdown)            │
│ - 샤프 비율 (sharpeRatio)            │
│ - 승률 (winRate)                     │
│ - 최종 자본 (finalCapital)           │
│ - 최고 자본 (peakCapital)            │
│ - 거래 통계 (trades)                 │
└─────────────────────────────────────┘
```

### 6️⃣ 결과 저장 단계

```
BacktestService
    ↓
1. BacktestResult 저장
   - 성과 지표
   - 실행 시간
   - 상태 (COMPLETED/FAILED)
    ↓
2. BacktestTrade 저장
   - 각 거래 내역
   - 매수/매도 가격
   - 수익률
   - 잔고 정보
```

---

## 주요 컴포넌트

### 📦 BacktestService
- **역할**: 백테스트 전체 프로세스 관리
- **주요 기능**:
  - 비동기 백테스트 실행
  - Config 생성
  - 결과 저장
  - 거래 내역 저장

### 🎮 BacktestEngine
- **역할**: 백테스트 실행 엔진
- **주요 기능**:
  - 과거 데이터 로드
  - 전략 생성 및 초기화
  - 전략 실행 루프
  - 자산 곡선 기록

### 🏭 StrategyFactory
- **역할**: 전략 인스턴스 생성
- **지원 전략**:
  - BUY_AND_HOLD
  - MA_CROSS
  - RSI
  - BOLLINGER_BANDS

### 📊 Strategy (인터페이스)
- **역할**: 매매 신호 생성
- **주요 메서드**:
  - `initialize()`: 전략 초기화
  - `generateSignal()`: BUY/SELL/HOLD 신호 생성
  - `getName()`: 전략 이름 반환

### 💰 OrderExecutor
- **역할**: 주문 실행 및 잔고 관리
- **주요 기능**:
  - 매수 주문 실행
  - 매도 주문 실행
  - 수수료 및 슬리피지 적용
  - Trade 객체 생성

### 📈 PerformanceCalculator
- **역할**: 성과 지표 계산
- **계산 항목**:
  - 수익률
  - 최대 낙폭
  - 샤프 비율
  - 승률
  - 거래 통계

---

## 전략 선택 방법

백테스트 실행 시 `strategyName` 필드로 전략을 선택합니다:

```javascript
// 예시 1: Buy and Hold
{
  "strategyName": "BUY_AND_HOLD",
  "strategyParams": null
}

// 예시 2: MA Cross (기본 파라미터)
{
  "strategyName": "MA_CROSS",
  "strategyParams": {}
}

// 예시 3: MA Cross (커스텀 파라미터)
{
  "strategyName": "MA_CROSS",
  "strategyParams": {
    "shortPeriod": 10,
    "longPeriod": 30
  }
}

// 예시 4: RSI
{
  "strategyName": "RSI",
  "strategyParams": {
    "period": 14,
    "oversoldLevel": 30,
    "overboughtLevel": 70
  }
}

// 예시 5: Bollinger Bands
{
  "strategyName": "BOLLINGER_BANDS",
  "strategyParams": {
    "period": 20,
    "stdDevMultiplier": 2.0
  }
}
```

---

## 전략별 신호 생성 로직

### BUY_AND_HOLD
```
if (첫 번째 캔들) → BUY
if (마지막 캔들) → SELL
else → HOLD
```

### MA_CROSS
```
if (단기MA가 장기MA 상향 돌파 && 코인 미보유) → BUY
if (단기MA가 장기MA 하향 돌파 && 코인 보유) → SELL
else → HOLD
```

### RSI
```
if (RSI <= 과매도 && 코인 미보유) → BUY
if (RSI >= 과매수 && 코인 보유) → SELL
else → HOLD
```

### BOLLINGER_BANDS
```
if (가격 < 하단밴드 && 코인 미보유) → BUY
if (가격 > 상단밴드 && 코인 보유) → SELL
else → HOLD
```

---

## 주문 실행 규칙

### 매수 (BUY)
1. 매수 금액 = 현금의 99%
2. 매수 수량 = 매수 금액 / 현재가
3. 수수료 = 매수 금액 × 수수료율 (0.05%)
4. 슬리피지 = 매수 금액 × 슬리피지율 (0.01%)
5. 총 비용 = 매수 금액 + 수수료 + 슬리피지
6. 현금 차감, 코인 보유량 증가

### 매도 (SELL)
1. 매도 수량 = 보유 코인 전량
2. 매도 금액 = 매도 수량 × 현재가
3. 수수료 = 매도 금액 × 수수료율 (0.05%)
4. 슬리피지 = 매도 금액 × 슬리피지율 (0.01%)
5. 순수익 = 매도 금액 - 수수료 - 슬리피지
6. 현금 증가, 코인 보유량 0

---

## 성과 지표 설명

### 총 수익률 (Total Return)
```
(최종 자본 - 초기 자본) / 초기 자본 × 100
```

### 연간 수익률 (Annual Return)
```
총 수익률 × (365 / 백테스트 기간 일수)
```

### 최대 낙폭 (Max Drawdown)
```
(최고점 - 현재 가치) / 최고점 × 100
```
- 최고점 대비 최대 손실 비율

### 샤프 비율 (Sharpe Ratio)
```
(평균 수익률 - 무위험 수익률) / 수익률 표준편차
```
- 위험 대비 수익률 지표
- 높을수록 좋음 (1.0 이상이면 양호)

### 승률 (Win Rate)
```
승리 거래 수 / 전체 거래 수 × 100
```

---

## 데이터베이스 스키마

### backtest_configs
- 백테스트 설정 정보 저장
- 전략 이름 및 파라미터 JSON

### backtest_results
- 백테스트 실행 결과
- 성과 지표 (수익률, 낙폭, 샤프 비율 등)

### backtest_trades
- 개별 거래 내역
- 매수/매도 가격, 수량, 수익률

### market_candles
- 과거 캔들 데이터
- 백테스트 데이터 소스

---

## 정리

✅ **현재 버전**: 단일 버전 (V1, V2, V3 구분 없음)
✅ **전략 선택**: 4가지 전략 중 선택
✅ **전략 커스터마이징**: 각 전략의 파라미터 조정 가능
✅ **수수료/슬리피지**: 실거래 환경 반영
✅ **성과 측정**: 6가지 주요 지표로 평가

**전략 추가 방법**: 새로운 전략을 추가하려면
1. `Strategy` 인터페이스 구현
2. `StrategyFactory`에 등록
3. 프론트엔드 UI에 전략 옵션 추가
