package com.ctf.mapper;

import com.ctf.entity.Submission;
import com.ctf.dto.scoring.QuestionSolveCount;
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
     * 统计某题「除指定参赛者之外」已正确解出的人次。
     * 因 submissions 以 (contest_user_id, question_id) 唯一，排除当前参赛者即得「本次入账前」的 solve_count（条款 B1）。
     */
    int countCorrectByQuestionExcludingUser(@Param("questionId") Integer questionId,
                                            @Param("contestUserId") Integer contestUserId);
    /** §12.1：全场正确解出总次数（按解出事件计，非题目数）。 */
    long countTotalCorrectSolves();
    /**
     * 每题的全场正确解出次数（含题目基础分 points），仅返回至少被解出一次的启用题目。
     * 供概览按 B1 公式现场计算「当前分」，判定 §12.3/§12.4（D1：不建汇总表）。
     */
    List<QuestionSolveCount> selectSolveCountsPerQuestion();
    Integer insert(Submission submission);
    Integer update(Submission submission);
    Integer delete(@Param("id") Integer id);
}
