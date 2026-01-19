package com.vibe.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 白名单配置管理类
 * 提供白名单路径匹配功能
 */
@Slf4j
@Configuration
public class WhitelistConfig {

    @Autowired
    private WhitelistProperties whitelistProperties;

    /**
     * Ant风格路径匹配器（支持 * 和 ** 通配符）
     */
    private final PathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 初始化时打印白名单配置
     */
    @PostConstruct
    public void init() {
        log.info("========== Gateway Whitelist Configuration ==========");
        List<String> paths = whitelistProperties.getPaths();
        if (paths == null || paths.isEmpty()) {
            log.warn("白名单配置为空，所有请求都需要认证");
        } else {
            log.info("白名单路径数量: {}", paths.size());
            paths.forEach(path -> log.info("  - {}", path));
        }
        log.info("====================================================");
    }

    /**
     * 判断请求路径是否在白名单中
     *
     * @param requestPath 请求路径
     * @return true-在白名单中（不需要认证），false-不在白名单中（需要认证）
     */
    public boolean isWhitelisted(String requestPath) {
        if (requestPath == null || requestPath.isEmpty()) {
            return false;
        }

        List<String> paths = whitelistProperties.getPaths();
        if (paths == null || paths.isEmpty()) {
            return false;
        }

        // 使用 Ant 风格路径匹配（支持通配符）
        for (String pattern : paths) {
            if (pathMatcher.match(pattern, requestPath)) {
                log.debug("请求路径 [{}] 匹配白名单规则 [{}]，跳过认证", requestPath, pattern);
                return true;
            }
        }

        return false;
    }

    /**
     * 获取所有白名单路径
     *
     * @return 白名单路径列表
     */
    public List<String> getWhitelistPaths() {
        return whitelistProperties.getPaths();
    }
}
