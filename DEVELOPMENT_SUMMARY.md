# Upbit Cryptobot 개발 문서

## 프로젝트 개요
업비트(Upbit) API를 활용한 암호화폐 자동매매 시스템

**기술 스택**
- **Backend**: Spring Boot 3.5.9, Java 17, H2 Database, JPA/Hibernate
- **Frontend**: React 18.2.0, React Router, Axios
- **Authentication**: JWT (JSON Web Token)
- **API Integration**: Upbit REST API, WebClient (Reactive)

---

## 주요 기능

### 1. 사용자 인증 시스템
- JWT 기반 로그인/회원가입
- 이메일 기반 사용자 인증
- 비밀번호 암호화 (BCrypt)
- Access Token 만료 시간: 1시간

### 2. API 키 관리
- 업비트 API 키 등록/조회/삭제
- AES-256 암호화로 안전하게 저장
- 활성/비활성 상태 관리
- 다중 API 키 지원

### 3. 업비트 API 연동
- 실시간 계좌 잔고 조회
- 마켓 정보 조회
- 시세 정보 조회 (Ticker)
- 캔들 차트 데이터 조회 (분봉, 일봉, 주봉, 월봉)
- 현재가 조회

### 4. 계좌 요약 정보
- 총 매수금액 계산
- 총 평가금액 계산
- 보유 자산 현황
- 평가손익 및 수익률 계산
- 실시간 현재가 반영

### 5. 트레이딩 스캔
- 다양한 기술적 분석 전략
  - RSI (과매수/과매도)
  - 골든크로스/데드크로스
  - 볼린저밴드
  - MACD
  - 거래량 급증
- 시간봉별 분석 (1분, 3분, 5분, 15분, 30분, 60분, 240분, 일봉)
- 상위 거래량 코인 자동 스캔

### 6. 테스트 모드 / 실거래 모드
- **테스트 모드**
  - 가상 자금으로 매매 연습
  - 초기 자금: 10,000,000 KRW
  - 실제 현재가로 평가금액 계산
  - 거래 내역 추적
  - 잔고 관리
  - 모드 초기화 기능

- **실거래 모드**
  - 실제 업비트 API 사용
  - 실제 자금 거래
  - 실시간 계좌 정보

### 7. 대시보드
- 전체 자산 현황
- 보유 코인 목록
- 평가손익 차트 (Pie Chart)
- 실시간 데이터 자동 갱신 (30초)

---

## 데이터베이스 스키마

### users 테이블
사용자 기본 정보 관리

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 사용자 ID |
| email | VARCHAR(255) | UNIQUE, NOT NULL | 이메일 (로그인 ID) |
| password | VARCHAR(255) | NOT NULL | 비밀번호 (BCrypt 암호화) |
| username | VARCHAR(100) | - | 사용자명 |
| phone_number | VARCHAR(20) | - | 전화번호 |
| enabled | BOOLEAN | DEFAULT TRUE | 활성화 상태 |
| upbit_access_key | VARCHAR(255) | - | 레거시 업비트 액세스 키 (암호화) |
| upbit_secret_key | VARCHAR(255) | - | 레거시 업비트 시크릿 키 (암호화) |
| created_at | TIMESTAMP | - | 생성일시 |
| updated_at | TIMESTAMP | - | 수정일시 |

**인덱스**
- `idx_users_email` on email (UNIQUE)

---

### api_keys 테이블
업비트 API 키 관리

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | API 키 ID |
| user_id | BIGINT | FK → users.id, NOT NULL | 사용자 ID |
| name | VARCHAR(100) | NOT NULL | API 키 이름 |
| access_key | VARCHAR(255) | NOT NULL | 액세스 키 (AES-256 암호화) |
| secret_key | VARCHAR(255) | NOT NULL | 시크릿 키 (AES-256 암호화) |
| is_active | BOOLEAN | DEFAULT TRUE | 활성화 상태 |
| created_at | TIMESTAMP | - | 생성일시 |
| updated_at | TIMESTAMP | - | 수정일시 |

**인덱스**
- `idx_api_keys_user_id` on user_id
- `idx_api_keys_is_active` on is_active

---

### trading_modes 테이블
사용자별 거래 모드 설정

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 거래 모드 ID |
| user_id | BIGINT | FK → users.id, NOT NULL | 사용자 ID |
| mode | VARCHAR(20) | NOT NULL | 거래 모드 (TEST, LIVE) |
| test_initial_balance | DOUBLE | DEFAULT 10000000.0 | 테스트 모드 초기 자금 |
| created_at | TIMESTAMP | - | 생성일시 |
| updated_at | TIMESTAMP | - | 수정일시 |

