package com.ctf.dto.scoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoringResult {
    private Integer awardedPoints;
    private Integer bonusPoints;
    private Boolean firstBlood;

    public ScoringResult(Integer awardedPoints, Boolean firstBlood) {
        this.awardedPoints = awardedPoints;
        this.bonusPoints = 0;
        this.firstBlood = firstBlood;
    }
}
