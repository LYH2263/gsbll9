package com.ctf.service;

import com.ctf.dto.scoring.FirstBloodDTO;
import com.ctf.dto.scoring.QuestionSolveStat;
import com.ctf.dto.scoring.ScoringConfigDTO;
import com.ctf.dto.scoring.ScoringOverviewDTO;
import com.ctf.dto.scoring.ScoringResult;
import com.ctf.entity.ContestConfig;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoringService 衰减/奖金/门禁/概览单元测试 (P1+P2)")
class ScoringServiceTest {

    @Mock
    private FirstBloodMapper firstBloodMapper;

    @Mock
    private ContestUserMapper contestUserMapper;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private SubmissionMapper submissionMapper;

    @Mock
    private ContestConfigMapper contestConfigMapper;

    @Mock
    private ContestTimeUtil contestTimeUtil;

    @InjectMocks
    private ScoringService scoringService;

    private Integer contestUserId;
    private Integer questionId;
    private Integer userId;

    @BeforeEach
    void setUp() {
        contestUserId = 1000;
        questionId = 100;
        userId = 1;
    }

    private void stubRunning() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.RUNNING);
    }

    private void stubQuestionExists() {
        Question question = new Question();
        question.setId(questionId);
        when(questionMapper.selectByIdForUpdate(questionId)).thenReturn(question);
    }

    private void stubConfig(int minPoints, int decayStep, int bonus) {
        when(contestConfigMapper.selectByKey(ScoringService.KEY_MIN_POINTS))
                .thenReturn(config(ScoringService.KEY_MIN_POINTS, String.valueOf(minPoints)));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_DECAY_STEP))
                .thenReturn(config(ScoringService.KEY_DECAY_STEP, String.valueOf(decayStep)));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FIRST_BLOOD_BONUS))
                .thenReturn(config(ScoringService.KEY_FIRST_BLOOD_BONUS, String.valueOf(bonus)));
    }

    private ContestConfig config(String key, String value) {
        return ContestConfig.builder().configKey(key).configValue(value).build();
    }

    // ---------- P1: 基础分/衰减/奖金 ----------

    @Test
    @DisplayName("首次解出(默认 decay=0) → 入账=基础分，无奖金，记录一血")
    void awardSolve_firstBlood_defaults_shouldAwardBasePoints() {
        stubRunning();
        stubQuestionExists();
        when(contestConfigMapper.selectByKey(anyString())).thenReturn(null);
        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(0);
        when(firstBloodMapper.insert(any(FirstBlood.class))).thenReturn(1);

        ScoringResult result = scoringService.awardSolve(contestUserId, questionId, userId, 200);

        assertEquals(200, result.getAwardedPoints());
        assertEquals(0, result.getBonusPoints());
        assertTrue(result.getFirstBlood());
        verify(contestUserMapper).incrementScore(contestUserId, 200);
    }

    @Test
    @DisplayName("非首次解出(唯一约束冲突) → 不记一血、无奖金，仍按当前分入账")
    void awardSolve_duplicateFirstBlood_shouldStillAwardPoints() {
        stubRunning();
        stubQuestionExists();
        when(contestConfigMapper.selectByKey(anyString())).thenReturn(null);
        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(1);
        when(firstBloodMapper.insert(any(FirstBlood.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry"));

        ScoringResult result = scoringService.awardSolve(contestUserId, questionId, userId, 300);

        assertFalse(result.getFirstBlood());
        assertEquals(0, result.getBonusPoints());
        assertEquals(300, result.getAwardedPoints());
        verify(contestUserMapper).incrementScore(contestUserId, 300);
    }

    @Test
    @DisplayName("B1: 第2次解出 solveCount=1(入账前) → 按 max(min, base-step*1) 衰减")
    void awardSolve_secondSolve_shouldDecayByOneStep() {
        stubRunning();
        stubQuestionExists();
        stubConfig(60, 10, 0);
        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(1);
        when(firstBloodMapper.insert(any(FirstBlood.class)))
                .thenThrow(new DuplicateKeyException("dup"));

        ScoringResult result = scoringService.awardSolve(contestUserId, questionId, userId, 100);

        assertEquals(90, result.getAwardedPoints());
        verify(contestUserMapper).incrementScore(contestUserId, 90);
    }

    @Test
    @DisplayName("B1: 首次解出 solveCount=0(入账前) → 仍为基础分，不预衰减")
    void awardSolve_firstSolve_solveCountZero_shouldBeBase() {
        stubRunning();
        stubQuestionExists();
        stubConfig(60, 10, 0);
        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(0);
        when(firstBloodMapper.insert(any(FirstBlood.class))).thenReturn(1);

        ScoringResult result = scoringService.awardSolve(contestUserId, questionId, userId, 100);

        assertEquals(100, result.getAwardedPoints());
    }

    @Test
    @DisplayName("B1: 衰减低于下限 → 钳制到 min_points")
    void awardSolve_decayBelowMin_shouldClampToMin() {
        stubRunning();
        stubQuestionExists();
        stubConfig(60, 10, 0);
        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(10);
        when(firstBloodMapper.insert(any(FirstBlood.class)))
                .thenThrow(new DuplicateKeyException("dup"));

        ScoringResult result = scoringService.awardSolve(contestUserId, questionId, userId, 100);

        assertEquals(60, result.getAwardedPoints());
        verify(contestUserMapper).incrementScore(contestUserId, 60);
    }

    @Test
    @DisplayName("B4: decay_step=0 → 恒等于基础分(再钳制到 min_points)")
    void awardSolve_decayStepZero_shouldEqualBase() {
        stubRunning();
        stubQuestionExists();
        stubConfig(50, 0, 0);
        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(3);
        when(firstBloodMapper.insert(any(FirstBlood.class)))
                .thenThrow(new DuplicateKeyException("dup"));

        ScoringResult result = scoringService.awardSolve(contestUserId, questionId, userId, 100);

        assertEquals(100, result.getAwardedPoints());
    }

    @Test
    @DisplayName("基础分低于最低分 → 钳制到 min，题目判为 at_min 而非 at_base")
    void awardSolve_baseBelowMin_shouldClampToMin() {
        stubRunning();
        stubQuestionExists();
        stubConfig(50, 10, 0);
        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(0);
        when(firstBloodMapper.insert(any(FirstBlood.class))).thenReturn(1);

        ScoringResult result = scoringService.awardSolve(contestUserId, questionId, userId, 30);

        assertEquals(50, result.getAwardedPoints());
    }

    @Test
    @DisplayName("B3: 一血奖金仅在首次插入成功时发放，并定格写入 first_bloods.bonus_points")
    void awardSolve_firstBlood_shouldAddBonusAndPersist() {
        stubRunning();
        stubQuestionExists();
        stubConfig(1, 0, 15);
        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(0);
        when(firstBloodMapper.insert(any(FirstBlood.class))).thenReturn(1);

        ScoringResult result = scoringService.awardSolve(contestUserId, questionId, userId, 100);

        assertEquals(100, result.getAwardedPoints());
        assertEquals(15, result.getBonusPoints());
        assertTrue(result.getFirstBlood());
        verify(contestUserMapper).incrementScore(contestUserId, 115);

        ArgumentCaptor<FirstBlood> captor = ArgumentCaptor.forClass(FirstBlood.class);
        verify(firstBloodMapper).insert(captor.capture());
        assertEquals(15, captor.getValue().getBonusPoints());
    }

    @Test
    @DisplayName("B3: 非一血(唯一约束冲突) → 不发放奖金")
    void awardSolve_nonFirstBlood_shouldNotAddBonus() {
        stubRunning();
        stubQuestionExists();
        stubConfig(1, 10, 15);
        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(1);
        when(firstBloodMapper.insert(any(FirstBlood.class)))
                .thenThrow(new DuplicateKeyException("dup"));

        ScoringResult result = scoringService.awardSolve(contestUserId, questionId, userId, 100);

        assertEquals(90, result.getAwardedPoints());
        assertEquals(0, result.getBonusPoints());
        assertFalse(result.getFirstBlood());
        verify(contestUserMapper).incrementScore(contestUserId, 90);
    }

    @Test
    @DisplayName("奖金为 0 → 一血记录 bonus_points=0")
    void awardSolve_bonusZero_shouldPersistZero() {
        stubRunning();
        stubQuestionExists();
        stubConfig(1, 0, 0);
        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(0);
        when(firstBloodMapper.insert(any(FirstBlood.class))).thenReturn(1);

        scoringService.awardSolve(contestUserId, questionId, userId, 100);

        ArgumentCaptor<FirstBlood> captor = ArgumentCaptor.forClass(FirstBlood.class);
        verify(firstBloodMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getBonusPoints());
    }

    @Test
    @DisplayName("题目不存在 → 抛异常，不加分")
    void awardSolve_questionNotFound_shouldThrow() {
        stubRunning();
        when(questionMapper.selectByIdForUpdate(questionId)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> scoringService.awardSolve(contestUserId, questionId, userId, 100));
        verify(contestUserMapper, never()).incrementScore(anyInt(), anyInt());
        verify(firstBloodMapper, never()).insert(any());
    }

    // ---------- P2: 赛期门禁 C1-C3 ----------

    @Test
    @DisplayName("C1: NOT_STARTED → 禁止计分")
    void awardSolve_notStarted_shouldThrow() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.NOT_STARTED);

        assertThrows(IllegalArgumentException.class,
                () -> scoringService.awardSolve(contestUserId, questionId, userId, 100));
        verify(contestUserMapper, never()).incrementScore(anyInt(), anyInt());
        verify(firstBloodMapper, never()).insert(any());
    }

    @Test
    @DisplayName("C1: READY（start 之前）→ 禁止计分")
    void awardSolve_ready_shouldThrow() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.READY);

        assertThrows(IllegalArgumentException.class,
                () -> scoringService.awardSolve(contestUserId, questionId, userId, 100));
        verify(firstBloodMapper, never()).insert(any());
    }

    @Test
    @DisplayName("C2: FINISHED 且 freeze_on_end=true → 禁止计分")
    void awardSolve_finishedFrozen_shouldThrow() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.FINISHED);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FREEZE_ON_END))
                .thenReturn(config(ScoringService.KEY_FREEZE_ON_END, "true"));

        assertThrows(IllegalArgumentException.class,
                () -> scoringService.awardSolve(contestUserId, questionId, userId, 100));
        verify(contestUserMapper, never()).incrementScore(anyInt(), anyInt());
    }

    @Test
    @DisplayName("C2: FINISHED 且 freeze_on_end=false → 允许入账（加时赛）")
    void awardSolve_finishedNotFrozen_shouldAllow() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.FINISHED);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FREEZE_ON_END))
                .thenReturn(config(ScoringService.KEY_FREEZE_ON_END, "false"));
        stubQuestionExists();
        stubConfig(1, 0, 0);
        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(0);
        when(firstBloodMapper.insert(any(FirstBlood.class))).thenReturn(1);

        ScoringResult result = scoringService.awardSolve(contestUserId, questionId, userId, 100);

        assertEquals(100, result.getAwardedPoints());
        verify(contestUserMapper).incrementScore(contestUserId, 100);
    }

    @Test
    @DisplayName("C3: RESULTS_PUBLISHED → 禁止计分（只读）")
    void awardSolve_resultsPublished_shouldThrow() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.RESULTS_PUBLISHED);

        assertThrows(IllegalArgumentException.class,
                () -> scoringService.awardSolve(contestUserId, questionId, userId, 100));
        verify(contestUserMapper, never()).incrementScore(anyInt(), anyInt());
    }

    @Test
    @DisplayName("freeze_on_end 配置缺失 → 默认 true（赛后冻结）")
    void freezeOnEnd_missing_shouldDefaultTrue() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.FINISHED);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FREEZE_ON_END)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> scoringService.assertScoringAllowed());
    }

    @Test
    @DisplayName("isScoringActive：RUNNING→true；冻结→false")
    void isScoringActive_shouldReflectGate() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.RUNNING);
        assertTrue(scoringService.isScoringActive());

        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.NOT_STARTED);
        assertFalse(scoringService.isScoringActive());
    }

    @Test
    @DisplayName("只读查询（一血/概览）在 RESULTS_PUBLISHED 阶段仍可用")
    void readOnlyQueries_availableAfterResults() {
        when(firstBloodMapper.selectAllWithDetails()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> scoringService.getAllFirstBloods());
    }

    // ---------- P2: 概览 D1/D2 ----------

    @Test
    @DisplayName("D1: 概览五项指标现场聚合，无冗余表")
    void getOverview_shouldAggregateLive() {
        when(contestConfigMapper.selectByKey(ScoringService.KEY_MIN_POINTS))
                .thenReturn(config(ScoringService.KEY_MIN_POINTS, "60"));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_DECAY_STEP))
                .thenReturn(config(ScoringService.KEY_DECAY_STEP, "10"));
        // 题目1: base=100, correct=0 → 当前100(at base)
        // 题目2: base=100, correct=1 → 当前90
        // 题目3: base=100, correct=5 → 当前60(at min)
        // 题目4: base=30,  correct=0 → 当前60(at min, base<min)
        List<QuestionSolveStat> stats = List.of(
                stat(1, 100, 0),
                stat(2, 100, 1),
                stat(3, 100, 5),
                stat(4, 30, 0)
        );
        when(firstBloodMapper.selectActiveQuestionSolveStats()).thenReturn(stats);
        when(firstBloodMapper.countAll()).thenReturn(2);
        when(firstBloodMapper.sumBonusPoints()).thenReturn(30);

        ScoringOverviewDTO overview = scoringService.getOverview();

        assertEquals(6, overview.getTotalCorrectSolves()); // 0+1+5+0
        assertEquals(2, overview.getQuestionsWithFirstBlood());
        assertEquals(1, overview.getQuestionsAtBasePoints());   // only q1
        assertEquals(2, overview.getQuestionsAtMinPoints());    // q3 + q4
        assertEquals(30, overview.getTotalFirstBloodBonusAwarded());
    }

    @Test
    @DisplayName("D1: 空数据 → 概览各项为 0，不抛异常")
    void getOverview_empty_shouldReturnZeros() {
        when(contestConfigMapper.selectByKey(anyString())).thenReturn(null);
        when(firstBloodMapper.selectActiveQuestionSolveStats()).thenReturn(Collections.emptyList());
        when(firstBloodMapper.countAll()).thenReturn(0);
        when(firstBloodMapper.sumBonusPoints()).thenReturn(0);

        ScoringOverviewDTO overview = scoringService.getOverview();

        assertEquals(0, overview.getTotalCorrectSolves());
        assertEquals(0, overview.getQuestionsWithFirstBlood());
        assertEquals(0, overview.getQuestionsAtBasePoints());
        assertEquals(0, overview.getQuestionsAtMinPoints());
        assertEquals(0, overview.getTotalFirstBloodBonusAwarded());
    }

    // ---------- 配置读写 ----------

    @Test
    @DisplayName("配置读取：键缺失 → 返回 §8 默认值（含 freeze=true, timezone=Asia/Shanghai）")
    void getConfig_missingKeys_shouldReturnDefaults() {
        when(contestConfigMapper.selectByKey(anyString())).thenReturn(null);

        ScoringConfigDTO config = scoringService.getConfig();

        assertEquals(1, config.getMinPoints());
        assertEquals(0, config.getDecayStep());
        assertEquals(0, config.getFirstBloodBonus());
        assertTrue(config.getFreezeOnEnd());
        assertEquals("Asia/Shanghai", config.getOverviewTimezone());
    }

    @Test
    @DisplayName("配置写入：upsert 五个冻结键，键名完全一致")
    void updateConfig_shouldUpsertFrozenKeys() {
        ScoringConfigDTO dto = ScoringConfigDTO.builder()
                .minPoints(60).decayStep(10).firstBloodBonus(15)
                .freezeOnEnd(false).overviewTimezone("Asia/Shanghai").build();

        scoringService.updateConfig(dto);

        ArgumentCaptor<ContestConfig> captor = ArgumentCaptor.forClass(ContestConfig.class);
        verify(contestConfigMapper, times(5)).upsert(captor.capture());
        List<String> keys = captor.getAllValues().stream().map(ContestConfig::getConfigKey).toList();
        assertTrue(keys.contains("scoring.min_points"));
        assertTrue(keys.contains("scoring.decay_step"));
        assertTrue(keys.contains("scoring.first_blood_bonus"));
        assertTrue(keys.contains("scoring.freeze_on_end"));
        assertTrue(keys.contains("scoring.overview_timezone"));
    }

    @Test
    @DisplayName("配置写入：负数/空值应被拒绝")
    void updateConfig_invalid_shouldThrow() {
        ScoringConfigDTO bad = ScoringConfigDTO.builder()
                .minPoints(-1).decayStep(0).firstBloodBonus(0)
                .freezeOnEnd(true).overviewTimezone("Asia/Shanghai").build();
        assertThrows(IllegalArgumentException.class, () -> scoringService.updateConfig(bad));

        ScoringConfigDTO noFreeze = ScoringConfigDTO.builder()
                .minPoints(1).decayStep(0).firstBloodBonus(0)
                .freezeOnEnd(null).overviewTimezone("Asia/Shanghai").build();
        assertThrows(IllegalArgumentException.class, () -> scoringService.updateConfig(noFreeze));

        ScoringConfigDTO blankTz = ScoringConfigDTO.builder()
                .minPoints(1).decayStep(0).firstBloodBonus(0)
                .freezeOnEnd(true).overviewTimezone("  ").build();
        assertThrows(IllegalArgumentException.class, () -> scoringService.updateConfig(blankTz));

        verify(contestConfigMapper, never()).upsert(any());
    }

    private QuestionSolveStat stat(int id, int base, int correct) {
        QuestionSolveStat s = new QuestionSolveStat();
        s.setQuestionId(id);
        s.setBasePoints(base);
        s.setCorrectCount(correct);
        return s;
    }
}
