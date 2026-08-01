package com.ctf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContestConfigDTO {
    private String startTime;
    private String readyTime;
    private String endTime;
    private String resultsTime;
}
