package com.vibe.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.common.core.utils.JwtUtils;
import com.vibe.gateway.config.WhitelistConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT认证全局过滤器
 * 拦截所有请求，验证JWT Token
 * 白名单内的请求跳过认证
 */
@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private WhitelistConfig whitelistConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 过滤器执行顺序（数值越小，优先级越高）
     */
    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        log.debug("请求路径: {} {}", request.getMethod(), path);

        // 1. 检查是否在白名单中
        if (whitelistConfig.isWhitelisted(path)) {
            log.debug("请求路径 [{}] 在白名单中，跳过认证", path);
            return chain.filter(exchange);
        }

        // 2. 获取Token
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            log.warn("请求路径 [{}] 未携带Token", path);
            return unauthorizedResponse(exchange, "未授权：缺少Token");
        }

        // 3. 验证Token
        if (!JwtUtils.validateToken(token)) {
            log.warn("请求路径 [{}] Token验证失败", path);
            return unauthorizedResponse(exchange, "未授权：Token无效或已过期");
        }

        // 4. 提取用户信息并传递到下游服务
        Long userId = JwtUtils.getUserIdFromToken(token);
        String username = JwtUtils.getUsernameFromToken(token);
        
        if (userId == null || !StringUtils.hasText(username)) {
            log.warn("请求路径 [{}] Token中缺少用户信息", path);
            return unauthorizedResponse(exchange, "未授权：Token信息不完整");
        }

        log.debug("认证成功: userId={}, username={}", userId, username);

        // 5. 将用户信息添加到请求头，传递给下游服务
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId.toString())
                .header("X-Username", username)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        return chain.filter(mutatedExchange);
    }

    /**
     * 从请求中提取Token
     * 优先从 Authorization 头中获取，格式: Bearer {token}
     */
    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 返回401未授权响应
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> result = new HashMap<>();
        result.put("code", HttpStatus.UNAUTHORIZED.value());
        result.put("message", message);
        result.put("data", null);
        result.put("timestamp", System.currentTimeMillis());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            log.error("序列化响应失败", e);
            bytes = "{\"code\":401,\"message\":\"未授权\"}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
