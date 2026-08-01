package com.ctf.controller;

import com.ctf.dto.ApiResponse;
import com.ctf.dto.LoginRequest;
import com.ctf.dto.LoginResponse;
import com.ctf.service.AuthService;
import com.ctf.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        log.info("Login request received: studentId={}", request.getStudentId());
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response, "Login successful");
    }

    @GetMapping("/verify")
    public ApiResponse<Boolean> verifyToken(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        boolean valid = jwtTokenUtil.validateToken(token);
        return ApiResponse.success(valid, valid ? "Token valid" : "Token invalid");
    }
}
