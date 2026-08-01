package com.ctf.service;

import com.ctf.entity.ContestUser;
import com.ctf.entity.Question;
import com.ctf.entity.Submission;
import com.ctf.mapper.CategoryMapper;
import com.ctf.mapper.ContestUserMapper;
import com.ctf.mapper.QuestionMapper;
import com.ctf.mapper.SubmissionMapper;
import com.ctf.mapper.UserMapper;
import com.ctf.util.ContestTimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContestService.submitAnswer 单元测试")
class ContestServiceTest {

    @Mock
    private ContestUserMapper contestUserMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private SubmissionMapper submissionMapper;

    @Mock
    private ScoringService scoringService;

    @Mock
    private ContestTimeUtil contestTimeUtil;

    @InjectMocks
    private ContestService contestService;

    private Integer userId;
    private Integer questionId;
    private Integer contestUserId;
    private String correctAnswer;
    private String wrongAnswer;

    @BeforeEach
    void setUp() {
        userId = 1;
        questionId = 100;
        contestUserId = 1000;
        correctAnswer = "CTF{test_flag_123}";
        wrongAnswer = "wrong_answer";
    }

    @Test
    @DisplayName("首次提交正确答案 → 委托ScoringService按基础分入账并记录一血")
    void submitCorrectAnswerFirstTime_shouldDelegateScoring() {
        ContestUser contestUser = createContestUser(0);
        Question question = createQuestion(correctAnswer, 100);

        when(contestUserMapper.selectByUserId(userId)).thenReturn(contestUser);
        when(questionMapper.selectById(questionId)).thenReturn(question);
        when(submissionMapper.selectByContestUserAndQuestion(contestUserId, questionId)).thenReturn(null);

        boolean result = contestService.submitAnswer(userId, questionId, correctAnswer);

        assertTrue(result);
        verify(submissionMapper).insert(any(Submission.class));
        verify(scoringService).awardSolve(eq(contestUserId), eq(questionId), eq(userId), eq(100));
        verify(contestUserMapper, never()).update(contestUser);
    }

    @Test
    @DisplayName("首次提交错误答案 → 不触发计分")
    void submitWrongAnswerFirstTime_shouldNotTriggerScoring() {
        ContestUser contestUser = createContestUser(50);
        Question question = createQuestion(correctAnswer, 100);

        when(contestUserMapper.selectByUserId(userId)).thenReturn(contestUser);
        when(questionMapper.selectById(questionId)).thenReturn(question);
        when(submissionMapper.selectByContestUserAndQuestion(contestUserId, questionId)).thenReturn(null);

        boolean result = contestService.submitAnswer(userId, questionId, wrongAnswer);

        assertFalse(result);
        verify(submissionMapper).insert(any(Submission.class));
        verify(scoringService, never()).awardSolve(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("已答对后再次提交正确答案 → 不重复计分")
    void submitCorrectAnswerAgainAfterAlreadyCorrect_shouldNotDuplicateScore() {
        ContestUser contestUser = createContestUser(100);
        Question question = createQuestion(correctAnswer, 100);
        Submission existingSubmission = createSubmission(true);

        when(contestUserMapper.selectByUserId(userId)).thenReturn(contestUser);
        when(questionMapper.selectById(questionId)).thenReturn(question);
        when(submissionMapper.selectByContestUserAndQuestion(contestUserId, questionId)).thenReturn(existingSubmission);

        boolean result = contestService.submitAnswer(userId, questionId, correctAnswer);

        assertTrue(result);
        verify(submissionMapper).update(existingSubmission);
        verify(scoringService, never()).awardSolve(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("用户不存在时 → 返回 false，不触发计分")
    void whenUserNotFound_shouldReturnFalse() {
        when(contestUserMapper.selectByUserId(userId)).thenReturn(null);

        boolean result = contestService.submitAnswer(userId, questionId, correctAnswer);

        assertFalse(result);
        verify(questionMapper, never()).selectById(any());
        verify(submissionMapper, never()).insert(any());
        verify(submissionMapper, never()).update(any());
        verify(scoringService, never()).awardSolve(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("题目不存在时 → 返回 false，不触发计分")
    void whenQuestionNotFound_shouldReturnFalse() {
        ContestUser contestUser = createContestUser(0);

        when(contestUserMapper.selectByUserId(userId)).thenReturn(contestUser);
        when(questionMapper.selectById(questionId)).thenReturn(null);

        boolean result = contestService.submitAnswer(userId, questionId, correctAnswer);

        assertFalse(result);
        verify(submissionMapper, never()).insert(any());
        verify(submissionMapper, never()).update(any());
        verify(scoringService, never()).awardSolve(anyInt(), anyInt(), anyInt(), anyInt());
    }

    private ContestUser createContestUser(int initialScore) {
        ContestUser contestUser = new ContestUser();
        contestUser.setId(contestUserId);
        contestUser.setUserId(userId);
        contestUser.setTotalScore(initialScore);
        return contestUser;
    }

    private Question createQuestion(String flag, int points) {
        Question question = new Question();
        question.setId(questionId);
        question.setFlag(flag);
        question.setPoints(points);
        return question;
    }

    private Submission createSubmission(boolean isCorrect) {
        Submission submission = new Submission();
        submission.setId(1);
        submission.setContestUserId(contestUserId);
        submission.setQuestionId(questionId);
        submission.setIsCorrect(isCorrect);
        return submission;
    }
}
