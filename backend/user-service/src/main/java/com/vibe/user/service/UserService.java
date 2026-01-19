package com.vibe.user.service;

import com.vibe.user.dto.*;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（包含token和用户信息）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 注册响应（包含token和用户信息）
     */
    LoginResponse register(RegisterRequest request);

    /**
     * 发送邮箱验证码
     *
     * @param request 发送验证码请求
     */
    void sendEmailCode(SendCodeRequest request);

    /**
     * 获取用户信息
     *
     * @param userId        用户ID
     * @param currentUserId 当前登录用户ID（用于判断是否关注）
     * @return 用户信息
     */
    UserInfoVO getUserInfo(Long userId, Long currentUserId);

    /**
     * 更新用户信息
     *
     * @param userId  用户ID
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    UserInfoVO updateUserInfo(Long userId, UpdateUserRequest request);

    /**
     * 关注用户
     *
     * @param followerId  关注者ID
     * @param followingId 被关注者ID
     */
    void followUser(Long followerId, Long followingId);

    /**
     * 取消关注用户
     *
     * @param followerId  关注者ID
     * @param followingId 被关注者ID
     */
    void unfollowUser(Long followerId, Long followingId);

    /**
     * 修改密码
     *
     * @param userId  用户ID
     * @param request 修改密码请求
     */
    void changePassword(Long userId, ChangePasswordRequest request);
}
