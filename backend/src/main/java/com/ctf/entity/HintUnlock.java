package com.ctf.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HintUnlock {
    private Integer id;
    private Integer contestUserId;
    private Integer hintId;
    private LocalDateTime unlockedAt;
}
