package com.ctf.service;

import com.ctf.dto.ContestConfigDTO;
import com.ctf.entity.ContestConfig;
import com.ctf.mapper.ContestConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ConfigService {

    @Autowired
    private ContestConfigMapper contestConfigMapper;

    public ContestConfigDTO getContestConfig() {
        String readyTime = getConfigValue("contest.readyTime", "2025-01-04 15:20:00");
        String startTime = getConfigValue("contest.startTime", "2025-01-04 15:30:00");
        String endTime = getConfigValue("contest.endTime", "2025-01-04 16:30:00");
        String resultsTime = getConfigValue("contest.resultsTime", "2025-01-04 17:00:00");
        
        return ContestConfigDTO.builder()
                .startTime(startTime)
                .readyTime(readyTime)
                .endTime(endTime)
                .resultsTime(resultsTime)
                .build();
    }

    public void updateContestConfig(ContestConfigDTO config) {
        try {
            updateConfigValue("contest.readyTime", config.getReadyTime());
            updateConfigValue("contest.startTime", config.getStartTime());
            updateConfigValue("contest.endTime", config.getEndTime());
            updateConfigValue("contest.resultsTime", config.getResultsTime());
            
            log.info("比赛配置已更新: {}", config);
        } catch (Exception e) {
            log.error("更新比赛配置失败", e);
            throw new RuntimeException("更新配置失败: " + e.getMessage());
        }
    }
    
    private String getConfigValue(String key, String defaultValue) {
        ContestConfig config = contestConfigMapper.selectByKey(key);
        return config != null ? config.getConfigValue() : defaultValue;
    }
    
    private void updateConfigValue(String key, String value) {
        ContestConfig config = ContestConfig.builder()
                .configKey(key)
                .configValue(value)
                .build();
        contestConfigMapper.upsert(config);
    }
}
