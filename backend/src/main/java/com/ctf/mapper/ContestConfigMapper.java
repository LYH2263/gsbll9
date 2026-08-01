package com.ctf.mapper;

import com.ctf.entity.ContestConfig;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ContestConfigMapper {
    
    @Select("SELECT * FROM contest_config WHERE config_key = #{key}")
    ContestConfig selectByKey(String key);
    
    @Insert("INSERT INTO contest_config (config_key, config_value) VALUES (#{configKey}, #{configValue}) " +
            "ON DUPLICATE KEY UPDATE config_value = #{configValue}, updated_at = CURRENT_TIMESTAMP")
    int upsert(ContestConfig config);
    
    @Update("UPDATE contest_config SET config_value = #{value}, updated_at = CURRENT_TIMESTAMP WHERE config_key = #{key}")
    int updateByKey(@Param("key") String key, @Param("value") String value);
}
