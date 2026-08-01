package com.ctf.service;

import com.ctf.dto.RankingItem;
import com.ctf.entity.Category;
import com.ctf.entity.ContestUser;
import com.ctf.entity.Question;
import com.ctf.entity.Submission;
import com.ctf.entity.User;
import com.ctf.mapper.CategoryMapper;
import com.ctf.mapper.ContestUserMapper;
import com.ctf.mapper.QuestionMapper;
import com.ctf.mapper.SubmissionMapper;
import com.ctf.mapper.UserMapper;
import com.ctf.util.ContestTimeUtil;
import com.ctf.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
public class ContestService {

    @Autowired
    private ContestUserMapper contestUserMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private ContestTimeUtil contestTimeUtil;

    public void initializeContestForUser(Integer userId) {
        log.info("Initializing contest for user: userId={}", userId);

        ContestUser existing = contestUserMapper.selectByUserId(userId);
        
        if (existing != null) {
            if (isContestRecordValid(existing)) {
                log.info("User has valid contest record, skipping re-initialization: userId={}", userId);
                return;
            } else {
                log.warn("Found invalid/corrupted contest record for user: userId={}, will reset and re-initialize", userId);
                log.warn("Corrupted record details: selectedQuestions={}, currentQuestionId={}, submitted={}",
                        existing.getSelectedQuestions(), existing.getCurrentQuestionId(), existing.getSubmitted());
                contestUserMapper.delete(existing.getId());
                log.info("Deleted corrupted contest record for user: userId={}", userId);
            }
        }

        List<Integer> selectedQuestionIds = selectRandomQuestions();

        if (selectedQuestionIds.isEmpty()) {
            log.error("No questions available for contest initialization! userId={}", userId);
            log.error("Please check: 1) Are there any active categories? 2) Do categories have active questions?");
            throw new IllegalStateException("No questions available for contest. Please contact administrator.");
        }

        ContestUser contestUser = new ContestUser();
        contestUser.setUserId(userId);
        contestUser.setSelectedQuestions(JsonUtil.toJson(selectedQuestionIds));
        contestUser.setCurrentQuestionId(selectedQuestionIds.get(0));
        contestUser.setTotalScore(0);
        contestUser.setSubmitted(false);
        contestUser.setStartTime(LocalDateTime.now());

        contestUserMapper.insert(contestUser);
        log.info("Contest initialized successfully for user: userId={}, questionCount={}, firstQuestionId={}", 
                userId, selectedQuestionIds.size(), selectedQuestionIds.get(0));
    }

    private boolean isContestRecordValid(ContestUser contestUser) {
        if (contestUser == null) {
            return false;
        }
        
        if (Boolean.TRUE.equals(contestUser.getSubmitted())) {
            log.info("Contest record is valid (already submitted): userId={}", contestUser.getUserId());
            return true;
        }
        
        List<Integer> selectedQuestions = getSelectedQuestions(contestUser);
        if (selectedQuestions.isEmpty()) {
            log.warn("Contest record is invalid: selectedQuestions is empty for userId={}", contestUser.getUserId());
            return false;
        }
        
        if (contestUser.getCurrentQuestionId() == null) {
            log.warn("Contest record is invalid: currentQuestionId is null for userId={}", contestUser.getUserId());
            return false;
        }
        
        boolean currentQuestionExists = selectedQuestions.contains(contestUser.getCurrentQuestionId());
        if (!currentQuestionExists) {
            log.warn("Contest record is invalid: currentQuestionId={} not in selectedQuestions for userId={}",
                    contestUser.getCurrentQuestionId(), contestUser.getUserId());
            return false;
        }
        
        log.info("Contest record is valid for userId={}: {} questions selected, currentQuestionId={}",
                contestUser.getUserId(), selectedQuestions.size(), contestUser.getCurrentQuestionId());
        return true;
    }

    private List<Integer> selectRandomQuestions() {
        List<Integer> selectedQuestions = new ArrayList<>();
        try {
            List<Category> activeCategories = categoryMapper.selectActive();
            log.info("Found {} active categories for question selection", activeCategories.size());
            
            if (activeCategories.isEmpty()) {
                log.warn("No active categories found! Checking all categories...");
                List<Category> allCategories = categoryMapper.selectAll();
                log.info("Total categories in database: {}", allCategories.size());
                
                for (Category cat : allCategories) {
                    log.info("Category: id={}, name={}, isActive={}", cat.getId(), cat.getName(), cat.getIsActive());
                }
                return selectedQuestions;
            }
            
            for (Category category : activeCategories) {
                log.info("Selecting random question from category: id={}, name={}", category.getId(), category.getName());
                Question q = questionMapper.selectRandomByCategory(category.getId());
                if (q != null) {
                    selectedQuestions.add(q.getId());
                    log.info("Selected question: id={}, title={} from category {}", q.getId(), q.getTitle(), category.getName());
                } else {
                    log.warn("No active questions found in category: id={}, name={}", category.getId(), category.getName());
                }
            }
            
            log.info("Total questions selected: {}", selectedQuestions.size());
        } catch (Exception e) {
            log.error("Error selecting random questions", e);
        }
        return selectedQuestions;
    }

    public ContestUser getContestUserByUserId(Integer userId) {
        return contestUserMapper.selectByUserId(userId);
    }

    public List<Integer> getSelectedQuestions(ContestUser contestUser) {
        if (contestUser.getSelectedQuestions() == null) {
            return new ArrayList<>();
        }
        List<Integer> questions = JsonUtil.fromJsonToList(contestUser.getSelectedQuestions(),
                new TypeReference<List<Integer>>() {});
        return questions != null ? questions : new ArrayList<>();
    }

