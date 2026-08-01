package com.ctf.mapper;

import com.ctf.entity.Hint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HintMapper {
    Hint selectById(@Param("id") Integer id);
    List<Hint> selectByQuestionId(@Param("questionId") Integer questionId);
    List<Hint> selectActiveByQuestionId(@Param("questionId") Integer questionId);
    Hint selectByQuestionAndNumber(@Param("questionId") Integer questionId, @Param("hintNumber") Integer hintNumber);
    Integer insert(Hint hint);
    Integer update(Hint hint);
    Integer delete(@Param("id") Integer id);
    Integer deleteByQuestionId(@Param("questionId") Integer questionId);
}
