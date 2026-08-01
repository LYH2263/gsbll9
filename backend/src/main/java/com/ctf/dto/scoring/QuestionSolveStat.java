package com.ctf.dto.scoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSolveStat {
    private Integer questionId;
    private Integer basePoints;
    private Integer correctCount;
}
