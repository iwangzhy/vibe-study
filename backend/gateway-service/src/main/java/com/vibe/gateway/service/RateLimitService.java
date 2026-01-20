package com.vibe.gateway.service;

import com.vibe.gateway.config.RateLimitProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 限流服务
 * 基于 Redis 实现双重限流逻辑
 */
@Slf4j
@Service
public class RateLimitService {
    
    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    
    @Autowired
    private RateLimitProperties rateLimitProperties;
    
    private static final String RATE_LIMIT_KEY_PREFIX = "rate:limit:send-code:";
    private static final String SHORT_SUFFIX = ":short";
    private static final String LONG_SUFFIX = ":long";
    
    /**
     * 限流检查结果
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final String message;
        private final long remainingTime;
        
        public RateLimitResult(boolean allowed, String message, long remainingTime) {
            this.allowed = allowed;
            this.message = message;
            this.remainingTime = remainingTime;
        }
        
        public boolean isAllowed() {
            return allowed;
        }
        
        public String getMessage() {
            return message;
        }
        
        public long getRemainingTime() {
            return remainingTime;
        }
        
        public static RateLimitResult allowed() {
            return new RateLimitResult(true, null, 0);
        }
        
        public static RateLimitResult rejected(String message, long remainingTime) {
            return new RateLimitResult(false, message, remainingTime);
        }
    }
    
    /**
     * 检查是否允许请求（双重限流）
     * 
     * @param email 邮箱地址
     * @return 限流检查结果
     */
    public Mono<RateLimitResult> checkRateLimit(String email) {
        if (!rateLimitProperties.isEnabled()) {
            return Mono.just(RateLimitResult.allowed());
        }
        
        String shortKey = RATE_LIMIT_KEY_PREFIX + email + SHORT_SUFFIX;
        String longKey = RATE_LIMIT_KEY_PREFIX + email + LONG_SUFFIX;
        
        // 先检查短期限流
        return checkLimit(shortKey, rateLimitProperties.getShortLimit(), rateLimitProperties.getShortWindow())
                .flatMap(shortResult -> {
                    if (!shortResult.isAllowed()) {
                        return Mono.just(shortResult);
                    }
                    
                    // 短期限流通过，检查长期限流
                    return checkLimit(longKey, rateLimitProperties.getLongLimit(), rateLimitProperties.getLongWindow());
                });
    }
    
    /**
     * 检查单个限流规则
     * 
     * @param key Redis key
     * @param limit 限制次数
     * @param windowSeconds 时间窗口（秒）
     * @return 限流检查结果
     */
    private Mono<RateLimitResult> checkLimit(String key, int limit, int windowSeconds) {
        return reactiveRedisTemplate.opsForValue()
                .get(key)
                .defaultIfEmpty("0")
                .flatMap(currentCountStr -> {
                    int currentCount = Integer.parseInt(currentCountStr);
                    
                    if (currentCount >= limit) {
                        // 超过限制，获取剩余时间
                        return reactiveRedisTemplate.getExpire(key)
                                .map(ttl -> {
                                    long remainingSeconds = ttl != null && ttl.getSeconds() > 0 ? ttl.getSeconds() : windowSeconds;
                                    String message = formatRateLimitMessage(windowSeconds, remainingSeconds);
                                    log.warn("限流触发: key={}, currentCount={}, limit={}, remainingSeconds={}", 
                                            key, currentCount, limit, remainingSeconds);
                                    return RateLimitResult.rejected(message, remainingSeconds);
                                });
                    }
                    
                    // 未超过限制，计数+1
                    return reactiveRedisTemplate.opsForValue()
                            .increment(key)
                            .flatMap(newCount -> {
                                if (newCount == 1) {
                                    // 第一次请求，设置过期时间
                                    return reactiveRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds))
                                            .thenReturn(RateLimitResult.allowed());
                                }
                                return Mono.just(RateLimitResult.allowed());
                            });
                })
                .onErrorResume(e -> {
                    // Redis 异常时，放行请求（降级策略）
                    log.error("限流检查失败，放行请求: key={}, error={}", key, e.getMessage());
                    return Mono.just(RateLimitResult.allowed());
                });
    }
    
    /**
     * 格式化限流提示信息
     * 
     * @param windowSeconds 时间窗口
     * @param remainingSeconds 剩余时间
     * @return 提示信息
     */
    private String formatRateLimitMessage(int windowSeconds, long remainingSeconds) {
        String timeUnit;
        long time;
        
        if (windowSeconds >= 3600) {
            // 1小时限流
            timeUnit = "小时";
            time = remainingSeconds / 60; // 转换为分钟显示
            if (time == 0) {
                time = 1; // 至少显示1分钟
            }
            return String.format("验证码发送过于频繁，请 %d 分钟后再试", time);
        } else {
            // 1分钟限流
            timeUnit = "秒";
            time = remainingSeconds;
            return String.format("验证码发送过于频繁，请 %d 秒后再试", time);
        }
    }
}
