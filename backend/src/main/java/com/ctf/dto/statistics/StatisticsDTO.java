package com.ctf.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsDTO {
    private UserStatisticsDTO userStatistics;
    private List<QuestionStatisticsDTO> questionStatistics;
    private List<CategoryStatisticsDTO> categoryStatistics;
    private List<ProgressDistributionDTO> progressDistribution;
    private LocalDateTime updatedAt;
}
