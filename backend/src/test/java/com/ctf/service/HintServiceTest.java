package com.ctf.service;

import com.ctf.dto.hint.HintDTO;
import com.ctf.entity.ContestUser;
import com.ctf.entity.Hint;
import com.ctf.mapper.ContestUserMapper;
import com.ctf.mapper.HintMapper;
import com.ctf.mapper.HintUnlockMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HintService 提示扣分与动态计分正交（B5）")
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
    @DisplayName("解锁带扣分提示 → 按既有逻辑独立扣分，不经过计分模块")
    void unlockHintWithPenalty_shouldDeductIndependently() {
        ContestUser contestUser = new ContestUser();
        contestUser.setId(1000);
        contestUser.setUserId(1);
        contestUser.setTotalScore(200);

        Hint hint = new Hint();
        hint.setId(5);
        hint.setQuestionId(100);
        hint.setHintNumber(1);
        hint.setContent("hint content");
        hint.setPenalty(new BigDecimal("0.5"));
        hint.setIsActive(true);

        when(contestUserMapper.selectByUserId(1)).thenReturn(contestUser);
        when(hintMapper.selectById(5)).thenReturn(hint);
        when(hintUnlockMapper.selectByContestUserAndHint(1000, 5)).thenReturn(null);
        when(hintMapper.selectActiveByQuestionId(100)).thenReturn(Collections.singletonList(hint));

        HintDTO result = hintService.unlockHint(1, 5);

        assertTrue(result.getUnlocked());
        verify(hintUnlockMapper).insert(any());
        // 0.5 × 100 = 50 分扣罚，与动态入账分正交（B5）
        assertEquals(150, contestUser.getTotalScore());
        verify(contestUserMapper).update(contestUser);
    }

    @Test
    @DisplayName("解锁零扣分提示 → 总分不变")
    void unlockHintWithoutPenalty_shouldKeepScore() {
        ContestUser contestUser = new ContestUser();
        contestUser.setId(1000);
        contestUser.setUserId(1);
        contestUser.setTotalScore(200);

        Hint hint = new Hint();
        hint.setId(5);
        hint.setQuestionId(100);
        hint.setHintNumber(1);
        hint.setContent("hint content");
        hint.setPenalty(BigDecimal.ZERO);
        hint.setIsActive(true);

        when(contestUserMapper.selectByUserId(1)).thenReturn(contestUser);
        when(hintMapper.selectById(5)).thenReturn(hint);
        when(hintUnlockMapper.selectByContestUserAndHint(1000, 5)).thenReturn(null);
        when(hintMapper.selectActiveByQuestionId(100)).thenReturn(Collections.singletonList(hint));

        hintService.unlockHint(1, 5);

        assertEquals(200, contestUser.getTotalScore());
        verify(contestUserMapper, never()).update(any());
    }
}
