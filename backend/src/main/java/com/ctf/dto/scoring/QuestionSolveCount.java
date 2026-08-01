package com.ctf.dto.scoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 每题的全场正确解出次数投影（P2 概览用）。
 * {@code points} 为题目基础分（questions.points），{@code solveCount} 为该题全场正确解出人次。
 * 概览据此按 B1 公式现场算「当前分」，判定 §12.3/§12.4（D1：不新建汇总表）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSolveCount {
    private Integer questionId;
    private Integer points;
    private Long solveCount;
}
