package com.ctf.dto.scoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 计分概览 DTO（P2 §12，D1 现场聚合，D2 管理端权限）。五字段严格对应 §12 五指标，
 * 口径不得自行发挥；字段 camelCase，与前端一一对应：
 * <ul>
 *   <li>{@code totalCorrectSolves}          ← §12.1 total_correct_solves（按解出事件计）</li>
 *   <li>{@code questionsWithFirstBlood}     ← §12.2 questions_with_first_blood</li>
 *   <li>{@code questionsAtBasePoints}       ← §12.3 questions_at_base_points</li>
 *   <li>{@code questionsAtMinPoints}        ← §12.4 questions_at_min_points</li>
 *   <li>{@code totalFirstBloodBonusAwarded} ← §12.5 total_first_blood_bonus_awarded</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoringOverviewDTO {
    private Long totalCorrectSolves;
    private Long questionsWithFirstBlood;
    private Long questionsAtBasePoints;
    private Long questionsAtMinPoints;
    private Long totalFirstBloodBonusAwarded;
}
