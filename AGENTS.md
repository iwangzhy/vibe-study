# AGENTS 开发指南

> 本文档为 AI 编程助手（如 OpenCode、Cursor、GitHub Copilot 等）提供项目开发规范和指南  
> **项目**: Vibe-Study 社交媒体学习平台  
> **架构**: Spring Cloud Alibaba 微服务 + Vue 3 前端  
> **目的**: 用于学习高并发分布式系统的实战项目  
> **最后更新**: 2026-01-19

---

## 📋 目录

1. [项目概览](#1-项目概览)
2. [构建与运行命令](#2-构建与运行命令)
3. [代码风格指南](#3-代码风格指南)
4. [API 规范](#4-api-规范)
5. [数据库规范](#5-数据库规范)
6. [Git 提交规范](#6-git-提交规范)
7. [常见问题](#7-常见问题)

---

## 1. 项目概览

### 1.1 技术栈

**后端**
- Java 11, Spring Boot 2.7.18, Spring Cloud 2021.0.8
- Spring Cloud Alibaba 2021.0.5.0 (Nacos, Gateway, OpenFeign)
- MySQL 8.0.33, MyBatis-Plus 3.5.5, Druid 1.2.20
- Redis 6.x (Redisson 3.24.3), Kafka 3.1.2
- JWT 0.11.5, Lombok 1.18.30, Hutool 5.8.24

**前端**
- Vue 3.5.24, TypeScript 5.9.3, Vite 7.2.4
- Vue Router 4.6.4, Pinia 3.0.4
- Element Plus 2.13.1, Axios 1.13.2
- unplugin-auto-import (自动导入 Vue API)

### 1.2 项目结构

```
vibe-study/
├── backend/                  # 后端微服务
│   ├── common/              # 公共模块（core, web, redis, mq）
│   ├── gateway-service/     # API网关（端口8080）
│   ├── user-service/        # 用户服务（端口8081）
│   └── pom.xml             # Maven父POM
├── frontend-web/            # PC端前端（端口3000）
│   ├── src/
│   │   ├── api/            # API接口层
│   │   ├── components/     # 通用组件
│   │   ├── layouts/        # 布局组件
│   │   ├── router/         # 路由配置
│   │   ├── store/          # Pinia状态管理
│   │   ├── types/          # TypeScript类型
│   │   ├── utils/          # 工具函数
│   │   └── views/          # 页面组件
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
├── docs/                    # 项目文档（技术要求、架构设计等）
├── AGENTS.md               # 本文档
└── AGENTS-TASKS.md         # 任务跟踪清单
```

### 1.3 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Frontend Web | 3000 | 前端开发服务器 |
| Gateway Service | 8080 | API网关 |
| User Service | 8081 | 用户服务 |
| Nacos | 8848 | 服务注册与配置中心 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |

---

## 2. 构建与运行命令

### 2.1 前端命令

```bash
cd frontend-web

# 安装依赖
npm install

# 开发环境运行（热重载，端口3000）
npm run dev

# 生产构建（输出到 dist/）
npm run build

# 预览生产构建
npm run preview
```

**注意**: 前端项目当前**未配置测试框架**，建议后续添加 Vitest。

### 2.2 后端命令

```bash
cd backend

# 编译所有模块（父POM + 所有子模块）
mvn clean install

# 编译单个模块
cd user-service
mvn clean install

# 运行用户服务（开发环境）
cd backend/user-service
mvn spring-boot:run

# 运行网关服务
cd backend/gateway-service
mvn spring-boot:run

# 打包为 JAR（生产环境）
mvn clean package -DskipTests

# 运行 JAR
java -jar target/user-service-1.0.0.jar
```

**注意**: 后端项目当前**未配置单元测试**，建议后续添加 JUnit 5 + Mockito。

### 2.3 运行单个测试（待实现）

前后端均未配置测试框架，以下为建议配置后的命令：

```bash
# 前端（建议使用 Vitest）
npm run test                          # 运行所有测试
npm run test -- UserService.spec.ts  # 运行单个测试文件

# 后端（建议使用 JUnit 5）
mvn test                                    # 运行所有测试
mvn test -Dtest=UserServiceTest             # 运行单个测试类
mvn test -Dtest=UserServiceTest#testLogin  # 运行单个测试方法
```

---

## 3. 代码风格指南

### 3.1 后端代码规范（Java）

#### 导入顺序
```java
// 1. Java标准库
import java.io.Serializable;
import java.time.LocalDateTime;

// 2. 第三方库（Spring, MyBatis, Lombok等）
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

// 3. 项目内部包（按字母顺序）
import com.vibe.common.core.domain.Result;
import com.vibe.user.dto.LoginRequest;
```

#### 命名规范
- **类名**: PascalCase（`UserService`, `LoginRequest`）
- **方法名**: camelCase（`getUserInfo`, `sendEmailCode`）
- **常量**: UPPER_SNAKE_CASE（`MAX_RETRY_COUNT`）
- **包名**: 全小写（`com.vibe.user.controller`）
- **实体类**: 使用 `@Data` + `@TableName`（MyBatis-Plus）
- **DTO/VO**: 使用后缀 `Request`, `Response`, `VO`（`LoginRequest`, `UserInfoVO`）

#### 注解规范
```java
// Controller 类
@Slf4j                          // 日志（必须）
@RestController                 // REST控制器
@RequestMapping("/api/user")    // 路径前缀

// Service 类
@Slf4j                          // 日志
@Service                        // 服务层

// Entity 类
@Data                           // Lombok getter/setter
@TableName("user")              // MyBatis-Plus表名
public class User implements Serializable {
    @TableId                    // 主键
    private Long id;
    
    @TableField("created_at")   // 字段映射
    private LocalDateTime createdAt;
}
```

#### 日志规范
```java
// 使用 @Slf4j 注解
@Slf4j
public class UserController {
    // 正常流程
    log.info("用户登录: account={}", request.getAccount());
    
    // 错误日志
    log.error("登录失败: {}", e.getMessage(), e);
    
    // 调试日志（开发环境）
    log.debug("查询参数: {}", params);
}
```

#### 异常处理
```java
// 业务异常 - 抛出 BusinessException
if (user == null) {
    throw new BusinessException("用户不存在");
}

// 全局异常处理器会自动捕获并返回统一格式
// GlobalExceptionHandler 位于 common-web 模块
```

#### 响应格式
```java
// 所有 Controller 方法返回 Result<T>
@PostMapping("/login")
public Result<LoginResponse> login(@RequestBody LoginRequest request) {
    LoginResponse response = userService.login(request);
    return Result.success(response);        // 成功
}

// 其他响应方式
Result.success()                            // 无数据成功
Result.success(data)                        // 带数据成功
Result.fail("错误信息")                      // 业务失败
Result.error("系统错误")                     // 系统错误
Result.unauthorized("未授权")                // 401
```

#### 类型安全
```java
// 必须使用泛型，避免原始类型
Result<LoginResponse>           // ✅ 正确
Result                          // ❌ 错误

// 集合类型
List<User> users                // ✅
List users                      // ❌
```

### 3.2 前端代码规范（Vue 3 + TypeScript）

#### 文件命名
- **组件**: PascalCase（`UserProfile.vue`, `BasicLayout.vue`）
- **工具函数**: camelCase（`request.ts`, `formatDate.ts`）
- **类型定义**: camelCase（`user.ts`, `api.ts`）
- **路由**: kebab-case（`/user/profile`, `/auth/login`）

#### 组件结构（Composition API）
```vue
<script setup lang="ts">
// 1. 导入（自动导入的无需手动写）
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserInfo } from '@/api/user'
import type { UserInfoVO } from '@/types/user'

// 2. 响应式状态
const router = useRouter()
const userStore = useUserStore()  // Pinia store（自动导入）
const userInfo = ref<UserInfoVO | null>(null)
const loading = ref(false)

// 3. 计算属性
const isLogin = computed(() => !!userStore.accessToken)

// 4. 方法
const fetchUserInfo = async () => {
  loading.value = true
  try {
    const res = await getUserInfo(userId)
    userInfo.value = res.data
  } catch (error) {
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

// 5. 生命周期
onMounted(() => {
  fetchUserInfo()
})
</script>

<template>
  <!-- 模板 -->
</template>

<style scoped lang="scss">
/* 样式 */
</style>
```

#### TypeScript 类型定义
```typescript
// 定义接口（导出供其他模块使用）
export interface UserInfoVO {
  id: number
  username: string
  nickname: string
  avatar: string
  followingCount: number
  followerCount: number
  isFollowing: boolean
}

// API 响应类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 使用类型
const userInfo = ref<UserInfoVO | null>(null)
const handleLogin = async (data: LoginRequest): Promise<LoginResponse> => {
  // ...
}
```

#### API 调用
```typescript
// 使用封装的 request 工具（src/utils/request.ts）
import request from '@/utils/request'

// GET 请求
export const getUserInfo = (id: number) => {
  return request.get<ApiResponse<UserInfoVO>>(`/user/info/${id}`)
}

// POST 请求
export const login = (data: LoginRequest) => {
  return request.post<ApiResponse<LoginResponse>>('/user/auth/login', data)
}

// PUT 请求
export const updateUser = (data: UpdateUserRequest) => {
  return request.put<ApiResponse<UserInfoVO>>('/user/info', data)
}

// DELETE 请求
export const deletePost = (id: number) => {
  return request.delete<ApiResponse<void>>(`/post/${id}`)
}
```

#### 错误处理
```typescript
// 方式1: try-catch（推荐）
const handleLogin = async () => {
  try {
    const res = await login(loginForm.value)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    // request 拦截器会自动显示错误消息
    console.error('登录失败:', error)
  }
}

// 方式2: Promise.catch
login(loginForm.value)
  .then(res => {
    ElMessage.success('登录成功')
  })
  .catch(error => {
    console.error('登录失败:', error)
  })
```

#### 路径别名
```typescript
// 使用 @ 别名指向 src 目录
import UserProfile from '@/components/UserProfile.vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
```

---

## 4. API 规范

### 4.1 RESTful API 设计

**路径格式**: `/api/{模块}/{操作}`

```
GET    /api/user/info/{id}           # 获取用户信息
POST   /api/user/auth/login          # 用户登录
POST   /api/user/auth/register       # 用户注册
PUT    /api/user/info                # 更新用户信息
DELETE /api/user/follow/{id}         # 取消关注
```

### 4.2 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "john"
  },
  "timestamp": 1705654321000
}
```

**状态码**:
- `200`: 成功
- `400`: 请求参数错误
- `401`: 未授权（Token失效）
- `403`: 无权限
- `500`: 服务器内部错误

### 4.3 认证授权

**JWT Token 格式**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**前端自动添加**（在 `request.ts` 拦截器中）:
```typescript
config.headers.Authorization = `Bearer ${userStore.accessToken}`
```