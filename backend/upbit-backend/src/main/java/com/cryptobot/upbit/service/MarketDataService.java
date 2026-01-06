package com.cryptobot.upbit.service;

import com.cryptobot.upbit.dto.backtest.AvailableMarketData;
import com.cryptobot.upbit.dto.backtest.CandleDto;
import com.cryptobot.upbit.dto.backtest.DataSyncStatus;
import com.cryptobot.upbit.entity.MarketCandle;
import com.cryptobot.upbit.repository.MarketCandleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final MarketCandleRepository candleRepository;
    private final WebClient.Builder webClientBuilder;

    // 동기화 작업 상태 관리
    private final Map<String, DataSyncStatus> syncTasks = new ConcurrentHashMap<>();

    /**
     * 과거 캔들 데이터 동기화 (비동기)
     */
    @Async
    public CompletableFuture<Void> syncMarketData(String taskId, String market, String timeframe,
                                                   LocalDate startDate, LocalDate endDate) {
        log.info("Starting data sync for {}-{} from {} to {}", market, timeframe, startDate, endDate);

        try {
            // 초기 상태 설정
            int estimatedRecords = estimateRecordCount(timeframe, startDate, endDate);
            updateTaskStatus(taskId, "IN_PROGRESS", 0.0, 0, estimatedRecords, "데이터 수집 시작");

            // 업비트 API 타임프레임 변환
            String upbitTimeframe = convertTimeframe(timeframe);

            // 데이터 수집
            List<MarketCandle> candles = fetchCandlesFromUpbit(market, upbitTimeframe, startDate, endDate, taskId);

            // 데이터베이스에 저장
            saveCandlesBatch(candles, taskId);

            // 완료 상태 업데이트
            updateTaskStatus(taskId, "COMPLETED", 100.0, candles.size(), estimatedRecords,
                    "데이터 수집 완료: " + candles.size() + "개");

            log.info("Data sync completed for {}-{}: {} records", market, timeframe, candles.size());

        } catch (Exception e) {
            log.error("Data sync failed for {}-{}", market, timeframe, e);
            updateTaskStatus(taskId, "FAILED", 0.0, 0, 0, "오류: " + e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 업비트 API로부터 캔들 데이터 수집
     */
    private List<MarketCandle> fetchCandlesFromUpbit(String market, String timeframe,
                                                      LocalDate startDate, LocalDate endDate,
                                                      String taskId) {
        List<MarketCandle> allCandles = new ArrayList<>();
        LocalDateTime currentEnd = endDate.atTime(23, 59, 59);
        LocalDateTime targetStart = startDate.atStartOfDay();

        int batchCount = 0;
        int totalBatches = estimateRecordCount(timeframe, startDate, endDate) / 200 + 1;

        while (currentEnd.isAfter(targetStart)) {
            try {
                // API 호출
                List<MarketCandle> batch = fetchCandleBatch(market, timeframe, currentEnd);

                if (batch.isEmpty()) {
                    break;
                }

                // 날짜 범위 필터링
                List<MarketCandle> filteredBatch = batch.stream()
                        .filter(candle -> !candle.getTimestamp().isBefore(targetStart))
                        .collect(Collectors.toList());

                allCandles.addAll(filteredBatch);

                // 진행 상태 업데이트
                batchCount++;
                double progress = Math.min(100.0, (batchCount * 100.0) / totalBatches);
                updateTaskStatus(taskId, "IN_PROGRESS", progress, allCandles.size(),
                        totalBatches * 200, "데이터 수집 중... (" + batchCount + "/" + totalBatches + ")");

                // 다음 배치를 위해 시간 조정
                currentEnd = batch.get(batch.size() - 1).getTimestamp().minusSeconds(1);

                // API 호출 제한 준수 (초당 10회)
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("Error fetching candle batch", e);
                break;
            }
        }

        return allCandles;
    }

    /**
     * 단일 배치 캔들 데이터 가져오기 (최대 200개)
     */
    private List<MarketCandle> fetchCandleBatch(String market, String timeframe, LocalDateTime to) {
        WebClient webClient = webClientBuilder
                .baseUrl("https://api.upbit.com/v1")
                .build();

        String endpoint;

        // 타임프레임에 따라 엔드포인트 결정
        switch (timeframe) {
            case "minutes/1":
            case "minutes/5":
            case "minutes/15":
            case "minutes/30":
            case "minutes/60":
                endpoint = "/candles/" + timeframe;
                break;
            case "days":
                endpoint = "/candles/days";
                break;
            case "weeks":
                endpoint = "/candles/weeks";
                break;
            case "months":
                endpoint = "/candles/months";
                break;
            default:
                throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        }

        // 시간 포맷: yyyy-MM-dd HH:mm:ss
        String toParam = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(endpoint)
                            .queryParam("market", market)
                            .queryParam("to", toParam)
                            .queryParam("count", "200")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseCandleResponse(response, market, getSimpleTimeframe(timeframe));

        } catch (Exception e) {
            log.error("Failed to fetch candle batch from Upbit API", e);
            return Collections.emptyList();
        }
    }

    /**
     * API 응답을 MarketCandle 엔티티로 변환
     */
    private List<MarketCandle> parseCandleResponse(String json, String market, String timeframe) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> candles = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});

            return candles.stream()
                    .map(candle -> {
                        // candle_date_time_kst 필드 파싱
                        String timestampStr = (String) candle.get("candle_date_time_kst");
                        LocalDateTime timestamp = LocalDateTime.parse(timestampStr,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

                        return MarketCandle.builder()
                                .market(market)
                                .timeframe(timeframe)
                                .timestamp(timestamp)
                                .openingPrice(((Number) candle.get("opening_price")).doubleValue())
                                .highPrice(((Number) candle.get("high_price")).doubleValue())
                                .lowPrice(((Number) candle.get("low_price")).doubleValue())
                                .closingPrice(((Number) candle.get("trade_price")).doubleValue())
                                .volume(((Number) candle.get("candle_acc_trade_volume")).doubleValue())
                                .accTradePrice(((Number) candle.get("candle_acc_trade_price")).doubleValue())
                                .build();
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to parse candle response", e);
            return Collections.emptyList();
        }
    }

    /**
     * 캔들 데이터 배치 저장
     */
    @Transactional
    public void saveCandlesBatch(List<MarketCandle> candles, String taskId) {
        int saved = 0;
        int skipped = 0;

        for (MarketCandle candle : candles) {
            // 중복 체크
            if (!candleRepository.existsByMarketAndTimeframeAndTimestamp(
                    candle.getMarket(), candle.getTimeframe(), candle.getTimestamp())) {
                candleRepository.save(candle);
                saved++;
            } else {
                skipped++;
            }
        }

        log.info("Saved {} candles, skipped {} duplicates", saved, skipped);
    }

    /**
     * 사용 가능한 시장 데이터 목록 조회
     */
    public List<AvailableMarketData> getAvailableMarketData() {
        List<AvailableMarketData> result = new ArrayList<>();

        // 모든 마켓 조회
        List<String> markets = candleRepository.findDistinctMarkets();
        log.info("Found {} distinct markets", markets.size());
        log.info("Markets: {}", markets);

        for (String market : markets) {
            // 각 마켓의 타임프레임 조회
            List<String> timeframes = candleRepository.findDistinctTimeframesByMarket(market);
            log.info("Market {}: found {} timeframes", market, timeframes.size());

            for (String timeframe : timeframes) {
                try {
                    // 데이터 기간 및 개수 조회
                    Object[] dateRange = candleRepository.findDateRangeByMarketAndTimeframe(market, timeframe);
                    long count = candleRepository.countByMarketAndTimeframe(market, timeframe);

                    if (dateRange != null && dateRange.length == 2
                            && dateRange[0] != null && dateRange[1] != null) {

                        // Object를 LocalDateTime으로 안전하게 변환
                        LocalDateTime startDate = convertToLocalDateTime(dateRange[0]);
                        LocalDateTime endDate = convertToLocalDateTime(dateRange[1]);

                        if (startDate != null && endDate != null) {
                            log.info("Adding data: market={}, timeframe={}, start={}, end={}, count={}",
                                    market, timeframe, startDate, endDate, count);

                            result.add(AvailableMarketData.builder()
                                    .market(market)
                                    .timeframe(timeframe)
                                    .startDate(startDate)
                                    .endDate(endDate)
                                    .recordCount(count)
                                    .build());
                        } else {
                            log.warn("Failed to convert dates for market={}, timeframe={}", market, timeframe);
                        }
                    } else {
                        log.warn("Skipping data: dateRange is null or invalid for market={}, timeframe={}",
                                market, timeframe);
                    }
                } catch (Exception e) {
                    log.error("Error processing market data for market={}, timeframe={}",
                            market, timeframe, e);
                }
            }
        }

        log.info("Returning {} available market data items", result.size());
        return result;
    }

    /**
     * 동기화 작업 상태 조회
     */
    public DataSyncStatus getTaskStatus(String taskId) {
        return syncTasks.getOrDefault(taskId, DataSyncStatus.builder()
                .taskId(taskId)
                .status("NOT_FOUND")
                .message("작업을 찾을 수 없습니다")
                .build());
    }

    /**
     * 작업 상태 업데이트
     */
    private void updateTaskStatus(String taskId, String status, Double progress,
                                   Integer processed, Integer total, String message) {
        DataSyncStatus statusObj = DataSyncStatus.builder()
                .taskId(taskId)
                .status(status)
                .progress(progress)
                .recordsProcessed(processed)
                .totalRecords(total)
                .message(message)
                .build();

        syncTasks.put(taskId, statusObj);
    }

    /**
     * 타임프레임 변환 (내부 형식 -> 업비트 API 형식)
     */
    private String convertTimeframe(String timeframe) {
        switch (timeframe) {
            case "1m": return "minutes/1";
            case "5m": return "minutes/5";
            case "15m": return "minutes/15";
            case "30m": return "minutes/30";
            case "1h": return "minutes/60";
            case "1d": return "days";
            case "1w": return "weeks";
            case "1M": return "months";
            default: throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        }
    }

    /**
     * API 타임프레임 -> 간단한 형식으로 변환
     */
    private String getSimpleTimeframe(String apiTimeframe) {
        switch (apiTimeframe) {
            case "minutes/1": return "1m";
            case "minutes/5": return "5m";
            case "minutes/15": return "15m";
            case "minutes/30": return "30m";
            case "minutes/60": return "1h";
            case "days": return "1d";
            case "weeks": return "1w";
            case "months": return "1M";
            default: return apiTimeframe;
        }
    }

    /**
     * 예상 레코드 수 계산
     */
    private int estimateRecordCount(String timeframe, LocalDate startDate, LocalDate endDate) {
        long daysBetween = endDate.toEpochDay() - startDate.toEpochDay();

        switch (timeframe) {
            case "1m": return (int) (daysBetween * 24 * 60);
            case "5m": return (int) (daysBetween * 24 * 12);
            case "15m": return (int) (daysBetween * 24 * 4);
            case "30m": return (int) (daysBetween * 24 * 2);
            case "1h": return (int) (daysBetween * 24);
            case "1d": return (int) daysBetween;
            case "1w": return (int) (daysBetween / 7);
            case "1M": return (int) (daysBetween / 30);
            default: return 0;
        }
    }

    /**
     * 전체 캔들 데이터 개수 조회 (디버그용)
     */
    public long getTotalCandleCount() {
        return candleRepository.count();
    }

    /**
     * Object를 LocalDateTime으로 안전하게 변환
     */
    private LocalDateTime convertToLocalDateTime(Object obj) {
        if (obj == null) {
            return null;
        }

        // 이미 LocalDateTime인 경우
        if (obj instanceof LocalDateTime) {
            return (LocalDateTime) obj;
        }

        // java.sql.Timestamp인 경우
        if (obj instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) obj).toLocalDateTime();
        }

        // java.util.Date인 경우
        if (obj instanceof java.util.Date) {
            return LocalDateTime.ofInstant(
                    ((java.util.Date) obj).toInstant(),
                    ZoneId.systemDefault()
            );
        }

        // 변환 실패
        log.warn("Cannot convert {} to LocalDateTime", obj.getClass().getName());
        return null;
    }
}
