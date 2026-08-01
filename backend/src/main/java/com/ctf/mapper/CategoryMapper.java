package com.ctf.mapper;

import com.ctf.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {
    Category selectById(@Param("id") Integer id);
    List<Category> selectAll();
    List<Category> selectActive();
    Integer insert(Category category);
    Integer update(Category category);
    Integer delete(@Param("id") Integer id);
}
