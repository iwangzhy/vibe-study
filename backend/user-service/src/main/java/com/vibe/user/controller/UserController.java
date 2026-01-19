package com.vibe.user.controller;

import com.vibe.common.core.domain.Result;
import com.vibe.common.core.utils.JwtUtils;
import com.vibe.user.dto.*;
import com.vibe.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/auth/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        log.info("用户登录: account={}", request.getAccount());
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }

    /**
     * 用户注册
     */
    @PostMapping("/auth/register")
    public Result<LoginResponse> register(@Validated @RequestBody RegisterRequest request) {
        log.info("用户注册: username={}, email={}", request.getUsername(), request.getEmail());
        LoginResponse response = userService.register(request);
        return Result.success(response);
    }

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/auth/send-code")
    public Result<Void> sendCode(@Validated @RequestBody SendCodeRequest request) {
        log.info("发送邮箱验证码: email={}", request.getEmail());
        userService.sendEmailCode(request);
        return Result.success();
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info/{id}")
    public Result<UserInfoVO> getUserInfo(
            @PathVariable Long id,
            @RequestHeader(value = JwtUtils.HEADER_STRING, required = false) String authHeader) {
        log.info("获取用户信息: userId={}", id);
        
        // 获取当前登录用户ID（如果已登录）
        Long currentUserId = null;
        if (authHeader != null) {
            String token = JwtUtils.extractToken(authHeader);
            if (token != null && JwtUtils.validateToken(token)) {
                currentUserId = JwtUtils.getUserIdFromToken(token);
            }
        }
        
        UserInfoVO userInfo = userService.getUserInfo(id, currentUserId);
        return Result.success(userInfo);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public Result<UserInfoVO> updateUserInfo(
            @RequestHeader(JwtUtils.HEADER_STRING) String authHeader,
            @Validated @RequestBody UpdateUserRequest request) {
        
        // 从token获取用户ID
        String token = JwtUtils.extractToken(authHeader);
        Long userId = JwtUtils.getUserIdFromToken(token);
        
        log.info("更新用户信息: userId={}", userId);
        UserInfoVO userInfo = userService.updateUserInfo(userId, request);
        return Result.success(userInfo);
    }

    /**
     * 关注用户
     */
    @PostMapping("/follow/{id}")
    public Result<Void> followUser(
            @PathVariable Long id,
            @RequestHeader(JwtUtils.HEADER_STRING) String authHeader) {
        
        // 从token获取用户ID
        String token = JwtUtils.extractToken(authHeader);
        Long userId = JwtUtils.getUserIdFromToken(token);
        
        log.info("关注用户: followerId={}, followingId={}", userId, id);
        userService.followUser(userId, id);
        return Result.success();
    }

    /**
     * 取消关注用户
     */
    @DeleteMapping("/follow/{id}")
    public Result<Void> unfollowUser(
            @PathVariable Long id,
            @RequestHeader(JwtUtils.HEADER_STRING) String authHeader) {
        
        // 从token获取用户ID
        String token = JwtUtils.extractToken(authHeader);
        Long userId = JwtUtils.getUserIdFromToken(token);
        
        log.info("取消关注用户: followerId={}, followingId={}", userId, id);
        userService.unfollowUser(userId, id);
        return Result.success();
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info/current")
    public Result<UserInfoVO> getCurrentUserInfo(
            @RequestHeader(JwtUtils.HEADER_STRING) String authHeader) {
        
        // 从token获取用户ID
        String token = JwtUtils.extractToken(authHeader);
        Long userId = JwtUtils.getUserIdFromToken(token);
        
        log.info("获取当前用户信息: userId={}", userId);
        UserInfoVO userInfo = userService.getUserInfo(userId, userId);
        return Result.success(userInfo);
    }

    /**
     * 修改密码
     */
    @PutMapping("/auth/change-password")
    public Result<Void> changePassword(
            @RequestHeader(JwtUtils.HEADER_STRING) String authHeader,
            @Validated @RequestBody ChangePasswordRequest request) {
        
        // 从token获取用户ID
        String token = JwtUtils.extractToken(authHeader);
        Long userId = JwtUtils.getUserIdFromToken(token);
        
        log.info("修改密码: userId={}", userId);
        userService.changePassword(userId, request);
        return Result.success();
    }
}
