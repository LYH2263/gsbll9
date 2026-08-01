package com.ctf.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirstBlood {
    private Integer id;
    private Integer questionId;
    private Integer userId;
    private Integer contestUserId;
    private Integer bonusAwarded;
    private LocalDateTime achievedAt;
}
