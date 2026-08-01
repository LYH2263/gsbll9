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
    Integer countCorrectByQuestionExcludeUser(
            @Param("questionId") Integer questionId,
            @Param("excludeContestUserId") Integer excludeContestUserId);
    Integer countCorrectByQuestionId(@Param("questionId") Integer questionId);
    Integer countAllCorrect();
    Integer insert(Submission submission);
    Integer update(Submission submission);
    Integer delete(@Param("id") Integer id);
}
