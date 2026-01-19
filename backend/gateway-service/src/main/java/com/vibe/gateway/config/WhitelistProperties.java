package com.vibe.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 白名单配置属性类
 * 从 application.yml 中读取 whitelist.paths 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "whitelist")
public class WhitelistProperties {

    /**
     * 白名单路径列表（不需要认证的路径）
     */
    private List<String> paths = new ArrayList<>();
}
