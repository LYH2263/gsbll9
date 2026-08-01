package com.ctf.mapper;

import com.ctf.entity.Submission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubmissionMapper {
    Submission selectById(@Param("id") Integer id);
    Submission selectByContestUserAndQuestion(
            @Param("contestUserId") Integer contestUserId,
            @Param("questionId") Integer questionId);
    List<Submission> selectByContestUserId(@Param("contestUserId") Integer contestUserId);
    /**
     * 全场维度统计某题已被正确解出的次数（B1 的 solve_count 数据源，§7）。
     */
    int countCorrectByQuestionId(@Param("questionId") Integer questionId);
    /**
     * 分题统计全场正确解出次数（§12 概览现场聚合用，D1 不落冗余表）。
     */
    List<com.ctf.dto.scoring.QuestionSolveCount> countCorrectGroupByQuestion();
    Integer insert(Submission submission);
    Integer update(Submission submission);
    Integer delete(@Param("id") Integer id);
}
