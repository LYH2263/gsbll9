package com.ctf.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Submission {
    private Integer id;
    private Integer contestUserId;
    private Integer questionId;
    private String userAnswer;
    private Boolean isCorrect;
    private LocalDateTime submittedAt;
}