**인덱스**
- `idx_trading_modes_user_id` on user_id

**Enum Values**
- `mode`: TEST (테스트 모드), LIVE (실거래 모드)

---

### test_balances 테이블
테스트 모드 가상 잔고

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 잔고 ID |
| user_id | BIGINT | FK → users.id, NOT NULL | 사용자 ID |
| currency | VARCHAR(10) | NOT NULL | 화폐 코드 (KRW, BTC, ETH 등) |
| balance | DOUBLE | NOT NULL, DEFAULT 0.0 | 보유 수량 |
| locked | DOUBLE | DEFAULT 0.0 | 주문 중인 수량 |
| avg_buy_price | DOUBLE | DEFAULT 0.0 | 평균 매수가 |
| created_at | TIMESTAMP | - | 생성일시 |
| updated_at | TIMESTAMP | - | 수정일시 |

**제약조건**
- UNIQUE(user_id, currency) - 사용자별 화폐당 하나의 잔고

**인덱스**
- `idx_test_balances_user_id` on user_id
- `idx_test_balances_currency` on currency

---

### test_trades 테이블
테스트 모드 거래 내역

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 거래 ID |
| user_id | BIGINT | FK → users.id, NOT NULL | 사용자 ID |
| market | VARCHAR(20) | NOT NULL | 마켓 코드 (KRW-BTC 등) |
| order_type | VARCHAR(10) | NOT NULL | 주문 타입 (BUY, SELL) |
| price | DOUBLE | NOT NULL | 주문 가격 |
| volume | DOUBLE | NOT NULL | 주문 수량 |
| total_amount | DOUBLE | NOT NULL | 총 주문 금액 |
| fee | DOUBLE | DEFAULT 0.0 | 거래 수수료 (0.05%) |
| strategy | VARCHAR(50) | - | 사용된 전략 |
| memo | VARCHAR(255) | - | 메모 |
| created_at | TIMESTAMP | - | 거래 일시 |

**인덱스**
- `idx_test_trades_user_id` on user_id
- `idx_test_trades_created_at` on created_at (DESC)
- `idx_test_trades_market` on market

**Enum Values**
- `order_type`: BUY (매수), SELL (매도)

---

## API 엔드포인트

### 인증 API (`/api/auth`)

