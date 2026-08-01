package com.ctf.mapper;

import com.ctf.dto.statistics.CategoryStatisticsDTO;
import com.ctf.dto.statistics.QuestionStatisticsDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface StatisticsMapper {
    
    Integer countTotalParticipants();
    
    Integer countStartedUsers();
    
    Integer countSubmittedUsers();
    
    List<QuestionStatisticsDTO> getQuestionStatistics();
    
    List<CategoryStatisticsDTO> getCategoryStatistics();
    
    List<Map<String, Object>> getProgressDistribution();
}
