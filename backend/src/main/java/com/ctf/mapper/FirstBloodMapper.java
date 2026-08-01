package com.ctf.mapper;

import com.ctf.dto.FirstBloodRecordDTO;
import com.ctf.entity.FirstBlood;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FirstBloodMapper {
    FirstBlood selectByQuestionId(@Param("questionId") Integer questionId);

    Integer insert(FirstBlood firstBlood);

    List<FirstBloodRecordDTO> selectAllRecords();

    Integer countAll();

    Integer sumBonusAwarded();
}
