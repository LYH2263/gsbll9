package com.ctf.controller;

import com.ctf.dto.ApiResponse;
import com.ctf.dto.scoring.FirstBloodDTO;
import com.ctf.dto.scoring.ScoringConfigDTO;
import com.ctf.dto.scoring.ScoringOverviewDTO;
import com.ctf.service.ScoringService;
import com.ctf.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public ApiResponse<List<FirstBloodDTO>> getFirstBloods(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        List<FirstBloodDTO> firstBloods = scoringService.getAllFirstBloods();
        return ApiResponse.success(firstBloods);
    }

    @GetMapping("/overview")
    public ApiResponse<ScoringOverviewDTO> getOverview(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        return ApiResponse.success(scoringService.getOverview());
    }

    @GetMapping("/config")
    public ApiResponse<ScoringConfigDTO> getConfig(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        return ApiResponse.success(scoringService.getConfig());
    }

    @PutMapping("/config")
    public ApiResponse<Void> updateConfig(@RequestHeader("Authorization") String token,
                                          @RequestBody ScoringConfigDTO config) {
        checkAdminPermission(token);
        scoringService.updateConfig(config);
        return ApiResponse.success(null, "Scoring config updated");
    }
}
