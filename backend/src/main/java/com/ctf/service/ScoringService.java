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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ScoringService {

    static final String KEY_MIN_POINTS = "scoring.min_points";
    static final String KEY_DECAY_STEP = "scoring.decay_step";
    static final String KEY_FIRST_BLOOD_BONUS = "scoring.first_blood_bonus";
    static final String KEY_FREEZE_ON_END = "scoring.freeze_on_end";
    static final String KEY_OVERVIEW_TIMEZONE = "scoring.overview_timezone";

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
    private SubmissionMapper submissionMapper;

    @Autowired
    private ContestConfigMapper contestConfigMapper;

    @Autowired
    private ContestTimeUtil contestTimeUtil;

    /**
     * 赛期门禁（C1-C3）。权威判断放在 Service 层，避免只拦 Controller 被绕过。
     * NOT_STARTED/READY：比赛未开始，禁止计分（C1）。
     * RUNNING：允许。
     * FINISHED：仅当 scoring.freeze_on_end=false 时允许（加时赛，C2）；默认 true 冻结。
     * RESULTS_PUBLISHED：成绩已公布，只读，禁止计分（C3）。
     */
    public void assertScoringAllowed() {
        ContestTimeUtil.ContestStatus status = contestTimeUtil.getCurrentStatus();
        switch (status) {
            case NOT_STARTED:
            case READY:
                throw new IllegalArgumentException("比赛未开始，暂不计分");
            case RUNNING:
                return;
            case FINISHED:
                if (isFreezeOnEnd()) {
                    throw new IllegalArgumentException("比赛已结束，计分已冻结");
                }
                return;
            case RESULTS_PUBLISHED:
                throw new IllegalArgumentException("成绩已公布，计分已冻结");
            default:
                throw new IllegalArgumentException("当前赛期不允许计分");
        }
    }

    /**
     * 供状态接口/前端判断当前是否允许提交计分（含加时赛）。
     */
    public boolean isScoringActive() {
        try {
            assertScoringAllowed();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Transactional
    public ScoringResult awardSolve(Integer contestUserId, Integer questionId, Integer userId, Integer basePoints) {
        assertScoringAllowed();

        Question locked = questionMapper.selectByIdForUpdate(questionId);
        if (locked == null) {
            throw new IllegalArgumentException("Question not found: " + questionId);
        }

        int base = basePoints != null ? basePoints : 0;
        int minPoints = getNonNegativeConfig(KEY_MIN_POINTS, DEFAULT_MIN_POINTS);
        int decayStep = getNonNegativeConfig(KEY_DECAY_STEP, DEFAULT_DECAY_STEP);
        int bonusConfig = getNonNegativeConfig(KEY_FIRST_BLOOD_BONUS, DEFAULT_FIRST_BLOOD_BONUS);

        int solveCount = submissionMapper.countCorrectByQuestionId(questionId);
        int currentPoints = Math.max(minPoints, base - decayStep * solveCount);

        boolean isFirstBlood = recordFirstBlood(questionId, userId, contestUserId, bonusConfig);
        int bonusPoints = isFirstBlood ? bonusConfig : 0;

        contestUserMapper.incrementScore(contestUserId, currentPoints + bonusPoints);

        log.info("Solve awarded: contestUserId={}, questionId={}, userId={}, basePoints={}, solveCountBefore={}, " +
                        "minPoints={}, decayStep={}, currentPoints={}, firstBlood={}, bonusPoints={}",
                contestUserId, questionId, userId, base, solveCount,
                minPoints, decayStep, currentPoints, isFirstBlood, bonusPoints);

        return new ScoringResult(currentPoints, bonusPoints, isFirstBlood);
    }

    private boolean recordFirstBlood(Integer questionId, Integer userId, Integer contestUserId, int bonusPoints) {
        FirstBlood firstBlood = new FirstBlood();
        firstBlood.setQuestionId(questionId);
        firstBlood.setUserId(userId);
        firstBlood.setContestUserId(contestUserId);
        firstBlood.setBonusPoints(bonusPoints);
        try {
            firstBloodMapper.insert(firstBlood);
            return true;
        } catch (DuplicateKeyException e) {
            log.info("First blood already exists for questionId={}, current solver userId={} is not first blood",
                    questionId, userId);
            return false;
        }
    }

    public List<FirstBloodDTO> getAllFirstBloods() {
        return firstBloodMapper.selectAllWithDetails();
    }

    /**
     * P2 概览：基于一血表/submissions/questions 现场聚合，不新建汇总表（D1）。
     */
    public ScoringOverviewDTO getOverview() {
        int minPoints = getNonNegativeConfig(KEY_MIN_POINTS, DEFAULT_MIN_POINTS);
        int decayStep = getNonNegativeConfig(KEY_DECAY_STEP, DEFAULT_DECAY_STEP);

        int totalCorrectSolves = 0;
        int questionsAtBase = 0;
        int questionsAtMin = 0;

        List<QuestionSolveStat> stats = firstBloodMapper.selectActiveQuestionSolveStats();
        for (QuestionSolveStat stat : stats) {
            int base = stat.getBasePoints() != null ? stat.getBasePoints() : 0;
            int correctCount = stat.getCorrectCount() != null ? stat.getCorrectCount() : 0;
            totalCorrectSolves += correctCount;

            int currentPoints = Math.max(minPoints, base - decayStep * correctCount);
            if (currentPoints == base) {
                questionsAtBase++;
            }
            if (currentPoints == minPoints) {
                questionsAtMin++;
            }
        }

        int questionsWithFirstBlood = firstBloodMapper.countAll();
        int totalBonusAwarded = firstBloodMapper.sumBonusPoints();

        return ScoringOverviewDTO.builder()
                .totalCorrectSolves(totalCorrectSolves)
                .questionsWithFirstBlood(questionsWithFirstBlood)
                .questionsAtBasePoints(questionsAtBase)
                .questionsAtMinPoints(questionsAtMin)
                .totalFirstBloodBonusAwarded(totalBonusAwarded)
                .build();
    }

    public ScoringConfigDTO getConfig() {
        return ScoringConfigDTO.builder()
                .minPoints(getNonNegativeConfig(KEY_MIN_POINTS, DEFAULT_MIN_POINTS))
                .decayStep(getNonNegativeConfig(KEY_DECAY_STEP, DEFAULT_DECAY_STEP))
                .firstBloodBonus(getNonNegativeConfig(KEY_FIRST_BLOOD_BONUS, DEFAULT_FIRST_BLOOD_BONUS))
                .freezeOnEnd(getBooleanConfig(KEY_FREEZE_ON_END, DEFAULT_FREEZE_ON_END))
                .overviewTimezone(getStringConfig(KEY_OVERVIEW_TIMEZONE, DEFAULT_OVERVIEW_TIMEZONE))
                .build();
    }

    @Transactional
    public void updateConfig(ScoringConfigDTO config) {
        validateNonNegative(config.getMinPoints(), "min_points");
        validateNonNegative(config.getDecayStep(), "decay_step");
        validateNonNegative(config.getFirstBloodBonus(), "first_blood_bonus");
        if (config.getFreezeOnEnd() == null) {
            throw new IllegalArgumentException("freeze_on_end must be true or false");
        }
        if (config.getOverviewTimezone() == null || config.getOverviewTimezone().trim().isEmpty()) {
            throw new IllegalArgumentException("overview_timezone must not be empty");
        }

        upsertConfig(KEY_MIN_POINTS, String.valueOf(config.getMinPoints()));
        upsertConfig(KEY_DECAY_STEP, String.valueOf(config.getDecayStep()));
        upsertConfig(KEY_FIRST_BLOOD_BONUS, String.valueOf(config.getFirstBloodBonus()));
        upsertConfig(KEY_FREEZE_ON_END, String.valueOf(config.getFreezeOnEnd()));
        upsertConfig(KEY_OVERVIEW_TIMEZONE, config.getOverviewTimezone().trim());
        log.info("Scoring config updated: {}", config);
    }

    boolean isFreezeOnEnd() {
        return getBooleanConfig(KEY_FREEZE_ON_END, DEFAULT_FREEZE_ON_END);
    }

    private void validateNonNegative(Integer value, String name) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(name + " must be a non-negative integer");
        }
    }

    private void upsertConfig(String key, String value) {
        ContestConfig config = ContestConfig.builder()
                .configKey(key)
                .configValue(value)
                .build();
        contestConfigMapper.upsert(config);
    }

    private int getNonNegativeConfig(String key, int defaultValue) {
        ContestConfig config = contestConfigMapper.selectByKey(key);
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(config.getConfigValue().trim());
            return value >= 0 ? value : defaultValue;
        } catch (NumberFormatException e) {
            log.warn("Invalid integer for config key {}: {}, using default {}", key, config.getConfigValue(), defaultValue);
            return defaultValue;
        }
    }

    private boolean getBooleanConfig(String key, boolean defaultValue) {
        ContestConfig config = contestConfigMapper.selectByKey(key);
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(config.getConfigValue().trim());
    }

    private String getStringConfig(String key, String defaultValue) {
        ContestConfig config = contestConfigMapper.selectByKey(key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().trim().isEmpty()) {
            return defaultValue;
        }
        return config.getConfigValue().trim();
    }
}
