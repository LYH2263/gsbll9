package com.ctf.service;

import com.ctf.dto.LoginRequest;
import com.ctf.dto.LoginResponse;
import com.ctf.entity.User;
import com.ctf.exception.UnauthorizedException;
import com.ctf.mapper.UserMapper;
import com.ctf.util.ContestTimeUtil;
import com.ctf.util.JwtTokenUtil;
import com.ctf.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private ContestTimeUtil contestTimeUtil;

    @Value("${jwt.expiration}")
    private Long expiration;

    public LoginResponse login(LoginRequest request) {
        log.info("User login attempt: studentId={}", request.getStudentId());

        User user = userMapper.selectByStudentId(request.getStudentId());
        if (user == null || !user.getIsActive()) {
            log.warn("Login failed: user not found or inactive, studentId={}", request.getStudentId());
            throw new UnauthorizedException("学号或密码错误");
        }

        if (!PasswordUtil.matchPassword(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password, studentId={}", request.getStudentId());
            throw new UnauthorizedException("学号或密码错误");
        }

        if (!"admin".equals(user.getRole())) {
            ContestTimeUtil.ContestStatus status = contestTimeUtil.getCurrentStatus();
            if (status == ContestTimeUtil.ContestStatus.NOT_STARTED) {
                log.warn("Login rejected: contest not started yet, studentId={}", request.getStudentId());
                throw new UnauthorizedException("比赛尚未开放登录，请等待登录开放时间");
            }
        }

        // 生成JWT Token
        String token = jwtTokenUtil.generateToken(user.getId(), user.getStudentId(), user.getRole());
        log.info("User login successful: userId={}, studentId={}", user.getId(), user.getStudentId());

        return LoginResponse.builder()
                .userId(user.getId())
                .studentId(user.getStudentId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .token(token)
                .expiresIn(expiration / 1000)
                .build();
    }

    public User getUserById(Integer userId) {
        return userMapper.selectById(userId);
    }

    public User getUserByStudentId(String studentId) {
        return userMapper.selectByStudentId(studentId);
    }
}
