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
    /**
     * 原子累加总分（计分模块专用，避免并发 read-modify-write 丢失更新）。
     */
    Integer addScore(@Param("id") Integer id, @Param("delta") Integer delta);
    Integer delete(@Param("id") Integer id);
}
