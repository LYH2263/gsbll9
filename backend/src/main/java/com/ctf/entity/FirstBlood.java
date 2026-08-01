package com.ctf.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 一血记录实体（动态计分模块 P0）。
 * 表示某题在全场范围内第一次被判定为正确提交的事件，一题至多一条（库级唯一约束在 question_id 上）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirstBlood {
    private Integer id;
    private Integer questionId;
    private Integer userId;
    private Integer contestUserId;
    private Integer awardedBonus;
    private LocalDateTime achievedAt;
}
