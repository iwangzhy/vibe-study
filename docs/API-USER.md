# 用户模块 API 接口文档

> **版本**: v1.0.0  
> **基础路径**: `/api/user`  
> **更新时间**: 2026-01-19

---

## 目录

1. [认证接口](#1-认证接口)
   - [1.1 用户登录](#11-用户登录)
   - [1.2 用户注册](#12-用户注册)
   - [1.3 发送邮箱验证码](#13-发送邮箱验证码)
   - [1.4 修改密码](#14-修改密码)
2. [用户信息接口](#2-用户信息接口)
   - [2.1 获取用户信息](#21-获取用户信息)
   - [2.2 获取当前登录用户信息](#22-获取当前登录用户信息)
   - [2.3 更新用户信息](#23-更新用户信息)
3. [用户关系接口](#3-用户关系接口)
   - [3.1 关注用户](#31-关注用户)
   - [3.2 取消关注用户](#32-取消关注用户)
4. [通用响应格式](#4-通用响应格式)
5. [错误码说明](#5-错误码说明)

---

## 1. 认证接口

### 1.1 用户登录

**接口**: `POST /api/user/auth/login`

**描述**: 用户通过用户名/邮箱/手机号和密码登录系统

**请求头**: 无需认证

**请求体**:
```json
{
  "account": "testuser1",
  "password": "123456"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| account | String | 是 | 用户名/邮箱/手机号 |
| password | String | 是 | 密码 |

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userInfo": {
      "id": 20260119123456000001,
      "username": "testuser1",
      "nickname": "测试用户1",
      "avatar": "https://api.dicebear.com/7.x/avataaars/svg?seed=1",
      "email": "testuser1@vibe.com",
      "phone": "13800138001",
      "gender": 1,
      "birthday": "1995-05-15",
      "bio": "热爱编程和技术分享",
      "status": 1,
      "followingCount": 10,
      "followerCount": 5,
      "isFollowing": false,
      "createdAt": "2025-01-01T10:00:00",
      "updatedAt": "2025-01-10T15:30:00"
    }
  },
  "timestamp": 1705654321000
}
```

**错误响应**:
- `401 Unauthorized`: 用户名或密码错误
- `403 Forbidden`: 账号已被禁用

---

### 1.2 用户注册

**接口**: `POST /api/user/auth/register`

**描述**: 用户注册新账号

**请求头**: 无需认证

**请求体**:
```json
{
  "username": "newuser",
  "password": "pass123",
  "nickname": "新用户",
  "email": "newuser@example.com",
  "code": "123456"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名（3-20位，字母数字下划线） |
| password | String | 是 | 密码（6-20位，包含字母和数字） |
| nickname | String | 是 | 昵称（1-50位） |
| email | String | 是 | 邮箱 |
| code | String | 是 | 邮箱验证码（6位数字） |

**响应示例**: 同登录接口

**错误响应**:
- `400 Bad Request`: 验证码错误/已过期、用户名已存在、邮箱已被注册

---

### 1.3 发送邮箱验证码

**接口**: `POST /api/user/auth/send-code`

**描述**: 发送邮箱验证码（用于注册）

**请求头**: 无需认证

**请求体**:
```json
{
  "email": "user@example.com"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| email | String | 是 | 邮箱地址 |

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1705654321000
}
```

**注意事项**:
- 验证码有效期：5分钟
- 同一邮箱 60 秒内只能发送一次
- 开发环境下验证码会打印在控制台

---

### 1.4 修改密码

**接口**: `PUT /api/user/auth/change-password`

**描述**: 修改当前登录用户的密码

**请求头**: 
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "oldPassword": "oldPass123",
  "newPassword": "newPass123",
  "confirmPassword": "newPass123"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| oldPassword | String | 是 | 旧密码 |
| newPassword | String | 是 | 新密码（6-20位，包含字母和数字） |
| confirmPassword | String | 是 | 确认新密码 |

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1705654321000
}
```

**错误响应**:
- `400 Bad Request`: 旧密码错误、两次输入的新密码不一致、新密码与旧密码相同
- `401 Unauthorized`: 未登录或 Token 失效

---

## 2. 用户信息接口

### 2.1 获取用户信息

**接口**: `GET /api/user/info/{id}`

**描述**: 获取指定用户的公开信息

**请求头**: 可选（传入 Token 可判断是否关注）
```
Authorization: Bearer {token}  # 可选
```

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 20260119123456000001,
    "username": "testuser1",
    "nickname": "测试用户1",
    "avatar": "https://api.dicebear.com/7.x/avataaars/svg?seed=1",
    "email": "testuser1@vibe.com",
    "phone": null,
    "gender": 1,
    "birthday": "1995-05-15",
    "bio": "热爱编程和技术分享",
    "status": 1,
    "followingCount": 10,
    "followerCount": 5,
    "isFollowing": true,
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-10T15:30:00"
  },
  "timestamp": 1705654321000
}
```

**错误响应**:
- `404 Not Found`: 用户不存在

---

### 2.2 获取当前登录用户信息

**接口**: `GET /api/user/info/current`

**描述**: 获取当前登录用户的完整信息

**请求头**: 
```
Authorization: Bearer {token}
```

**响应示例**: 同获取用户信息接口

**错误响应**:
- `401 Unauthorized`: 未登录或 Token 失效

---

### 2.3 更新用户信息

**接口**: `PUT /api/user/info`

**描述**: 更新当前登录用户的信息

**请求头**: 
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "nickname": "新昵称",
  "avatar": "https://example.com/avatar.jpg",
  "gender": 1,
  "birthday": "1995-05-15",
  "bio": "新的个人简介"
}
```

**请求参数** (所有字段均为可选):
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| nickname | String | 否 | 昵称（1-50位） |
| avatar | String | 否 | 头像URL |
| gender | Integer | 否 | 性别（0-未知，1-男，2-女） |
| birthday | String | 否 | 生日（格式：yyyy-MM-dd） |
| bio | String | 否 | 个人简介（最多500字） |

**响应示例**: 返回更新后的用户信息（同获取用户信息接口）

**错误响应**:
- `400 Bad Request`: 参数校验失败
- `401 Unauthorized`: 未登录或 Token 失效

---

## 3. 用户关系接口

### 3.1 关注用户

**接口**: `POST /api/user/follow/{id}`

**描述**: 关注指定用户

**请求头**: 
```
Authorization: Bearer {token}
```

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 要关注的用户ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1705654321000
}
```

**错误响应**:
- `400 Bad Request`: 不能关注自己、已关注该用户
- `401 Unauthorized`: 未登录或 Token 失效
- `404 Not Found`: 用户不存在

---

### 3.2 取消关注用户

**接口**: `DELETE /api/user/follow/{id}`

**描述**: 取消关注指定用户

**请求头**: 
```
Authorization: Bearer {token}
```

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 要取消关注的用户ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1705654321000
}
```

**错误响应**:
- `400 Bad Request`: 未关注该用户
- `401 Unauthorized`: 未登录或 Token 失效

---

## 4. 通用响应格式

所有接口均返回统一格式的 JSON 响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1705654321000
}
```

**字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码（200-成功，其他-失败） |
| message | String | 响应消息 |
| data | Object | 响应数据（可为 null） |
| timestamp | Long | 时间戳（毫秒） |

---

## 5. 错误码说明

| 状态码 | 说明 | 常见场景 |
|--------|------|----------|
| 200 | 成功 | 操作成功 |
| 400 | 请求参数错误 | 参数校验失败、业务逻辑错误 |
| 401 | 未授权 | 未登录、Token 失效 |
| 403 | 无权限 | 账号被禁用、权限不足 |
| 404 | 资源不存在 | 用户不存在、资源未找到 |
| 500 | 服务器内部错误 | 系统异常 |

---

## 6. 认证说明

### JWT Token 使用

**请求头格式**:
```
Authorization: Bearer {token}
```

**Token 获取**:
- 通过登录或注册接口获取
- Token 有效期：7天（可配置）
- Token 过期后需要重新登录

**Token 携带**:
- 需要认证的接口必须在请求头中携带 Token
- 前端应在 axios 拦截器中自动添加 Token

---

## 7. 测试账号

开发环境提供以下测试账号：

| 用户名 | 密码 | 说明 |
|--------|------|------|
| testuser1 | 123456 | 普通用户 |
| testuser2 | 123456 | 普通用户 |
| testuser3 | 123456 | 普通用户 |

---

## 8. 注意事项

1. **密码安全**: 所有密码使用 BCrypt 加密存储
2. **验证码**: 开发环境验证码会打印在控制台，生产环境需接入邮件服务
3. **缓存策略**: 用户信息会缓存到 Redis，更新后自动失效
4. **并发控制**: 关注/取消关注使用数据库唯一索引防止重复
5. **软删除**: 用户删除使用逻辑删除（deleted 字段）
6. **关注统计**: 关注数和粉丝数实时从数据库统计

---

## 9. 变更日志

### v1.0.0 (2026-01-19)
- ✅ 用户登录接口
- ✅ 用户注册接口
- ✅ 发送邮箱验证码接口
- ✅ 获取用户信息接口
- ✅ 更新用户信息接口
- ✅ 关注/取消关注接口
- ✅ 获取当前用户信息接口
- ✅ 修改密码接口
