package com.ctf.service;

import com.ctf.dto.scoring.QuestionSolveCount;
import com.ctf.dto.scoring.ScoringConfigDTO;
import com.ctf.dto.scoring.ScoringOverviewDTO;
import com.ctf.entity.ContestConfig;
import com.ctf.entity.ContestUser;
import com.ctf.entity.FirstBlood;
import com.ctf.entity.Question;
import com.ctf.mapper.ContestConfigMapper;
import com.ctf.mapper.ContestUserMapper;
import com.ctf.mapper.FirstBloodMapper;
import com.ctf.mapper.QuestionMapper;
import com.ctf.mapper.SubmissionMapper;
import com.ctf.util.ContestTimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoringService 单元测试（B1-B5 / C1-C2 / §12）")
class ScoringServiceTest {

    @Mock
    private FirstBloodMapper firstBloodMapper;

    @Mock
    private ContestUserMapper contestUserMapper;

    @Mock
    private SubmissionMapper submissionMapper;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private ContestConfigMapper contestConfigMapper;

    @Mock
    private ContestTimeUtil contestTimeUtil;

    @InjectMocks
    private ScoringService scoringService;

    private Integer userId;
    private Integer questionId;
    private Integer contestUserId;

    @BeforeEach
    void setUp() {
        userId = 1;
        questionId = 100;
        contestUserId = 1000;
        // 默认赛中；C1/C2 用例在各自测试中覆盖该 stub
        lenient().when(contestTimeUtil.getCurrentStatus())
                .thenReturn(ContestTimeUtil.ContestStatus.RUNNING);
    }

    // ========== P1：B1/B3/B4 回归 ==========

    @Test
    @DisplayName("第 1 次解出 → 一血落库成功，默认配置下入账基础分")
    void firstSolveGlobally_shouldInsertFirstBloodAndAwardBasePoints() {
        ContestUser contestUser = createContestUser(50);
        Question question = createQuestion(100);

        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(0);
        when(contestConfigMapper.selectByKey(anyString())).thenReturn(null);
        when(firstBloodMapper.insertIgnore(any(FirstBlood.class))).thenReturn(1);

        int awarded = scoringService.recordCorrectSolve(contestUser, question);

        assertEquals(100, awarded);
        assertEquals(150, contestUser.getTotalScore());
        verify(firstBloodMapper).insertIgnore(argThat(fb ->
                fb.getQuestionId().equals(questionId)
                        && fb.getUserId().equals(userId)
                        && fb.getContestUserId().equals(contestUserId)));
        verify(contestUserMapper).addScore(contestUserId, 100);
    }

