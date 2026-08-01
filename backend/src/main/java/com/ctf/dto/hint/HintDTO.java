package com.ctf.dto.hint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HintDTO {
    private Integer id;
    private Integer questionId;
    private Integer hintNumber;
    private String content;
    private BigDecimal penalty;
    private Boolean isActive;
    private Boolean unlocked;
}
