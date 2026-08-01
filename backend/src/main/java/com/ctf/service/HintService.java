package com.ctf.service;

import com.ctf.dto.hint.HintDTO;
import com.ctf.entity.ContestUser;
import com.ctf.entity.Hint;
import com.ctf.entity.HintUnlock;
import com.ctf.mapper.ContestUserMapper;
import com.ctf.mapper.HintMapper;
import com.ctf.mapper.HintUnlockMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class HintService {

    @Autowired
    private HintMapper hintMapper;

    @Autowired
    private HintUnlockMapper hintUnlockMapper;

    @Autowired
    private ContestUserMapper contestUserMapper;

    public List<Hint> getHintsByQuestionId(Integer questionId) {
        return hintMapper.selectByQuestionId(questionId);
    }

    public List<Hint> getActiveHintsByQuestionId(Integer questionId) {
        return hintMapper.selectActiveByQuestionId(questionId);
    }

    public List<HintDTO> getHintsWithUnlockStatus(Integer questionId, Integer userId) {
        List<Hint> hints = hintMapper.selectActiveByQuestionId(questionId);
        ContestUser contestUser = contestUserMapper.selectByUserId(userId);
        
        List<HintDTO> result = new ArrayList<>();
        List<Integer> unlockedHintIds = new ArrayList<>();
        
        if (contestUser != null) {
            List<HintUnlock> unlocks = hintUnlockMapper.selectByContestUserId(contestUser.getId());
            for (HintUnlock unlock : unlocks) {
                unlockedHintIds.add(unlock.getHintId());
            }
        }
        
        for (Hint hint : hints) {
            HintDTO dto = new HintDTO();
            dto.setId(hint.getId());
            dto.setQuestionId(hint.getQuestionId());
            dto.setHintNumber(hint.getHintNumber());
            dto.setContent(hint.getContent());
            dto.setPenalty(hint.getPenalty());
            dto.setIsActive(hint.getIsActive());
            dto.setUnlocked(unlockedHintIds.contains(hint.getId()));
            result.add(dto);
        }
        
        return result;
    }

    @Transactional
    public Hint createHint(Hint hint) {
        if (hint.getHintNumber() < 1 || hint.getHintNumber() > 3) {
            throw new IllegalArgumentException("Hint number must be between 1 and 3");
        }
        
        Hint existing = hintMapper.selectByQuestionAndNumber(hint.getQuestionId(), hint.getHintNumber());
        if (existing != null) {
            throw new IllegalArgumentException("Hint number " + hint.getHintNumber() + " already exists for this question");
        }
        
        if (hint.getPenalty() == null) {
            hint.setPenalty(BigDecimal.ZERO);
        }
        if (hint.getIsActive() == null) {
            hint.setIsActive(true);
        }
        
        hintMapper.insert(hint);
        log.info("Hint created: questionId={}, hintNumber={}", hint.getQuestionId(), hint.getHintNumber());
        return hint;
    }

    @Transactional
    public Hint updateHint(Integer id, Hint hint) {
        Hint existing = hintMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Hint not found");
        }
        
        hint.setId(id);
        hintMapper.update(hint);
        log.info("Hint updated: id={}", id);
        return hintMapper.selectById(id);
    }

    @Transactional
    public void deleteHint(Integer id) {
        Hint existing = hintMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Hint not found");
        }
        
        hintMapper.delete(id);
        log.info("Hint deleted: id={}", id);
    }

    @Transactional
    public void deleteHintsByQuestionId(Integer questionId) {
        hintMapper.deleteByQuestionId(questionId);
        log.info("All hints deleted for question: id={}", questionId);
    }

    @Transactional
    public HintDTO unlockHint(Integer userId, Integer hintId) {
        ContestUser contestUser = contestUserMapper.selectByUserId(userId);
        if (contestUser == null) {
            throw new IllegalArgumentException("Contest user not found. Please start the contest first.");
        }
        
        Hint hint = hintMapper.selectById(hintId);
        if (hint == null || !hint.getIsActive()) {
            throw new IllegalArgumentException("Hint not found or inactive");
        }
        
        HintUnlock existing = hintUnlockMapper.selectByContestUserAndHint(contestUser.getId(), hintId);
        if (existing != null) {
            log.info("Hint already unlocked: hintId={}, userId={}", hintId, userId);
            return toDTO(hint, true);
        }
        
        List<Hint> allHints = hintMapper.selectActiveByQuestionId(hint.getQuestionId());
        int nextUnlockNumber = 1;
        for (Hint h : allHints) {
            HintUnlock unlocked = hintUnlockMapper.selectByContestUserAndHint(contestUser.getId(), h.getId());
            if (unlocked != null) {
                nextUnlockNumber = Math.max(nextUnlockNumber, h.getHintNumber() + 1);
            }
        }
        
        if (hint.getHintNumber() > nextUnlockNumber) {
            throw new IllegalArgumentException("Please unlock hints in order. Next available: hint " + nextUnlockNumber);
        }
        
        HintUnlock hintUnlock = new HintUnlock();
        hintUnlock.setContestUserId(contestUser.getId());
        hintUnlock.setHintId(hintId);
        hintUnlockMapper.insert(hintUnlock);
        
        if (hint.getPenalty() != null && hint.getPenalty().compareTo(BigDecimal.ZERO) > 0) {
            int currentScore = contestUser.getTotalScore();
            int penalty = hint.getPenalty().multiply(new BigDecimal("100")).intValue();
            int newScore = Math.max(0, currentScore - penalty);
            contestUser.setTotalScore(newScore);
            contestUserMapper.update(contestUser);
            log.info("Applied penalty for hint: userId={}, hintId={}, penalty={}, oldScore={}, newScore={}",
                    userId, hintId, hint.getPenalty(), currentScore, newScore);
        }
        
        log.info("Hint unlocked: userId={}, hintId={}, penalty={}", userId, hintId, hint.getPenalty());
        return toDTO(hint, true);
    }

    private HintDTO toDTO(Hint hint, boolean unlocked) {
        HintDTO dto = new HintDTO();
        dto.setId(hint.getId());
        dto.setQuestionId(hint.getQuestionId());
        dto.setHintNumber(hint.getHintNumber());
        dto.setContent(hint.getContent());
        dto.setPenalty(hint.getPenalty());
        dto.setIsActive(hint.getIsActive());
        dto.setUnlocked(unlocked);
        return dto;
    }
}
