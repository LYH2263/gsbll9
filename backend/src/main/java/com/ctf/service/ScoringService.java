package com.ctf.service;

import com.ctf.dto.scoring.FirstBloodItem;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 计分与一血模块 Service。
 * P0：基础分入账 + 一血落库。
 * P1（条款 B1–B5）：动态衰减 current_points = max(scoring.min_points, 基础分 - scoring.decay_step × solve_count)，
 *   solve_count 取「入账前」的全场正确解出次数（调用方须在本次提交记录落库前调用本方法）；
 *   一血奖金仅在 first_bloods 唯一约束放行写入的那一次一并入账（B3）；
 *   已入账分永不回算（B2）；提示扣分与本模块正交（B5）。
 * P2（条款 C1–C3、§12/D1–D2）：赛期门禁（复用 ContestTimeUtil）+ 赛后冻结（scoring.freeze_on_end）
 *   + 管理端计分概览（现场聚合，无冗余汇总表）。
 */
@Slf4j
@Service
public class ScoringService {

    /** §8 已冻结配置键（字符串不得更改） */
    public static final String KEY_MIN_POINTS = "scoring.min_points";
    public static final String KEY_DECAY_STEP = "scoring.decay_step";
    public static final String KEY_FIRST_BLOOD_BONUS = "scoring.first_blood_bonus";
    public static final String KEY_FREEZE_ON_END = "scoring.freeze_on_end";
    public static final String KEY_OVERVIEW_TIMEZONE = "scoring.overview_timezone";

    /** §8 默认值 */
    private static final int DEFAULT_MIN_POINTS = 1;
    private static final int DEFAULT_DECAY_STEP = 0;
    private static final int DEFAULT_FIRST_BLOOD_BONUS = 0;
    private static final boolean DEFAULT_FREEZE_ON_END = true;

    @Autowired
    private FirstBloodMapper firstBloodMapper;

    @Autowired
    private ContestUserMapper contestUserMapper;

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ContestConfigMapper contestConfigMapper;

    @Autowired
    private ContestTimeUtil contestTimeUtil;

    /**
     * C1/C2 赛期门禁：当前是否允许产生新的正确计入与一血（复用 ContestTimeUtil，不另建时间源）。
     * C1：start 之前（NOT_STARTED / READY）一律关闭；
     * C2：end 之后（FINISHED / RESULTS_PUBLISHED）当 scoring.freeze_on_end=true（默认）关闭，
     *     freeze_on_end=false 时仍开放（加时赛演练）。
     */
    public boolean isScoringOpen() {
        ContestTimeUtil.ContestStatus status = contestTimeUtil.getCurrentStatus();
        switch (status) {
            case RUNNING:
                return true;
            case FINISHED:
            case RESULTS_PUBLISHED:
                return !getBooleanConfig(KEY_FREEZE_ON_END, DEFAULT_FREEZE_ON_END);
            case NOT_STARTED:
            case READY:
            default:
                return false;
        }
    }

    /**
     * 在一次提交被判定为正确后入账：按 B1 计算入账分，写入一血（若全场首个，B3 附奖金）并累加总分。
     * 必须在调用方的事务路径内、且在本次提交记录写入 submissions 之前执行，
     * 以保证 solve_count 为「入账前」时点（B1），同时一血落库与加分不撕裂。
     * 赛期门禁关闭时抛 IllegalArgumentException，整个提交事务随之回滚，
     * 不会出现「提交已标正确但分数或一血部分落库」的撕裂状态。
     *
     * @return 本次入账分
     */
    @Transactional
    public int recordCorrectSolve(ContestUser contestUser, Question question) {
        if (!isScoringOpen()) {
            log.warn("Scoring rejected by phase gate: userId={}, questionId={}", contestUser.getUserId(), question.getId());
            throw new IllegalArgumentException("Contest is not active");
        }

        // 基础分字段：questions.points（第一轮约定清单冻结）
        int basePoints = question.getPoints() != null ? question.getPoints() : 0;
        // B1：入账前 solve_count（此时本次正确提交尚未写入 submissions）
        int solveCount = submissionMapper.countCorrectByQuestionId(question.getId());
        int minPoints = getIntConfig(KEY_MIN_POINTS, DEFAULT_MIN_POINTS);
        int decayStep = getIntConfig(KEY_DECAY_STEP, DEFAULT_DECAY_STEP);
        int awardedPoints = Math.max(minPoints, basePoints - decayStep * solveCount);

        int firstBloodBonus = getIntConfig(KEY_FIRST_BLOOD_BONUS, DEFAULT_FIRST_BLOOD_BONUS);
        FirstBlood firstBlood = new FirstBlood();
        firstBlood.setQuestionId(question.getId());
        firstBlood.setUserId(contestUser.getUserId());
        firstBlood.setContestUserId(contestUser.getId());
        // 奖金快照落列：§12 指标 total_first_blood_bonus_awarded 的数据源，不受后续配置变更影响（B2 同哲学）
        firstBlood.setBonusAwarded(firstBloodBonus);
        // B3：依赖 unique_first_blood_question 唯一约束判定一血，命中才发奖金，不可重复刷
        boolean isFirstBlood = firstBloodMapper.insertIgnore(firstBlood) > 0;
        if (isFirstBlood) {
            awardedPoints += firstBloodBonus;
        }

        // 原子累加，避免并发解出时 read-modify-write 丢失更新
        contestUserMapper.addScore(contestUser.getId(), awardedPoints);
        contestUser.setTotalScore(contestUser.getTotalScore() + awardedPoints);

        log.info("Correct solve recorded: userId={}, questionId={}, basePoints={}, solveCount(before)={}, awardedPoints={}, firstBlood={}, newTotalScore={}",
                contestUser.getUserId(), question.getId(), basePoints, solveCount, awardedPoints, isFirstBlood, contestUser.getTotalScore());
        return awardedPoints;
    }

