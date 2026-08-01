package com.ctf.service;

import com.ctf.entity.ContestUser;
import com.ctf.entity.Hint;
import com.ctf.mapper.ContestUserMapper;
import com.ctf.mapper.HintMapper;
import com.ctf.mapper.HintUnlockMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HintService 提示扣分与动态分正交 (B5)")
class HintServiceTest {

    @Mock
    private HintMapper hintMapper;
    @Mock
    private HintUnlockMapper hintUnlockMapper;
    @Mock
    private ContestUserMapper contestUserMapper;

    @InjectMocks
    private HintService hintService;

    @Test
    @DisplayName("B5: 解锁提示扣分独立于动态分，直接从 total_score 扣减（penalty*100），不触碰 ScoringService")
    void unlockHint_withPenalty_shouldDeductIndependently() {
        Integer userId = 1;
        Integer contestUserId = 1000;
        ContestUser cu = new ContestUser();
        cu.setId(contestUserId);
        cu.setUserId(userId);
        cu.setTotalScore(250);

        Hint hint = new Hint();
        hint.setId(10);
        hint.setQuestionId(100);
        hint.setHintNumber(1);
        hint.setPenalty(new BigDecimal("0.5")); // 50 分
        hint.setIsActive(true);

        when(contestUserMapper.selectByUserId(userId)).thenReturn(cu);
        when(hintMapper.selectById(10)).thenReturn(hint);
        when(hintUnlockMapper.selectByContestUserAndHint(contestUserId, 10)).thenReturn(null);
        when(hintMapper.selectActiveByQuestionId(100)).thenReturn(Collections.singletonList(hint));

        hintService.unlockHint(userId, 10);

        ArgumentCaptor<ContestUser> captor = ArgumentCaptor.forClass(ContestUser.class);
        verify(contestUserMapper).update(captor.capture());
        assertEquals(200, captor.getValue().getTotalScore()); // 250 - 50
        verify(hintUnlockMapper).insert(any());
    }

    @Test
    @DisplayName("B5: 已解锁的提示再次解锁 → 不重复扣分")
    void unlockHint_alreadyUnlocked_shouldNotDeductAgain() {
        Integer userId = 1;
        Integer contestUserId = 1000;
        ContestUser cu = new ContestUser();
        cu.setId(contestUserId);
        cu.setTotalScore(250);

        Hint hint = new Hint();
        hint.setId(10);
        hint.setQuestionId(100);
        hint.setHintNumber(1);
        hint.setPenalty(new BigDecimal("0.5"));
        hint.setIsActive(true);

        when(contestUserMapper.selectByUserId(userId)).thenReturn(cu);
        when(hintMapper.selectById(10)).thenReturn(hint);
        when(hintUnlockMapper.selectByContestUserAndHint(contestUserId, 10))
                .thenReturn(new com.ctf.entity.HintUnlock());

        hintService.unlockHint(userId, 10);

        verify(contestUserMapper, never()).update(any());
        verify(hintUnlockMapper, never()).insert(any());
    }

    @Test
    @DisplayName("B5: 扣减不低于 0 分（夹取）")
    void unlockHint_penaltyExceedsScore_shouldClampToZero() {
        Integer userId = 1;
        Integer contestUserId = 1000;
        ContestUser cu = new ContestUser();
        cu.setId(contestUserId);
        cu.setTotalScore(30);

        Hint hint = new Hint();
        hint.setId(10);
        hint.setQuestionId(100);
        hint.setHintNumber(1);
        hint.setPenalty(new BigDecimal("1.0")); // 100 分
        hint.setIsActive(true);

        when(contestUserMapper.selectByUserId(userId)).thenReturn(cu);
        when(hintMapper.selectById(10)).thenReturn(hint);
        when(hintUnlockMapper.selectByContestUserAndHint(contestUserId, 10)).thenReturn(null);
        when(hintMapper.selectActiveByQuestionId(100)).thenReturn(Collections.singletonList(hint));

        hintService.unlockHint(userId, 10);

        ArgumentCaptor<ContestUser> captor = ArgumentCaptor.forClass(ContestUser.class);
        verify(contestUserMapper).update(captor.capture());
        assertEquals(0, captor.getValue().getTotalScore());
    }
}
