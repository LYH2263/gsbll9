package com.ctf.dto.scoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端一血只读展示 DTO（动态计分模块 P0，§6）。
 * 携带题目标识、用户标识与达成时间，字段 camelCase，与前端一一对应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirstBloodDTO {
    private Integer questionId;
    private String questionTitle;
    private Integer userId;
    private String studentId;
    private String fullName;
    private LocalDateTime achievedAt;
}
