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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态计分与一血服务（独立于 ContestService，遵循条款 A2）。第一轮冻结的唯一计分 Service，
 * P1 在其内扩展，不新建第二套表 / Service（§3 P1 / A4）。
 *
 * <p>P0（§3 / §5）：基础分入账（语义来自 {@code questions.points}，禁止写死 +1）、一血落库（同题仅一条，
 * 依赖 {@code first_bloods.question_id} 唯一约束）、管理端只读。
 *
 * <p>P1（条款 B1–B5 / §10）：
 * <ul>
 *   <li>B1：{@code current_points = max(scoring.min_points, 基础分 - scoring.decay_step × solve_count)}，
 *       其中 solve_count 取「入账前」（见 {@link #resolveAccruedPoints}）。</li>
 *   <li>B2：已入账分数永不因后续衰减被改写——本方法一次性把该次入账分写入总分，之后不回算。</li>
 *   <li>B3：{@code scoring.first_blood_bonus} 仅在写入一血成功的那一次一并发放；靠一血表唯一约束去重，不建奖金表。</li>
 *   <li>B4：{@code decay_step = 0} 时退化为「恒等于基础分再夹逼 min_points」。</li>
 *   <li>B5：提示扣分与动态分值正交，仍由 HintService 负责，不折算进 decay_step。</li>
 * </ul>
 */
@Slf4j
@Service
public class ScoringService {

    // ===== 规格书 §8 已冻结的配置键（键字符串不得改名，含 scoring. 前缀）=====
    public static final String KEY_MIN_POINTS = "scoring.min_points";
    public static final String KEY_DECAY_STEP = "scoring.decay_step";
    public static final String KEY_FIRST_BLOOD_BONUS = "scoring.first_blood_bonus";
    public static final String KEY_FREEZE_ON_END = "scoring.freeze_on_end";
    public static final String KEY_OVERVIEW_TIMEZONE = "scoring.overview_timezone";

    // ===== §8 默认值（配置缺失 / 非法值时回退）=====
    private static final int DEFAULT_MIN_POINTS = 1;
    private static final int DEFAULT_DECAY_STEP = 0;
    private static final int DEFAULT_FIRST_BLOOD_BONUS = 0;
    private static final boolean DEFAULT_FREEZE_ON_END = true;
    private static final String DEFAULT_OVERVIEW_TIMEZONE = "Asia/Shanghai";

    @Autowired
    private FirstBloodMapper firstBloodMapper;

    @Autowired
    private ContestUserMapper contestUserMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private ContestConfigMapper contestConfigMapper;

    @Autowired
    private ContestTimeUtil contestTimeUtil;

    /**
     * P2 门禁（C1/C2）——计分是否允许入账，作为「钱路径」的权威判定，
     * 不依赖 Controller 层的 {@code isContestActive}（避免只拦 Controller 被 Service 绕过）。
     * <ul>
     *   <li>C1：未开始（start 之前）→ 禁止入账与一血。</li>
     *   <li>C2：已结束且 {@code scoring.freeze_on_end}=true → 冻结，禁止入账与一血；
     *       若为 false（加时赛演练）→ 仍可入账。</li>
     *   <li>比赛进行中（RUNNING）→ 允许。</li>
     * </ul>
     */
    public boolean isAccrualAllowed() {
        ContestTimeUtil.ContestStatus status = contestTimeUtil.getCurrentStatus();
        if (status == ContestTimeUtil.ContestStatus.NOT_STARTED
                || status == ContestTimeUtil.ContestStatus.READY) {
            return false; // C1：start 之前禁止产生动态分与一血
        }
        if (status == ContestTimeUtil.ContestStatus.FINISHED
                || status == ContestTimeUtil.ContestStatus.RESULTS_PUBLISHED) {
            // C2：结束后是否仍可入账取决于 freeze_on_end（默认 true=冻结）
            return !getScoringConfig().getFreezeOnEnd();
        }
        return true; // RUNNING
    }

    /**
     * 在「本次提交被判定正确且此前未答对」的同一事务路径上执行计分（由 {@link ContestService#submitAnswer} 调用）：
     * 先按全场维度尝试写入一血，再按 B1 计算入账分并累加到该参赛者总分；若本次达成一血，一并发放一血奖金（B3）。
     *
     * @return 本次实际入账分（含一血奖金，若命中）
     */
    @Transactional
    public int awardForCorrectSubmission(ContestUser contestUser, Question question) {
        // C1/C2 权威门禁：赛前或（赛后且 freeze_on_end=true）时，不产生动态分、不写一血。
        if (!isAccrualAllowed()) {
            log.info("Scoring skipped by freeze/gate (C1/C2): userId={}, questionId={}, status={}",
                    contestUser.getUserId(), question.getId(), contestTimeUtil.getCurrentStatus());
            return 0;
        }

        ScoringConfigDTO config = getScoringConfig();

        int accrued = resolveAccruedPoints(question, contestUser, config);
        int bonus = 0;
        boolean gotFirstBlood = recordFirstBloodIfAbsent(contestUser, question, config.getFirstBloodBonus());
        if (gotFirstBlood) {
            // B3：一血奖金仅在成功写入一血的这一次发放；去重由一血表唯一约束保证。
            bonus = Math.max(0, config.getFirstBloodBonus());
            accrued += bonus;
        }

        int newScore = (contestUser.getTotalScore() == null ? 0 : contestUser.getTotalScore()) + accrued;
        contestUser.setTotalScore(newScore);
        contestUserMapper.update(contestUser);

        log.info("Scored correct solve: userId={}, questionId={}, firstBlood={}, bonus={}, accrued={}, newTotal={}",
                contestUser.getUserId(), question.getId(), gotFirstBlood, bonus, accrued, newScore);
        return accrued;
    }

    /**
     * 计算本次解出的入账分（不含一血奖金）。条款 B1：
     * {@code current_points = max(min_points, 基础分 - decay_step × solve_count)}。
     *
     * <p><b>solve_count 时点 = 入账前</b>：submissions 以 {@code (contest_user_id, question_id)} 唯一，
     * 且本方法被调用时当前参赛者的正确提交行已落库，故「排除当前参赛者的正确解出人次」恰为本次入账前的 solve_count。
     * 即第 N 次解出用衰减序列第 N 档之前的值，第一次解出（solve_count=0）仍接近基础分（B1）。
     * decay_step=0 时退化为「基础分再夹逼 min_points」（B4）。已算出的入账分随即写入总分且永不回算（B2）。
     */
    private int resolveAccruedPoints(Question question, ContestUser contestUser, ScoringConfigDTO config) {
        int basePoints = question.getPoints() == null ? 0 : question.getPoints();
        int solveCountBefore = submissionMapper.countCorrectByQuestionExcludingUser(
                question.getId(), contestUser.getId());

        int decayed = basePoints - config.getDecayStep() * solveCountBefore;
        int current = Math.max(config.getMinPoints(), decayed);

        log.debug("resolveAccruedPoints: questionId={}, base={}, decayStep={}, solveCountBefore={}, minPoints={}, result={}",
                question.getId(), basePoints, config.getDecayStep(), solveCountBefore, config.getMinPoints(), current);
        return current;
    }

    /**
     * 全场维度判断该题此前是否已有正确解出（一血）；若无则写入。
     * 「同题仅一条」由 {@code first_bloods.question_id} 唯一约束保证，并发下靠约束兜底（§5 / B3）。
     *
     * @return 本次是否达成一血
     */
    private boolean recordFirstBloodIfAbsent(ContestUser contestUser, Question question, int bonusToAward) {
        if (firstBloodMapper.countByQuestionId(question.getId()) > 0) {
            return false;
        }
        FirstBlood firstBlood = new FirstBlood();
        firstBlood.setQuestionId(question.getId());
        firstBlood.setUserId(contestUser.getUserId());
        firstBlood.setContestUserId(contestUser.getId());
        // B2：记录「达成当时」实际发放的奖金分值，供 §12.5 求和；后续 config 变更不回算此行。
        firstBlood.setAwardedBonus(Math.max(0, bonusToAward));
        try {
            firstBloodMapper.insert(firstBlood);
            log.info("First blood recorded: questionId={}, userId={}, bonus={}",
                    question.getId(), contestUser.getUserId(), firstBlood.getAwardedBonus());
            return true;
        } catch (DuplicateKeyException e) {
            // 并发下另一提交已抢先写入一血；唯一约束兜底，本次不算一血，也不发放奖金（B3）。
            log.info("First blood already taken concurrently: questionId={}", question.getId());
            return false;
        }
    }

    /**
     * 管理端只读：列出全部一血记录（题目标识 + 用户标识 + 达成时间）。
     */
    public List<FirstBloodDTO> listFirstBloods() {
        List<FirstBlood> records = firstBloodMapper.selectAll();
        List<FirstBloodDTO> result = new ArrayList<>();
        for (FirstBlood fb : records) {
            FirstBloodDTO dto = new FirstBloodDTO();
            dto.setQuestionId(fb.getQuestionId());
            dto.setUserId(fb.getUserId());
            dto.setAchievedAt(fb.getAchievedAt());

            Question question = questionMapper.selectById(fb.getQuestionId());
            if (question != null) {
                dto.setQuestionTitle(question.getTitle());
            }
            User user = userMapper.selectById(fb.getUserId());
            if (user != null) {
                dto.setStudentId(user.getStudentId());
                dto.setFullName(user.getFullName());
            }
            result.add(dto);
        }
        return result;
    }

    // ===== P1 §10：计分配置读写（挂在第一轮 /scoring 前缀下，使用 §8 冻结键）=====

    /**
     * 读取动态计分配置；配置缺失或非法值回退 §8 默认值。
     */
    public ScoringConfigDTO getScoringConfig() {
        return ScoringConfigDTO.builder()
                .minPoints(getIntConfig(KEY_MIN_POINTS, DEFAULT_MIN_POINTS))
                .decayStep(getIntConfig(KEY_DECAY_STEP, DEFAULT_DECAY_STEP))
                .firstBloodBonus(getIntConfig(KEY_FIRST_BLOOD_BONUS, DEFAULT_FIRST_BLOOD_BONUS))
                .freezeOnEnd(getBoolConfig(KEY_FREEZE_ON_END, DEFAULT_FREEZE_ON_END))
                .overviewTimezone(getStringConfig(KEY_OVERVIEW_TIMEZONE, DEFAULT_OVERVIEW_TIMEZONE))
                .build();
    }

    /**
     * 写入动态计分配置；仅写入本次请求携带（非 null）的字段，键名沿用 §8 冻结字符串。
     * 非负整数字段做下限夹逼，避免非法值污染衰减公式。
     */
    @Transactional
    public ScoringConfigDTO updateScoringConfig(ScoringConfigDTO config) {
        if (config.getMinPoints() != null) {
            upsert(KEY_MIN_POINTS, String.valueOf(Math.max(0, config.getMinPoints())));
        }
        if (config.getDecayStep() != null) {
            upsert(KEY_DECAY_STEP, String.valueOf(Math.max(0, config.getDecayStep())));
        }
        if (config.getFirstBloodBonus() != null) {
            upsert(KEY_FIRST_BLOOD_BONUS, String.valueOf(Math.max(0, config.getFirstBloodBonus())));
        }
        if (config.getFreezeOnEnd() != null) {
            upsert(KEY_FREEZE_ON_END, String.valueOf(config.getFreezeOnEnd()));
        }
        if (config.getOverviewTimezone() != null && !config.getOverviewTimezone().trim().isEmpty()) {
            upsert(KEY_OVERVIEW_TIMEZONE, config.getOverviewTimezone().trim());
        }
        log.info("Scoring config updated: {}", config);
        return getScoringConfig();
    }

    private void upsert(String key, String value) {
        contestConfigMapper.upsert(ContestConfig.builder().configKey(key).configValue(value).build());
    }

    // ===== P2 §12：计分概览（D1 现场聚合，禁止新建汇总表；D2 管理端权限由 Controller 保证）=====

    /**
     * 计算 §12 五项聚合指标，全部基于 P0/P1 的 first_bloods、submissions、questions 现场计算（D1）。
     * 对空表 / 空配置安全：无数据时各项为 0。
     */
    public ScoringOverviewDTO getOverview() {
        ScoringConfigDTO config = getScoringConfig();
        int minPoints = config.getMinPoints();
        int decayStep = config.getDecayStep();

        long totalCorrectSolves = submissionMapper.countTotalCorrectSolves();          // §12.1
        long questionsWithFirstBlood = firstBloodMapper.countQuestionsWithFirstBlood(); // §12.2
        long totalFirstBloodBonusAwarded = firstBloodMapper.sumAwardedBonus();          // §12.5

        long questionsAtBasePoints = 0; // §12.3
        long questionsAtMinPoints = 0;  // §12.4
        List<QuestionSolveCount> perQuestion = submissionMapper.selectSolveCountsPerQuestion();
        for (QuestionSolveCount q : perQuestion) {
            int base = q.getPoints() == null ? 0 : q.getPoints();
            long solveCount = q.getSolveCount() == null ? 0L : q.getSolveCount();
            // 与 B1 同一公式现场计算当前分：max(min, base - step × solveCount)
            long decayed = base - (long) decayStep * solveCount;
            long current = Math.max(minPoints, decayed);
            if (current == base) {
                questionsAtBasePoints++;
            }
            if (current == minPoints) {
                questionsAtMinPoints++;
            }
        }

        return ScoringOverviewDTO.builder()
                .totalCorrectSolves(totalCorrectSolves)
                .questionsWithFirstBlood(questionsWithFirstBlood)
                .questionsAtBasePoints(questionsAtBasePoints)
                .questionsAtMinPoints(questionsAtMinPoints)
                .totalFirstBloodBonusAwarded(totalFirstBloodBonusAwarded)
                .build();
    }

    private int getIntConfig(String key, int defaultValue) {
        ContestConfig config = contestConfigMapper.selectByKey(key);
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(config.getConfigValue().trim());
            return Math.max(0, value); // 非负整数语义，非法负值回退到 0 下限
        } catch (NumberFormatException e) {
            log.warn("Invalid int config for key={}, value={}, fallback to default={}",
                    key, config.getConfigValue(), defaultValue);
            return defaultValue;
        }
    }

    private boolean getBoolConfig(String key, boolean defaultValue) {
        ContestConfig config = contestConfigMapper.selectByKey(key);
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(config.getConfigValue().trim());
    }

    private String getStringConfig(String key, String defaultValue) {
        ContestConfig config = contestConfigMapper.selectByKey(key);
        return (config != null && config.getConfigValue() != null) ? config.getConfigValue() : defaultValue;
    }
}
