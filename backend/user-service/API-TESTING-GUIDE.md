# User Service API Testing Guide

## Overview
User Service API接口测试指南，包含所有用户服务端点的测试方法。

**Base URL**: `http://localhost:8080/api/user` (通过网关)  
**Direct URL**: `http://localhost:8081/api/user` (直连用户服务)

---

## Authentication Endpoints

### 1. 发送邮箱验证码

**POST** `/auth/send-code`

发送6位数字验证码到指定邮箱（有效期5分钟）。

**Request Body**:
```json
{
  "email": "test@example.com"
}
```

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**验证码会打印在控制台**，实际项目中应该通过邮件发送。

**cURL Example**:
```bash
curl -X POST http://localhost:8080/api/user/auth/send-code \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'
```

---

### 2. 用户注册

**POST** `/auth/register`

使用邮箱验证码注册新用户。

**Request Body**:
```json
{
  "username": "testuser",
  "password": "123456",
  "nickname": "测试用户",
  "email": "test@example.com",
  "code": "123456"
}
```

**Validation Rules**:
- `username`: 3-20个字符，只能包含字母、数字和下划线
- `password`: 6-20个字符
- `nickname`: 1-20个字符
- `email`: 有效的邮箱格式
- `code`: 必填（从邮箱验证码获取）

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "userInfo": {
      "id": 1234567890,
      "username": "testuser",
      "nickname": "测试用户",
      "avatar": "https://api.dicebear.com/7.x/avataaars/svg?seed=1234567890",
      "email": "test@example.com",
      "phone": null,
      "gender": 0,
      "birthday": null,
      "bio": null,
      "status": 1,
      "followingCount": 0,
      "followerCount": 0,
      "isFollowing": false,
      "createdAt": "2026-01-19 10:30:00"
    }
  }
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:8080/api/user/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456",
    "nickname": "测试用户",
    "email": "test@example.com",
    "code": "123456"
  }'
```

---

### 3. 用户登录

**POST** `/auth/login`

使用用户名/邮箱/手机号登录。

**Request Body**:
```json
{
  "account": "testuser",
  "password": "123456"
}
```

**account字段支持**:
- 用户名
- 邮箱
- 手机号

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "userInfo": {
      "id": 1234567890,
      "username": "testuser",
      "nickname": "测试用户",
      "avatar": "https://api.dicebear.com/7.x/avataaars/svg?seed=1234567890",
      "email": "test@example.com",
      "followingCount": 0,
      "followerCount": 0,
      "createdAt": "2026-01-19 10:30:00"
    }
  }
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:8080/api/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "account": "testuser",
    "password": "123456"
  }'
```

---

## User Information Endpoints

### 4. 获取当前登录用户信息

**GET** `/info/current`

获取当前登录用户的详细信息。

**Headers**:
```
Authorization: Bearer <token>
```

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1234567890,
    "username": "testuser",
    "nickname": "测试用户",
    "avatar": "https://api.dicebear.com/7.x/avataaars/svg?seed=1234567890",
    "email": "test@example.com",
    "phone": null,
    "gender": 0,
    "birthday": null,
    "bio": null,
    "status": 1,
    "followingCount": 0,
    "followerCount": 0,
    "isFollowing": false,
    "createdAt": "2026-01-19 10:30:00"
  }
}
```

**cURL Example**:
```bash
curl -X GET http://localhost:8080/api/user/info/current \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

---

### 5. 获取指定用户信息

**GET** `/info/{id}`

获取指定用户的公开信息。

**Path Parameters**:
- `id`: 用户ID

**Headers** (可选):
```
Authorization: Bearer <token>
```

如果提供了token，会返回当前用户是否关注了该用户。

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 9876543210,
    "username": "otheruser",
    "nickname": "其他用户",
    "avatar": "https://api.dicebear.com/7.x/avataaars/svg?seed=9876543210",
    "email": "other@example.com",
    "gender": 1,
    "birthday": "1995-06-15",
    "bio": "这是我的个人简介",
    "status": 1,
    "followingCount": 50,
    "followerCount": 100,
    "isFollowing": false,
    "createdAt": "2026-01-10 08:00:00"
  }
}
```

**cURL Example**:
```bash
# 未登录查询
curl -X GET http://localhost:8080/api/user/info/9876543210

# 已登录查询
curl -X GET http://localhost:8080/api/user/info/9876543210 \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

---

### 6. 更新用户信息

**PUT** `/info`

更新当前登录用户的个人信息。

**Headers**:
```
Authorization: Bearer <token>
```

**Request Body** (所有字段都是可选的):
```json
{
  "nickname": "新昵称",
  "avatar": "https://example.com/avatar.jpg",
  "gender": 1,
  "birthday": "1995-06-15",
  "bio": "这是我的新简介"
}
```

**Field Descriptions**:
- `nickname`: 1-20个字符
- `avatar`: 头像URL
- `gender`: 0-未知，1-男，2-女
- `birthday`: 格式 yyyy-MM-dd
- `bio`: 个人简介，最多200个字符

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1234567890,
    "username": "testuser",
    "nickname": "新昵称",
    "avatar": "https://example.com/avatar.jpg",
    "gender": 1,
    "birthday": "1995-06-15",
    "bio": "这是我的新简介",
    "followingCount": 0,
    "followerCount": 0,
    "createdAt": "2026-01-19 10:30:00"
  }
}
```

**cURL Example**:
```bash
curl -X PUT http://localhost:8080/api/user/info \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "新昵称",
    "bio": "这是我的新简介",
    "gender": 1
  }'
