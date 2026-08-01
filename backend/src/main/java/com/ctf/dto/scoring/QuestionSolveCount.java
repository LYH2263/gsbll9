package com.ctf.dto.scoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单题全场正确解出次数（B1 solve_count / §12 概览的现场聚合源）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSolveCount {
    private Integer questionId;
    private Integer solveCount;
}
