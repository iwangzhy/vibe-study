# Spring Cloud Gateway 最佳实践指南

> **项目**: Vibe-Study 社交媒体学习平台  
> **版本**: Spring Cloud Gateway 3.1.9 (Spring Cloud 2021.0.8)  
> **最后更新**: 2026-01-19

---

## 📋 目录

1. [Gateway 核心概念](#1-gateway-核心概念)
2. [路由配置最佳实践](#2-路由配置最佳实践)
3. [过滤器使用指南](#3-过滤器使用指南)
4. [认证与鉴权](#4-认证与鉴权)
5. [跨域配置](#5-跨域配置)
6. [性能优化](#6-性能优化)
7. [监控与日志](#7-监控与日志)
8. [常见问题与解决方案](#8-常见问题与解决方案)

---

## 1. Gateway 核心概念

### 1.1 三大核心组件

```
┌─────────────────────────────────────────────────────┐
│                    API Gateway                       │
│                   (端口: 8080)                       │
├─────────────────────────────────────────────────────┤
│                                                       │
│  ┌─────────┐    ┌─────────┐    ┌──────────┐        │
│  │ Route   │───→│ Predicate│───→│ Filter   │        │
│  │ (路由)  │    │ (断言)   │    │ (过滤器)  │        │
│  └─────────┘    └─────────┘    └──────────┘        │
│       │              │                │              │
│       │              │                │              │
└───────┼──────────────┼────────────────┼──────────────┘
        │              │                │
        ▼              ▼                ▼
   配置路由规则    匹配请求条件    处理请求/响应
        │              │                │
        └──────────────┴────────────────┘
                       │
                       ▼
        ┌──────────────────────────────┐
        │      Downstream Services      │
        ├──────────────────────────────┤
        │  user-service    (8081)      │
        │  post-service    (8082)      │
        │  comment-service (8083)      │
        └──────────────────────────────┘
```

#### **Route（路由）**
路由是 Gateway 的基本构建块，包含：
- **ID**: 路由唯一标识
- **URI**: 目标服务地址（支持 `http://`、`https://`、`lb://服务名`）
- **Predicates**: 断言集合（匹配条件）
- **Filters**: 过滤器集合（请求/响应处理）

#### **Predicate（断言）**
用于匹配 HTTP 请求的任何内容（请求头、请求参数、路径等）：
- `Path`: 路径匹配（如 `/api/user/**`）
- `Method`: HTTP 方法匹配（如 `GET`, `POST`）
- `Header`: 请求头匹配
- `Query`: 请求参数匹配
- `Cookie`: Cookie 匹配

#### **Filter（过滤器）**
在请求发送到下游服务前或响应返回客户端前修改请求/响应：
- **Pre Filter**: 请求发送前执行（如添加请求头、鉴权）
- **Post Filter**: 响应返回前执行（如修改响应头、日志记录）

### 1.2 请求处理流程

```
客户端请求
    │
    ▼
┌─────────────────────────────────────────────┐
│          1. Gateway Handler Mapping          │
│     (匹配请求到具体的 Route)                 │
└─────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────┐
│          2. Gateway Web Handler              │
│     (执行 Filter Chain)                      │
├─────────────────────────────────────────────┤
│  Pre Filters:                                │
│   ├─ Global Filters (全局过滤器)            │
│   ├─ Route Filters (路由过滤器)             │
│   └─ Custom Filters (自定义过滤器)          │
│                                               │
│  ┌───────────────────────────────────┐      │
│  │  3. 转发请求到下游服务              │      │
│  │     (通过 LoadBalancer 负载均衡)    │      │
│  └───────────────────────────────────┘      │
│                                               │
│  Post Filters:                               │
│   ├─ 修改响应头                              │
│   ├─ 日志记录                                │
│   └─ 监控统计                                │
└─────────────────────────────────────────────┘
    │
    ▼
返回给客户端
```

---

## 2. 路由配置最佳实践

### 2.1 ✅ 推荐：统一路径前缀，不使用 StripPrefix

**原则**: 保持前端请求路径与后端服务路径一致，避免路径转换混乱。

#### **配置示例**

```yaml
spring:
  cloud:
    gateway:
      routes:
        # 用户服务路由
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          # ✅ 不使用 StripPrefix，直接转发完整路径
        
        # 动态服务路由
        - id: post-service
          uri: lb://post-service
          predicates:
            - Path=/api/post/**
        
        # 评论服务路由
        - id: comment-service
          uri: lb://comment-service
          predicates:
            - Path=/api/comment/**
```

#### **后端 Controller 配置**

```java
// user-service
@RestController
@RequestMapping("/api/user")  // ✅ 与 Gateway 路径匹配
public class UserController {
    
    @PostMapping("/auth/login")  // 完整路径: /api/user/auth/login
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        // ...
    }
}

// post-service
@RestController
@RequestMapping("/api/post")  // ✅ 与 Gateway 路径匹配
public class PostController {
    
    @GetMapping("/list")  // 完整路径: /api/post/list
    public Result<List<PostVO>> getPostList() {
        // ...
    }
}
```

#### **请求流转示例**

```
前端请求: POST http://localhost:8080/api/user/auth/login
    ↓
Gateway 匹配: Path=/api/user/**
    ↓
转发到 user-service: POST http://user-service:8081/api/user/auth/login
    ↓
UserController 接收: @PostMapping("/auth/login") 
    (基于 @RequestMapping("/api/user"))
    ↓
成功响应: 200 OK ✅
```

### 2.2 ❌ 不推荐：使用 StripPrefix

#### **问题配置**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=1  # ❌ 会移除 /api 前缀
```

#### **导致的问题**

```
前端请求: POST http://localhost:8080/api/user/auth/login
    ↓
Gateway 匹配: Path=/api/user/**
    ↓
StripPrefix=1 处理: 移除第一段 /api
    ↓
转发到 user-service: POST http://user-service:8081/user/auth/login
    ↓
UserController 期望: /api/user/auth/login
    ↓
结果: 404 Not Found ❌
```

#### **可能的使用场景（谨慎使用）**

如果你的后端服务**没有** `/api` 前缀，可以使用 `StripPrefix=2`：

```yaml
# Gateway 配置
routes:
  - id: user-service
    uri: lb://user-service
    predicates:
      - Path=/api/user/**
    filters:
      - StripPrefix=2  # 移除 /api/user
```

```java
// 后端 Controller（不推荐这种设计）
@RestController
@RequestMapping("/")  // ❌ 没有统一前缀
public class UserController {
    
    @PostMapping("/auth/login")  // 路径: /auth/login
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        // ...
    }
}
```

**为什么不推荐**：
1. 后端服务失去了命名空间隔离
2. 多个服务可能出现路径冲突
3. 直接访问后端服务（绕过 Gateway）时路径不一致

### 2.3 路由优先级与顺序

**规则**: 路由按配置顺序匹配，**先匹配先生效**。

#### **示例：精确路径优先**

```yaml
spring:
  cloud:
    gateway:
      routes:
        # 1. 特殊路径：健康检查（优先级高）
        - id: user-health-check
          uri: lb://user-service
          predicates:
            - Path=/api/user/health
            - Method=GET
          order: 1  # 显式设置优先级（数字越小越优先）
        
        # 2. 认证接口：无需鉴权
        - id: user-auth
          uri: lb://user-service
          predicates:
            - Path=/api/user/auth/**
          order: 2
        
        # 3. 通用路径：需要鉴权（优先级低）
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          filters:
            - AuthFilter  # 自定义鉴权过滤器
          order: 10
```

**匹配逻辑**：
- 请求 `/api/user/health` → 匹配 `user-health-check`（无鉴权）
- 请求 `/api/user/auth/login` → 匹配 `user-auth`（无鉴权）
- 请求 `/api/user/info/123` → 匹配 `user-service`（需鉴权）

### 2.4 负载均衡配置

#### **基于 Nacos 的服务发现**

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true                  # 启用服务发现路由
          lower-case-service-id: true    # 服务名转小写
      
      routes:
        - id: user-service
          uri: lb://user-service  # lb:// 表示使用 LoadBalancer 负载均衡
          predicates:
            - Path=/api/user/**
```

#### **自定义负载均衡策略**

```java
@Configuration
public class LoadBalancerConfig {
    
    /**
     * 自定义负载均衡策略：轮询
     */
    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RoundRobinLoadBalancer(
            loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
            name
        );
    }
}
```

### 2.5 动态路由配置

#### **基于 Nacos 配置中心**

```yaml
# nacos 配置: gateway-routes.yml
spring:
  cloud:
    gateway:
      routes:
        - id: ${route.id:user-service}
          uri: ${route.uri:lb://user-service}
          predicates:
            - Path=${route.path:/api/user/**}
          metadata:
            version: ${route.version:v1}
```

#### **通过代码动态添加路由**

```java
@Slf4j
@Service
public class DynamicRouteService {
    
    @Resource
    private RouteDefinitionWriter routeDefinitionWriter;
    
    @Resource
    private ApplicationEventPublisher publisher;
    
    /**
     * 动态添加路由
     */
    public void addRoute(RouteDefinition definition) {
        routeDefinitionWriter.save(Mono.just(definition)).subscribe();
        // 发布路由刷新事件
        publisher.publishEvent(new RefreshRoutesEvent(this));
        log.info("动态添加路由: {}", definition.getId());
    }
    
    /**
     * 动态删除路由
     */
    public void deleteRoute(String routeId) {
        routeDefinitionWriter.delete(Mono.just(routeId)).subscribe();
        publisher.publishEvent(new RefreshRoutesEvent(this));
        log.info("动态删除路由: {}", routeId);
    }
}
```

---

## 3. 过滤器使用指南

### 3.1 内置过滤器（GatewayFilter）

#### **AddRequestHeader - 添加请求头**

```yaml
routes:
  - id: user-service
    uri: lb://user-service
    predicates:
      - Path=/api/user/**
    filters:
      - AddRequestHeader=X-Gateway-Source, vibe-gateway
      - AddRequestHeader=X-Request-Time, ${timestamp}
```

#### **AddResponseHeader - 添加响应头**

```yaml
filters:
  - AddResponseHeader=X-Response-Time, ${timestamp}
  - AddResponseHeader=X-Gateway-Version, 1.0.0
```

#### **RemoveRequestHeader - 移除请求头**

```yaml
filters:
  - RemoveRequestHeader=Cookie  # 移除敏感信息
  - RemoveRequestHeader=X-Internal-Auth
```

#### **SetPath - 重写路径**

```yaml
# 将 /api/v1/user/* 转换为 /user/*
routes:
  - id: user-service-v1
    uri: lb://user-service
    predicates:
      - Path=/api/v1/user/**
    filters:
      - SetPath=/user/{segment}
```

#### **RewritePath - 正则重写路径**

```yaml
# 将 /red/blue 转换为 /blue
filters:
  - RewritePath=/red(?<segment>/?.*), $\{segment}
```

#### **PrefixPath - 添加路径前缀**

```yaml
# 将 /user/info 转换为 /api/user/info
filters:
  - PrefixPath=/api
```

#### **RedirectTo - 重定向**

```yaml
# 301 永久重定向
filters:
  - RedirectTo=301, https://vibe-study.com

# 302 临时重定向
filters:
  - RedirectTo=302, /api/v2/user/info
```

#### **Retry - 重试**

```yaml
filters:
  - name: Retry
    args:
      retries: 3                    # 重试次数
      statuses: BAD_GATEWAY         # 触发重试的状态码
      methods: GET, POST            # 支持重试的方法
      backoff:
        firstBackoff: 10ms          # 首次重试延迟
        maxBackoff: 50ms            # 最大重试延迟
        factor: 2                   # 延迟倍数
        basedOnPreviousValue: false
```

#### **RequestRateLimiter - 限流**

```yaml
filters:
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 10  # 每秒生成令牌数
      redis-rate-limiter.burstCapacity: 20  # 令牌桶容量
      redis-rate-limiter.requestedTokens: 1 # 每次请求消耗令牌数
      key-resolver: "#{@userKeyResolver}"   # 限流 Key 解析器
```

**自定义限流 Key 解析器**：

```java
@Configuration
public class RateLimiterConfig {
    
    /**
     * 基于用户 ID 限流
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String token = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (token != null) {
                Long userId = JwtUtils.getUserIdFromToken(token);
                return Mono.just(userId.toString());
            }
            // 未登录用户基于 IP 限流
            return Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        };
    }
    
    /**
     * 基于 IP 限流
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }
    
    /**
     * 基于请求路径限流
     */
    @Bean
    public KeyResolver pathKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getPath().value());
    }
}
```

#### **CircuitBreaker - 熔断器**

```yaml
filters:
  - name: CircuitBreaker
    args:
      name: userServiceCircuitBreaker
      fallbackUri: forward:/fallback/user  # 降级处理
```

**熔断器配置**：

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 10                 # 滑动窗口大小
        minimumNumberOfCalls: 5               # 最小调用次数
        failureRateThreshold: 50              # 失败率阈值（50%）
        waitDurationInOpenState: 10000        # 熔断器打开后等待时间（10秒）
        permittedNumberOfCallsInHalfOpenState: 3  # 半开状态允许调用次数
```

### 3.2 全局过滤器（GlobalFilter）

#### **自定义全局过滤器：请求日志**

```java
@Slf4j
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();
        String clientIp = getClientIp(request);
        
        long startTime = System.currentTimeMillis();
        log.info("请求开始: method={}, path={}, clientIp={}", method, path, clientIp);
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            long duration = System.currentTimeMillis() - startTime;
            log.info("请求结束: method={}, path={}, status={}, duration={}ms",
                method, path, response.getStatusCode(), duration);
        }));
    }
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;  // 最高优先级
    }
    
    private String getClientIp(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String ip = headers.getFirst("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0];
        }
        ip = headers.getFirst("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }
        return request.getRemoteAddress().getAddress().getHostAddress();
    }
}
```

#### **自定义全局过滤器：JWT 鉴权**

```java
@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {
    
    @Value("${whitelist.paths}")
    private List<String> whitelistPaths;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        
        // 白名单路径放行
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }
        
        // 获取 Token
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "未提供认证令牌");
        }
        
        String token = authHeader.substring(7);
        
        // 验证 Token
        if (!JwtUtils.validateToken(token)) {
            return unauthorized(exchange, "认证令牌无效或已过期");
        }
        
        // 将用户信息添加到请求头
        Long userId = JwtUtils.getUserIdFromToken(token);
        ServerHttpRequest mutatedRequest = request.mutate()
            .header("X-User-Id", userId.toString())
            .build();
        
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
    
    @Override
    public int getOrder() {
        return -100;  // 在日志过滤器之后执行
    }
    
    private boolean isWhitelisted(String path) {
        return whitelistPaths.stream().anyMatch(pattern -> 
            new AntPathMatcher().match(pattern, path)
        );
    }
    
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        log.warn("认证失败: path={}, message={}", 
            exchange.getRequest().getPath().value(), message);
        
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        String body = String.format(
            "{\"code\":401,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
            message, System.currentTimeMillis()
        );
        
        return response.writeWith(Mono.just(
            response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))
        ));
    }
}
```

#### **自定义全局过滤器：响应体修改**

```java
@Slf4j
@Component
public class ResponseWrapperFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                    return super.writeWith(fluxBody.map(dataBuffer -> {
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        DataBufferUtils.release(dataBuffer);
                        
                        String responseBody = new String(content, StandardCharsets.UTF_8);
                        log.debug("响应体: {}", responseBody);
                        
                        // 修改响应体（示例：添加 Gateway 版本号）
                        // String modifiedBody = modifyResponse(responseBody);
                        
                        return bufferFactory.wrap(content);
                    }));
                }
                return super.writeWith(body);
            }
        };
        
        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }
    
    @Override
    public int getOrder() {
        return -200;  // 最后执行
    }
}
```

### 3.3 过滤器执行顺序

```
客户端请求
    ↓
┌─────────────────────────────────────────┐
│  GlobalFilter (Ordered.HIGHEST_PRECEDENCE) │
│  └─ RequestLogFilter (order = -1000)     │
├─────────────────────────────────────────┤
│  GlobalFilter (order = -100)             │
│  └─ AuthFilter                           │
├─────────────────────────────────────────┤
│  Route Filters (GatewayFilter)           │
│  ├─ AddRequestHeader                     │
│  ├─ RequestRateLimiter                   │
│  └─ CircuitBreaker                       │
├─────────────────────────────────────────┤
│  转发请求到下游服务                      │
├─────────────────────────────────────────┤
│  Post Filters (倒序执行)                 │
│  ├─ ResponseWrapperFilter                │
│  └─ AddResponseHeader                    │
└─────────────────────────────────────────┘
    ↓
返回给客户端
```

**Order 值规则**：
- **负数越小，优先级越高**（如 -1000 > -100）
- **Ordered.HIGHEST_PRECEDENCE**: -2147483648（最高优先级）
- **Ordered.LOWEST_PRECEDENCE**: 2147483647（最低优先级）
- **默认值**: 0

---

## 4. 认证与鉴权

### 4.1 JWT 认证架构

```
┌─────────────┐           ┌─────────────┐
│   Frontend  │           │   Gateway   │
│             │           │   (8080)    │
└─────────────┘           └─────────────┘
      │                         │
      │ 1. POST /api/user/auth/login
      │    { account, password }
      ├────────────────────────>│
      │                         │ 2. 转发到 user-service
      │                         ├─────────────────────────┐
      │                         │                         │
      │                         │ ┌─────────────────────┐ │
      │                         │ │   user-service      │ │
      │                         │ │   (8081)            │ │
      │                         │ └─────────────────────┘ │
      │                         │ 3. 验证用户名密码         │
      │                         │ 4. 生成 JWT Token       │
      │                         │ 5. 缓存用户信息到 Redis │
      │                         │<─────────────────────────┘
      │ 6. 返回 Token           │
      │<────────────────────────┤
      │                         │
      │ 7. 后续请求带上 Token   │
      │    Authorization: Bearer <token>
      ├────────────────────────>│
      │                         │ 8. AuthFilter 验证 Token
      │                         │ 9. 提取 userId 添加到请求头
      │                         │    X-User-Id: 123456
      │                         │ 10. 转发到下游服务
      │                         ├─────────────────────────┐
      │                         │                         │
      │                         │ ┌─────────────────────┐ │
      │                         │ │  Downstream Service │ │
      │                         │ └─────────────────────┘ │
      │                         │ 11. 从请求头获取 userId │
      │                         │ 12. 执行业务逻辑         │
      │                         │<─────────────────────────┘
      │ 13. 返回响应            │
      │<────────────────────────┤
      │                         │
```

### 4.2 白名单配置

```yaml
# application.yml
whitelist:
  paths:
    # 认证接口（登录、注册）
    - /api/user/auth/**
    
    # 公开资源
    - /api/user/info/{id}  # 查看用户信息（公开）
    - /api/post/list       # 浏览动态列表（公开）
    
    # 健康检查
    - /actuator/health
    - /actuator/info
    
    # 静态资源
    - /static/**
    - /favicon.ico
    
    # API 文档
    - /doc.html
    - /v3/api-docs/**
    - /swagger-ui/**
```

### 4.3 JWT 工具类（在 common-core 模块）

```java
@Slf4j
public class JwtUtils {
    
    public static final String HEADER_STRING = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    
    @Value("${jwt.secret}")
    private static String secret;
    
    @Value("${jwt.expiration}")
    private static Long expiration;
    
    /**
     * 生成 Token
     */
    public static String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
            .setSubject(userId.toString())
            .claim("username", username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }
    
    /**
     * 验证 Token
     */
    public static boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Token 格式错误: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("Token 签名无效: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Token 验证失败: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * 从 Token 提取用户 ID
     */
    public static Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(secret)
            .parseClaimsJws(token)
            .getBody();
        return Long.parseLong(claims.getSubject());
    }
    
    /**
     * 从 Authorization 头提取 Token
     */
    public static String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            return authHeader.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
```

### 4.4 下游服务获取用户信息

由于 Gateway 已经验证了 Token 并将 `userId` 添加到请求头，下游服务可以直接使用：

```java
@RestController
@RequestMapping("/api/post")
public class PostController {
    
    /**
     * 创建动态（需要登录）
     */
    @PostMapping("/create")
    public Result<PostVO> createPost(
            @RequestHeader("X-User-Id") Long userId,  // ✅ 从请求头获取
            @RequestBody CreatePostRequest request) {
        
        log.info("创建动态: userId={}", userId);
        PostVO post = postService.createPost(userId, request);
        return Result.success(post);
    }
    
    /**
     * 浏览动态（无需登录）
     */
    @GetMapping("/list")
    public Result<List<PostVO>> getPostList() {
        // 公开接口，无需用户信息
        List<PostVO> posts = postService.getPostList();
        return Result.success(posts);
    }
}
```

---

## 5. 跨域配置

### 5.1 全局跨域配置（推荐）

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origin-patterns: "*"      # 允许所有来源（生产环境需限制）
            allowed-methods: "*"              # 允许所有方法
            allowed-headers: "*"              # 允许所有请求头
            allow-credentials: true           # 允许携带 Cookie
            max-age: 3600                     # 预检请求缓存时间（1小时）
```

### 5.2 生产环境跨域配置（安全）

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            # 只允许特定域名
            allowed-origins:
              - https://vibe-study.com
              - https://www.vibe-study.com
              - https://m.vibe-study.com
            
            # 只允许特定方法
            allowed-methods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            
            # 只允许特定请求头
            allowed-headers:
              - Authorization
              - Content-Type
              - X-Requested-With
            
            # 暴露特定响应头给前端
            exposed-headers:
              - X-Total-Count
              - X-Page-Number
            
            allow-credentials: true
            max-age: 3600
```

### 5.3 跨域配置优先级

**问题**: 如果下游服务也配置了跨域，会导致重复的 CORS 头。

**解决方案**: 只在 Gateway 配置跨域，下游服务移除跨域配置。

```java
// ❌ 下游服务不要配置跨域
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // ❌ 不要在这里配置，会与 Gateway 冲突
        // registry.addMapping("/**").allowedOrigins("*");
    }
}
```

### 5.4 自定义跨域过滤器（高级）

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsWebFilter(source);
    }
}
```

---

## 6. 性能优化

### 6.1 连接池配置

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        # 连接超时（毫秒）
        connect-timeout: 5000
        
        # 响应超时（毫秒）
        response-timeout: 10s
        
        # 连接池配置
        pool:
          type: ELASTIC               # 连接池类型（ELASTIC/FIXED）
          max-connections: 500        # 最大连接数
          max-idle-time: 60s          # 最大空闲时间
          max-life-time: 600s         # 最大存活时间
          acquire-timeout: 45s        # 获取连接超时时间
```

### 6.2 Netty 调优

```yaml
spring:
  cloud:
    gateway:
      # Netty 配置
      netty:
        # I/O 线程数（默认为 CPU 核心数）
        event-loop-threads: 4
        
        # 工作线程数
        worker-threads: 16
```

### 6.3 缓存路由配置

**使用本地缓存加速路由匹配**：

```java
@Configuration
public class RouteLocatorConfig {
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("user-service", r -> r
                .path("/api/user/**")
                .uri("lb://user-service")
                .metadata("cache", true)  // 启用缓存
            )
            .build();
    }
}
```

### 6.4 请求/响应压缩

```yaml
server:
  compression:
    enabled: true                     # 启用压缩
    mime-types:
      - application/json
      - application/xml
      - text/html
      - text/xml
      - text/plain
    min-response-size: 1024          # 最小压缩大小（1KB）
```

### 6.5 限流与熔断

参见 [3.1 内置过滤器](#31-内置过滤器gatewayfilter) 中的 `RequestRateLimiter` 和 `CircuitBreaker`。

---

## 7. 监控与日志

### 7.1 Actuator 健康检查

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

**访问健康检查**：
```bash
curl http://localhost:8080/actuator/health
```

**响应示例**：
```json
{
  "status": "UP",
  "components": {
    "gateway": {
      "status": "UP"
    },
    "nacos": {
      "status": "UP",
      "details": {
        "serverAddr": "localhost:8848",
        "namespace": "vibe-coding"
      }
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

### 7.2 路由监控

**查看所有路由**：
```bash
curl http://localhost:8080/actuator/gateway/routes
```

**响应示例**：
```json
[
  {
    "route_id": "user-service",
    "route_definition": {
      "id": "user-service",
      "predicates": [
        {
          "name": "Path",
          "args": {
            "pattern": "/api/user/**"
          }
        }
      ],
      "filters": [],
      "uri": "lb://user-service",
      "order": 0
    },
    "order": 0
  }
]
```

### 7.3 自定义监控指标

```java
@Component
public class GatewayMetricsFilter implements GlobalFilter, Ordered {
    
    private final MeterRegistry meterRegistry;
    
    public GatewayMetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return chain.filter(exchange).doFinally(signalType -> {
            sample.stop(Timer.builder("gateway.requests")
                .tag("path", path)
                .tag("method", method)
                .tag("status", exchange.getResponse().getStatusCode().toString())
                .register(meterRegistry));
        });
    }
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
```

### 7.4 日志配置

```yaml
logging:
  level:
    root: INFO
    org.springframework.cloud.gateway: DEBUG  # Gateway 日志
    reactor.netty: DEBUG                      # Netty 日志
    com.vibe: DEBUG                           # 项目日志
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/gateway.log
    max-size: 100MB
    max-history: 30
```

---

## 8. 常见问题与解决方案

### 8.1 404 Not Found

**问题**: 请求返回 404

**排查步骤**：

1. **检查路由配置是否正确**
   ```yaml
   routes:
     - id: user-service
       uri: lb://user-service
       predicates:
         - Path=/api/user/**  # ✅ 确保路径匹配
   ```

2. **检查是否使用了 StripPrefix**
   ```yaml
   filters:
     - StripPrefix=1  # ❌ 可能导致路径不匹配
   ```

3. **检查下游服务是否启动**
   ```bash
   curl http://localhost:8081/api/user/health
   ```

4. **查看 Gateway 日志**
   ```bash
   # 开启 DEBUG 日志
   logging.level.org.springframework.cloud.gateway: DEBUG
   ```

5. **验证 Nacos 服务注册**
   - 访问 Nacos 控制台：http://localhost:8848/nacos
   - 检查 `user-service` 是否在线

### 8.2 CORS 跨域错误

**问题**: 浏览器提示跨域错误

```
Access to XMLHttpRequest at 'http://localhost:8080/api/user/auth/login' 
from origin 'http://localhost:3000' has been blocked by CORS policy
```

**解决方案**：

1. **确保 Gateway 配置了跨域**（参见 [5. 跨域配置](#5-跨域配置)）

2. **移除下游服务的跨域配置**（避免重复响应头）

3. **检查是否允许 Credentials**
   ```yaml
   allow-credentials: true  # ✅ 允许携带 Cookie
   ```

4. **前端配置**
   ```javascript
   axios.defaults.withCredentials = true;  // ✅ 允许发送 Cookie
   ```

### 8.3 Token 验证失败

**问题**: 请求返回 401 Unauthorized

**排查步骤**：

1. **检查 Token 是否正确携带**
   ```javascript
   // 前端请求头
   headers: {
     'Authorization': `Bearer ${token}`  // ✅ 注意 Bearer 前缀
   }
   ```

2. **检查 Token 是否过期**
   ```java
   // 后端验证
   if (JwtUtils.validateToken(token)) {
       // Token 有效
   } else {
       // Token 无效或过期
   }
   ```

3. **检查白名单配置**
   ```yaml
   whitelist:
     paths:
       - /api/user/auth/**  # ✅ 登录接口在白名单内
   ```

4. **检查 JWT secret 是否一致**
   ```yaml
   # Gateway 和 user-service 的 jwt.secret 必须相同
   jwt:
     secret: vibe-social-media-secret-key-2024
   ```

### 8.4 超时问题

**问题**: 请求超时 504 Gateway Timeout

**解决方案**：

1. **调整 Gateway 超时配置**
   ```yaml
   spring:
     cloud:
       gateway:
         httpclient:
           connect-timeout: 5000      # 连接超时
           response-timeout: 30s      # 响应超时（根据业务调整）
   ```

2. **调整下游服务超时配置**
   ```yaml
   # user-service
   spring:
     mvc:
       async:
         request-timeout: 30000  # 30秒
   ```

3. **检查数据库查询性能**
   ```yaml
   # 开启 SQL 日志
   mybatis-plus:
     configuration:
       log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
   ```

### 8.5 负载均衡失败

**问题**: 请求转发失败，提示服务不可用

**排查步骤**：

1. **检查服务是否注册到 Nacos**
   ```bash
   # 访问 Nacos 控制台
   http://localhost:8848/nacos
   ```

2. **检查 LoadBalancer 配置**
   ```yaml
   spring:
     cloud:
       loadbalancer:
         ribbon:
           enabled: false  # ✅ 禁用 Ribbon，使用新版 LoadBalancer
   ```

3. **检查服务实例健康状态**
   ```bash
   curl http://localhost:8081/actuator/health
   ```

### 8.6 内存溢出

**问题**: Gateway 运行一段时间后内存溢出

**解决方案**：

1. **限制请求/响应缓冲区大小**
   ```yaml
   spring:
     codec:
       max-in-memory-size: 10MB  # 限制内存缓冲区
   ```

2. **避免在过滤器中缓存大量数据**
   ```java
   // ❌ 不要这样做
   private static final Map<String, Object> cache = new ConcurrentHashMap<>();
   
   // ✅ 使用 Redis 或本地缓存（带过期时间）
   @Cacheable(value = "routes", expire = 300)
   public RouteDefinition getRoute(String id) {
       // ...
   }
   ```

3. **调整 JVM 参数**
   ```bash
   java -jar gateway-service.jar \
     -Xms512m \
     -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200
   ```

---

## 附录

### A. 完整配置示例

```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-service
  
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
        namespace: vibe-coding
        group: DEFAULT_GROUP
    
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      
      routes:
        # 用户服务
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          order: 10
        
        # 动态服务
        - id: post-service
          uri: lb://post-service
          predicates:
            - Path=/api/post/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
          order: 10
        
        # 评论服务
        - id: comment-service
          uri: lb://comment-service
          predicates:
            - Path=/api/comment/**
          order: 10
      
      # 跨域配置
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origin-patterns: "*"
            allowed-methods: "*"
            allowed-headers: "*"
            allow-credentials: true
            max-age: 3600
      
      # HTTP 客户端配置
      httpclient:
        connect-timeout: 5000
        response-timeout: 10s
        pool:
          type: ELASTIC
          max-connections: 500

# JWT 配置
jwt:
  secret: vibe-social-media-secret-key-2024
  expiration: 1800000
  refresh-expiration: 604800000

# 白名单配置
whitelist:
  paths:
    - /api/user/auth/**
    - /actuator/**
    - /doc.html
    - /v3/api-docs/**

# 日志配置
logging:
  level:
    root: INFO
    org.springframework.cloud.gateway: DEBUG
    com.vibe: DEBUG

# 监控配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,gateway
  endpoint:
    health:
      show-details: always
```

### B. 参考资源

- **官方文档**: https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/
- **Spring Cloud Alibaba**: https://github.com/alibaba/spring-cloud-alibaba
- **Nacos 文档**: https://nacos.io/zh-cn/docs/what-is-nacos.html
- **Resilience4j**: https://resilience4j.readme.io/docs/getting-started

---

**文档作者**: OpenCode  
**项目地址**: E:\worksapce_ai\vibe-study  
**最后更新**: 2026-01-19