```

---

## Follow/Unfollow Endpoints

### 7. 关注用户

**POST** `/follow/{id}`

关注指定用户。

**Headers**:
```
Authorization: Bearer <token>
```

**Path Parameters**:
- `id`: 要关注的用户ID

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**Business Rules**:
- 不能关注自己
- 不能重复关注
- 被关注的用户必须存在

**cURL Example**:
```bash
curl -X POST http://localhost:8080/api/user/follow/9876543210 \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

---

### 8. 取消关注用户

**DELETE** `/follow/{id}`

取消关注指定用户。

**Headers**:
```
Authorization: Bearer <token>
```

**Path Parameters**:
- `id`: 要取消关注的用户ID

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**Business Rules**:
- 必须已经关注该用户才能取消

**cURL Example**:
```bash
curl -X DELETE http://localhost:8080/api/user/follow/9876543210 \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

---

## Error Response Format

所有API在发生错误时都会返回统一格式：

```json
{
  "code": 400,
  "message": "错误描述",
  "data": null
}
```

**Common Error Codes**:
- `400`: 请求参数错误
- `401`: 未授权（Token无效或已过期）
- `403`: 禁止访问（账号被禁用）
- `404`: 资源不存在
- `500`: 服务器内部错误

**Example Error Response**:
```json
{
  "code": 400,
  "message": "用户名已存在",
  "data": null
}
```

---

## Complete Test Flow

### 完整测试流程示例

```bash
# 1. 发送验证码
curl -X POST http://localhost:8080/api/user/auth/send-code \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com"}'

# 控制台会打印验证码，假设是 "123456"

# 2. 注册用户 Alice
curl -X POST http://localhost:8080/api/user/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "123456",
    "nickname": "Alice",
    "email": "alice@example.com",
    "code": "123456"
  }'

# 保存返回的 token，例如: TOKEN_ALICE="eyJ..."

# 3. 注册另一个用户 Bob（重复步骤1-2）
# TOKEN_BOB="eyJ..."

# 4. Alice 查看自己的信息
curl -X GET http://localhost:8080/api/user/info/current \
  -H "Authorization: Bearer $TOKEN_ALICE"

# 5. Alice 关注 Bob
curl -X POST http://localhost:8080/api/user/follow/{bob_id} \
  -H "Authorization: Bearer $TOKEN_ALICE"

# 6. Alice 查看 Bob 的信息（会显示 isFollowing: true）
curl -X GET http://localhost:8080/api/user/info/{bob_id} \
  -H "Authorization: Bearer $TOKEN_ALICE"

# 7. Alice 更新个人资料
curl -X PUT http://localhost:8080/api/user/info \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "Alice Wang",
    "bio": "Hello, this is Alice!",
    "gender": 2
  }'

# 8. Alice 取消关注 Bob
curl -X DELETE http://localhost:8080/api/user/follow/{bob_id} \
  -H "Authorization: Bearer $TOKEN_ALICE"

# 9. Alice 登出后重新登录
curl -X POST http://localhost:8080/api/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "account": "alice@example.com",
    "password": "123456"
  }'
```

---

## Testing with Postman

### Postman Collection Setup

1. **创建Environment**:
   - `base_url`: `http://localhost:8080`
   - `token`: (登录后设置)
   - `user_id`: (登录后设置)

2. **设置Authorization**:
   - Type: `Bearer Token`
   - Token: `{{token}}`

3. **自动设置Token脚本**（在登录/注册请求的Tests标签添加）:
```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("token", jsonData.data.token);
    pm.environment.set("user_id", jsonData.data.userInfo.id);
}
```

---

## Important Notes

### 1. Token Management
- Token有效期为7天
- Token格式: `Bearer <jwt_token>`
- 需要在请求头中携带: `Authorization: Bearer <token>`

### 2. Redis Cache
- 用户信息缓存1小时
- 关注/粉丝关系实时同步到Redis
- 邮箱验证码缓存5分钟

### 3. Data Validation
- 所有请求参数都经过javax.validation验证
- 错误信息会在响应中返回

### 4. Database Requirements
- 需要MySQL数据库运行
- 执行`backend/user-service/src/main/resources/db/schema.sql`创建表

### 5. Dependencies
- **Nacos**: 配置中心和注册中心
- **Redis**: 缓存服务
- **MySQL**: 数据库服务

---

## Quick Start Commands

```bash
# 1. 启动Nacos (默认端口 8848)
# Windows: startup.cmd -m standalone
# Linux/Mac: sh startup.sh -m standalone

# 2. 启动Redis (默认端口 6379)
redis-server

# 3. 启动MySQL (默认端口 3306)
# 创建数据库: vibe_user

# 4. 启动User Service
cd backend/user-service
mvn spring-boot:run

# 5. 启动Gateway Service
cd backend/gateway-service
mvn spring-boot:run

# 6. 测试API
curl -X POST http://localhost:8080/api/user/auth/send-code \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'
```

---

## Troubleshooting

### 常见问题

1. **Token过期**: 重新登录获取新token
2. **验证码过期**: 5分钟内必须使用，过期需重新发送
3. **用户名已存在**: 尝试不同的用户名
4. **邮箱已被注册**: 使用其他邮箱或找回密码
5. **Redis连接失败**: 检查Redis服务是否启动
6. **数据库连接失败**: 检查MySQL服务和数据库配置

### Debug Tips

- 查看用户服务日志: 所有操作都有日志记录
- 检查Redis缓存: 使用`redis-cli`查看key
- 查看数据库数据: 直接查询`user`和`user_relation`表
