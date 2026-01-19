package com.vibe.user.service.impl;

import com.vibe.common.core.domain.StatusCode;
import com.vibe.common.core.exception.BusinessException;
import com.vibe.common.core.utils.SnowflakeIdGenerator;
import com.vibe.user.dto.*;
import com.vibe.user.entity.User;
import com.vibe.user.entity.UserRelation;
import com.vibe.user.mapper.UserMapper;
import com.vibe.user.mapper.UserRelationMapper;
import com.vibe.user.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务测试")
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRelationMapper userRelationMapper;

    @Mock
    private RedisService redisService;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    @InjectMocks
    private UserServiceImpl userService;

    private BCryptPasswordEncoder passwordEncoder;
    private User testUser;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        
        // 准备测试数据
        testUser = new User();
        testUser.setId(1000000000000000001L);
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("123456"));
        testUser.setNickname("测试用户");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setGender(1);
        testUser.setStatus(1);
        testUser.setDeleted(0);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("用户登录 - 成功")
    void testLogin_Success() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setAccount("testuser");
        request.setPassword("123456");

        when(userMapper.selectOne(any())).thenReturn(testUser);
        when(userRelationMapper.countFollowing(anyLong())).thenReturn(10L);
        when(userRelationMapper.countFollower(anyLong())).thenReturn(5L);

        // When
        LoginResponse response = userService.login(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getUserInfo());
        assertEquals("testuser", response.getUserInfo().getUsername());
        assertEquals("测试用户", response.getUserInfo().getNickname());
        
        verify(userMapper, times(1)).selectOne(any());
        verify(redisService, times(1)).setUserInfo(anyLong(), any());
    }

    @Test
    @DisplayName("用户登录 - 用户不存在")
    void testLogin_UserNotFound() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setAccount("nonexistent");
        request.setPassword("123456");

        when(userMapper.selectOne(any())).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.login(request));
        
        assertEquals(StatusCode.UNAUTHORIZED, exception.getCode());
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("用户登录 - 密码错误")
    void testLogin_WrongPassword() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setAccount("testuser");
        request.setPassword("wrongpassword");

        when(userMapper.selectOne(any())).thenReturn(testUser);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.login(request));
        
        assertEquals(StatusCode.UNAUTHORIZED, exception.getCode());
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("用户登录 - 账号已被禁用")
    void testLogin_UserDisabled() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setAccount("testuser");
        request.setPassword("123456");

        testUser.setStatus(0); // 禁用状态
        when(userMapper.selectOne(any())).thenReturn(testUser);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.login(request));
        
        assertEquals(StatusCode.FORBIDDEN, exception.getCode());
        assertEquals("账号已被禁用", exception.getMessage());
    }

    @Test
    @DisplayName("用户注册 - 成功")
    void testRegister_Success() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("pass123");
        request.setNickname("新用户");
        request.setEmail("new@example.com");
        request.setCode("123456");

        when(redisService.getEmailCode(anyString())).thenReturn("123456");
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(idGenerator.nextId()).thenReturn(1000000000000000002L);
        when(userMapper.insert(any())).thenReturn(1);
        when(userRelationMapper.countFollowing(anyLong())).thenReturn(0L);
        when(userRelationMapper.countFollower(anyLong())).thenReturn(0L);

        // When
        LoginResponse response = userService.register(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getUserInfo());
        assertEquals("newuser", response.getUserInfo().getUsername());
        
        verify(userMapper, times(1)).insert(any());
        verify(redisService, times(1)).deleteEmailCode(anyString());
        verify(redisService, times(1)).setUserInfo(anyLong(), any());
    }

    @Test
    @DisplayName("用户注册 - 验证码错误")
    void testRegister_WrongCode() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setCode("wrong");

        when(redisService.getEmailCode(anyString())).thenReturn("123456");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.register(request));
        
        assertEquals(StatusCode.BAD_REQUEST, exception.getCode());
        assertEquals("验证码错误", exception.getMessage());
    }

    @Test
    @DisplayName("用户注册 - 用户名已存在")
    void testRegister_UsernameExists() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("new@example.com");
        request.setCode("123456");

        when(redisService.getEmailCode(anyString())).thenReturn("123456");
        when(userMapper.selectCount(any())).thenReturn(1L);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.register(request));
        
        assertEquals(StatusCode.BAD_REQUEST, exception.getCode());
        assertEquals("用户名已存在", exception.getMessage());
    }

    @Test
    @DisplayName("获取用户信息 - 成功")
    void testGetUserInfo_Success() {
        // Given
        Long userId = 1000000000000000001L;
        Long currentUserId = 1000000000000000002L;

        when(redisService.getUserInfo(userId)).thenReturn(null);
        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userRelationMapper.countFollowing(userId)).thenReturn(10L);
        when(userRelationMapper.countFollower(userId)).thenReturn(5L);
        when(userRelationMapper.checkFollowing(currentUserId, userId)).thenReturn(1);

        // When
        UserInfoVO userInfo = userService.getUserInfo(userId, currentUserId);

        // Then
        assertNotNull(userInfo);
        assertEquals("testuser", userInfo.getUsername());
        assertEquals("测试用户", userInfo.getNickname());
        assertEquals(10L, userInfo.getFollowingCount());
        assertEquals(5L, userInfo.getFollowerCount());
        assertTrue(userInfo.getIsFollowing());
        
        verify(redisService, times(1)).setUserInfo(userId, userInfo);
    }

    @Test
    @DisplayName("获取用户信息 - 用户不存在")
    void testGetUserInfo_UserNotFound() {
        // Given
        Long userId = 999L;

        when(redisService.getUserInfo(userId)).thenReturn(null);
        when(userMapper.selectById(userId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.getUserInfo(userId, null));
        
        assertEquals(StatusCode.NOT_FOUND, exception.getCode());
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @DisplayName("更新用户信息 - 成功")
    void testUpdateUserInfo_Success() {
        // Given
        Long userId = 1000000000000000001L;
        UpdateUserRequest request = new UpdateUserRequest();
        request.setNickname("新昵称");
        request.setGender(2);
        request.setBio("新的个人简介");

        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userMapper.updateById(any())).thenReturn(1);
        when(userRelationMapper.countFollowing(userId)).thenReturn(10L);
        when(userRelationMapper.countFollower(userId)).thenReturn(5L);

        // When
        UserInfoVO userInfo = userService.updateUserInfo(userId, request);

        // Then
        assertNotNull(userInfo);
        assertEquals("新昵称", userInfo.getNickname());
        assertEquals(2, userInfo.getGender());
        assertEquals("新的个人简介", userInfo.getBio());
        
        verify(userMapper, times(1)).updateById(any());
        verify(redisService, times(1)).deleteUserInfo(userId);
    }

    @Test
    @DisplayName("关注用户 - 成功")
    void testFollowUser_Success() {
        // Given
        Long followerId = 1000000000000000001L;
        Long followingId = 1000000000000000002L;

        User followingUser = new User();
        followingUser.setId(followingId);
        followingUser.setDeleted(0);

        when(userMapper.selectById(followingId)).thenReturn(followingUser);
        when(userRelationMapper.selectCount(any())).thenReturn(0L);
        when(idGenerator.nextId()).thenReturn(1L);
        when(userRelationMapper.insert(any())).thenReturn(1);

        // When
        userService.followUser(followerId, followingId);

        // Then
        verify(userRelationMapper, times(1)).insert(any());
        verify(redisService, times(1)).addFollowing(followerId, followingId);
        verify(redisService, times(1)).deleteUserInfo(followerId);
        verify(redisService, times(1)).deleteUserInfo(followingId);
    }

    @Test
    @DisplayName("关注用户 - 不能关注自己")
    void testFollowUser_CannotFollowSelf() {
        // Given
        Long userId = 1000000000000000001L;

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.followUser(userId, userId));
        
        assertEquals(StatusCode.BAD_REQUEST, exception.getCode());
        assertEquals("不能关注自己", exception.getMessage());
    }

    @Test
    @DisplayName("关注用户 - 已关注该用户")
    void testFollowUser_AlreadyFollowed() {
        // Given
        Long followerId = 1000000000000000001L;
        Long followingId = 1000000000000000002L;

        User followingUser = new User();
        followingUser.setId(followingId);
        followingUser.setDeleted(0);

        when(userMapper.selectById(followingId)).thenReturn(followingUser);
        when(userRelationMapper.selectCount(any())).thenReturn(1L);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.followUser(followerId, followingId));
        
        assertEquals(StatusCode.BAD_REQUEST, exception.getCode());
        assertEquals("已关注该用户", exception.getMessage());
    }

    @Test
    @DisplayName("取消关注用户 - 成功")
    void testUnfollowUser_Success() {
        // Given
        Long followerId = 1000000000000000001L;
        Long followingId = 1000000000000000002L;

        UserRelation relation = UserRelation.builder()
                .id(1L)
                .followerId(followerId)
                .followingId(followingId)
                .build();

        when(userRelationMapper.selectOne(any())).thenReturn(relation);
        when(userRelationMapper.deleteById(anyLong())).thenReturn(1);

        // When
        userService.unfollowUser(followerId, followingId);

        // Then
        verify(userRelationMapper, times(1)).deleteById(1L);
        verify(redisService, times(1)).removeFollowing(followerId, followingId);
        verify(redisService, times(1)).deleteUserInfo(followerId);
        verify(redisService, times(1)).deleteUserInfo(followingId);
    }

    @Test
    @DisplayName("修改密码 - 成功")
    void testChangePassword_Success() {
        // Given
        Long userId = 1000000000000000001L;
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("123456");
        request.setNewPassword("newPass123");
        request.setConfirmPassword("newPass123");

        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userMapper.updateById(any())).thenReturn(1);

        // When
        userService.changePassword(userId, request);

        // Then
        verify(userMapper, times(1)).updateById(any());
        verify(redisService, times(1)).deleteUserInfo(userId);
    }

    @Test
    @DisplayName("修改密码 - 两次密码不一致")
    void testChangePassword_PasswordMismatch() {
        // Given
        Long userId = 1000000000000000001L;
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("123456");
        request.setNewPassword("newPass123");
        request.setConfirmPassword("differentPass");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.changePassword(userId, request));
        
        assertEquals(StatusCode.BAD_REQUEST, exception.getCode());
        assertEquals("两次输入的新密码不一致", exception.getMessage());
    }

    @Test
    @DisplayName("修改密码 - 旧密码错误")
    void testChangePassword_WrongOldPassword() {
        // Given
        Long userId = 1000000000000000001L;
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongpass");
        request.setNewPassword("newPass123");
        request.setConfirmPassword("newPass123");

        when(userMapper.selectById(userId)).thenReturn(testUser);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.changePassword(userId, request));
        
        assertEquals(StatusCode.BAD_REQUEST, exception.getCode());
        assertEquals("旧密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("修改密码 - 新旧密码相同")
    void testChangePassword_SameAsOld() {
        // Given
        Long userId = 1000000000000000001L;
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("123456");
        request.setNewPassword("123456");
        request.setConfirmPassword("123456");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.changePassword(userId, request));
        
        assertEquals(StatusCode.BAD_REQUEST, exception.getCode());
        assertEquals("新密码不能与旧密码相同", exception.getMessage());
    }
}
