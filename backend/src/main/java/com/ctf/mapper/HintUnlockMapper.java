package com.ctf.mapper;

import com.ctf.entity.HintUnlock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HintUnlockMapper {
    HintUnlock selectById(@Param("id") Integer id);
    List<HintUnlock> selectByContestUserId(@Param("contestUserId") Integer contestUserId);
    HintUnlock selectByContestUserAndHint(@Param("contestUserId") Integer contestUserId, @Param("hintId") Integer hintId);
    Integer insert(HintUnlock hintUnlock);
    Integer delete(@Param("id") Integer id);
    Integer deleteByContestUserId(@Param("contestUserId") Integer contestUserId);
}
