package com.vibe.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis服务
 */
@Slf4j
@Service
public class RedisService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis Key前缀
     */
    private static final String USER_INFO_KEY = "user:info:";
    private static final String USER_FOLLOWING_KEY = "user:following:";
    private static final String USER_FOLLOWER_KEY = "user:follower:";
    private static final String EMAIL_CODE_KEY = "email:code:";

    /**
     * 默认过期时间（1小时）
     */
    private static final long DEFAULT_EXPIRE_TIME = 3600L;

    /**
     * 验证码过期时间（5分钟）
     */
    private static final long CODE_EXPIRE_TIME = 300L;

    /**
     * 存储用户信息
     */
    public void setUserInfo(Long userId, Object userInfo) {
        try {
            redisTemplate.opsForValue().set(USER_INFO_KEY + userId, userInfo, DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("存储用户信息到Redis失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 获取用户信息
     */
    public Object getUserInfo(Long userId) {
        try {
            return redisTemplate.opsForValue().get(USER_INFO_KEY + userId);
        } catch (Exception e) {
            log.error("从Redis获取用户信息失败: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 删除用户信息缓存
     */
    public void deleteUserInfo(Long userId) {
        try {
            redisTemplate.delete(USER_INFO_KEY + userId);
        } catch (Exception e) {
            log.error("删除用户信息缓存失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 添加关注关系到Redis
     */
    public void addFollowing(Long followerId, Long followingId) {
        try {
            // 添加到关注列表
            redisTemplate.opsForSet().add(USER_FOLLOWING_KEY + followerId, followingId);
            // 添加到粉丝列表
            redisTemplate.opsForSet().add(USER_FOLLOWER_KEY + followingId, followerId);
        } catch (Exception e) {
            log.error("添加关注关系到Redis失败: followerId={}, followingId={}, error={}", followerId, followingId, e.getMessage());
        }
    }

    /**
     * 从Redis删除关注关系
     */
    public void removeFollowing(Long followerId, Long followingId) {
        try {
            // 从关注列表删除
            redisTemplate.opsForSet().remove(USER_FOLLOWING_KEY + followerId, followingId);
            // 从粉丝列表删除
            redisTemplate.opsForSet().remove(USER_FOLLOWER_KEY + followingId, followerId);
        } catch (Exception e) {
            log.error("从Redis删除关注关系失败: followerId={}, followingId={}, error={}", followerId, followingId, e.getMessage());
        }
    }

    /**
     * 检查是否关注
     */
    public boolean isFollowing(Long followerId, Long followingId) {
        try {
            Boolean result = redisTemplate.opsForSet().isMember(USER_FOLLOWING_KEY + followerId, followingId);
            return result != null && result;
        } catch (Exception e) {
            log.error("检查关注关系失败: followerId={}, followingId={}, error={}", followerId, followingId, e.getMessage());
            return false;
        }
    }

    /**
     * 获取关注数
     */
    public Long getFollowingCount(Long userId) {
        try {
            return redisTemplate.opsForSet().size(USER_FOLLOWING_KEY + userId);
        } catch (Exception e) {
            log.error("获取关注数失败: userId={}, error={}", userId, e.getMessage());
            return 0L;
        }
    }

    /**
     * 获取粉丝数
     */
    public Long getFollowerCount(Long userId) {
        try {
            return redisTemplate.opsForSet().size(USER_FOLLOWER_KEY + userId);
        } catch (Exception e) {
            log.error("获取粉丝数失败: userId={}, error={}", userId, e.getMessage());
            return 0L;
        }
    }

    /**
     * 存储邮箱验证码
     */
    public void setEmailCode(String email, String code) {
        try {
            redisTemplate.opsForValue().set(EMAIL_CODE_KEY + email, code, CODE_EXPIRE_TIME, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("存储邮箱验证码失败: email={}, error={}", email, e.getMessage());
        }
    }

    /**
     * 获取邮箱验证码
     */
    public String getEmailCode(String email) {
        try {
            Object code = redisTemplate.opsForValue().get(EMAIL_CODE_KEY + email);
            return code == null ? null : code.toString();
        } catch (Exception e) {
            log.error("获取邮箱验证码失败: email={}, error={}", email, e.getMessage());
            return null;
        }
    }

    /**
     * 删除邮箱验证码
     */
    public void deleteEmailCode(String email) {
        try {
            redisTemplate.delete(EMAIL_CODE_KEY + email);
        } catch (Exception e) {
            log.error("删除邮箱验证码失败: email={}, error={}", email, e.getMessage());
        }
    }

    /**
     * 初始化用户关注关系缓存
     */
    public void initUserFollowingCache(Long userId, Set<Long> followingIds) {
        try {
            String key = USER_FOLLOWING_KEY + userId;
            redisTemplate.delete(key);
            if (followingIds != null && !followingIds.isEmpty()) {
                redisTemplate.opsForSet().add(key, followingIds.toArray());
            }
        } catch (Exception e) {
            log.error("初始化用户关注关系缓存失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 初始化用户粉丝关系缓存
     */
    public void initUserFollowerCache(Long userId, Set<Long> followerIds) {
        try {
            String key = USER_FOLLOWER_KEY + userId;
            redisTemplate.delete(key);
            if (followerIds != null && !followerIds.isEmpty()) {
                redisTemplate.opsForSet().add(key, followerIds.toArray());
            }
        } catch (Exception e) {
            log.error("初始化用户粉丝关系缓存失败: userId={}, error={}", userId, e.getMessage());
        }
    }
}
