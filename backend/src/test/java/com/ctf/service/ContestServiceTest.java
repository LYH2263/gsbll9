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
    private ContestTimeUtil contestTimeUtil;

    @Mock
    private ScoringService scoringService;

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
    @DisplayName("首次提交正确答案 → 委托计分模块入账")
    void submitCorrectAnswerFirstTime_shouldDelegateToScoringService() {
        ContestUser contestUser = createContestUser(0);
        Question question = createQuestion(correctAnswer, 100);

        when(contestUserMapper.selectByUserId(userId)).thenReturn(contestUser);
        when(questionMapper.selectById(questionId)).thenReturn(question);
        when(submissionMapper.selectByContestUserAndQuestion(contestUserId, questionId)).thenReturn(null);

        boolean result = contestService.submitAnswer(userId, questionId, correctAnswer);

        assertTrue(result);
        verify(submissionMapper).insert(any(Submission.class));
        verify(scoringService).recordCorrectSolve(contestUser, question);
    }

    @Test
    @DisplayName("首次提交错误答案 → 分数不变")
    void submitWrongAnswerFirstTime_shouldNotChangeScore() {
        ContestUser contestUser = createContestUser(50);
        Question question = createQuestion(correctAnswer, 100);

        when(contestUserMapper.selectByUserId(userId)).thenReturn(contestUser);
        when(questionMapper.selectById(questionId)).thenReturn(question);
        when(submissionMapper.selectByContestUserAndQuestion(contestUserId, questionId)).thenReturn(null);

        boolean result = contestService.submitAnswer(userId, questionId, wrongAnswer);

        assertFalse(result);
        verify(submissionMapper).insert(any(Submission.class));
        verify(scoringService, never()).recordCorrectSolve(any(), any());
        assertEquals(50, contestUser.getTotalScore());
    }

    @Test
    @DisplayName("已答对后再次提交正确答案 → 分数不重复增加")
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
        verify(scoringService, never()).recordCorrectSolve(any(), any());
        assertEquals(100, contestUser.getTotalScore());
    }

    @Test
    @DisplayName("已答对后提交错误答案 → 正确行不被降级，且不重复入账")
    void submitWrongAnswerAfterAlreadyCorrect_shouldNotDowngradeNorRescore() {
        ContestUser contestUser = createContestUser(100);
        Question question = createQuestion(correctAnswer, 100);
        Submission existingSubmission = createSubmission(true);

        when(contestUserMapper.selectByUserId(userId)).thenReturn(contestUser);
        when(questionMapper.selectById(questionId)).thenReturn(question);
        when(submissionMapper.selectByContestUserAndQuestion(contestUserId, questionId)).thenReturn(existingSubmission);

        boolean result = contestService.submitAnswer(userId, questionId, wrongAnswer);

        assertFalse(result);
        verify(submissionMapper).update(existingSubmission);
        // 正确状态保持 true，后续再答对也不会触发第二次入账
        assertTrue(existingSubmission.getIsCorrect());
        verify(scoringService, never()).recordCorrectSolve(any(), any());
        assertEquals(100, contestUser.getTotalScore());
    }

    @Test
    @DisplayName("已答对→答错→再答对 → 全程至多入账一次")
    void resubmitCorrectAfterDowngrade_shouldNotRescore() {
        ContestUser contestUser = createContestUser(100);
        Question question = createQuestion(correctAnswer, 100);
        Submission existingSubmission = createSubmission(true);

        when(contestUserMapper.selectByUserId(userId)).thenReturn(contestUser);
        when(questionMapper.selectById(questionId)).thenReturn(question);
        when(submissionMapper.selectByContestUserAndQuestion(contestUserId, questionId)).thenReturn(existingSubmission);

        // 先答错（行保持正确），再答对：alreadyAnsweredCorrectly 始终为 true
        contestService.submitAnswer(userId, questionId, wrongAnswer);
        boolean result = contestService.submitAnswer(userId, questionId, correctAnswer);

        assertTrue(result);
        verify(scoringService, never()).recordCorrectSolve(any(), any());
        assertEquals(100, contestUser.getTotalScore());
    }

    @Test
    @DisplayName("用户不存在时 → 返回 false，不抛异常")
    void whenUserNotFound_shouldReturnFalse() {
        when(contestUserMapper.selectByUserId(userId)).thenReturn(null);

        boolean result = contestService.submitAnswer(userId, questionId, correctAnswer);

        assertFalse(result);
        verify(questionMapper, never()).selectById(any());
        verify(submissionMapper, never()).insert(any());
        verify(submissionMapper, never()).update(any());
        verify(contestUserMapper, never()).update(any());
    }

    @Test
    @DisplayName("题目不存在时 → 返回 false，不抛异常")
    void whenQuestionNotFound_shouldReturnFalse() {
        ContestUser contestUser = createContestUser(0);

        when(contestUserMapper.selectByUserId(userId)).thenReturn(contestUser);
        when(questionMapper.selectById(questionId)).thenReturn(null);

        boolean result = contestService.submitAnswer(userId, questionId, correctAnswer);

        assertFalse(result);
        verify(submissionMapper, never()).insert(any());
        verify(submissionMapper, never()).update(any());
        verify(contestUserMapper, never()).update(any());
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
