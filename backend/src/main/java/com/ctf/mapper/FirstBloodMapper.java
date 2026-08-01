package com.ctf.mapper;

import com.ctf.dto.scoring.FirstBloodDTO;
import com.ctf.dto.scoring.QuestionSolveStat;
import com.ctf.entity.FirstBlood;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FirstBloodMapper {
    FirstBlood selectByQuestionId(@Param("questionId") Integer questionId);
    List<FirstBloodDTO> selectAllWithDetails();
    Integer countAll();
    Integer sumBonusPoints();
    List<QuestionSolveStat> selectActiveQuestionSolveStats();
    Integer insert(FirstBlood firstBlood);
}
