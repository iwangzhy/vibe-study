# Vibe Social Media - 项目初始化完成

## 项目概述

基于Spring Cloud Alibaba微服务架构和Vue3前端框架的社交媒体平台项目已完成初始化。

## 后端项目结构

```
backend/
├── common/                          # 公共模块
│   ├── common-core/                # 核心工具类
│   │   ├── domain/                 # 统一响应、状态码
│   │   ├── exception/              # 业务异常类
│   │   └── utils/                  # 工具类（雪花算法、JSON工具）
│   ├── common-web/                 # Web公共模块
│   │   ├── config/                 # 跨域配置
│   │   └── handler/                # 全局异常处理器
│   ├── common-redis/               # Redis模块
│   └── common-mq/                  # Kafka消息队列模块
├── gateway-service/                # API网关服务
│   ├── filter/                     # 过滤器（JWT鉴权、日志）
│   └── config/                     # 网关配置
├── user-service/                   # 用户服务
│   ├── controller/                 # 控制器
│   ├── service/                    # 业务逻辑
│   ├── mapper/                     # 数据访问层
│   ├── entity/                     # 实体类
│   └── dto/                        # 数据传输对象
└── pom.xml                         # 父POM
```

### 技术栈

- **Spring Boot**: 2.7.18
- **Spring Cloud**: 2021.0.8
- **Spring Cloud Alibaba**: 2021.0.5.0
- **Nacos**: 服务注册与配置中心
- **Spring Cloud Gateway**: API网关
- **MySQL**: 8.0.33
- **MyBatis Plus**: 3.5.5
- **Redis**: 缓存和分布式锁
- **Kafka**: 消息队列
- **JWT**: Token认证

### 核心功能

- 统一响应格式封装
- 全局异常处理
- 雪花算法ID生成
- JWT Token认证
- 跨域配置
- Nacos服务注册与配置管理

## 前端项目结构

```
frontend-web/
├── src/
│   ├── api/                        # API接口定义
│   │   └── user.ts                 # 用户相关API
│   ├── assets/                     # 静态资源
│   │   ├── images/                 # 图片
│   │   └── styles/                 # 样式
│   ├── components/                 # 通用组件
│   ├── layouts/                    # 布局组件
│   │   └── BasicLayout.vue         # 基础布局
│   ├── router/                     # 路由配置
│   │   └── index.ts                # 路由定义
│   ├── store/                      # 状态管理
│   │   ├── user.ts                 # 用户Store
│   │   └── app.ts                  # 应用Store
│   ├── utils/                      # 工具类
│   │   └── request.ts              # Axios封装
│   ├── views/                      # 页面组件
│   │   ├── auth/                   # 认证页面
│   │   │   ├── Login.vue           # 登录页
│   │   │   └── Register.vue        # 注册页
│   │   ├── user/                   # 用户页面
│   │   │   ├── Profile.vue         # 个人主页
│   │   │   └── Settings.vue        # 设置页
│   │   ├── error/                  # 错误页面
│   │   │   └── NotFound.vue        # 404页面
│   │   └── Home.vue                # 首页
│   ├── App.vue                     # 根组件
│   └── main.ts                     # 入口文件
├── .env.development                # 开发环境变量
├── .env.production                 # 生产环境变量
├── vite.config.ts                  # Vite配置
└── package.json                    # 依赖配置
```

### 技术栈

- **Vue 3**: 渐进式JavaScript框架
- **TypeScript**: 类型化的JavaScript
- **Vite**: 下一代前端构建工具
- **Vue Router 4**: 路由管理
- **Pinia**: 状态管理
- **Element Plus**: UI组件库
- **Axios**: HTTP客户端

### 核心功能

- Vue Router路由守卫（登录验证）
- Pinia状态持久化
- Axios请求/响应拦截器
- 统一错误处理
- Token自动刷新机制
- Element Plus按需引入

## 快速开始

### 前置要求

- JDK 11+
- Node.js 16+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Nacos 2.x

### 后端启动

1. 启动Nacos服务器
   ```bash
   # Windows
   cd nacos/bin
   startup.cmd -m standalone

   # Linux/Mac
   sh startup.sh -m standalone
   ```

2. 创建数据库
   ```bash
   # 执行SQL脚本
   mysql -u root -p < backend/user-service/src/main/resources/db/schema.sql
   ```

3. 配置Nacos
   - 访问 http://localhost:8848/nacos
   - 登录（nacos/nacos）
   - 创建命名空间：dev
   - 创建配置文件（参考 backend/nacos-config.md）

4. 启动服务
   ```bash
   cd backend

   # 启动网关服务
   cd gateway-service
   mvn spring-boot:run

   # 启动用户服务
   cd user-service
   mvn spring-boot:run
   ```

### 前端启动

```bash
cd frontend-web

# 安装依赖（如果未安装）
npm install

# 启动开发服务器
npm run dev

# 访问 http://localhost:3000
```

## 环境配置

### 后端配置

- Gateway服务端口: 8080
- User服务端口: 8081
- Nacos地址: localhost:8848
- MySQL地址: localhost:3306
- Redis地址: localhost:6379

### 前端配置

- 开发服务器端口: 3000
- API代理地址: http://localhost:8080

## 后续开发计划

根据学习计划month1.md，接下来需要完成：

1. **用户服务后端开发**（第3周）
   - 用户注册与登录接口
   - JWT Token生成与验证
   - 用户信息管理
   - 关注与粉丝功能

2. **用户模块前端开发**（第4周）
   - 登录注册页面完善
   - 个人主页开发
   - 关注/粉丝列表
   - 用户信息编辑

## 注意事项

1. 确保所有服务（Nacos、MySQL、Redis）已启动
2. 首次启动需要在Nacos中配置相关配置文件
3. 数据库需要先创建并执行schema.sql
4. 开发时注意端口占用情况
5. JWT密钥在生产环境需要更换为复杂密钥

## 文档参考

- [Nacos配置说明](backend/nacos-config.md)
- [学习计划 - 第一个月](docs/learning-plan-month1.md)
