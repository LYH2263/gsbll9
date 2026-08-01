package com.ctf.service;

import com.ctf.dto.scoring.FirstBloodDTO;
import com.ctf.dto.scoring.QuestionSolveCount;
import com.ctf.dto.scoring.ScoringConfigDTO;
import com.ctf.dto.scoring.ScoringOverviewDTO;
import com.ctf.entity.ContestConfig;
import com.ctf.entity.ContestUser;
import com.ctf.entity.FirstBlood;
import com.ctf.entity.Question;
import com.ctf.entity.User;
import com.ctf.mapper.ContestConfigMapper;
import com.ctf.mapper.ContestUserMapper;
import com.ctf.mapper.FirstBloodMapper;
import com.ctf.mapper.QuestionMapper;
import com.ctf.mapper.SubmissionMapper;
import com.ctf.mapper.UserMapper;
import com.ctf.util.ContestTimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoringService 计分/一血/配置/门禁/概览单元测试（P0+P1+P2）")
class ScoringServiceTest {

    @Mock private FirstBloodMapper firstBloodMapper;
    @Mock private ContestUserMapper contestUserMapper;
    @Mock private QuestionMapper questionMapper;
    @Mock private UserMapper userMapper;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private ContestConfigMapper contestConfigMapper;
    @Mock private ContestTimeUtil contestTimeUtil;

    @InjectMocks
    private ScoringService scoringService;

    private ContestUser contestUser;
    private Question question;

    @BeforeEach
    void setUp() {
        contestUser = new ContestUser();
        contestUser.setId(1000);
        contestUser.setUserId(1);
        contestUser.setTotalScore(0);

        question = new Question();
        question.setId(100);
        question.setTitle("测试题");
        question.setPoints(100);
    }

    private void stubConfig(String key, String value) {
        ContestConfig cfg = value == null ? null
                : ContestConfig.builder().configKey(key).configValue(value).build();
        lenient().when(contestConfigMapper.selectByKey(key)).thenReturn(cfg);
    }