    public Question getCurrentQuestion(Integer userId) {
        ContestUser contestUser = contestUserMapper.selectByUserId(userId);
        if (contestUser == null || contestUser.getCurrentQuestionId() == null) {
            return null;
        }
        return questionMapper.selectById(contestUser.getCurrentQuestionId());
    }

    public Question getQuestionById(Integer questionId) {
        return questionMapper.selectById(questionId);
    }

    public boolean submitAnswer(Integer userId, Integer questionId, String answer) {
        log.info("Submitting answer: userId={}, questionId={}", userId, questionId);

        ContestUser contestUser = contestUserMapper.selectByUserId(userId);
        if (contestUser == null) {
            log.warn("Contest user not found: userId={}", userId);
            return false;
        }

        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            log.warn("Question not found: questionId={}", questionId);
            return false;
        }

        boolean isCorrect = question.getFlag().trim().equalsIgnoreCase(answer.trim());

        Submission existing = submissionMapper.selectByContestUserAndQuestion(contestUser.getId(), questionId);
        boolean alreadyAnsweredCorrectly = existing != null && existing.getIsCorrect();

        if (existing != null) {
            existing.setUserAnswer(answer);
            existing.setIsCorrect(isCorrect);
            submissionMapper.update(existing);
        } else {
            Submission submission = new Submission();
            submission.setContestUserId(contestUser.getId());
            submission.setQuestionId(questionId);
            submission.setUserAnswer(answer);
            submission.setIsCorrect(isCorrect);
            submissionMapper.insert(submission);
        }

        if (isCorrect && !alreadyAnsweredCorrectly) {
            int score = contestUser.getTotalScore() + question.getPoints();
            contestUser.setTotalScore(score);
            contestUserMapper.update(contestUser);
        }

        log.info("Answer submitted: userId={}, questionId={}, correct={}", userId, questionId, isCorrect);
        return isCorrect;
    }

    public void moveToNextQuestion(Integer userId) {
        ContestUser contestUser = contestUserMapper.selectByUserId(userId);
        if (contestUser == null) {
            return;
        }

        List<Integer> selectedQuestions = getSelectedQuestions(contestUser);
        int currentIndex = selectedQuestions.indexOf(contestUser.getCurrentQuestionId());
        if (currentIndex < selectedQuestions.size() - 1) {
            contestUser.setCurrentQuestionId(selectedQuestions.get(currentIndex + 1));
            contestUserMapper.update(contestUser);
        }
    }

    public void moveToPreviousQuestion(Integer userId) {
        ContestUser contestUser = contestUserMapper.selectByUserId(userId);
        if (contestUser == null) {
            return;
        }

        List<Integer> selectedQuestions = getSelectedQuestions(contestUser);
        int currentIndex = selectedQuestions.indexOf(contestUser.getCurrentQuestionId());
        if (currentIndex > 0) {
            contestUser.setCurrentQuestionId(selectedQuestions.get(currentIndex - 1));
            contestUserMapper.update(contestUser);
        }
    }

    public void jumpToQuestion(Integer userId, Integer questionId) {
        ContestUser contestUser = contestUserMapper.selectByUserId(userId);
        if (contestUser == null) {
            return;
        }

        List<Integer> selectedQuestions = getSelectedQuestions(contestUser);
        if (selectedQuestions.contains(questionId)) {
            contestUser.setCurrentQuestionId(questionId);
            contestUserMapper.update(contestUser);
        }
    }

    public List<RankingItem> getRankings() {
        List<ContestUser> contestUsers = contestUserMapper.selectAll();
        List<RankingItem> rankings = new ArrayList<>();

        for (ContestUser cu : contestUsers) {
            User user = userMapper.selectById(cu.getUserId());
            if (user == null) {
                continue;
            }

            long useTime = 0;
            if (cu.getSubmitTime() != null && cu.getStartTime() != null) {
                useTime = ChronoUnit.SECONDS.between(cu.getStartTime(), cu.getSubmitTime());
            }

            RankingItem item = new RankingItem();
            item.setStudentId(user.getStudentId());
            item.setFullName(user.getFullName());
            item.setScore(cu.getTotalScore());
            item.setUseTime(useTime);
            item.setSubmitted(cu.getSubmitted());
            item.setSubmitTime(cu.getSubmitTime());
            rankings.add(item);
        }

        // 排序：先按分数降序，再按用时升序
        rankings.sort((a, b) -> {
            int scoreCompare = b.getScore().compareTo(a.getScore());
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return a.getUseTime().compareTo(b.getUseTime());
        });

        // 设置排名
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank(i + 1);
        }

        return rankings;
    }

    public void finalizeSubmission(Integer userId) {
        ContestUser contestUser = contestUserMapper.selectByUserId(userId);
        if (contestUser != null) {
            contestUser.setSubmitted(true);
            contestUser.setSubmitTime(LocalDateTime.now());
            contestUserMapper.update(contestUser);
            log.info("Contest submission finalized: userId={}", userId);
        }
    }

    public int cleanupCorruptedContestRecords() {
        log.info("Starting cleanup of corrupted contest records...");
        
        List<ContestUser> allContestUsers = contestUserMapper.selectAll();
        int cleanedCount = 0;
        
        for (ContestUser cu : allContestUsers) {
            if (Boolean.TRUE.equals(cu.getSubmitted())) {
                continue;
            }
            
            if (!isContestRecordValid(cu)) {
                log.warn("Cleaning up corrupted record: contestUserId={}, userId={}, selectedQuestions={}, currentQuestionId={}",
                        cu.getId(), cu.getUserId(), cu.getSelectedQuestions(), cu.getCurrentQuestionId());
                contestUserMapper.delete(cu.getId());
                cleanedCount++;
            }
        }
        
        log.info("Cleanup completed. Removed {} corrupted contest records", cleanedCount);
        return cleanedCount;
    }
}
