package com.ctf.dto.scoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoringOverviewDTO {
    private Integer totalCorrectSolves;
    private Integer questionsWithFirstBlood;
    private Integer questionsAtBasePoints;
    private Integer questionsAtMinPoints;
    private Integer totalFirstBloodBonusAwarded;
}