    /** 桩：比赛进行中 + 一组计分配置。 */
    private void stubRunningWithConfig(String min, String step, String bonus) {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.RUNNING);
        stubConfig(ScoringService.KEY_MIN_POINTS, min);
        stubConfig(ScoringService.KEY_DECAY_STEP, step);
        stubConfig(ScoringService.KEY_FIRST_BLOOD_BONUS, bonus);
        stubConfig(ScoringService.KEY_FREEZE_ON_END, "true");
        stubConfig(ScoringService.KEY_OVERVIEW_TIMEZONE, "Asia/Shanghai");
    }

    // ============ B1 / B4 入账分 ============

    @Test
    @DisplayName("B4：decay_step=0 → 入账分恒等于基础分")
    void zeroDecayEqualsBase() {
        stubRunningWithConfig("1", "0", "0");
        when(firstBloodMapper.countByQuestionId(100)).thenReturn(1);
        when(submissionMapper.countCorrectByQuestionExcludingUser(100, 1000)).thenReturn(3);

        assertEquals(100, scoringService.awardForCorrectSubmission(contestUser, question));
        assertEquals(100, contestUser.getTotalScore());
    }

    @Test
    @DisplayName("B1：入账前 solve_count 代入（第2次解出 base-step×1）")
    void solveCountBeforeAccrual() {
        stubRunningWithConfig("60", "10", "15");
        when(firstBloodMapper.countByQuestionId(100)).thenReturn(1);
        when(submissionMapper.countCorrectByQuestionExcludingUser(100, 1000)).thenReturn(1);

        assertEquals(90, scoringService.awardForCorrectSubmission(contestUser, question));
    }

    @Test
    @DisplayName("B1 触底：衰减低于 min → 钳制 min_points")
    void clampToMin() {
        stubRunningWithConfig("60", "10", "0");
        when(firstBloodMapper.countByQuestionId(100)).thenReturn(1);
        when(submissionMapper.countCorrectByQuestionExcludingUser(100, 1000)).thenReturn(10);

        assertEquals(60, scoringService.awardForCorrectSubmission(contestUser, question));
    }

    // ============ B3 一血奖金 ============

    @Test
    @DisplayName("B3：首杀 → 入账分+奖金，并记录 awardedBonus 一条")
    void firstBloodAddsBonus() {
        stubRunningWithConfig("60", "10", "15");
        when(firstBloodMapper.countByQuestionId(100)).thenReturn(0);
        when(submissionMapper.countCorrectByQuestionExcludingUser(100, 1000)).thenReturn(0);

        assertEquals(115, scoringService.awardForCorrectSubmission(contestUser, question));
        assertEquals(115, contestUser.getTotalScore());
        ArgumentCaptor<FirstBlood> captor = ArgumentCaptor.forClass(FirstBlood.class);
        verify(firstBloodMapper).insert(captor.capture());
        assertEquals(15, captor.getValue().getAwardedBonus());
    }

    @Test
    @DisplayName("B3：奖金为零 → 首杀不加分，awardedBonus=0")
    void zeroBonus() {
        stubRunningWithConfig("60", "10", "0");
        when(firstBloodMapper.countByQuestionId(100)).thenReturn(0);
        when(submissionMapper.countCorrectByQuestionExcludingUser(100, 1000)).thenReturn(0);

        assertEquals(100, scoringService.awardForCorrectSubmission(contestUser, question));
        ArgumentCaptor<FirstBlood> captor = ArgumentCaptor.forClass(FirstBlood.class);
        verify(firstBloodMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getAwardedBonus());
    }

    @Test
    @DisplayName("B3：非首杀不发奖金，不写第二条一血")
    void nonFirstBloodNoBonus() {
        stubRunningWithConfig("60", "10", "15");
        when(firstBloodMapper.countByQuestionId(100)).thenReturn(1);
        when(submissionMapper.countCorrectByQuestionExcludingUser(100, 1000)).thenReturn(0);

        assertEquals(100, scoringService.awardForCorrectSubmission(contestUser, question));
        verify(firstBloodMapper, never()).insert(any(FirstBlood.class));
    }

    @Test
    @DisplayName("B3 并发：唯一约束抛 DuplicateKey → 本次不算首杀、不发奖金")
    void concurrentFirstBloodDuplicate() {
        stubRunningWithConfig("60", "10", "15");
        when(firstBloodMapper.countByQuestionId(100)).thenReturn(0);
        when(submissionMapper.countCorrectByQuestionExcludingUser(100, 1000)).thenReturn(0);
        doThrow(new org.springframework.dao.DuplicateKeyException("dup"))
                .when(firstBloodMapper).insert(any(FirstBlood.class));

        assertEquals(100, scoringService.awardForCorrectSubmission(contestUser, question)); // 无奖金
        assertEquals(100, contestUser.getTotalScore());
    }

    // ============ C1 / C2 门禁 ============

    @Test
    @DisplayName("C1：赛前（NOT_STARTED）→ 不入账不写一血，返回 0")
    void gateBeforeStart() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.NOT_STARTED);

        assertEquals(0, scoringService.awardForCorrectSubmission(contestUser, question));
        assertEquals(0, contestUser.getTotalScore());
        verify(firstBloodMapper, never()).insert(any());
        verify(contestUserMapper, never()).update(any());
    }

    @Test
    @DisplayName("C2：赛后 freeze_on_end=true → 冻结，返回 0")
    void gateAfterEndFrozen() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.FINISHED);
        stubConfig(ScoringService.KEY_MIN_POINTS, "1");
        stubConfig(ScoringService.KEY_DECAY_STEP, "0");
        stubConfig(ScoringService.KEY_FIRST_BLOOD_BONUS, "0");
        stubConfig(ScoringService.KEY_FREEZE_ON_END, "true");
        stubConfig(ScoringService.KEY_OVERVIEW_TIMEZONE, "Asia/Shanghai");

        assertEquals(0, scoringService.awardForCorrectSubmission(contestUser, question));
        verify(contestUserMapper, never()).update(any());
    }

    @Test
    @DisplayName("C2：赛后 freeze_on_end=false → 仍可入账（加时赛）")
    void gateAfterEndNotFrozen() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.FINISHED);
        stubConfig(ScoringService.KEY_MIN_POINTS, "1");
        stubConfig(ScoringService.KEY_DECAY_STEP, "0");
        stubConfig(ScoringService.KEY_FIRST_BLOOD_BONUS, "0");
        stubConfig(ScoringService.KEY_FREEZE_ON_END, "false");
        stubConfig(ScoringService.KEY_OVERVIEW_TIMEZONE, "Asia/Shanghai");
        when(firstBloodMapper.countByQuestionId(100)).thenReturn(1);
        when(submissionMapper.countCorrectByQuestionExcludingUser(100, 1000)).thenReturn(0);

        assertEquals(100, scoringService.awardForCorrectSubmission(contestUser, question));
        assertEquals(100, contestUser.getTotalScore());
    }

    // ============ 配置回退 ============

    @Test
    @DisplayName("配置缺失 → 回退 §8 默认值")
    void configDefaults() {
        stubConfig(ScoringService.KEY_MIN_POINTS, null);
        stubConfig(ScoringService.KEY_DECAY_STEP, null);
        stubConfig(ScoringService.KEY_FIRST_BLOOD_BONUS, null);
        stubConfig(ScoringService.KEY_FREEZE_ON_END, null);
        stubConfig(ScoringService.KEY_OVERVIEW_TIMEZONE, null);

        ScoringConfigDTO c = scoringService.getScoringConfig();
        assertEquals(1, c.getMinPoints());
        assertEquals(0, c.getDecayStep());
        assertEquals(0, c.getFirstBloodBonus());
        assertTrue(c.getFreezeOnEnd());
        assertEquals("Asia/Shanghai", c.getOverviewTimezone());
    }

    @Test
    @DisplayName("配置非法值 → 回退默认值")
    void configInvalidFallback() {
        stubConfig(ScoringService.KEY_MIN_POINTS, "abc");
        stubConfig(ScoringService.KEY_DECAY_STEP, null);
        stubConfig(ScoringService.KEY_FIRST_BLOOD_BONUS, null);
        stubConfig(ScoringService.KEY_FREEZE_ON_END, null);
        stubConfig(ScoringService.KEY_OVERVIEW_TIMEZONE, null);

        assertEquals(1, scoringService.getScoringConfig().getMinPoints());
    }

    @Test
    @DisplayName("更新配置 → 用 §8 冻结键 upsert")
    void updateConfigFrozenKeys() {
        stubConfig(ScoringService.KEY_MIN_POINTS, "50");
        stubConfig(ScoringService.KEY_DECAY_STEP, "5");
        stubConfig(ScoringService.KEY_FIRST_BLOOD_BONUS, "20");
        stubConfig(ScoringService.KEY_FREEZE_ON_END, null);
        stubConfig(ScoringService.KEY_OVERVIEW_TIMEZONE, null);

        scoringService.updateScoringConfig(ScoringConfigDTO.builder()
                .minPoints(50).decayStep(5).firstBloodBonus(20).build());

        ArgumentCaptor<ContestConfig> captor = ArgumentCaptor.forClass(ContestConfig.class);
        verify(contestConfigMapper, times(3)).upsert(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(c -> ScoringService.KEY_MIN_POINTS.equals(c.getConfigKey())));
    }

    // ============ §12 概览（D1 现场聚合）============

    @Test
    @DisplayName("§12：概览五指标现场聚合正确")
    void overviewAggregates() {
        stubConfig(ScoringService.KEY_MIN_POINTS, "60");
        stubConfig(ScoringService.KEY_DECAY_STEP, "10");
        stubConfig(ScoringService.KEY_FIRST_BLOOD_BONUS, "15");
        stubConfig(ScoringService.KEY_FREEZE_ON_END, "true");
        stubConfig(ScoringService.KEY_OVERVIEW_TIMEZONE, "Asia/Shanghai");
        when(submissionMapper.countTotalCorrectSolves()).thenReturn(7L);
        when(firstBloodMapper.countQuestionsWithFirstBlood()).thenReturn(2L);
        when(firstBloodMapper.sumAwardedBonus()).thenReturn(30L);
        // q1: base100 solve0 → current100=base（at base）; q2: base100 solve5 → max(60,50)=60=min（at min）;
        // q3: base100 solve4 → max(60,60)=60=min（at min）
        when(submissionMapper.selectSolveCountsPerQuestion()).thenReturn(Arrays.asList(
                new QuestionSolveCount(1, 100, 0L),
                new QuestionSolveCount(2, 100, 5L),
                new QuestionSolveCount(3, 100, 4L)
        ));

        ScoringOverviewDTO o = scoringService.getOverview();
        assertEquals(7L, o.getTotalCorrectSolves());
        assertEquals(2L, o.getQuestionsWithFirstBlood());
        assertEquals(30L, o.getTotalFirstBloodBonusAwarded());
        assertEquals(1L, o.getQuestionsAtBasePoints());  // 仅 q1
        assertEquals(2L, o.getQuestionsAtMinPoints());   // q2, q3
    }

    @Test
    @DisplayName("§12：空表/空配置 → 概览全 0，不抛异常")
    void overviewEmptySafe() {
        stubConfig(ScoringService.KEY_MIN_POINTS, null);
        stubConfig(ScoringService.KEY_DECAY_STEP, null);
        stubConfig(ScoringService.KEY_FIRST_BLOOD_BONUS, null);
        stubConfig(ScoringService.KEY_FREEZE_ON_END, null);
        stubConfig(ScoringService.KEY_OVERVIEW_TIMEZONE, null);
        when(submissionMapper.countTotalCorrectSolves()).thenReturn(0L);
        when(firstBloodMapper.countQuestionsWithFirstBlood()).thenReturn(0L);
        when(firstBloodMapper.sumAwardedBonus()).thenReturn(0L);
        when(submissionMapper.selectSolveCountsPerQuestion()).thenReturn(List.of());

        ScoringOverviewDTO o = scoringService.getOverview();
        assertEquals(0L, o.getTotalCorrectSolves());
        assertEquals(0L, o.getQuestionsAtBasePoints());
        assertEquals(0L, o.getQuestionsAtMinPoints());
    }

    // ============ 只读列表 ============

    @Test
    @DisplayName("管理端只读列表 → 组装题目与用户标识")
    void listFirstBloods() {
        FirstBlood fb = new FirstBlood(1, 100, 1, 1000, 15, LocalDateTime.now());
        when(firstBloodMapper.selectAll()).thenReturn(List.of(fb));
        when(questionMapper.selectById(100)).thenReturn(question);
        User user = new User();
        user.setId(1);
        user.setStudentId("2021001");
        user.setFullName("张三");
        when(userMapper.selectById(1)).thenReturn(user);

        List<FirstBloodDTO> result = scoringService.listFirstBloods();
        assertEquals(1, result.size());
        assertEquals("2021001", result.get(0).getStudentId());
        assertEquals("测试题", result.get(0).getQuestionTitle());
    }
}
