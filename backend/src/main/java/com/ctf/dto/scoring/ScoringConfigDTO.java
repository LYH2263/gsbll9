package com.ctf.dto.scoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * P1 衰减/奖金配置（对应 §8 已冻结键 scoring.min_points / scoring.decay_step / scoring.first_blood_bonus）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoringConfigDTO {
    private Integer minPoints;
    private Integer decayStep;
    private Integer firstBloodBonus;
}
