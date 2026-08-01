package com.ctf.controller;

import com.ctf.dto.ApiResponse;
import com.ctf.dto.ContestConfigDTO;
import com.ctf.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/admin/config")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/contest")
    public ApiResponse<ContestConfigDTO> getContestConfig() {
        log.info("Get contest config request");
        ContestConfigDTO config = configService.getContestConfig();
        return ApiResponse.success(config);
    }

    @PutMapping("/contest")
    public ApiResponse<Void> updateContestConfig(@RequestBody ContestConfigDTO config) {
        log.info("Update contest config: {}", config);
        validateTimeConfig(config);
        configService.updateContestConfig(config);
        return ApiResponse.success(null);
    }

    private void validateTimeConfig(ContestConfigDTO config) {
        LocalDateTime readyTime = LocalDateTime.parse(config.getReadyTime(), FORMATTER);
        LocalDateTime startTime = LocalDateTime.parse(config.getStartTime(), FORMATTER);
        LocalDateTime endTime = LocalDateTime.parse(config.getEndTime(), FORMATTER);
        LocalDateTime resultsTime = LocalDateTime.parse(config.getResultsTime(), FORMATTER);

        if (!startTime.isAfter(readyTime)) {
            throw new IllegalArgumentException("开始时间必须晚于准备时间");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
        if (!resultsTime.isAfter(endTime)) {
            throw new IllegalArgumentException("成绩公布时间必须晚于结束时间");
        }
    }
}
