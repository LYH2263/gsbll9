package com.ctf.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionStatisticsDTO {
    private Integer questionId;
    private String questionTitle;
    private String categoryName;
    private Integer attemptCount;
    private Integer correctCount;
    private Double correctRate;
}