    /**
     * 管理端只读：全场各题一血列表（用户标识 + 达成时间）。
     */
    public List<FirstBloodItem> getFirstBloodList() {
        return firstBloodMapper.selectAllWithDetail();
    }

    /**
     * 管理端只读：P2 计分概览（§12 五项指标，D1 现场聚合，无冗余汇总表）。
     */
    public ScoringOverviewDTO getOverview() {
        int minPoints = getIntConfig(KEY_MIN_POINTS, DEFAULT_MIN_POINTS);
        int decayStep = getIntConfig(KEY_DECAY_STEP, DEFAULT_DECAY_STEP);

        List<QuestionSolveCount> solveCounts = submissionMapper.countCorrectGroupByQuestion();
        Map<Integer, Integer> solveCountByQuestion = new HashMap<>();
        int totalCorrectSolves = 0;
        for (QuestionSolveCount row : solveCounts) {
            solveCountByQuestion.put(row.getQuestionId(), row.getSolveCount());
            totalCorrectSolves += row.getSolveCount();
        }

        int questionsAtBasePoints = 0;
        int questionsAtMinPoints = 0;
        for (Question question : questionMapper.selectAll()) {
            int basePoints = question.getPoints() != null ? question.getPoints() : 0;
            int solveCount = solveCountByQuestion.getOrDefault(question.getId(), 0);
            int currentPoints = Math.max(minPoints, basePoints - decayStep * solveCount);
            if (currentPoints == basePoints) {
                questionsAtBasePoints++;
            }
            if (currentPoints == minPoints) {
                questionsAtMinPoints++;
            }
        }

        ScoringOverviewDTO overview = new ScoringOverviewDTO();
        overview.setTotalCorrectSolves(totalCorrectSolves);
        overview.setQuestionsWithFirstBlood(firstBloodMapper.countAll());
        overview.setQuestionsAtBasePoints(questionsAtBasePoints);
        overview.setQuestionsAtMinPoints(questionsAtMinPoints);
        overview.setTotalFirstBloodBonusAwarded(firstBloodMapper.sumBonusAwarded());
        return overview;
    }

    /**
     * 管理端：读取计分配置（§8 键，缺省回退默认值）。
     */
    public ScoringConfigDTO getScoringConfig() {
        return new ScoringConfigDTO(
                getIntConfig(KEY_MIN_POINTS, DEFAULT_MIN_POINTS),
                getIntConfig(KEY_DECAY_STEP, DEFAULT_DECAY_STEP),
                getIntConfig(KEY_FIRST_BLOOD_BONUS, DEFAULT_FIRST_BLOOD_BONUS));
    }

    /**
     * 管理端：写入计分配置（§8 键，均为非负整数）。
     */
    @Transactional
    public void updateScoringConfig(ScoringConfigDTO config) {
        validateNonNegative("minPoints", config.getMinPoints());
        validateNonNegative("decayStep", config.getDecayStep());
        validateNonNegative("firstBloodBonus", config.getFirstBloodBonus());

        upsertConfig(KEY_MIN_POINTS, config.getMinPoints());
        upsertConfig(KEY_DECAY_STEP, config.getDecayStep());
        upsertConfig(KEY_FIRST_BLOOD_BONUS, config.getFirstBloodBonus());
        log.info("Scoring config updated: minPoints={}, decayStep={}, firstBloodBonus={}",
                config.getMinPoints(), config.getDecayStep(), config.getFirstBloodBonus());
    }

    private void validateNonNegative(String field, Integer value) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(field + " must be a non-negative integer");
        }
    }

    private int getIntConfig(String key, int defaultValue) {
        try {
            ContestConfig config = contestConfigMapper.selectByKey(key);
            if (config == null || config.getConfigValue() == null) {
                return defaultValue;
            }
            return Integer.parseInt(config.getConfigValue().trim());
        } catch (Exception e) {
            log.warn("Failed to parse scoring config key={}, fallback to default {}", key, defaultValue);
            return defaultValue;
        }
    }

    private boolean getBooleanConfig(String key, boolean defaultValue) {
        try {
            ContestConfig config = contestConfigMapper.selectByKey(key);
            if (config == null || config.getConfigValue() == null) {
                return defaultValue;
            }
            return Boolean.parseBoolean(config.getConfigValue().trim());
        } catch (Exception e) {
            log.warn("Failed to parse scoring config key={}, fallback to default {}", key, defaultValue);
            return defaultValue;
        }
    }

    private void upsertConfig(String key, Integer value) {
        ContestConfig config = ContestConfig.builder()
                .configKey(key)
                .configValue(String.valueOf(value))
                .build();
        contestConfigMapper.upsert(config);
    }
}
