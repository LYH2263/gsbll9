package com.ctf.service;

import com.ctf.dto.FirstBloodRecordDTO;
import com.ctf.dto.ScoringConfigDTO;
import com.ctf.dto.ScoringOverviewDTO;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoringService P0/P1/P2 计分、衰减、一血奖金、门禁与概览测试")
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

    private Integer contestUserId;
    private Integer userId;
    private Integer questionId;

    @BeforeEach
    void setUp() {
        contestUserId = 1000;
        userId = 1;
        questionId = 100;
        // awardCorrectSolve 通过 self 代理调用 REQUIRES_NEW 一血方法，
        // 单元测试中将 self 指向实例本身以走真实方法（无 Spring 容器时事务注解不生效）。
        ReflectionTestUtils.setField(scoringService, "self", scoringService);
    }

    private void stubConfig(int minPoints, int decayStep, int firstBloodBonus) {
        when(contestConfigMapper.selectByKey(ScoringService.KEY_MIN_POINTS))
                .thenReturn(configOf(String.valueOf(minPoints)));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_DECAY_STEP))
                .thenReturn(configOf(String.valueOf(decayStep)));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FIRST_BLOOD_BONUS))
                .thenReturn(configOf(String.valueOf(firstBloodBonus)));
    }

    private void stubConfigFull(int minPoints, int decayStep, int firstBloodBonus,
                                boolean freezeOnEnd, String overviewTimezone) {
        stubConfig(minPoints, decayStep, firstBloodBonus);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FREEZE_ON_END))
                .thenReturn(configOf(String.valueOf(freezeOnEnd)));
        when(contestConfigMapper.selectByKey(ScoringService.KEY_OVERVIEW_TIMEZONE))
                .thenReturn(configOf(overviewTimezone));
    }

    private ContestConfig configOf(String value) {
        return ContestConfig.builder().configValue(value).build();
    }

    private void mockRunning() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.RUNNING);
    }

    @Test
    @DisplayName("B1: solve_count 取入账前（排除当前用户），第 1 次解出按基础分入账")
    void awardCorrectSolve_firstSolve_usesBasePoints() {
        mockRunning();
        stubConfig(1, 10, 0);
        when(submissionMapper.countCorrectByQuestionExcludeUser(questionId, contestUserId)).thenReturn(0);
        ContestUser user = createContestUser(0);
        Question question = createQuestion(100);

        scoringService.awardCorrectSolve(user, question);

        assertEquals(100, user.getTotalScore());
        verify(submissionMapper).countCorrectByQuestionExcludeUser(questionId, contestUserId);
        verify(firstBloodMapper).insert(any());
    }

    @Test
    @DisplayName("B1: 已有 1 人解出 → 第 2 人按衰减一档入账（base - decayStep）")
    void awardCorrectSolve_secondSolve_appliesDecay() {
        mockRunning();
        stubConfig(60, 10, 0);
        when(submissionMapper.countCorrectByQuestionExcludeUser(questionId, contestUserId)).thenReturn(1);
        ContestUser user = createContestUser(0);
        Question question = createQuestion(100);

        scoringService.awardCorrectSolve(user, question);

        assertEquals(90, user.getTotalScore());
    }

    @Test
    @DisplayName("B1: 衰减到下限以下时钳制为 min_points")
    void awardCorrectSolve_clampsToMinPoints() {
        mockRunning();
        stubConfig(60, 20, 0);
        when(submissionMapper.countCorrectByQuestionExcludeUser(questionId, contestUserId)).thenReturn(5);
        ContestUser user = createContestUser(0);
        Question question = createQuestion(100);

        scoringService.awardCorrectSolve(user, question);

        assertEquals(60, user.getTotalScore());
    }

    @Test
    @DisplayName("B4: decay_step=0 时恒等于基础分（再夹 min_points）")
    void awardCorrectSolve_decayStepZero_equalsBasePoints() {
        mockRunning();
        stubConfig(1, 0, 0);
        when(submissionMapper.countCorrectByQuestionExcludeUser(questionId, contestUserId)).thenReturn(9);
        ContestUser user = createContestUser(0);
        Question question = createQuestion(25);

        scoringService.awardCorrectSolve(user, question);

        assertEquals(25, user.getTotalScore());
    }

    @Test
    @DisplayName("边界：基础分低于 min_points 时以基础分为准（公式 max(min, base) 不抬高基础分）")
    void awardCorrectSolve_baseBelowMin_usesBase() {
        mockRunning();
        stubConfig(50, 0, 0);
        when(submissionMapper.countCorrectByQuestionExcludeUser(questionId, contestUserId)).thenReturn(0);
        ContestUser user = createContestUser(0);
        Question question = createQuestion(30);

        scoringService.awardCorrectSolve(user, question);

        assertEquals(30, user.getTotalScore());
    }

    @Test
    @DisplayName("B3: 一血成功 → 在当前分基础上加一血奖金，并把奖金写入 bonus_awarded")
    void awardCorrectSolve_firstBlood_addsBonus() {
        mockRunning();
        stubConfig(1, 10, 15);
        when(submissionMapper.countCorrectByQuestionExcludeUser(questionId, contestUserId)).thenReturn(0);
        ContestUser user = createContestUser(0);
        Question question = createQuestion(100);

        scoringService.awardCorrectSolve(user, question);

        assertEquals(115, user.getTotalScore());
        ArgumentCaptor<FirstBlood> captor = ArgumentCaptor.forClass(FirstBlood.class);
        verify(firstBloodMapper).insert(captor.capture());
        assertEquals(15, captor.getValue().getBonusAwarded());
        verify(contestUserMapper, times(2)).update(user);
    }

    @Test
    @DisplayName("B3: 一血唯一约束冲突 → 不发放奖金，仅入账当前分（争一血失败方基础分不丢）")
    void awardCorrectSolve_duplicateFirstBlood_noBonus() {
        mockRunning();
        stubConfig(60, 10, 15);
        when(submissionMapper.countCorrectByQuestionExcludeUser(questionId, contestUserId)).thenReturn(2);
        doThrow(new DuplicateKeyException("duplicate")).when(firstBloodMapper).insert(any());
        ContestUser user = createContestUser(0);
        Question question = createQuestion(100);

        scoringService.awardCorrectSolve(user, question);

        assertEquals(80, user.getTotalScore());
        verify(contestUserMapper, times(1)).update(user);
    }

    @Test
    @DisplayName("B5: 一血奖金为 0 时不额外加分，但仍写一血行（bonus_awarded=0）")
    void awardCorrectSolve_zeroBonus_noExtraUpdate() {
        mockRunning();
        stubConfig(1, 10, 0);
        when(submissionMapper.countCorrectByQuestionExcludeUser(questionId, contestUserId)).thenReturn(0);
        ContestUser user = createContestUser(0);
        Question question = createQuestion(100);

        scoringService.awardCorrectSolve(user, question);

        assertEquals(100, user.getTotalScore());
        verify(contestUserMapper, times(1)).update(user);
        ArgumentCaptor<FirstBlood> captor = ArgumentCaptor.forClass(FirstBlood.class);
        verify(firstBloodMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getBonusAwarded());
    }

    @Test
    @DisplayName("题目 points 为 null → 以 0 为基础分参与计算，不抛异常")
    void awardCorrectSolve_nullPoints_treatedAsZero() {
        mockRunning();
        stubConfig(1, 0, 0);
        when(submissionMapper.countCorrectByQuestionExcludeUser(questionId, contestUserId)).thenReturn(0);
        ContestUser user = createContestUser(5);
        Question question = createQuestion(null);

        scoringService.awardCorrectSolve(user, question);

        assertEquals(5, user.getTotalScore());
    }

    @Test
    @DisplayName("C1: 未开始/准备阶段禁止计分（抛 IllegalStateException）")
    void awardCorrectSolve_beforeStart_blocked() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.NOT_STARTED);
        ContestUser user = createContestUser(0);
        Question question = createQuestion(100);

        assertThrows(IllegalStateException.class, () -> scoringService.awardCorrectSolve(user, question));
        verify(contestUserMapper, never()).update(any());
        verify(firstBloodMapper, never()).insert(any());
    }

    @Test
    @DisplayName("C2: 赛后且 freeze_on_end=true → 禁止计分")
    void isScoringAllowed_afterEndFrozen_false() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.FINISHED);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FREEZE_ON_END)).thenReturn(configOf("true"));

        assertFalse(scoringService.isScoringAllowed());
    }

    @Test
    @DisplayName("C2: 赛后且 freeze_on_end=false → 允许计分（加时演练）")
    void isScoringAllowed_afterEndNotFrozen_true() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.FINISHED);
        when(contestConfigMapper.selectByKey(ScoringService.KEY_FREEZE_ON_END)).thenReturn(configOf("false"));

        assertTrue(scoringService.isScoringAllowed());
    }

    @Test
    @DisplayName("C3: 成绩公布阶段一律禁止计分，只读查询仍可用")
    void isScoringAllowed_resultsPublished_false() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.RESULTS_PUBLISHED);
        when(firstBloodMapper.selectAllRecords()).thenReturn(List.of());

        assertFalse(scoringService.isScoringAllowed());
        assertNotNull(scoringService.listFirstBloodRecords());
    }

    @Test
    @DisplayName("赛中 RUNNING 允许计分")
    void isScoringAllowed_running_true() {
        when(contestTimeUtil.getCurrentStatus()).thenReturn(ContestTimeUtil.ContestStatus.RUNNING);
        assertTrue(scoringService.isScoringAllowed());
    }

    @Test
    @DisplayName("D1-D2: getOverview 复用表现场聚合五个指标，不新建统计表")
    void getOverview_aggregatesFromExistingTables() {
        stubConfigFull(60, 10, 15, true, "Asia/Shanghai");
        when(submissionMapper.countAllCorrect()).thenReturn(42);
        when(firstBloodMapper.countAll()).thenReturn(3);
        when(firstBloodMapper.sumBonusAwarded()).thenReturn(45);

        Question qBase = createQuestion(100);
        qBase.setIsActive(true);
        Question qMin = createQuestion(100);
        qMin.setIsActive(true);
        qMin.setId(200);
        Question qInactive = createQuestion(100);
        qInactive.setIsActive(false);
        qInactive.setId(300);
        when(questionMapper.selectAll()).thenReturn(List.of(qBase, qMin, qInactive));
        when(submissionMapper.countCorrectByQuestionId(questionId)).thenReturn(0);
        when(submissionMapper.countCorrectByQuestionId(200)).thenReturn(5);

        ScoringOverviewDTO overview = scoringService.getOverview();

        assertEquals(42, overview.getTotalCorrectSolves());
        assertEquals(3, overview.getQuestionsWithFirstBlood());
        assertEquals(1, overview.getQuestionsAtBasePoints());
        assertEquals(1, overview.getQuestionsAtMinPoints());
        assertEquals(45, overview.getTotalFirstBloodBonusAwarded());
    }

    @Test
    @DisplayName("配置缺失时回退默认值（min=1, decay=0, bonus=0, freeze=true, tz=Asia/Shanghai）")
    void getScoringConfig_missingKeys_fallbacksToDefaults() {
        when(contestConfigMapper.selectByKey(anyString())).thenReturn(null);

        ScoringConfigDTO config = scoringService.getScoringConfig();

        assertEquals(1, config.getMinPoints());
        assertEquals(0, config.getDecayStep());
        assertEquals(0, config.getFirstBloodBonus());
        assertTrue(config.getFreezeOnEnd());
        assertEquals("Asia/Shanghai", config.getOverviewTimezone());
    }

    @Test
    @DisplayName("updateScoringConfig 校验非负并写入五个 scoring.* 键")
    void updateScoringConfig_validatesAndUpsertsKeys() {
        ScoringConfigDTO config = ScoringConfigDTO.builder()
                .minPoints(10).decayStep(5).firstBloodBonus(20)
                .freezeOnEnd(false).overviewTimezone("UTC").build();

        scoringService.updateScoringConfig(config);

        verify(contestConfigMapper, times(5)).upsert(any(ContestConfig.class));
        verify(contestConfigMapper).upsert(argThat(c -> ScoringService.KEY_MIN_POINTS.equals(c.getConfigKey()) && "10".equals(c.getConfigValue())));
        verify(contestConfigMapper).upsert(argThat(c -> ScoringService.KEY_DECAY_STEP.equals(c.getConfigKey()) && "5".equals(c.getConfigValue())));
        verify(contestConfigMapper).upsert(argThat(c -> ScoringService.KEY_FIRST_BLOOD_BONUS.equals(c.getConfigKey()) && "20".equals(c.getConfigValue())));
        verify(contestConfigMapper).upsert(argThat(c -> ScoringService.KEY_FREEZE_ON_END.equals(c.getConfigKey()) && "false".equals(c.getConfigValue())));
        verify(contestConfigMapper).upsert(argThat(c -> ScoringService.KEY_OVERVIEW_TIMEZONE.equals(c.getConfigKey()) && "UTC".equals(c.getConfigValue())));
    }

    @Test
    @DisplayName("updateScoringConfig 拒绝负数；freezeOnEnd/null 时区采用默认值")
    void updateScoringConfig_rejectsNegativeAndDefaults() {
        assertThrows(IllegalArgumentException.class,
                () -> scoringService.updateScoringConfig(ScoringConfigDTO.builder().minPoints(-1).decayStep(0).firstBloodBonus(0).build()));
        assertThrows(IllegalArgumentException.class,
                () -> scoringService.updateScoringConfig(ScoringConfigDTO.builder().minPoints(1).decayStep(-1).firstBloodBonus(0).build()));
        assertThrows(IllegalArgumentException.class,
                () -> scoringService.updateScoringConfig(ScoringConfigDTO.builder().minPoints(1).decayStep(0).firstBloodBonus(-1).build()));

        ScoringConfigDTO config = ScoringConfigDTO.builder()
                .minPoints(1).decayStep(0).firstBloodBonus(0).freezeOnEnd(null).overviewTimezone(null).build();
        scoringService.updateScoringConfig(config);
        verify(contestConfigMapper).upsert(argThat(c -> ScoringService.KEY_FREEZE_ON_END.equals(c.getConfigKey()) && "true".equals(c.getConfigValue())));
        verify(contestConfigMapper).upsert(argThat(c -> ScoringService.KEY_OVERVIEW_TIMEZONE.equals(c.getConfigKey()) && "Asia/Shanghai".equals(c.getConfigValue())));
    }

    @Test
    @DisplayName("listFirstBloodRecords → 委托 Mapper 返回只读列表")
    void listFirstBloodRecords_delegatesToMapper() {
        FirstBloodRecordDTO record = new FirstBloodRecordDTO();
        when(firstBloodMapper.selectAllRecords()).thenReturn(List.of(record));
        List<FirstBloodRecordDTO> result = scoringService.listFirstBloodRecords();
        assertEquals(1, result.size());
    }

    private ContestUser createContestUser(int initialScore) {
        ContestUser contestUser = new ContestUser();
        contestUser.setId(contestUserId);
        contestUser.setUserId(userId);
        contestUser.setTotalScore(initialScore);
        return contestUser;
    }

    private Question createQuestion(Integer points) {
        Question question = new Question();
        question.setId(questionId);
        question.setPoints(points);
        question.setIsActive(true);
        return question;
    }
}
