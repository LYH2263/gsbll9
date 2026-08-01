package com.ctf.mapper;

import com.ctf.dto.scoring.FirstBloodItem;
import com.ctf.entity.FirstBlood;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FirstBloodMapper {
    FirstBlood selectById(@Param("id") Integer id);
    FirstBlood selectByQuestionId(@Param("questionId") Integer questionId);
    /**
     * 依赖 unique_first_blood_question 唯一约束幂等写入。
     * @return 1 表示本次写入成功（达成一血）；0 表示该题已有一血，本次被忽略
     */
    Integer insertIgnore(FirstBlood firstBlood);
    List<FirstBloodItem> selectAllWithDetail();
    /**
     * 已产生一血的题目数（§12 指标 questions_with_first_blood）。
     */
    int countAll();
    /**
     * 已发放的一血奖金合计（§12 指标 total_first_blood_bonus_awarded），无记录时返回 0。
     */
    int sumBonusAwarded();
}
