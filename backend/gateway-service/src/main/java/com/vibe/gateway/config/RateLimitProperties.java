package com.vibe.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 限流配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "rate-limit.send-code")
public class RateLimitProperties {
    
    /**
     * 是否启用限流
     */
    private boolean enabled = true;
    
    /**
     * 短期限制：时间窗口内最大请求次数
     */
    private int shortLimit = 1;
    
    /**
     * 短期限制：时间窗口（秒）
     */
    private int shortWindow = 60;
    
    /**
     * 长期限制：时间窗口内最大请求次数
     */
    private int longLimit = 5;
    
    /**
     * 长期限制：时间窗口（秒）
     */
    private int longWindow = 3600;
}
