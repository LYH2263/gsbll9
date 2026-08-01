package com.ctf.dto.scoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
