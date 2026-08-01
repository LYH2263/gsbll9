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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private SubmissionMapper submissionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ContestConfigMapper contestConfigMapper;

    @Autowired
    private ContestTimeUtil contestTimeUtil;

    @Autowired
    @Lazy
    private ScoringService self;

    public boolean isScoringAllowed() {
        ContestTimeUtil.ContestStatus status = contestTimeUtil.getCurrentStatus();
        switch (status) {
            case NOT_STARTED:
            case READY:
                return false;
            case RUNNING:
                return true;
            case FINISHED:
                return !isFreezeOnEnd();
            case RESULTS_PUBLISHED:
            default:
                return false;
        }
    }

    public String getScoringBlockedReason() {
        ContestTimeUtil.ContestStatus status = contestTimeUtil.getCurrentStatus();
        switch (status) {
            case NOT_STARTED:
            case READY:
                return "比赛尚未开始，暂不能提交";
            case FINISHED:
                return isFreezeOnEnd() ? "比赛已结束，计分已冻结" : null;
            case RESULTS_PUBLISHED:
                return "成绩已公布，计分已冻结";
            default:
                return null;
        }
    }

    @Transactional
    public void awardCorrectSolve(ContestUser contestUser, Question question) {
        if (contestUser == null || question == null) {
            return;
        }
        if (!isScoringAllowed()) {
            log.warn("Scoring blocked by contest phase: questionId={}, contestUserId={}",
                    question.getId(), contestUser.getId());
            throw new IllegalStateException("当前赛期不允许计分");
        }

        int basePoints = question.getPoints() != null ? question.getPoints() : 0;
        ScoringConfigDTO config = getScoringConfig();

        int solveCountBefore = countSolvesBeforeThis(contestUser.getId(), question.getId());
        int currentPoints = calculateCurrentPoints(basePoints, solveCountBefore, config);

        int newScore = contestUser.getTotalScore() + currentPoints;
        contestUser.setTotalScore(newScore);
        contestUserMapper.update(contestUser);
        log.info("Current points awarded: contestUserId={}, questionId={}, basePoints={}, solveCountBefore={}, currentPoints={}, newScore={}",
                contestUser.getId(), question.getId(), basePoints, solveCountBefore, currentPoints, newScore);

        int bonus = config.getFirstBloodBonus() != null ? config.getFirstBloodBonus() : 0;
        boolean isFirstBlood = self.recordFirstBlood(contestUser, question, bonus);
        if (isFirstBlood && bonus > 0) {
            int scoreWithBonus = contestUser.getTotalScore() + bonus;
            contestUser.setTotalScore(scoreWithBonus);
            contestUserMapper.update(contestUser);
            log.info("First blood bonus awarded: contestUserId={}, questionId={}, bonus={}, newScore={}",
                    contestUser.getId(), question.getId(), bonus, scoreWithBonus);
        }
    }

    private int countSolvesBeforeThis(Integer contestUserId, Integer questionId) {
        Integer count = submissionMapper.countCorrectByQuestionExcludeUser(questionId, contestUserId);
        return count != null ? count : 0;
    }

    int calculateCurrentPoints(int basePoints, int solveCount, ScoringConfigDTO config) {
        int minPoints = config.getMinPoints() != null ? config.getMinPoints() : DEFAULT_MIN_POINTS;
        int decayStep = config.getDecayStep() != null ? config.getDecayStep() : DEFAULT_DECAY_STEP;
        int raw = basePoints - decayStep * solveCount;
        return Math.max(minPoints, raw);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordFirstBlood(ContestUser contestUser, Question question, int bonusAwarded) {
        try {
            FirstBlood firstBlood = new FirstBlood();
            firstBlood.setQuestionId(question.getId());
            firstBlood.setContestUserId(contestUser.getId());
            firstBlood.setUserId(contestUser.getUserId());
            firstBlood.setBonusAwarded(bonusAwarded);
            firstBloodMapper.insert(firstBlood);
            log.info("First blood recorded: questionId={}, userId={}, contestUserId={}, bonusAwarded={}",
                    question.getId(), contestUser.getUserId(), contestUser.getId(), bonusAwarded);
            return true;
        } catch (DuplicateKeyException e) {
            log.info("First blood already taken for questionId={}, no bonus for contestUserId={}",
                    question.getId(), contestUser.getId());
            return false;
        }
    }

    public List<FirstBloodRecordDTO> listFirstBloodRecords() {
        return firstBloodMapper.selectAllRecords();
    }

    public ScoringConfigDTO getScoringConfig() {
        return ScoringConfigDTO.builder()
                .minPoints(readIntConfig(KEY_MIN_POINTS, DEFAULT_MIN_POINTS))
                .decayStep(readIntConfig(KEY_DECAY_STEP, DEFAULT_DECAY_STEP))
                .firstBloodBonus(readIntConfig(KEY_FIRST_BLOOD_BONUS, DEFAULT_FIRST_BLOOD_BONUS))
                .freezeOnEnd(readBoolConfig(KEY_FREEZE_ON_END, DEFAULT_FREEZE_ON_END))
                .overviewTimezone(readStringConfig(KEY_OVERVIEW_TIMEZONE, DEFAULT_OVERVIEW_TIMEZONE))
                .build();
    }

    public void updateScoringConfig(ScoringConfigDTO config) {
        if (config.getMinPoints() == null || config.getMinPoints() < 0) {
            throw new IllegalArgumentException("min_points 必须为非负整数");
        }
        if (config.getDecayStep() == null || config.getDecayStep() < 0) {
            throw new IllegalArgumentException("decay_step 必须为非负整数");
        }
        if (config.getFirstBloodBonus() == null || config.getFirstBloodBonus() < 0) {
            throw new IllegalArgumentException("first_blood_bonus 必须为非负整数");
        }
        if (config.getFreezeOnEnd() == null) {
            config.setFreezeOnEnd(DEFAULT_FREEZE_ON_END);
        }
        if (config.getOverviewTimezone() == null || config.getOverviewTimezone().trim().isEmpty()) {
            config.setOverviewTimezone(DEFAULT_OVERVIEW_TIMEZONE);
        }
        upsertConfig(KEY_MIN_POINTS, String.valueOf(config.getMinPoints()));
        upsertConfig(KEY_DECAY_STEP, String.valueOf(config.getDecayStep()));
        upsertConfig(KEY_FIRST_BLOOD_BONUS, String.valueOf(config.getFirstBloodBonus()));
        upsertConfig(KEY_FREEZE_ON_END, String.valueOf(config.getFreezeOnEnd()));
        upsertConfig(KEY_OVERVIEW_TIMEZONE, config.getOverviewTimezone().trim());
        log.info("Scoring config updated: {}", config);
    }

    public boolean isFreezeOnEnd() {
        return readBoolConfig(KEY_FREEZE_ON_END, DEFAULT_FREEZE_ON_END);
    }

    public ScoringOverviewDTO getOverview() {
        ScoringConfigDTO config = getScoringConfig();

        int totalCorrectSolves = 0;
        Integer correctSum = submissionMapper.countAllCorrect();
        if (correctSum != null) {
            totalCorrectSolves = correctSum;
        }

        int questionsWithFirstBlood = firstBloodMapper.countAll();
        Integer bonusSum = firstBloodMapper.sumBonusAwarded();
        int totalFirstBloodBonusAwarded = bonusSum != null ? bonusSum : 0;

        List<Question> allQuestions = questionMapper.selectAll();
        int questionsAtBase = 0;
        int questionsAtMin = 0;
        int minPoints = config.getMinPoints() != null ? config.getMinPoints() : DEFAULT_MIN_POINTS;
        for (Question q : allQuestions) {
            if (!Boolean.TRUE.equals(q.getIsActive())) {
                continue;
            }
            int base = q.getPoints() != null ? q.getPoints() : 0;
            Integer solves = submissionMapper.countCorrectByQuestionId(q.getId());
            int solveCount = solves != null ? solves : 0;
            int current = calculateCurrentPoints(base, solveCount, config);
            if (current == base) {
                questionsAtBase++;
            }
            if (current == minPoints) {
                questionsAtMin++;
            }
        }

        return ScoringOverviewDTO.builder()
                .totalCorrectSolves(totalCorrectSolves)
                .questionsWithFirstBlood(questionsWithFirstBlood)
                .questionsAtBasePoints(questionsAtBase)
                .questionsAtMinPoints(questionsAtMin)
                .totalFirstBloodBonusAwarded(totalFirstBloodBonusAwarded)
                .build();
    }

    private int readIntConfig(String key, int defaultValue) {
        ContestConfig config = contestConfigMapper.selectByKey(key);
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(config.getConfigValue().trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer for config key={}, value={}, using default={}", key, config.getConfigValue(), defaultValue);
            return defaultValue;
        }
    }

    private boolean readBoolConfig(String key, boolean defaultValue) {
        ContestConfig config = contestConfigMapper.selectByKey(key);
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        String value = config.getConfigValue().trim();
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        log.warn("Invalid boolean for config key={}, value={}, using default={}", key, value, defaultValue);
        return defaultValue;
    }

    private String readStringConfig(String key, String defaultValue) {
        ContestConfig config = contestConfigMapper.selectByKey(key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().trim().isEmpty()) {
            return defaultValue;
        }
        return config.getConfigValue().trim();
    }

    private void upsertConfig(String key, String value) {
        ContestConfig config = ContestConfig.builder()
                .configKey(key)
                .configValue(value)
                .build();
        contestConfigMapper.upsert(config);
    }
}
