package com.ctf.mapper;

import com.ctf.entity.ContestUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContestUserMapper {
    ContestUser selectById(@Param("id") Integer id);
    ContestUser selectByUserId(@Param("userId") Integer userId);
    List<ContestUser> selectAll();
    List<ContestUser> selectBySubmitted(@Param("submitted") Boolean submitted);
    Integer insert(ContestUser contestUser);
    Integer update(ContestUser contestUser);
    Integer incrementScore(@Param("id") Integer id, @Param("points") Integer points);
    Integer delete(@Param("id") Integer id);
}
