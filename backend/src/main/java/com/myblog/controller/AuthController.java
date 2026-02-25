package com.myblog.controller;

import com.myblog.common.annotation.RateLimit;
import com.myblog.common.result.Result;
import com.myblog.dto.*;
import com.myblog.entity.User;
import com.myblog.security.JwtBlacklistService;
import com.myblog.security.JwtUtils;
import com.myblog.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final JwtBlacklistService jwtBlacklistService;

    @RateLimit(maxRequests = 5, timeWindow = 60, prefix = "login", message = "登录尝试过于频繁，请1分钟后再试")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateToken(authentication);

        User user = (User) authentication.getPrincipal();

        AuthResponse response = AuthResponse.builder()
                .token(jwt)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }

    @RateLimit(maxRequests = 3, timeWindow = 60, prefix = "register", message = "注册请求过于频繁，请1分钟后再试")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getNickname());

        String jwt = jwtUtils.generateTokenFromUsername(user.getUsername());

        AuthResponse response = AuthResponse.builder()
                .token(jwt)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(ApiResponse.success("注册成功", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        AuthResponse response = AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 用户登出
     * 将当前Token加入黑名单，使其失效
     */
    @PostMapping("/logout")
    public ResponseEntity<Result<String>> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            // 获取当前用户ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = null;
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                userId = ((User) authentication.getPrincipal()).getId();
            }
            
            // 获取Token剩余有效期并加入黑名单
            Long expirationTime = jwtUtils.getExpirationTime(token);
            // 计算剩余秒数
            Long remainingSeconds = (expirationTime - System.currentTimeMillis()) / 1000;
            if (remainingSeconds > 0) {
                jwtBlacklistService.addToBlacklist(token, userId, remainingSeconds);
            }
        }
        
        // 清除SecurityContext
        SecurityContextHolder.clearContext();
        
        return ResponseEntity.ok(Result.success("登出成功"));
    }
}
