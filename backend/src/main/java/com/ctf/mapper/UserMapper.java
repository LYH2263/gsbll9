package com.ctf.mapper;

import com.ctf.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    User selectById(@Param("id") Integer id);
    User selectByStudentId(@Param("studentId") String studentId);
    List<User> selectAll();
    Integer insert(User user);
    Integer update(User user);
    Integer delete(@Param("id") Integer id);
}
