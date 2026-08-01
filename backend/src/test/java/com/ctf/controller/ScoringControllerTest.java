package com.ctf.controller;

import com.ctf.dto.ApiResponse;
import com.ctf.dto.scoring.ScoringConfigDTO;
import com.ctf.dto.scoring.ScoringOverviewDTO;
import com.ctf.service.ScoringService;
import com.ctf.util.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoringController 权限模型（D2 / §6 安全）")
class ScoringControllerTest {

    @Mock
    private ScoringService scoringService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @InjectMocks
    private ScoringController scoringController;

    private static final String USER_TOKEN = "Bearer user-token";
    private static final String ADMIN_TOKEN = "Bearer admin-token";

    @Test
    @DisplayName("普通用户访问一血列表 → SecurityException（越权拒绝）")
    void nonAdmin_getFirstBloods_shouldThrow() {
        when(jwtTokenUtil.getRoleFromToken("user-token")).thenReturn("user");

        assertThrows(SecurityException.class, () -> scoringController.getFirstBloods(USER_TOKEN));
        verifyNoInteractions(scoringService);
    }

    @Test
    @DisplayName("普通用户读/写计分配置 → SecurityException（越权拒绝）")
    void nonAdmin_configEndpoints_shouldThrow() {
        when(jwtTokenUtil.getRoleFromToken("user-token")).thenReturn("user");

        assertThrows(SecurityException.class, () -> scoringController.getScoringConfig(USER_TOKEN));
        assertThrows(SecurityException.class, () ->
                scoringController.updateScoringConfig(USER_TOKEN, new ScoringConfigDTO(1, 0, 0)));
        verifyNoInteractions(scoringService);
    }

    @Test
    @DisplayName("普通用户访问计分概览 → SecurityException（越权拒绝）")
    void nonAdmin_getOverview_shouldThrow() {
        when(jwtTokenUtil.getRoleFromToken("user-token")).thenReturn("user");

        assertThrows(SecurityException.class, () -> scoringController.getOverview(USER_TOKEN));
        verifyNoInteractions(scoringService);
    }

    @Test
    @DisplayName("管理员访问计分概览 → 返回五项指标")
    void admin_getOverview_shouldReturnMetrics() {
        when(jwtTokenUtil.getRoleFromToken("admin-token")).thenReturn("admin");
        ScoringOverviewDTO overview = new ScoringOverviewDTO(11, 1, 0, 2, 15);
        when(scoringService.getOverview()).thenReturn(overview);

        ApiResponse<ScoringOverviewDTO> response = scoringController.getOverview(ADMIN_TOKEN);

        assertEquals(200, response.getCode());
        assertEquals(11, response.getData().getTotalCorrectSolves());
        assertEquals(15, response.getData().getTotalFirstBloodBonusAwarded());
    }

    @Test
    @DisplayName("管理员访问一血列表 → 正常返回")
    void admin_getFirstBloods_shouldReturnList() {
        when(jwtTokenUtil.getRoleFromToken("admin-token")).thenReturn("admin");
        when(scoringService.getFirstBloodList()).thenReturn(java.util.Collections.emptyList());

        ApiResponse<?> response = scoringController.getFirstBloods(ADMIN_TOKEN);

        assertEquals(200, response.getCode());
        verify(scoringService).getFirstBloodList();
    }
}
