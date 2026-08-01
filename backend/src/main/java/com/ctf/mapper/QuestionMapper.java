package com.ctf.mapper;

import com.ctf.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionMapper {
    Question selectById(@Param("id") Integer id);
    List<Question> selectAll();
    List<Question> selectByCategoryId(@Param("categoryId") Integer categoryId);
    List<Question> selectActiveByCategoryId(@Param("categoryId") Integer categoryId);
    Question selectRandomByCategory(@Param("categoryId") Integer categoryId);
    Integer insert(Question question);
    Integer update(Question question);
    Integer delete(@Param("id") Integer id);
    Integer countActiveByCategory(@Param("categoryId") Integer categoryId);
}
