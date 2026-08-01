package com.ctf.controller;

import com.ctf.dto.ApiResponse;
import com.ctf.dto.scoring.FirstBloodItem;
import com.ctf.dto.scoring.ScoringConfigDTO;
import com.ctf.dto.scoring.ScoringOverviewDTO;
import com.ctf.service.ScoringService;
import com.ctf.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 计分模块接口（路径前缀 /api/scoring，P1 衰减配置读写也挂载在本前缀下）。
 * 一血写入只允许由服务端在正确提交时触发，本 Controller 不提供任何写接口。
 */
@Slf4j
@RestController
@RequestMapping("/scoring")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class ScoringController {

    @Autowired
    private ScoringService scoringService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private String getRoleFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtTokenUtil.getRoleFromToken(token);
    }

    private void checkAdminPermission(String token) {
        String role = getRoleFromToken(token);
        if (!"admin".equals(role)) {
            throw new SecurityException("Permission denied");
        }
    }

    @GetMapping("/first-bloods")
    public ApiResponse<List<FirstBloodItem>> getFirstBloods(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        List<FirstBloodItem> firstBloods = scoringService.getFirstBloodList();
        return ApiResponse.success(firstBloods);
    }

    /**
     * P1（§10）：衰减/奖金配置读写挂载在第一轮冻结前缀 /api/scoring 之下。
     */
    @GetMapping("/config")
    public ApiResponse<ScoringConfigDTO> getScoringConfig(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        return ApiResponse.success(scoringService.getScoringConfig());
    }

    @PutMapping("/config")
    public ApiResponse<ScoringConfigDTO> updateScoringConfig(@RequestHeader("Authorization") String token,
                                                             @RequestBody ScoringConfigDTO config) {
        checkAdminPermission(token);
        scoringService.updateScoringConfig(config);
        log.info("Scoring config updated: {}", config);
        return ApiResponse.success(scoringService.getScoringConfig(), "Scoring config updated successfully");
    }

    /**
     * P2（§12，D2）：计分概览，管理端只读，权限模型与 /admin/** 一致。
     */
    @GetMapping("/overview")
    public ApiResponse<ScoringOverviewDTO> getOverview(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        return ApiResponse.success(scoringService.getOverview());
    }
}
