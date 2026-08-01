package com.ctf.mapper;

import com.ctf.entity.FirstBlood;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 一血记录 Mapper（动态计分模块 P0）。
 * insert 依赖表上的 question_id 唯一约束保证「一题至多一条」，不靠应用层 if 兜底。
 */
@Mapper
public interface FirstBloodMapper {
    FirstBlood selectByQuestionId(@Param("questionId") Integer questionId);

    List<FirstBlood> selectAll();

    int countByQuestionId(@Param("questionId") Integer questionId);

    Integer insert(FirstBlood firstBlood);

    /** §12.2：已产生一血的题目数。 */
    long countQuestionsWithFirstBlood();

    /** §12.5：已发放的一血奖金合计（各行 awarded_bonus 求和，旧数据 NULL 记 0）。 */
    long sumAwardedBonus();
}
