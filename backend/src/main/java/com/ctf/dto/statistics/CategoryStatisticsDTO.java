package com.ctf.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatisticsDTO {
    private Integer categoryId;
    private String categoryName;
    private Integer totalQuestions;
    private Integer totalPoints;
    private Double averageScore;
}
