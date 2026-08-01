package com.ctf.service;

import com.ctf.dto.statistics.CategoryStatisticsDTO;
import com.ctf.dto.statistics.ProgressDistributionDTO;
import com.ctf.dto.statistics.QuestionStatisticsDTO;
import com.ctf.dto.statistics.StatisticsDTO;
import com.ctf.dto.statistics.UserStatisticsDTO;
import com.ctf.entity.Category;
import com.ctf.entity.Question;
import com.ctf.mapper.CategoryMapper;
import com.ctf.mapper.QuestionMapper;
import com.ctf.mapper.StatisticsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AdminService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private StatisticsMapper statisticsMapper;

    // Category Management
    public List<Category> getAllCategories() {
        return categoryMapper.selectAll();
    }

    public Category getCategoryById(Integer id) {
        return categoryMapper.selectById(id);
    }

    public void addCategory(Category category) {
        categoryMapper.insert(category);
        log.info("Category added: id={}, name={}", category.getId(), category.getName());
    }

    public void updateCategory(Category category) {
        categoryMapper.update(category);
        log.info("Category updated: id={}, name={}", category.getId(), category.getName());
    }

    public void deleteCategory(Integer id) {
        categoryMapper.delete(id);
        log.info("Category deleted: id={}", id);
    }

    // Question Management
    public List<Question> getAllQuestions() {
        return questionMapper.selectAll();
    }

    public List<Question> getQuestionsByCategory(Integer categoryId) {
        return questionMapper.selectByCategoryId(categoryId);
    }

    public Question getQuestionById(Integer id) {
        return questionMapper.selectById(id);
    }

    public void addQuestion(Question question) {
        questionMapper.insert(question);
        log.info("Question added: id={}, title={}", question.getId(), question.getTitle());
    }

    public void updateQuestion(Question question) {
        questionMapper.update(question);
        log.info("Question updated: id={}, title={}", question.getId(), question.getTitle());
    }

    public void deleteQuestion(Integer id) {
        questionMapper.delete(id);
        log.info("Question deleted: id={}", id);
    }

    // Statistics
    public StatisticsDTO getStatistics() {
        log.info("=== Loading statistics ===");
        
        StatisticsDTO statistics = new StatisticsDTO();
        
        UserStatisticsDTO userStats = new UserStatisticsDTO();
        Integer totalParticipants = statisticsMapper.countTotalParticipants();
        Integer startedCount = statisticsMapper.countStartedUsers();
        Integer submittedCount = statisticsMapper.countSubmittedUsers();
        
        userStats.setTotalParticipants(totalParticipants);
        userStats.setStartedCount(startedCount);
        userStats.setSubmittedCount(submittedCount);
        statistics.setUserStatistics(userStats);
        
        log.info("User statistics - total: {}, started: {}, submitted: {}", 
                totalParticipants, startedCount, submittedCount);
        
        List<QuestionStatisticsDTO> questionStats = statisticsMapper.getQuestionStatistics();
        statistics.setQuestionStatistics(questionStats);
        log.info("Question statistics - {} questions loaded", questionStats.size());
        for (QuestionStatisticsDTO q : questionStats) {
            log.info("  Question {}: attemptCount={}, correctCount={}, correctRate={}", 
                    q.getQuestionId(), q.getAttemptCount(), q.getCorrectCount(), q.getCorrectRate());
        }
        
        List<CategoryStatisticsDTO> categoryStats = statisticsMapper.getCategoryStatistics();
        statistics.setCategoryStatistics(categoryStats);
        log.info("Category statistics - {} categories loaded", categoryStats.size());
        for (CategoryStatisticsDTO c : categoryStats) {
            log.info("  Category {}: totalQuestions={}, totalPoints={}, averageScore={}", 
                    c.getCategoryName(), c.getTotalQuestions(), c.getTotalPoints(), c.getAverageScore());
        }
        
        List<ProgressDistributionDTO> progressDist = calculateProgressDistribution();
        statistics.setProgressDistribution(progressDist);
        log.info("Progress distribution - {} ranges loaded", progressDist.size());
        for (ProgressDistributionDTO p : progressDist) {
            log.info("  Range {}: count={}, percentage={}", 
                    p.getRange(), p.getCount(), p.getPercentage());
        }
        
        statistics.setUpdatedAt(LocalDateTime.now());
        
        log.info("=== Statistics loaded successfully ===");
        return statistics;
    }
    
    private List<ProgressDistributionDTO> calculateProgressDistribution() {
        List<Map<String, Object>> rawData = statisticsMapper.getProgressDistribution();
        List<ProgressDistributionDTO> result = new ArrayList<>();
        
        int total = rawData.stream()
                .mapToInt(m -> ((Number) m.get("count")).intValue())
                .sum();
        
        String[] expectedRanges = {"0题", "1-3题", "4-6题", "7-10题", "10题以上"};
        
        for (String range : expectedRanges) {
            ProgressDistributionDTO dto = new ProgressDistributionDTO();
            dto.setRange(range);
            
            int count = 0;
            for (Map<String, Object> row : rawData) {
                if (range.equals(row.get("range"))) {
                    count = ((Number) row.get("count")).intValue();
                    break;
                }
            }
            
            dto.setCount(count);
            dto.setPercentage(total > 0 ? Math.round(count * 100.0 / total * 100.0) / 100.0 : 0.0);
            result.add(dto);
        }
        
        return result;
    }
}
