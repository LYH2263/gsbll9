package com.ctf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirstBloodRecordDTO {
    private Integer questionId;
    private String questionTitle;
    private Integer categoryId;
    private String categoryName;
    private Integer points;
    private Integer userId;
    private String studentId;
    private String fullName;
    private LocalDateTime achievedAt;
}
