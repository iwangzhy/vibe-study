package com.vibe.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.gateway.service.RateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 发送验证码限流过滤器
 * 对 /api/user/auth/send-code 接口进行双重限流：
 * 1. 短期限流：同一邮箱 1 分钟内最多发送 1 次
 * 2. 长期限流：同一邮箱 1 小时内最多发送 5 次
 */
@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {
    
    @Autowired
    private RateLimitService rateLimitService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String SEND_CODE_PATH = "/api/user/auth/send-code";
    
    /**
     * 过滤器执行顺序（在认证过滤器之前执行）
     */
    @Override
    public int getOrder() {
        return -200;
    }
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        // 只对发送验证码接口进行限流
        if (!SEND_CODE_PATH.equals(path)) {
            return chain.filter(exchange);
        }
        
        log.debug("限流检查: path={}, method={}", path, request.getMethod());
        
        // 读取请求体获取邮箱地址
        return DataBufferUtils.join(request.getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    
                    String bodyString = new String(bytes, StandardCharsets.UTF_8);
                    
                    // 解析邮箱地址
                    String email = extractEmail(bodyString);
                    if (!StringUtils.hasText(email)) {
                        log.warn("无法从请求体中提取邮箱地址: body={}", bodyString);
                        // 无法提取邮箱时，放行请求，让后端验证处理
                        return chain.filter(rebuildExchange(exchange, bytes));
                    }
                    
                    // 进行限流检查
                    return rateLimitService.checkRateLimit(email)
                            .flatMap(result -> {
                                if (!result.isAllowed()) {
                                    // 触发限流，返回 429
                                    log.warn("限流拦截: email={}, message={}, remainingTime={}s", 
                                            email, result.getMessage(), result.getRemainingTime());
                                    return rateLimitedResponse(exchange, result.getMessage(), result.getRemainingTime());
                                }
                                
                                // 限流通过，重建请求并继续处理
                                log.debug("限流检查通过: email={}", email);
                                return chain.filter(rebuildExchange(exchange, bytes));
                            });
                })
                .onErrorResume(e -> {
                    // 异常时放行请求（降级策略）
                    log.error("限流过滤器异常，放行请求: error={}", e.getMessage(), e);
                    return chain.filter(exchange);
                });
    }
    
    /**
     * 从请求体 JSON 中提取邮箱地址
     * 
     * @param bodyString 请求体字符串
     * @return 邮箱地址，如果提取失败返回 null
     */
    private String extractEmail(String bodyString) {
        try {
            JsonNode jsonNode = objectMapper.readTree(bodyString);
            JsonNode emailNode = jsonNode.get("email");
            if (emailNode != null && !emailNode.isNull()) {
                return emailNode.asText();
            }
        } catch (JsonProcessingException e) {
            log.error("解析请求体 JSON 失败: body={}, error={}", bodyString, e.getMessage());
        }
        return null;
    }
    
    /**
     * 重建 ServerWebExchange，因为请求体已被读取
     * 
     * @param exchange 原始 exchange
     * @param bodyBytes 请求体字节数组
     * @return 重建后的 exchange
     */
    private ServerWebExchange rebuildExchange(ServerWebExchange exchange, byte[] bodyBytes) {
        ServerHttpRequest request = exchange.getRequest();
        
        // 创建新的请求体
        DataBuffer dataBuffer = exchange.getResponse().bufferFactory().wrap(bodyBytes);
        Flux<DataBuffer> bodyFlux = Flux.just(dataBuffer);
        
        // 创建新的请求装饰器
        ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(request) {
            @Override
            public Flux<DataBuffer> getBody() {
                return bodyFlux;
            }
        };
        
        // 返回新的 exchange
        return exchange.mutate().request(decoratedRequest).build();
    }
    
    /**
     * 返回 429 限流响应
     * 
     * @param exchange ServerWebExchange
     * @param message 错误消息
     * @param remainingTime 剩余等待时间（秒）
     * @return Mono<Void>
     */
    private Mono<Void> rateLimitedResponse(ServerWebExchange exchange, String message, long remainingTime) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        // 添加 Retry-After 响应头（标准 HTTP 头，告知客户端应该等待多久）
        response.getHeaders().set("Retry-After", String.valueOf(remainingTime));
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", HttpStatus.TOO_MANY_REQUESTS.value());
        result.put("message", message);
        result.put("data", null);
        result.put("timestamp", System.currentTimeMillis());
        
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            log.error("序列化响应失败", e);
            bytes = "{\"code\":429,\"message\":\"请求过于频繁\"}".getBytes(StandardCharsets.UTF_8);
        }
        
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
