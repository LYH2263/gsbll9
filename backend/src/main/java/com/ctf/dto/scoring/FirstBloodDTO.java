package com.ctf.dto.scoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirstBloodDTO {
    private Integer id;
    private Integer questionId;
    private String questionTitle;
    private Integer userId;
    private String studentId;
    private String fullName;
    private Integer contestUserId;
    private LocalDateTime achievedAt;
}
