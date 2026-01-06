# 📌 업비트 자동매매 프로그램 작업지시서 (IntelliJ 기준)

## 1. 개발 목표
- **업비트 KRW 마켓 현물 자동매매 봇** 구현
- 전략은 **규칙 기반(EMA + RSI + MACD)**
- **안정성·재현성·리스크 관리 우선**, 수익 극대화는 2차 목표

---

## 2. 기술 스택
- Language: **Java 17**
- IDE: **IntelliJ**
- Framework: **Spring Boot**
- Scheduler: `@Scheduled`
- DB: **MariaDB**
- ORM: **JPA (Hibernate)**
- Exchange API: **Upbit REST API**
- Indicator: 직접 구현 또는 TA 라이브러리 사용 가능

---

## 3. 시스템 아키텍처
```
Scheduler (5분마다 실행)
   ↓
MarketDataService (캔들 수집)
   ↓
IndicatorService (EMA / RSI / MACD 계산)
   ↓
SignalEngine (진입/청산 신호 판단)
   ↓
PositionStateMachine (중복진입 방지)
   ↓
ExecutionService (주문/체결 관리)
   ↓
RiskManager (손실 제한 / 거래 중단 판단)
   ↓
DB + Log + Alert
```

---

## 4. 거래 기본 조건 (고정)
- 마켓: **KRW-XXX (현물)**
- 거래 방향: **롱(Long)만**
- 타임프레임: **5분봉**
- 봉 기준: **반드시 마감된 봉만 사용**
- 최초 대상 종목: **1~3개**

---

## 5. 상태 머신 (필수)
```
FLAT → ENTRY_PENDING → LONG → EXIT_PENDING → FLAT
```
- 동일 상태에서 **중복 주문 절대 금지**
- 신호 반복 발생 시 무시

---

## 6. 진입 알고리즘 (Entry Rules)

### 진입 조건 (모두 충족)
1. **이격 조건**
```
close <= EMA(25) * (1 - 0.20)
```

2. **과매도 조건**
```
RSI(14) <= 30
```

3. **타이밍 트리거**
```
MACD_HIST(t-1) < 0
AND
MACD_HIST(t) >= 0
```

4. **봉 확정 조건**
- 마지막 캔들은 반드시 종료된 캔들일 것

### 진입 시 처리
- 주문 방식: **공격적 지정가**
- 주문 가격:
```
현재 매도호가(ask) ± 호가단위 보정
```

---

## 7. 손절 / 익절 규칙 (우선순위 고정)

### 1️⃣ 손절 (최우선)
- 기준:
```
최근 스윙 저점 - buffer
```
- 실행 방식:
  - **시장가 매도**
  - 즉시 실행

### 2️⃣ 익절
- 기준:
```
TP = entry_price + 3 * (entry_price - stop_price)
```

### 3️⃣ 트레일링 스탑 (선택)
- 최고가 갱신 시 stop 가격 상향 조정

---

## 8. 리스크 관리 (절대 조건)
- **1회 거래 손실 한도**: 계좌의 0.5~1%
- **일일 최대 손실**: -X% 도달 시 **당일 거래 중단**
- **최대 동시 포지션 수**: 1~3
- API 또는 서버 오류 발생 시:
  - 신규 진입 중단
  - 기존 주문 상태 조회만 수행

---

## 9. 주문 처리 규칙 (Upbit 대응)
- 모든 주문의 **order_id DB 저장 필수**
- 주문 후 N초(10~30초) 대기
- 미체결 또는 부분체결 시:
  - 잔량 취소
  - 가격 재조정 후 재주문
- 체결 수량 기준으로 포지션 계산

---

## 10. DB 테이블 설계 (최소)

### trades
- id
- market
- entry_price
- stop_price
- take_profit
- qty
- status
- created_at

### orders
- order_id
- trade_id
- type (BUY / SELL)
- price
- qty
- filled_qty
- status

### candles
- market
- timeframe
- open
- high
- low
- close
- volume
- timestamp

### logs
- level
- message
- created_at

---

## 11. 구현 시 절대 금지 사항
- ❌ 진행 중인 캔들로 신호 판단
- ❌ 손절가 없이 진입
- ❌ 주문 결과 확인 없이 상태 변경
- ❌ 수수료 / 슬리피지 무시
- ❌ 예외 발생 후에도 신규 주문 지속

---

## 12. 테스트 단계
1. 백테스트 (수수료 포함)
2. 페이퍼 트레이딩
3. 소액 실거래
4. 알림 / 로그 정상 동작 확인

---

## 13. 최종 산출물
- Spring Boot 프로젝트
- 설정값 외부화 (`application.yml`)
- 전략 파라미터 분리
- 로그 및 예외 처리 완료

---

## 🔒 개발 원칙
> 수익보다 **안 죽는 봇**
> 안정성 → 재현성 → 수익성 순으로 개발할 것
