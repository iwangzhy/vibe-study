package com.vibe.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vibe.common.core.domain.StatusCode;
import com.vibe.common.core.exception.BusinessException;
import com.vibe.common.core.utils.JwtUtils;
import com.vibe.common.core.utils.SnowflakeIdGenerator;
import com.vibe.user.dto.*;
import com.vibe.user.entity.User;
import com.vibe.user.entity.UserRelation;
import com.vibe.user.mapper.UserMapper;
import com.vibe.user.mapper.UserRelationMapper;
import com.vibe.user.service.RedisService;
import com.vibe.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * 用户服务实现
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserRelationMapper userRelationMapper;

    @Resource
    private RedisService redisService;

    @Resource
    private SnowflakeIdGenerator idGenerator;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginResponse login(LoginRequest request) {
        // 查询用户（支持用户名、邮箱、手机号登录）
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .eq(User::getUsername, request.getAccount())
                .or().eq(User::getEmail, request.getAccount())
                .or().eq(User::getPhone, request.getAccount())
        );
        wrapper.eq(User::getDeleted, 0);

        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException(StatusCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(StatusCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(StatusCode.FORBIDDEN, "账号已被禁用");
        }

        // 生成token
        String token = JwtUtils.generateToken(user.getId(), user.getUsername());

        // 构建用户信息
        UserInfoVO userInfo = buildUserInfoVO(user, null);

        // 缓存用户信息
        redisService.setUserInfo(user.getId(), userInfo);

        return LoginResponse.builder()
                .token(token)
                .userInfo(userInfo)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        // 验证邮箱验证码
        String cachedCode = redisService.getEmailCode(request.getEmail());
        if (cachedCode == null) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "验证码已过期");
        }
        if (!cachedCode.equals(request.getCode())) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "验证码错误");
        }

        // 检查用户名是否存在
        LambdaQueryWrapper<User> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(User::getUsername, request.getUsername()).eq(User::getDeleted, 0);
        if (userMapper.selectCount(usernameWrapper) > 0) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "用户名已存在");
        }

        // 检查邮箱是否存在
        LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(User::getEmail, request.getEmail()).eq(User::getDeleted, 0);
        if (userMapper.selectCount(emailWrapper) > 0) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "邮箱已被注册");
        }

        // 创建用户
        User user = new User();
        user.setId(idGenerator.nextId());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=" + user.getId()); // 默认头像
        user.setGender(0);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);

        userMapper.insert(user);

        // 删除验证码
        redisService.deleteEmailCode(request.getEmail());

        // 生成token
        String token = JwtUtils.generateToken(user.getId(), user.getUsername());

        // 构建用户信息
        UserInfoVO userInfo = buildUserInfoVO(user, null);

        // 缓存用户信息
        redisService.setUserInfo(user.getId(), userInfo);

        return LoginResponse.builder()
                .token(token)
                .userInfo(userInfo)
                .build();
    }

    @Override
    public void sendEmailCode(SendCodeRequest request) {
        // 生成6位数字验证码
        String code = String.format("%06d", new Random().nextInt(999999));

        // 存储到Redis（5分钟有效）
        redisService.setEmailCode(request.getEmail(), code);

        // TODO: 实际项目中应该调用邮件服务发送验证码
        log.info("发送邮箱验证码: email={}, code={}", request.getEmail(), code);
        
        // 这里为了开发方便，直接打印验证码
        System.out.println("========== 邮箱验证码 ==========");
        System.out.println("邮箱: " + request.getEmail());
        System.out.println("验证码: " + code);
        System.out.println("有效期: 5分钟");
        System.out.println("===============================");
    }

    @Override
    public UserInfoVO getUserInfo(Long userId, Long currentUserId) {
        // 先从缓存获取
        Object cached = redisService.getUserInfo(userId);
        if (cached instanceof UserInfoVO) {
            UserInfoVO userInfo = (UserInfoVO) cached;
            // 如果是查询他人信息，需要判断是否关注
            if (currentUserId != null && !currentUserId.equals(userId)) {
                userInfo.setIsFollowing(redisService.isFollowing(currentUserId, userId));
            }
            return userInfo;
        }

        // 缓存未命中，从数据库查询
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(StatusCode.NOT_FOUND, "用户不存在");
        }

        // 构建用户信息
        UserInfoVO userInfo = buildUserInfoVO(user, currentUserId);

        // 缓存用户信息
        redisService.setUserInfo(userId, userInfo);

        return userInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO updateUserInfo(Long userId, UpdateUserRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(StatusCode.NOT_FOUND, "用户不存在");
        }

        // 更新用户信息
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getBirthday() != null) {
            user.setBirthday(request.getBirthday());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(user);

        // 删除缓存
        redisService.deleteUserInfo(userId);

        // 构建并返回用户信息
        return buildUserInfoVO(user, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void followUser(Long followerId, Long followingId) {
        // 不能关注自己
        if (followerId.equals(followingId)) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "不能关注自己");
        }

        // 检查被关注用户是否存在
        User followingUser = userMapper.selectById(followingId);
        if (followingUser == null || followingUser.getDeleted() == 1) {
            throw new BusinessException(StatusCode.NOT_FOUND, "用户不存在");
        }

        // 检查是否已关注
        LambdaQueryWrapper<UserRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRelation::getFollowerId, followerId)
                .eq(UserRelation::getFollowingId, followingId);
        if (userRelationMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "已关注该用户");
        }

        // 创建关注关系
        UserRelation relation = UserRelation.builder()
                .id(idGenerator.nextId())
                .followerId(followerId)
                .followingId(followingId)
                .createdAt(LocalDateTime.now())
                .build();
        userRelationMapper.insert(relation);

        // 更新Redis缓存
        redisService.addFollowing(followerId, followingId);

        // 清除用户信息缓存（因为关注数变了）
        redisService.deleteUserInfo(followerId);
        redisService.deleteUserInfo(followingId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollowUser(Long followerId, Long followingId) {
        // 检查是否已关注
        LambdaQueryWrapper<UserRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRelation::getFollowerId, followerId)
                .eq(UserRelation::getFollowingId, followingId);
        UserRelation relation = userRelationMapper.selectOne(wrapper);
        
        if (relation == null) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "未关注该用户");
        }

        // 删除关注关系
        userRelationMapper.deleteById(relation.getId());

        // 更新Redis缓存
        redisService.removeFollowing(followerId, followingId);

        // 清除用户信息缓存（因为关注数变了）
        redisService.deleteUserInfo(followerId);
        redisService.deleteUserInfo(followingId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // 验证新密码和确认密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "两次输入的新密码不一致");
        }

        // 验证新密码不能与旧密码相同
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "新密码不能与旧密码相同");
        }

        // 查询用户
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(StatusCode.NOT_FOUND, "用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "旧密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 清除用户信息缓存
        redisService.deleteUserInfo(userId);

        log.info("用户修改密码成功: userId={}", userId);
    }

    /**
     * 构建用户信息VO
     */
    private UserInfoVO buildUserInfoVO(User user, Long currentUserId) {
        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(user, vo);

        // 统计关注数和粉丝数
        Long followingCount = userRelationMapper.countFollowing(user.getId());
        Long followerCount = userRelationMapper.countFollower(user.getId());
        vo.setFollowingCount(followingCount);
        vo.setFollowerCount(followerCount);

        // 判断是否关注（查询他人信息时）
        if (currentUserId != null && !currentUserId.equals(user.getId())) {
            int count = userRelationMapper.checkFollowing(currentUserId, user.getId());
            vo.setIsFollowing(count > 0);
        } else {
            vo.setIsFollowing(false);
        }

        return vo;
    }
}
