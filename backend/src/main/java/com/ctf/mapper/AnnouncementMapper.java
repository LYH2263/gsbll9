package com.ctf.mapper;

import com.ctf.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnnouncementMapper {
    Announcement selectById(@Param("id") Integer id);
    List<Announcement> selectAll();
    List<Announcement> selectActive();
    Announcement selectLatestActive();
    Integer insert(Announcement announcement);
    Integer update(Announcement announcement);
    Integer delete(@Param("id") Integer id);
    Integer activate(@Param("id") Integer id);
    Integer deactivate(@Param("id") Integer id);
    Integer deactivateAll();
}