#### POST /api/auth/signup
회원가입

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "username": "홍길동",
  "phoneNumber": "010-1234-5678"
}
```

**Response**
```json
{
  "success": true,
  "accessToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "username": "홍길동",
    "phoneNumber": "010-1234-5678"
  }
}
```

#### POST /api/auth/login
로그인

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response**
```json
{
  "success": true,
  "accessToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "username": "홍길동"
  }
}
```

#### GET /api/auth/me
현재 사용자 정보 조회 (인증 필요)

**Headers**
```
Authorization: Bearer {token}
```

**Response**
```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "홍길동",
  "phoneNumber": "010-1234-5678",
  "apiKeyCount": 2
}
```

---

### API 키 관리 API (`/api/api-keys`)

#### GET /api/api-keys
등록된 API 키 목록 조회 (인증 필요)

**Response**
```json
{
  "success": true,
  "apiKeys": [
    {
      "id": 1,
      "name": "메인 계정",
      "accessKey": "ABC***XYZ",
      "isActive": true,
      "createdAt": "2026-01-05T10:00:00"
    }
  ]
}
```

#### POST /api/api-keys
API 키 등록 (인증 필요)

**Request Body**
```json
{
  "name": "메인 계정",
  "accessKey": "your-access-key",
  "secretKey": "your-secret-key"
}
```

**Response**
```json
{
  "success": true,
  "message": "API 키가 성공적으로 등록되었습니다",
  "apiKey": {
    "id": 1,
    "name": "메인 계정",
    "accessKey": "ABC***XYZ"
  }
}
```

#### DELETE /api/api-keys/{id}
API 키 삭제 (인증 필요)

---

### 계좌 정보 API (`/api/accounts`)

#### GET /api/accounts/summary
계좌 요약 정보 조회 (인증 필요)

**모드별 동작**
- **테스트 모드**: 가상 잔고 데이터 반환
- **실거래 모드**: 실제 업비트 계좌 데이터 반환

**Response**
```json
{
  "success": true,
  "krwBalance": 5000000.0,
  "totalBuyAmount": 3000000.0,
  "totalEvaluationAmount": 3500000.0,
  "totalAssets": 8500000.0,
  "totalProfitLoss": 500000.0,
  "totalProfitRate": 16.67,
  "holdings": [
    {
      "currency": "BTC",
      "balance": 0.05,
      "avgBuyPrice": 60000000.0,
      "currentPrice": 70000000.0,
      "buyAmount": 3000000.0,
      "evaluationAmount": 3500000.0,
      "profitLoss": 500000.0,
      "profitRate": 16.67
    }
  ],
  "isTestMode": true
}
```

---

### 스캔 API (`/api/scan`)

#### GET /api/scan/strategies
스캔 전략 실행

**Query Parameters**
- `timeFrame`: 시간봉 (1, 3, 5, 15, 30, 60, 240, 1d)
- `strategy`: 전략명 (rsi, cross, bollinger, macd, volume)

**Response**
```json
{
  "success": true,
  "timeFrame": "60",
  "strategy": "rsi",
  "signals": [
    {
      "market": "KRW-BTC",
      "koreanName": "비트코인",
      "currentPrice": 70000000.0,
      "signal": "BUY",
      "reason": "RSI 과매도 (25.3)",
      "confidence": 85.5
    }
  ]
}
```

---

### 트레이딩 API (`/api/trading`)

#### GET /api/trading/mode
현재 거래 모드 조회 (인증 필요)

**Response**
```json
{
  "success": true,
  "mode": "TEST",
  "testInitialBalance": 10000000.0
}
```

#### POST /api/trading/mode
거래 모드 변경 (인증 필요)

**Request Body**
```json
{
  "mode": "TEST"
}
```

**Response**
```json
{
  "success": true,
  "message": "테스트 모드로 전환되었습니다",
  "mode": "TEST"
}
```

#### POST /api/trading/test/reset
테스트 모드 초기화 (인증 필요)

**Response**
```json
{
  "success": true,
  "message": "테스트 모드가 초기화되었습니다"
}
```

#### POST /api/trading/order
주문 실행 (인증 필요)

**모드별 동작**
- **테스트 모드**: 가상 거래 실행
- **실거래 모드**: 실제 업비트 주문 실행

**Request Body**
```json
{
  "market": "KRW-BTC",
  "orderType": "BUY",
  "price": 70000000.0,
  "volume": 0.001,
  "strategy": "RSI",
  "memo": "RSI 과매도 매수"
}
```

**Response**
```json
{
  "success": true,
  "message": "매수 주문이 체결되었습니다",
  "trade": {
    "id": 1,
    "market": "KRW-BTC",
    "orderType": "BUY",
    "price": 70000000.0,
    "volume": 0.001,
    "totalAmount": 70000.0,
    "fee": 35.0
  }
}
```

#### GET /api/trading/test/balances
테스트 모드 잔고 조회 (인증 필요)

**Response**
```json
{
  "success": true,
  "balances": [
    {
      "currency": "KRW",
      "balance": 9930000.0,
      "avgBuyPrice": 1.0,
      "locked": 0.0
    },
    {
      "currency": "BTC",
      "balance": 0.001,
      "avgBuyPrice": 70000000.0,
      "locked": 0.0
    }
  ]
}
```

#### GET /api/trading/test/trades
테스트 모드 거래 내역 조회 (인증 필요)

**Response**
```json
{
  "success": true,
  "trades": [
    {
      "id": 1,
      "market": "KRW-BTC",
      "orderType": "BUY",
      "price": 70000000.0,
      "volume": 0.001,
      "totalAmount": 70000.0,
      "fee": 35.0,
      "strategy": "RSI",
      "memo": "RSI 과매도 매수",
      "createdAt": "2026-01-05T14:30:00"
    }
  ]
}
```

---

## 프론트엔드 구조

```
frontend/
├── public/
├── src/
│   ├── api/
│   │   ├── authApi.js         # 인증 API
│   │   ├── apiKeyApi.js       # API 키 관리
│   │   ├── accountApi.js      # 계좌 정보
│   │   ├── scanApi.js         # 스캔 전략
│   │   └── tradingApi.js      # 트레이딩
│   ├── components/
│   │   ├── auth/
│   │   │   ├── LoginForm.jsx
│   │   │   └── SignupForm.jsx
│   │   ├── layout/
│   │   │   ├── MainLayout.jsx
│   │   │   └── Sidebar.jsx
│   │   └── common/
│   │       └── PrivateRoute.jsx
│   ├── context/
│   │   └── AuthContext.jsx    # 인증 상태 관리
│   ├── hooks/
│   │   └── useAuth.js
│   ├── pages/
│   │   ├── LoginPage.jsx
│   │   ├── SignupPage.jsx
│   │   ├── DashboardPage.jsx
│   │   ├── TradingScanPage.jsx
│   │   ├── SettingsPage.jsx
│   │   └── ApiKeysPage.jsx
│   ├── utils/
│   │   └── tokenStorage.js    # JWT 토큰 관리
│   ├── App.jsx
│   └── index.js
└── package.json
```

---

## 백엔드 구조

```
backend/upbit-backend/
├── src/main/java/com/cryptobot/upbit/
│   ├── config/
│   │   ├── JpaConfig.java
│   │   ├── SecurityConfig.java
│   │   ├── WebConfig.java
│   │   ├── JwtProperties.java
│   │   └── UpbitApiProperties.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── ApiKeyController.java
│   │   ├── AccountController.java
│   │   ├── ScanController.java
│   │   └── TradingController.java
│   ├── dto/
│   │   ├── auth/
│   │   │   ├── SignupRequest.java
│   │   │   ├── LoginRequest.java
│   │   │   ├── AuthResponse.java
│   │   │   └── UserDto.java
│   │   ├── apikey/
│   │   │   └── ApiKeyRequest.java
│   │   └── trading/
│   │       └── OrderRequest.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── ApiKey.java
│   │   ├── TradingMode.java
│   │   ├── TestBalance.java
│   │   └── TestTrade.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── ApiKeyRepository.java
│   │   ├── TradingModeRepository.java
│   │   ├── TestBalanceRepository.java
│   │   └── TestTradeRepository.java
│   ├── security/
│   │   ├── JwtTokenProvider.java
│   │   └── JwtAuthenticationFilter.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── ApiKeyService.java
│   │   ├── UpbitApiService.java
│   │   ├── ScanService.java
│   │   ├── TradingService.java
│   │   └── EncryptionService.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── DuplicateEmailException.java
│   │   ├── InvalidCredentialsException.java
│   │   └── InvalidTokenException.java
│   └── UpbitBackendApplication.java
├── src/main/resources/
│   └── application.yml
└── build.gradle
```

---

## 주요 설정

### application.yml

```yaml
spring:
  application:
    name: upbit-cryptobot

  datasource:
    url: jdbc:h2:file:./data/upbit-cryptobot
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true

  h2:
    console:
      enabled: true
      path: /h2-console

