package com.ctf.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestUser {
    private Integer id;
    private Integer userId;
    @JsonIgnore
    private String selectedQuestions; // JSON array of question IDs
    private Integer currentQuestionId;
    private Integer totalScore;
    private Boolean submitted;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
