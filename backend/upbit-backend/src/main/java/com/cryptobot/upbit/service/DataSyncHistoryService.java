package com.cryptobot.upbit.service;

import com.cryptobot.upbit.entity.DataSyncHistory;
import com.cryptobot.upbit.entity.User;
import com.cryptobot.upbit.repository.DataSyncHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSyncHistoryService {

    private final DataSyncHistoryRepository dataSyncHistoryRepository;

    /**
     * 수집 이력 생성
     */
    @Transactional
    public DataSyncHistory createHistory(User user, String market, String timeframe,
                                         LocalDate startDate, LocalDate endDate, String taskId) {
        DataSyncHistory history = DataSyncHistory.builder()
                .user(user)
                .market(market)
                .timeframe(timeframe)
                .startDate(startDate)
                .endDate(endDate)
                .status("IN_PROGRESS")
                .taskId(taskId)
                .progress(0.0)
                .recordCount(0)
                .build();

        return dataSyncHistoryRepository.save(history);
    }

    /**
     * 수집 이력 업데이트 (진행률, 레코드 수)
     */
    @Transactional
    public void updateProgress(String taskId, Double progress, Integer recordCount) {
        Optional<DataSyncHistory> historyOpt = dataSyncHistoryRepository.findByTaskId(taskId);
        if (historyOpt.isPresent()) {
            DataSyncHistory history = historyOpt.get();
            history.setProgress(progress);
            history.setRecordCount(recordCount);
            dataSyncHistoryRepository.save(history);
        }
    }

    /**
     * 수집 완료 처리
     */
    @Transactional
    public void completeHistory(String taskId, Integer recordCount) {
        Optional<DataSyncHistory> historyOpt = dataSyncHistoryRepository.findByTaskId(taskId);
        if (historyOpt.isPresent()) {
            DataSyncHistory history = historyOpt.get();
            history.setStatus("COMPLETED");
            history.setProgress(100.0);
            history.setRecordCount(recordCount);
            history.setCompletedAt(LocalDateTime.now());
            dataSyncHistoryRepository.save(history);
            log.info("Data sync history completed: taskId={}, recordCount={}", taskId, recordCount);
        }
    }

    /**
     * 수집 실패 처리
     */
    @Transactional
    public void failHistory(String taskId, String errorMessage) {
        Optional<DataSyncHistory> historyOpt = dataSyncHistoryRepository.findByTaskId(taskId);
        if (historyOpt.isPresent()) {
            DataSyncHistory history = historyOpt.get();
            history.setStatus("FAILED");
            history.setErrorMessage(errorMessage);
            history.setCompletedAt(LocalDateTime.now());
            dataSyncHistoryRepository.save(history);
            log.error("Data sync history failed: taskId={}, error={}", taskId, errorMessage);
        }
    }

    /**
     * 사용자의 수집 이력 목록 조회
     */
    @Transactional(readOnly = true)
    public List<DataSyncHistory> getHistoryByUser(Long userId) {
        return dataSyncHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Task ID로 이력 조회
     */
    @Transactional(readOnly = true)
    public Optional<DataSyncHistory> getHistoryByTaskId(String taskId) {
        return dataSyncHistoryRepository.findByTaskId(taskId);
    }
}
