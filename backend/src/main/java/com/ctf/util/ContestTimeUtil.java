package com.ctf.util;

import com.ctf.entity.ContestConfig;
import com.ctf.mapper.ContestConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class ContestTimeUtil {

    @Autowired
    private ContestConfigMapper contestConfigMapper;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LocalDateTime getStartTime() {
        return getTimeFromConfig("contest.startTime", "2025-01-04 15:30:00");
    }

    private LocalDateTime getReadyTime() {
        return getTimeFromConfig("contest.readyTime", "2025-01-04 15:20:00");
    }

    private LocalDateTime getEndTime() {
        return getTimeFromConfig("contest.endTime", "2025-01-04 16:30:00");
    }

    private LocalDateTime getResultsTime() {
        return getTimeFromConfig("contest.resultsTime", "2025-01-04 17:00:00");
    }

    private LocalDateTime getTimeFromConfig(String key, String defaultValue) {
        try {
            ContestConfig config = contestConfigMapper.selectByKey(key);
            String value = config != null ? config.getConfigValue() : defaultValue;
            return LocalDateTime.parse(value, formatter);
        } catch (Exception e) {
            log.warn("Failed to parse time config for key: {}, using default: {}", key, defaultValue);
            return LocalDateTime.parse(defaultValue, formatter);
        }
    }

    public enum ContestStatus {
        NOT_STARTED, READY, RUNNING, FINISHED, RESULTS_PUBLISHED
    }

    public ContestStatus getCurrentStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(getReadyTime())) {
            return ContestStatus.NOT_STARTED;
        } else if (now.isBefore(getStartTime())) {
            return ContestStatus.READY;
        } else if (now.isBefore(getEndTime())) {
            return ContestStatus.RUNNING;
        } else if (now.isBefore(getResultsTime())) {
            return ContestStatus.FINISHED;
        } else {
            return ContestStatus.RESULTS_PUBLISHED;
        }
    }

    public Boolean canStartContest() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(getReadyTime());
    }

    public Boolean isContestActive() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(getStartTime()) && now.isBefore(getEndTime());
    }

    public Boolean isContestFinished() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(getEndTime());
    }

    public Long getRemainingTimeMillis() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target;

        ContestStatus status = getCurrentStatus();
        if (status == ContestStatus.NOT_STARTED) {
            target = getReadyTime();
        } else if (status == ContestStatus.READY) {
            target = getStartTime();
        } else if (status == ContestStatus.RUNNING) {
            target = getEndTime();
        } else if (status == ContestStatus.FINISHED) {
            target = getResultsTime();
        } else {
            return 0L;
        }

        long millis = java.time.temporal.ChronoUnit.MILLIS.between(now, target);
        return Math.max(0, millis);
    }

    public Long getContestStartTime() {
        return getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public Long getContestEndTime() {
        return getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
