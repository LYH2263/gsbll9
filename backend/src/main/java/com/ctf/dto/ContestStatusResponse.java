package com.ctf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestStatusResponse {
    private String status; // "preparing", "running", "finished"
    private String message;
    private Long remainingTime; // 毫秒
    private Boolean canStartContest;
    private Boolean contestActive;
}
