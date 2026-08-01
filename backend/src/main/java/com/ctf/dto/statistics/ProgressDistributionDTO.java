package com.ctf.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressDistributionDTO {
    private String range;
    private Integer count;
    private Double percentage;
}
