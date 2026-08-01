package com.ctf.dto.scoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * P2 计分概览（§12，D1：基于 first_bloods / submissions / questions 现场聚合，无冗余汇总表）。
 * 字段与 §12 五项指标一一对应（camelCase 映射）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoringOverviewDTO {
    /** total_correct_solves：全场正确解出总次数（按解出事件计） */
    private Integer totalCorrectSolves;
    /** questions_with_first_blood：已产生一血的题目数 */
    private Integer questionsWithFirstBlood;
    /** questions_at_base_points：当前分仍等于该题基础分的题目数 */
    private Integer questionsAtBasePoints;
    /** questions_at_min_points：当前分已等于 scoring.min_points 的题目数 */
    private Integer questionsAtMinPoints;
    /** total_first_blood_bonus_awarded：已发放的一血奖金合计 */
    private Integer totalFirstBloodBonusAwarded;
}
