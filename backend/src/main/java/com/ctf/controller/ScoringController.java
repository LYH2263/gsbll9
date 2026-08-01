package com.ctf.controller;

import com.ctf.dto.ApiResponse;
import com.ctf.dto.scoring.FirstBloodDTO;
import com.ctf.dto.scoring.ScoringConfigDTO;
import com.ctf.dto.scoring.ScoringOverviewDTO;
import com.ctf.service.ScoringService;
import com.ctf.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 动态计分与一血模块 Controller（模块 API 路径前缀：/scoring，实际路径 /api/scoring）。
 *
 * <p>P0（§6）：管理端一血只读。安全约束（§6）：一血写入只由服务端在正确提交时触发，
 * 本模块不提供「手动指定某人获得一血」的公开接口。
 *
 * <p>该前缀自本轮起冻结（A4）；P1/P2 的配置读写、概览等接口应挂在同一前缀下（§10）。
 * Controller 仅做鉴权与参数转发（A2）。
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

    private void checkAdminPermission(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String role = jwtTokenUtil.getRoleFromToken(token);
        if (!"admin".equals(role)) {
            throw new SecurityException("Permission denied");
        }
    }

    /**
     * 管理端只读：获取全部一血记录（题目标识 + 用户标识 + 达成时间）。
     */
    @GetMapping("/first-bloods")
    public ApiResponse<List<FirstBloodDTO>> getFirstBloods(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        List<FirstBloodDTO> firstBloods = scoringService.listFirstBloods();
        return ApiResponse.success(firstBloods);
    }

    /**
     * 管理端：读取动态计分配置（P1 §10，键名沿用 §8 冻结字符串）。
     * 挂在第一轮已建立的 /scoring 前缀下，未散落到旧 /config 接口（§10）。
     */
    @GetMapping("/config")
    public ApiResponse<ScoringConfigDTO> getScoringConfig(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        return ApiResponse.success(scoringService.getScoringConfig());
    }

    /**
     * 管理端：更新动态计分配置（P1 §10）。
     */
    @PutMapping("/config")
    public ApiResponse<ScoringConfigDTO> updateScoringConfig(@RequestHeader("Authorization") String token,
                                                             @RequestBody ScoringConfigDTO config) {
        checkAdminPermission(token);
        ScoringConfigDTO updated = scoringService.updateScoringConfig(config);
        log.info("Scoring config updated by admin");
        return ApiResponse.success(updated, "Scoring config updated successfully");
    }

    /**
     * 管理端：计分概览（P2 §12 五指标，D1 现场聚合、D2 管理端权限）。
     * 公布成绩阶段等只读查询仍可用（C2/C3）。
     */
    @GetMapping("/overview")
    public ApiResponse<ScoringOverviewDTO> getOverview(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        return ApiResponse.success(scoringService.getOverview());
    }
}
