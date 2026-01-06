package com.cryptobot.upbit.service;

import com.cryptobot.upbit.dto.trading.OrderRequest;
import com.cryptobot.upbit.entity.*;
import com.cryptobot.upbit.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradingService {

    private final TradingModeRepository tradingModeRepository;
    private final TestTradeRepository testTradeRepository;
    private final TestBalanceRepository testBalanceRepository;
    private final UserRepository userRepository;
    private final UpbitApiService upbitApiService;

    /**
     * 사용자의 거래 모드 조회
     */
    public TradingMode getTradingMode(Long userId) {
        return tradingModeRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // 거래 모드가 없으면 기본값(TEST 모드)으로 생성
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

                    TradingMode mode = new TradingMode();
                    mode.setUser(user);
                    mode.setMode(TradingMode.Mode.TEST);
                    mode.setTestInitialBalance(10000000.0); // 1000만원
                    return tradingModeRepository.save(mode);
                });
    }

    /**
     * 거래 모드 변경
     */
    @Transactional
    public TradingMode switchMode(Long userId, TradingMode.Mode mode) {
        TradingMode tradingMode = getTradingMode(userId);
        tradingMode.setMode(mode);
        return tradingModeRepository.save(tradingMode);
    }

    /**
     * 테스트 모드 초기화 (잔고 리셋)
     */
    @Transactional
    public void resetTestMode(Long userId) {
        TradingMode tradingMode = getTradingMode(userId);

        // 모든 테스트 거래 내역 삭제
        List<TestTrade> trades = testTradeRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (!trades.isEmpty()) {
            testTradeRepository.deleteAllInBatch(trades);
        }

        // 모든 테스트 잔고 삭제
        List<TestBalance> balances = testBalanceRepository.findByUserId(userId);
        if (!balances.isEmpty()) {
            testBalanceRepository.deleteAllInBatch(balances);
        }

        // KRW 초기 잔고 생성
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        TestBalance krwBalance = new TestBalance();
        krwBalance.setUser(user);
        krwBalance.setCurrency("KRW");
        krwBalance.setBalance(tradingMode.getTestInitialBalance());
        krwBalance.setAvgBuyPrice(1.0);
        krwBalance.setLocked(0.0);
        testBalanceRepository.save(krwBalance);

        log.info("Test mode reset for user: {}", userId);
    }

    /**
     * 테스트 모드 주문 실행
     */
    @Transactional
    public Map<String, Object> executeTestOrder(Long userId, OrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        String market = request.getMarket();
        String currency = market.split("-")[1]; // KRW-BTC -> BTC
        Double price = request.getPrice();
        Double volume = request.getVolume();
        Double totalAmount = price * volume;

        Map<String, Object> result = new HashMap<>();

        try {
            if ("BUY".equals(request.getOrderType())) {
                // 매수 처리
                TestBalance krwBalance = testBalanceRepository.findByUserIdAndCurrency(userId, "KRW")
                        .orElseThrow(() -> new RuntimeException("KRW 잔고를 찾을 수 없습니다"));

                // 수수료 포함 총 필요 금액
                Double fee = totalAmount * 0.0005;
                Double totalNeeded = totalAmount + fee;

                if (krwBalance.getBalance() < totalNeeded) {
                    result.put("success", false);
                    result.put("message", "잔고가 부족합니다");
                    return result;
                }

                // KRW 차감
                krwBalance.setBalance(krwBalance.getBalance() - totalNeeded);
                testBalanceRepository.save(krwBalance);

                // 코인 잔고 증가
                TestBalance coinBalance = testBalanceRepository.findByUserIdAndCurrency(userId, currency)
                        .orElseGet(() -> {
                            TestBalance newBalance = new TestBalance();
                            newBalance.setUser(user);
                            newBalance.setCurrency(currency);
                            newBalance.setBalance(0.0);
                            newBalance.setAvgBuyPrice(0.0);
                            return newBalance;
                        });

                coinBalance.updateAvgBuyPrice(price, volume);
                testBalanceRepository.save(coinBalance);

                // 거래 기록 저장
                TestTrade trade = new TestTrade();
                trade.setUser(user);
                trade.setMarket(market);
                trade.setOrderType(TestTrade.OrderType.BUY);
                trade.setPrice(price);
                trade.setVolume(volume);
                trade.setTotalAmount(totalAmount);
                trade.setStrategy(request.getStrategy());
                trade.setMemo(request.getMemo());
                trade.calculateFee();
                testTradeRepository.save(trade);

                result.put("success", true);
                result.put("message", "매수 주문이 체결되었습니다");
                result.put("trade", trade);

            } else if ("SELL".equals(request.getOrderType())) {
                // 매도 처리
                TestBalance coinBalance = testBalanceRepository.findByUserIdAndCurrency(userId, currency)
                        .orElseThrow(() -> new RuntimeException("보유 코인을 찾을 수 없습니다"));

                if (coinBalance.getBalance() < volume) {
                    result.put("success", false);
                    result.put("message", "보유 수량이 부족합니다");
                    return result;
                }

                // 코인 차감
                coinBalance.decreaseBalance(volume);
                testBalanceRepository.save(coinBalance);

                // KRW 증가 (수수료 제외)
                Double fee = totalAmount * 0.0005;
                Double receivedAmount = totalAmount - fee;

                TestBalance krwBalance = testBalanceRepository.findByUserIdAndCurrency(userId, "KRW")
                        .orElseThrow(() -> new RuntimeException("KRW 잔고를 찾을 수 없습니다"));

                krwBalance.setBalance(krwBalance.getBalance() + receivedAmount);
                testBalanceRepository.save(krwBalance);

                // 거래 기록 저장
                TestTrade trade = new TestTrade();
                trade.setUser(user);
                trade.setMarket(market);
                trade.setOrderType(TestTrade.OrderType.SELL);
                trade.setPrice(price);
                trade.setVolume(volume);
                trade.setTotalAmount(totalAmount);
                trade.setStrategy(request.getStrategy());
                trade.setMemo(request.getMemo());
                trade.calculateFee();
                testTradeRepository.save(trade);

                result.put("success", true);
                result.put("message", "매도 주문이 체결되었습니다");
                result.put("trade", trade);

            } else {
                result.put("success", false);
                result.put("message", "잘못된 주문 타입입니다");
            }

        } catch (Exception e) {
            log.error("Test order execution failed", e);
            result.put("success", false);
            result.put("message", "주문 처리 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 테스트 모드 잔고 조회
     */
    public Map<String, Object> getTestBalances(Long userId) {
        List<TestBalance> balances = testBalanceRepository.findByUserId(userId);

        // KRW 잔고가 없으면 초기화
        if (balances.isEmpty()) {
            resetTestMode(userId);
            balances = testBalanceRepository.findByUserId(userId);
        }

        List<Map<String, Object>> balanceList = balances.stream()
                .filter(b -> b.getBalance() > 0.00000001) // 0보다 큰 잔고만
                .map(b -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("currency", b.getCurrency());
                    map.put("balance", b.getBalance());
                    map.put("avgBuyPrice", b.getAvgBuyPrice());
                    map.put("locked", b.getLocked());
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("balances", balanceList);
        return result;
    }

    /**
     * 테스트 모드 거래 내역 조회
     */
    public Map<String, Object> getTestTrades(Long userId) {
        List<TestTrade> trades = testTradeRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<Map<String, Object>> tradeList = trades.stream()
                .map(t -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", t.getId());
                    map.put("market", t.getMarket());
                    map.put("orderType", t.getOrderType());
                    map.put("price", t.getPrice());
                    map.put("volume", t.getVolume());
                    map.put("totalAmount", t.getTotalAmount());
                    map.put("fee", t.getFee());
                    map.put("strategy", t.getStrategy());
                    map.put("memo", t.getMemo());
                    map.put("createdAt", t.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("trades", tradeList);
        return result;
    }

    /**
     * 테스트 모드 계좌 요약 조회
     */
    public Map<String, Object> getTestAccountSummary(Long userId) {
        List<TestBalance> balances = testBalanceRepository.findByUserId(userId);

        // KRW 잔고가 없으면 초기화
        if (balances.isEmpty()) {
            resetTestMode(userId);
            balances = testBalanceRepository.findByUserId(userId);
        }

        Double totalBuyAmount = 0.0; // 총 매수금액
        Double totalEvaluationAmount = 0.0; // 총 평가금액
        List<Map<String, Object>> holdings = new ArrayList<>();

        for (TestBalance balance : balances) {
            if (balance.getBalance() <= 0.00000001) {
                continue; // 잔고가 거의 없으면 스킵
            }

            String currency = balance.getCurrency();
            Double balanceAmount = balance.getBalance();
            Double avgBuyPrice = balance.getAvgBuyPrice();

            if ("KRW".equals(currency)) {
                // KRW는 별도 처리
                continue;
            }

            // 현재가 조회 (실시간 Upbit API 사용)
            String market = "KRW-" + currency;
            Double currentPrice = 0.0;
            try {
                currentPrice = upbitApiService.getCurrentPrice(market).block();
                if (currentPrice == null) {
                    currentPrice = avgBuyPrice; // API 실패시 평균 매수가 사용
                }
            } catch (Exception e) {
                log.warn("Failed to get current price for {}, using avg buy price", market);
                currentPrice = avgBuyPrice;
            }

            Double buyAmount = avgBuyPrice * balanceAmount; // 매수금액
            Double evaluationAmount = currentPrice * balanceAmount; // 평가금액
            Double profitLoss = evaluationAmount - buyAmount; // 평가손익
            Double profitRate = buyAmount > 0 ? (profitLoss / buyAmount * 100) : 0.0; // 수익률

            totalBuyAmount += buyAmount;
            totalEvaluationAmount += evaluationAmount;

            Map<String, Object> holding = new HashMap<>();
            holding.put("currency", currency);
            holding.put("balance", balanceAmount);
            holding.put("avgBuyPrice", avgBuyPrice);
            holding.put("currentPrice", currentPrice);
            holding.put("buyAmount", buyAmount);
            holding.put("evaluationAmount", evaluationAmount);
            holding.put("profitLoss", profitLoss);
            holding.put("profitRate", profitRate);
            holdings.add(holding);
        }

        // KRW 잔고 추가
        Double krwBalance = balances.stream()
                .filter(b -> "KRW".equals(b.getCurrency()))
                .findFirst()
                .map(TestBalance::getBalance)
                .orElse(0.0);

        // 총 평가금액 = 보유 자산 평가액 + KRW 잔고
        Double totalAssets = totalEvaluationAmount + krwBalance;

        // 총 평가손익
        Double totalProfitLoss = totalEvaluationAmount - totalBuyAmount;

        // 총 평가수익률
        Double totalProfitRate = totalBuyAmount > 0 ? (totalProfitLoss / totalBuyAmount * 100) : 0.0;

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("krwBalance", krwBalance);
        result.put("totalBuyAmount", totalBuyAmount);
        result.put("totalEvaluationAmount", totalEvaluationAmount);
        result.put("totalAssets", totalAssets);
        result.put("totalProfitLoss", totalProfitLoss);
        result.put("totalProfitRate", totalProfitRate);
        result.put("holdings", holdings);
        result.put("isTestMode", true);

        return result;
    }
}
