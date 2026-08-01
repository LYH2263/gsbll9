package com.ctf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankingItem {
    private Integer rank;
    private String studentId;
    private String fullName;
    private Integer score;
    private Long useTime; // 用时（秒）
    private Boolean submitted;
    private LocalDateTime submitTime;
}
