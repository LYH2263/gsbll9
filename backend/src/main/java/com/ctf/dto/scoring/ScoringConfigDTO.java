package com.ctf.dto.scoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 动态计分配置 DTO（P1 §10）。字段一一对应规格书 §8 已冻结的配置键：
 * <ul>
 *   <li>{@code minPoints}        ← {@code scoring.min_points}</li>
 *   <li>{@code decayStep}        ← {@code scoring.decay_step}</li>
 *   <li>{@code firstBloodBonus}  ← {@code scoring.first_blood_bonus}</li>
 *   <li>{@code freezeOnEnd}      ← {@code scoring.freeze_on_end}（P2 启用）</li>
 *   <li>{@code overviewTimezone} ← {@code scoring.overview_timezone}（P2 启用）</li>
 * </ul>
 * 字段 camelCase，与前端一一对应；配置键字符串不在此改名。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoringConfigDTO {
    private Integer minPoints;
    private Integer decayStep;
    private Integer firstBloodBonus;
    private Boolean freezeOnEnd;
    private String overviewTimezone;
}
