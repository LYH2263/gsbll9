package com.ctf.controller;

import com.ctf.dto.scoring.FirstBloodDTO;
import com.ctf.dto.scoring.ScoringConfigDTO;
import com.ctf.dto.scoring.ScoringOverviewDTO;
import com.ctf.service.ScoringService;
import com.ctf.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoringController 权限与概览接口测试 (P2)")
class ScoringControllerTest {

    private static final String USER_TOKEN = "Bearer user-token";
    private static final String ADMIN_TOKEN = "Bearer admin-token";

    @Mock
    private ScoringService scoringService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @InjectMocks
    private ScoringController scoringController;

    @BeforeEach
    void setUp() {
        // 不同测试只使用其中一种角色，故用 lenient 避免严格桩检误报
        lenient().when(jwtTokenUtil.getRoleFromToken("user-token")).thenReturn("user");
        lenient().when(jwtTokenUtil.getRoleFromToken("admin-token")).thenReturn("admin");
    }

    @Test
    @DisplayName("普通用户访问一血列表 → 抛 SecurityException，不触碰 Service")
    void nonAdmin_getFirstBloods_shouldThrow() {
        assertThrows(SecurityException.class, () -> scoringController.getFirstBloods(USER_TOKEN));
        verifyNoInteractions(scoringService);
    }

    @Test
    @DisplayName("普通用户访问概览 → 抛 SecurityException")
    void nonAdmin_getOverview_shouldThrow() {
        assertThrows(SecurityException.class, () -> scoringController.getOverview(USER_TOKEN));
        verify(scoringService, never()).getOverview();
    }

    @Test
    @DisplayName("普通用户访问配置 → 抛 SecurityException")
    void nonAdmin_configEndpoints_shouldThrow() {
        assertThrows(SecurityException.class, () -> scoringController.getConfig(USER_TOKEN));
        assertThrows(SecurityException.class,
                () -> scoringController.updateConfig(USER_TOKEN, ScoringConfigDTO.builder().build()));
        verifyNoInteractions(scoringService);
    }

    @Test
    @DisplayName("管理员访问概览 → 返回五项指标，code=200")
    void admin_getOverview_shouldReturnMetrics() {
        ScoringOverviewDTO overview = ScoringOverviewDTO.builder()
                .totalCorrectSolves(11)
                .questionsWithFirstBlood(1)
                .questionsAtBasePoints(0)
                .questionsAtMinPoints(2)
                .totalFirstBloodBonusAwarded(15)
                .build();
        when(scoringService.getOverview()).thenReturn(overview);

        var response = scoringController.getOverview(ADMIN_TOKEN);

        assertEquals(200, response.getCode());
        assertEquals(11, response.getData().getTotalCorrectSolves());
        assertEquals(15, response.getData().getTotalFirstBloodBonusAwarded());
        verify(scoringService).getOverview();
    }

    @Test
    @DisplayName("管理员访问一血列表 → 返回 DTO 列表")
    void admin_getFirstBloods_shouldReturnList() {
        FirstBloodDTO dto = new FirstBloodDTO();
        dto.setQuestionId(1);
        dto.setQuestionTitle("测试题");
        when(scoringService.getAllFirstBloods()).thenReturn(List.of(dto));

        var response = scoringController.getFirstBloods(ADMIN_TOKEN);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().size());
    }

    @Test
    @DisplayName("管理员读取/更新配置 → 委托 Service")
    void admin_configEndpoints_shouldDelegate() {
        ScoringConfigDTO cfg = ScoringConfigDTO.builder()
                .minPoints(60).decayStep(10).firstBloodBonus(15)
                .freezeOnEnd(true).overviewTimezone("Asia/Shanghai").build();
        when(scoringService.getConfig()).thenReturn(cfg);

        assertEquals(60, scoringController.getConfig(ADMIN_TOKEN).getData().getMinPoints());

        scoringController.updateConfig(ADMIN_TOKEN, cfg);
        verify(scoringService).updateConfig(cfg);
    }

    @Test
    @DisplayName("空数据概览 → 返回空列表不抛异常")
    void admin_getOverview_empty_shouldReturnEmpty() {
        when(scoringService.getAllFirstBloods()).thenReturn(Collections.emptyList());
        assertTrue(scoringController.getFirstBloods(ADMIN_TOKEN).getData().isEmpty());
    }
}