    @Test
    @DisplayName("第 2 次解出（B1 入账前 solve_count=1）→ 入账 max(60, 100-10×1)=90，无一血奖金")
    void secondSolve_shouldUseSolveCountBeforeThisSolve() {
        ContestUser contestUser = createContestUser(0);
        Question question = createQuestion(100);

        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(1);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_MIN_POINTS)).thenReturn(config("60"));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_DECAY_STEP)).thenReturn(config("10"));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FIRST_BLOOD_BONUS)).thenReturn(null);
        when(firstBloodMapper.insertIgnore(any(FirstBlood.class))).thenReturn(0);

        int awarded = scoringService.recordCorrectSolve(contestUser, question);

        assertEquals(90, awarded);
        verify(contestUserMapper).addScore(contestUserId, 90);
    }

    @Test
    @DisplayName("衰减触底 → 夹逼到 scoring.min_points")
    void decayBelowMinPoints_shouldClampToMinPoints() {
        ContestUser contestUser = createContestUser(0);
        Question question = createQuestion(100);

        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(10);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_MIN_POINTS)).thenReturn(config("60"));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_DECAY_STEP)).thenReturn(config("10"));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FIRST_BLOOD_BONUS)).thenReturn(null);
        when(firstBloodMapper.insertIgnore(any(FirstBlood.class))).thenReturn(0);

        int awarded = scoringService.recordCorrectSolve(contestUser, question);

        assertEquals(60, awarded);
    }

    @Test
    @DisplayName("B4：decay_step=0 时退化为恒等于基础分（再夹逼到 min_points）")
    void zeroDecayStep_shouldDegenerateToBasePoints() {
        ContestUser contestUser = createContestUser(0);
        Question question = createQuestion(100);

        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(5);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_MIN_POINTS)).thenReturn(config("60"));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_DECAY_STEP)).thenReturn(config("0"));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FIRST_BLOOD_BONUS)).thenReturn(null);
        when(firstBloodMapper.insertIgnore(any(FirstBlood.class))).thenReturn(0);

        int awarded = scoringService.recordCorrectSolve(contestUser, question);

        assertEquals(100, awarded);
    }

    @Test
    @DisplayName("基础分低于 min_points 时按 min_points 入账")
    void baseBelowMinPoints_shouldClampToMinPoints() {
        ContestUser contestUser = createContestUser(0);
        Question question = createQuestion(50);

        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(0);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_MIN_POINTS)).thenReturn(config("60"));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_DECAY_STEP)).thenReturn(config("10"));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FIRST_BLOOD_BONUS)).thenReturn(null);
        when(firstBloodMapper.insertIgnore(any(FirstBlood.class))).thenReturn(0);

        int awarded = scoringService.recordCorrectSolve(contestUser, question);

        assertEquals(60, awarded);
    }

    @Test
    @DisplayName("B3：一血命中时奖金随该次入账一并发放（100 + 15 = 115）")
    void firstBloodHit_shouldAddBonusInSameAward() {
        ContestUser contestUser = createContestUser(50);
        Question question = createQuestion(100);

        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(0);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_MIN_POINTS)).thenReturn(config("60"));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_DECAY_STEP)).thenReturn(config("10"));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FIRST_BLOOD_BONUS)).thenReturn(config("15"));
        when(firstBloodMapper.insertIgnore(any(FirstBlood.class))).thenReturn(1);

        int awarded = scoringService.recordCorrectSolve(contestUser, question);

        assertEquals(115, awarded);
        assertEquals(165, contestUser.getTotalScore());
        // 奖金快照随一血行落库
        verify(firstBloodMapper).insertIgnore(argThat(fb -> Integer.valueOf(15).equals(fb.getBonusAwarded())));
    }

    @Test
    @DisplayName("奖金为零时一血命中仍只入账基础分")
    void zeroBonus_firstBloodAwardsBaseOnly() {
        ContestUser contestUser = createContestUser(0);
        Question question = createQuestion(100);

        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(0);
        when(contestConfigMapper.selectByKey(anyString())).thenReturn(null);
        when(firstBloodMapper.insertIgnore(any(FirstBlood.class))).thenReturn(1);

        int awarded = scoringService.recordCorrectSolve(contestUser, question);

        assertEquals(100, awarded);
    }

    @Test
    @DisplayName("并发争夺同题一血：唯一约束只放行一人，奖金只发一次")
    void concurrentFirstBlood_bonusAwardedOnlyOnce() {
        ContestUser userA = createContestUser(0);
        ContestUser userB = createContestUser(0);
        userB.setId(2000);
        userB.setUserId(2);
        Question question = createQuestion(100);

        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(0);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_MIN_POINTS)).thenReturn(null);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_DECAY_STEP)).thenReturn(null);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FIRST_BLOOD_BONUS)).thenReturn(config("15"));
        // 模拟并发：A 的 INSERT IGNORE 成功（1），B 被唯一约束拦截（0）
        when(firstBloodMapper.insertIgnore(any(FirstBlood.class))).thenReturn(1, 0);

        int awardedA = scoringService.recordCorrectSolve(userA, question);
        int awardedB = scoringService.recordCorrectSolve(userB, question);

        assertEquals(115, awardedA);
        assertEquals(100, awardedB);
        verify(firstBloodMapper, times(2)).insertIgnore(any(FirstBlood.class));
        verify(contestUserMapper).addScore(contestUserId, 115);
        verify(contestUserMapper).addScore(2000, 100);
    }

    @Test
    @DisplayName("B2：后续衰减不回算已入账用户的分数")
    void laterDecay_shouldNeverRecalculateAwardedScore() {
        ContestUser userA = createContestUser(0);
        ContestUser userB = createContestUser(0);
        userB.setId(2000);
        userB.setUserId(2);
        Question question = createQuestion(100);

        // A 解出时：不衰减
        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(0, 1);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_MIN_POINTS)).thenReturn(config("60"), config("60"));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_DECAY_STEP)).thenReturn(config("0"), config("10"));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FIRST_BLOOD_BONUS)).thenReturn(null, null);
        when(firstBloodMapper.insertIgnore(any(FirstBlood.class))).thenReturn(1, 0);

        int awardedA = scoringService.recordCorrectSolve(userA, question);
        int awardedB = scoringService.recordCorrectSolve(userB, question);

        assertEquals(100, awardedA);
        assertEquals(90, awardedB);
        // A 的总分在 B 解出后保持 100，未被回算
        assertEquals(100, userA.getTotalScore());
        verify(contestUserMapper, times(1)).addScore(eq(contestUserId), anyInt());
    }

    // ========== P2：C1/C2 赛期门禁 ==========

    @Test
    @DisplayName("C1：赛前（NOT_STARTED）禁止入账与一血")
    void notStarted_shouldRejectScoring() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.NOT_STARTED);

        assertThrows(IllegalArgumentException.class, () ->
                scoringService.recordCorrectSolve(createContestUser(0), createQuestion(100)));

        verifyNoInteractions(firstBloodMapper, submissionMapper, contestUserMapper, questionMapper);
    }

    @Test
    @DisplayName("C1：准备阶段（READY，start 之前）禁止入账与一血")
    void readyPhase_shouldRejectScoring() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.READY);

        assertThrows(IllegalArgumentException.class, () ->
                scoringService.recordCorrectSolve(createContestUser(0), createQuestion(100)));

        verifyNoInteractions(firstBloodMapper, submissionMapper, contestUserMapper, questionMapper);
    }

    @Test
    @DisplayName("C2：赛后冻结开启（freeze_on_end 缺省默认 true）禁止入账与一血")
    void finishedWithDefaultFreeze_shouldRejectScoring() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.FINISHED);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FREEZE_ON_END)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () ->
                scoringService.recordCorrectSolve(createContestUser(0), createQuestion(100)));

        verifyNoInteractions(firstBloodMapper, submissionMapper, contestUserMapper, questionMapper);
    }

    @Test
    @DisplayName("C2：成绩公布阶段且冻结开启，禁止入账与一血")
    void resultsPublishedWithFreezeOn_shouldRejectScoring() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.RESULTS_PUBLISHED);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FREEZE_ON_END)).thenReturn(config("true"));

        assertThrows(IllegalArgumentException.class, () ->
                scoringService.recordCorrectSolve(createContestUser(0), createQuestion(100)));

        verifyNoInteractions(firstBloodMapper, submissionMapper, contestUserMapper, questionMapper);
    }

    @Test
    @DisplayName("C2：赛后 freeze_on_end=false（加时赛）仍可入账与一血")
    void finishedWithFreezeOff_shouldStillAward() {
        ContestUser contestUser = createContestUser(0);
        Question question = createQuestion(100);

        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.FINISHED);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FREEZE_ON_END)).thenReturn(config("false"));
        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(0);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_MIN_POINTS)).thenReturn(null);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_DECAY_STEP)).thenReturn(null);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FIRST_BLOOD_BONUS)).thenReturn(null);
        when(firstBloodMapper.insertIgnore(any(FirstBlood.class))).thenReturn(1);

        int awarded = scoringService.recordCorrectSolve(contestUser, question);

        assertEquals(100, awarded);
        verify(firstBloodMapper).insertIgnore(any(FirstBlood.class));
        verify(contestUserMapper).addScore(contestUserId, 100);
    }

    // ========== P2：§12 概览（D1 现场聚合） ==========

    @Test
    @DisplayName("§12：概览五项指标现场聚合（含基础分低于 min_points 边界）")
    void getOverview_shouldAggregateLive() {
        Question q1 = createQuestion(100);
        Question q2 = createQuestion(100);
        q2.setId(101);
        Question q3 = createQuestion(50); // 基础分低于 min_points=60 的边界
        q3.setId(102);

        when(contestConfigMapper.selectByKey(ScoringService.KEY_MIN_POINTS)).thenReturn(config("60"));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_DECAY_STEP)).thenReturn(config("10"));
        when(questionMapper.selectAll()).thenReturn(Arrays.asList(q1, q2, q3));
        when(submissionMapper.countCorrectGroupByQuestion()).thenReturn(Arrays.asList(
                new QuestionSolveCount(100, 1),   // q1: max(60, 100-10)=90 → 既非 base 也非 min
                new QuestionSolveCount(101, 10))); // q2: max(60, 0)=60 → at_min
        when(firstBloodMapper.countAll()).thenReturn(1);
        when(firstBloodMapper.sumBonusAwarded()).thenReturn(15);

        ScoringOverviewDTO overview = scoringService.getOverview();

        assertEquals(11, overview.getTotalCorrectSolves());
        assertEquals(1, overview.getQuestionsWithFirstBlood());
        assertEquals(0, overview.getQuestionsAtBasePoints());
        // q2 触底 60；q3 基础分 50 < min 60，当前分=60=min → 也计 at_min（不计 at_base）
        assertEquals(2, overview.getQuestionsAtMinPoints());
        assertEquals(15, overview.getTotalFirstBloodBonusAwarded());
    }

    @Test
    @DisplayName("§12：无题目无提交的旧库，概览全部为零不抛异常")
    void getOverview_emptyDatabase_shouldReturnZeros() {
        when(contestConfigMapper.selectByKey(anyString())).thenReturn(null);
        when(questionMapper.selectAll()).thenReturn(Collections.emptyList());
        when(submissionMapper.countCorrectGroupByQuestion()).thenReturn(Collections.emptyList());
        when(firstBloodMapper.countAll()).thenReturn(0);
        when(firstBloodMapper.sumBonusAwarded()).thenReturn(0);

        ScoringOverviewDTO overview = scoringService.getOverview();

        assertEquals(0, overview.getTotalCorrectSolves());
        assertEquals(0, overview.getQuestionsWithFirstBlood());
        assertEquals(0, overview.getQuestionsAtBasePoints());
        assertEquals(0, overview.getQuestionsAtMinPoints());
        assertEquals(0, overview.getTotalFirstBloodBonusAwarded());
    }

    // ========== 配置读写 ==========

    @Test
    @DisplayName("更新配置：负数校验拒绝")
    void updateScoringConfig_shouldRejectNegativeValues() {
        assertThrows(IllegalArgumentException.class, () ->
                scoringService.updateScoringConfig(new ScoringConfigDTO(-1, 0, 0)));
        verify(contestConfigMapper, never()).upsert(any());
    }

    @Test
    @DisplayName("更新配置：三个 §8 键按冻结字符串写入")
    void updateScoringConfig_shouldWriteFrozenKeys() {
        scoringService.updateScoringConfig(new ScoringConfigDTO(60, 10, 15));

        verify(contestConfigMapper).upsert(argThat(c ->
                ScoringService.KEY_MIN_POINTS.equals(c.getConfigKey()) && "60".equals(c.getConfigValue())));
        verify(contestConfigMapper).upsert(argThat(c ->
                ScoringService.KEY_DECAY_STEP.equals(c.getConfigKey()) && "10".equals(c.getConfigValue())));
        verify(contestConfigMapper).upsert(argThat(c ->
                ScoringService.KEY_FIRST_BLOOD_BONUS.equals(c.getConfigKey()) && "15".equals(c.getConfigValue())));
    }

    private ContestConfig config(String value) {
        return ContestConfig.builder().configValue(value).build();
    }

    private ContestUser createContestUser(int initialScore) {
        ContestUser contestUser = new ContestUser();
        contestUser.setId(contestUserId);
        contestUser.setUserId(userId);
        contestUser.setTotalScore(initialScore);
        return contestUser;
    }

    private Question createQuestion(int points) {
        Question question = new Question();
        question.setId(questionId);
        question.setPoints(points);
        return question;
    }
}