server:
  port: 9090

jwt:
  secret-key: ${JWT_SECRET_KEY:upbit-cryptobot-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm}
  access-token-expiration: 3600000

encryption:
  aes-key: ${AES_SECRET_KEY:upbit-aes-encryption-key-32-bytes-required-for-aes256}

upbit:
  api:
    base-url: https://api.upbit.com/v1

logging:
  level:
    org.hibernate.SQL: debug
    org.hibernate.type.descriptor.sql.BasicBinder: trace
    com.cryptobot.upbit: debug
```

### 환경 변수

프로덕션 환경에서는 다음 환경 변수를 설정해야 합니다:

```bash
# JWT 시크릿 키 (최소 256비트)
JWT_SECRET_KEY=your-super-secret-jwt-key-here

# AES 암호화 키 (정확히 32바이트)
AES_SECRET_KEY=your-32-byte-aes-encryption-key
```

---

## 보안 고려사항

### 1. 비밀번호 암호화
- BCrypt 알고리즘 사용 (strength 10)
- 단방향 해싱으로 복호화 불가능

### 2. API 키 암호화
- AES-256 대칭키 암호화
- 데이터베이스에 암호화된 상태로 저장
- 사용 시에만 복호화

### 3. JWT 토큰 보안
- HS256 알고리즘
- 256비트 이상 Secret Key
- 1시간 만료 시간
- localStorage에 저장 (XSS 주의)

### 4. CORS 설정
- 개발: localhost:3000만 허용
- 프로덕션: 실제 도메인으로 제한 필요

### 5. SQL Injection 방지
- JPA/Hibernate 사용으로 자동 방지
- PreparedStatement 자동 적용

---

## 테스트 모드 동작 원리

### 1. 초기화
- 사용자 최초 접속 시 자동으로 TEST 모드로 설정
- 초기 자금 10,000,000 KRW 지급

### 2. 주문 실행
**매수 프로세스**
1. KRW 잔고 확인
2. 수수료 계산 (0.05%)
3. KRW 차감
4. 코인 잔고 증가
5. 평균 매수가 갱신
6. 거래 내역 저장

**매도 프로세스**
1. 코인 잔고 확인
2. 코인 차감
3. 수수료 계산 (0.05%)
4. KRW 증가
5. 거래 내역 저장

### 3. 평가금액 계산
- 실시간 Upbit API로 현재가 조회
- `평가금액 = 보유수량 × 현재가`
- `평가손익 = 평가금액 - 매수금액`
- `수익률 = (평가손익 / 매수금액) × 100`

### 4. 초기화
- 모든 거래 내역 삭제
- 모든 잔고 삭제
- KRW 10,000,000 재지급

---

## 실거래 모드 동작 원리

### 1. 계좌 조회
- 등록된 API 키로 실제 업비트 계좌 조회
- 실시간 잔고 정보 반환

### 2. 주문 실행
- 실제 업비트 API 호출
- 실제 주문 체결
- **주의**: 실제 자금이 사용됨

---

## 스캔 전략 상세

### 1. RSI (Relative Strength Index)
- **과매수**: RSI > 70
- **과매도**: RSI < 30
- 14기간 기준

### 2. 골든크로스/데드크로스
- **골든크로스**: 단기 이동평균(5) > 장기 이동평균(20) → 매수 신호
- **데드크로스**: 단기 이동평균(5) < 장기 이동평균(20) → 매도 신호

### 3. 볼린저밴드
- **상단 돌파**: 가격 > 상단밴드 → 과매수
- **하단 돌파**: 가격 < 하단밴드 → 과매도
- 20기간, 2 표준편차

### 4. MACD
- **골든크로스**: MACD > Signal → 매수 신호
- **데드크로스**: MACD < Signal → 매도 신호

### 5. 거래량 급증
- 현재 거래량 > 평균 거래량 × 2
- 급격한 관심도 증가 감지

---

## 개발 프로세스

### Phase 1: 인증 시스템 구축
1. User Entity 및 Repository 생성
2. JWT 토큰 Provider 구현
3. Security 설정
4. 회원가입/로그인 API
5. 프론트엔드 인증 Context 구현

### Phase 2: API 키 관리
1. ApiKey Entity 생성
2. AES 암호화 서비스 구현
3. API 키 CRUD 구현
4. 프론트엔드 API 키 관리 페이지

### Phase 3: 업비트 API 연동
1. WebClient 설정
2. 업비트 JWT 생성
3. 계좌 조회 API
4. 마켓 정보 조회
5. 시세 정보 조회

### Phase 4: 트레이딩 스캔
1. 캔들 데이터 조회
2. 기술적 분석 알고리즘 구현
3. 스캔 서비스 구현
4. 프론트엔드 스캔 페이지

### Phase 5: 테스트 트레이딩
1. TradingMode, TestBalance, TestTrade Entity 생성
2. 가상 거래 로직 구현
3. 테스트 모드 초기화
4. 계좌 요약 모드별 분기
5. 주문 실행 구현

### Phase 6: 대시보드
1. 계좌 요약 정보 표시
2. 보유 자산 차트
3. 실시간 데이터 갱신

---

## 향후 개선 계획

### 기능 추가
- [ ] 자동 매매 봇 구현
- [ ] 백테스팅 시스템
- [ ] 알림 기능 (텔레그램, 이메일)
- [ ] 거래 내역 상세 조회
- [ ] 수익률 통계 및 차트
- [ ] 다양한 기술적 지표 추가

### 보안 강화
- [ ] Refresh Token 도입
- [ ] 2단계 인증 (2FA)
- [ ] API Rate Limiting
- [ ] 로그인 시도 제한

### 성능 최적화
- [ ] Redis 캐싱
- [ ] 데이터베이스 인덱스 최적화
- [ ] API 응답 캐싱

### UI/UX 개선
- [ ] 다크 모드
- [ ] 반응형 디자인 개선
- [ ] 차트 라이브러리 고도화
- [ ] 로딩 애니메이션

---

## 문제 해결 이력

### 1. JWT 토큰 인증 문제
**문제**: localStorage에서 토큰을 가져올 때 키 이름 불일치
**해결**: `getToken()` 유틸리티 함수로 통일

### 2. 테스트 모드 초기화 실패
**문제**: Unique constraint violation (KRW 잔고 중복)
**해결**: `deleteAllInBatch()` 사용하여 완전 삭제 후 재생성

### 3. CORS 에러
**문제**: 프론트엔드-백엔드 통신 차단
**해결**: SecurityConfig에 CORS 설정 추가

---

## 라이선스 및 참고

**라이선스**: MIT License

**참고 자료**
- [Upbit Open API 문서](https://docs.upbit.com/)
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [React 공식 문서](https://react.dev/)
- [JWT.io](https://jwt.io/)

---

**최종 업데이트**: 2026-01-05
**작성자**: Development Team
