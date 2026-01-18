# 第一个月学习计划 - 项目初始化与用户模块

> 时间：第1-4周  
> 目标：完成项目基础框架搭建和用户模块的前后端开发

---

## 第一周：后端工程初始化（第1周）

### 1.1 创建Maven多模块项目

**任务描述：**
搭建Spring Boot + Spring Cloud Alibaba微服务项目的基础架构，创建父工程和公共模块。

**实现步骤：**
1. 创建父工程（social-media-microservice）
2. 配置父工程pom.xml
3. 创建common公共模块
4. 创建gateway-service网关服务模块

**技术要点：**

#### 父工程pom.xml配置
- 使用`<dependencyManagement>`统一管理依赖版本
- Spring Boot版本：2.7.x
- Spring Cloud版本：2021.x
- Spring Cloud Alibaba版本：2021.x
- 注意版本对齐，避免依赖冲突
- 配置Maven编译插件（Java 8或11）

#### Common公共模块划分
- **common-core**：核心工具类模块
  - 统一响应格式（Result类：code、message、data）
  - 业务异常类（BusinessException）
  - 常量定义（StatusCode、ErrorCode）
  - 工具类（DateUtils、StringUtils、JsonUtils等）
  - 雪花算法ID生成器（SnowflakeIdGenerator）
  
- **common-web**：Web相关模块
  - 全局异常处理器（GlobalExceptionHandler）
  - 统一返回值封装（ResponseBodyAdvice）
  - 请求日志拦截器（LogInterceptor）
  - 跨域配置（CorsConfig）
  - Swagger配置（接口文档）
  
- **common-redis**：Redis工具模块
  - RedisTemplate配置
  - Redis工具类（RedisUtils）
  - 分布式锁实现（RedisLock）
  - 缓存注解封装
  
- **common-mq**：消息队列模块
  - Kafka配置
  - 消息发送工具类
  - 消息序列化配置

**注意事项：**
- 使用`<packaging>pom</packaging>`定义父工程
- 每个模块单独管理自己的依赖
- 公共模块不要引入具体业务逻辑
- 配置统一的日志框架Logback
- 定义统一的异常码规范（如：1000-1999用户模块，2000-2999动态模块）

---

### 1.2 Nacos注册中心与配置中心

**任务描述：**
配置Nacos作为微服务的注册中心和配置中心，实现服务注册发现和动态配置管理。

**实现步骤：**
1. 添加Nacos依赖
2. 配置bootstrap.yml
3. 配置Nacos命名空间和分组
4. 配置动态配置文件

**技术要点：**

#### Nacos服务注册
- 依赖：`spring-cloud-starter-alibaba-nacos-discovery`
- 配置服务名称（spring.application.name）
- 配置Nacos服务器地址
- 配置心跳间隔和健康检查

#### Nacos配置中心
- 依赖：`spring-cloud-starter-alibaba-nacos-config`
- 配置Data ID命名规则：`${spring.application.name}-${profile}.${file-extension}`
- 支持配置热更新（@RefreshScope注解）
- 配置优先级：bootstrap.yml > Nacos配置 > application.yml

#### 环境隔离
- 使用命名空间（Namespace）隔离环境：dev、test、prod
- 使用分组（Group）管理不同服务：DEFAULT_GROUP、USER_GROUP等
- 每个环境使用独立的命名空间ID

#### 敏感信息加密
- 数据库密码、Redis密码、秘钥等敏感信息加密存储
- 使用Jasypt加密工具
- 配置加密密钥（jasypt.encryptor.password）

**注意事项：**
- Nacos服务端需要单独部署（默认已安装）
- 配置文件支持YAML、Properties、JSON等格式
- 生产环境建议Nacos集群部署（3节点以上）
- 配置版本管理和回滚机制
- 定期备份Nacos配置数据

---

### 1.3 Spring Cloud Gateway网关服务

**任务描述：**
搭建API网关服务，实现统一的路由转发、鉴权、限流、日志记录等功能。

**实现步骤：**
1. 创建gateway-service模块
2. 配置路由规则
3. 实现JWT Token鉴权过滤器
4. 集成Sentinel限流
5. 配置全局过滤器

**技术要点：**

#### 路由配置
- 基于服务名的动态路由（lb://user-service）
- 路径重写（RewritePath）
- 断言工厂（Predicate）：路径匹配、请求方法匹配等
- 过滤器工厂（Filter）：添加请求头、移除响应头等

#### JWT Token鉴权
- 创建AuthGlobalFilter实现GlobalFilter接口
- 白名单配置（登录、注册、公开接口不需要鉴权）
- 从请求头获取Token（Authorization: Bearer {token}）
- 验证Token有效性（签名、过期时间）
- 解析Token获取用户信息（userId、username）
- 将用户信息传递给下游服务（通过请求头）

#### 限流配置
- 集成Sentinel实现网关限流
- 配置QPS限流规则
- 自定义限流降级返回
- 针对不同接口配置不同的限流策略
- IP限流、用户限流

#### 全局过滤器
- **日志过滤器（LoggingFilter）**
  - 记录请求路径、方法、参数
  - 生成Request ID用于链路追踪
  - 记录响应时间、响应状态码
  
- **跨域过滤器（CorsFilter）**
  - 配置允许的域名（开发环境：*，生产环境：具体域名）
  - 配置允许的请求方法（GET、POST、PUT、DELETE等）
  - 配置允许的请求头
  - 配置是否允许携带凭证（withCredentials）

#### 统一异常处理
- 捕获网关层异常（路由不存在、服务不可用等）
- 返回统一的错误格式
- 404、500、503等HTTP状态码处理

**注意事项：**
- Gateway基于WebFlux，使用响应式编程模型
- 不能使用@RequestBody、@ResponseBody等Spring MVC注解
- 过滤器执行顺序通过Order控制（数字越小优先级越高）
- Token验证失败返回401状态码
- 限流触发返回429状态码（Too Many Requests）
- 配置超时时间（connect-timeout、response-timeout）
- 开发环境可以开启详细的错误信息，生产环境隐藏敏感信息

---

## 第二周：前端工程初始化（第2周）

### 2.1 Vue3 + TypeScript + Vite项目创建

**任务描述：**
使用Vite快速创建Vue3 + TypeScript前端项目，配置基础开发环境。

**实现步骤：**
1. 使用Vite创建项目脚手架
2. 配置TypeScript
3. 配置路径别名
4. 配置开发环境

**技术要点：**

#### 创建项目
```bash
# 使用Vite创建项目
npm create vite@latest frontend-web -- --template vue-ts

# 安装依赖
npm install
```

#### TypeScript配置（tsconfig.json）
- 启用严格模式（strict: true）
- 配置路径映射（paths: { "@/*": ["./src/*"] }）
- 配置JSX支持（jsx: "preserve"）
- 配置类型声明文件（types: ["vite/client", "element-plus/global"]）

#### Vite配置（vite.config.ts）
- 配置路径别名（resolve.alias）
- 配置开发服务器（server.port、server.proxy）
- 配置构建选项（build.outDir、build.sourcemap）
- 配置环境变量（envDir、envPrefix）

#### 环境变量配置
- `.env.development`：开发环境配置
  - VITE_APP_BASE_API：后端API地址（http://localhost:8080）
  - VITE_APP_UPLOAD_URL：文件上传地址
  
- `.env.production`：生产环境配置
  - VITE_APP_BASE_API：生产环境API地址
  - VITE_APP_UPLOAD_URL：生产环境上传地址

#### 代码规范配置
- **ESLint配置（.eslintrc.js）**
  - 使用Vue3 + TypeScript规则
  - 配置解析器（@typescript-eslint/parser）
  - 配置规则（no-console、no-debugger等）
  
- **Prettier配置（.prettierrc.js）**
  - 单引号（singleQuote: true）
  - 末尾分号（semi: false）
  - 缩进2空格（tabWidth: 2）
  - 行宽限制（printWidth: 100）
  
- **EditorConfig配置（.editorconfig）**
  - 统一缩进风格
  - 统一换行符（LF）
  - 统一编码（UTF-8）

**注意事项：**
- Vite使用ES Module，启动速度比Webpack快
- TypeScript提供类型检查，减少运行时错误
- 开发环境开启sourcemap方便调试
- 生产环境关闭console.log输出
- 配置Husky + lint-staged实现提交前代码检查

---

### 2.2 核心依赖安装与配置

**任务描述：**
安装并配置前端项目的核心依赖，包括路由、状态管理、HTTP客户端、UI组件库等。

**实现步骤：**
1. 安装核心依赖包
2. 配置Vue Router
3. 配置Pinia状态管理
4. 封装Axios
5. 集成Element Plus

**技术要点：**

#### Vue Router 4.x配置
- 创建路由配置文件（src/router/index.ts）
- 配置路由模式（createWebHistory）
- 路由懒加载（() => import('@/views/xxx.vue')）
- 路由守卫（beforeEach）
  - 检查用户登录状态
  - Token验证
  - 白名单路由（登录、注册不需要验证）
  - 未登录跳转到登录页
- 动态路由（根据权限动态添加路由）
- 路由元信息（meta：title、requireAuth等）

#### Pinia状态管理
- 创建Store目录结构（src/store/）
- **用户Store（useUserStore）**
  - state：用户信息（userInfo）、Token（accessToken、refreshToken）、登录状态（isLogin）
  - actions：登录（login）、退出登录（logout）、获取用户信息（getUserInfo）、更新Token（refreshToken）
  - getters：用户ID、用户名、头像等
  - 持久化存储（使用pinia-plugin-persistedstate插件）
  
- **应用Store（useAppStore）**
  - state：加载状态（loading）、侧边栏状态（sidebarOpened）
  - actions：显示/隐藏Loading、切换侧边栏

#### Axios封装
- 创建Axios实例（src/utils/request.ts）
- 配置baseURL（从环境变量读取）
- 配置超时时间（timeout: 10000）
- **请求拦截器**
  - 自动添加Token到请求头（Authorization: Bearer {token}）
  - 添加Request ID（用于追踪）
  - 添加时间戳（防止缓存）
  - 显示Loading状态
  
- **响应拦截器**
  - 隐藏Loading状态
  - 统一处理响应数据（response.data.data）
  - 统一处理错误
    - 401：Token过期，自动刷新Token或跳转登录页
    - 403：无权限，提示用户
    - 404：接口不存在
    - 500：服务器错误
    - 网络错误：提示用户检查网络
  - 错误提示（使用Element Plus的Message组件）

- **请求取消**
  - 使用AbortController取消重复请求
  - 路由切换时取消未完成的请求

#### Element Plus集成
- 按需引入组件（使用unplugin-vue-components插件）
- 配置主题颜色
- 配置全局组件大小（size: 'default'）
- 配置国际化（中文）
- 配置图标（@element-plus/icons-vue）

**注意事项：**
- Pinia是Vuex的替代方案，API更简洁
- Token存储在localStorage，刷新页面不丢失
- 双Token机制：Access Token（短期）+ Refresh Token（长期）
- Access Token过期时使用Refresh Token自动刷新
- Axios拦截器中不要使用this，使用箭头函数
- 错误提示使用防抖，避免重复提示

---

### 2.3 基础布局与通用组件

**任务描述：**
创建应用的基础布局结构和通用组件，为后续页面开发打好基础。

**实现步骤：**
1. 创建Layout布局组件
2. 创建登录注册页面
3. 创建通用组件

**技术要点：**

#### Layout布局组件
- **基础布局（BasicLayout.vue）**
  - Header：导航栏（Logo、搜索框、用户菜单）
  - Sidebar：侧边栏（导航菜单）
  - Main：主内容区域（router-view）
  - Footer：页脚
  
- **空白布局（BlankLayout.vue）**
  - 用于登录、注册等不需要导航的页面
  - 只包含router-view

#### 登录注册页面
- **登录页面（Login.vue）**
  - 手机号/邮箱输入框
  - 密码输入框（显示/隐藏切换）
  - 记住我复选框
  - 登录按钮
  - 注册链接、忘记密码链接
  
- **注册页面（Register.vue）**
  - 手机号输入框
  - 验证码输入框 + 发送按钮（倒计时60秒）
  - 密码输入框
  - 确认密码输入框
  - 注册按钮
  - 已有账号登录链接

#### 通用组件
- **Loading组件（Loading.vue）**
  - 全屏Loading遮罩
  - 局部Loading状态
  
- **Empty组件（Empty.vue）**
  - 空状态展示（无数据、无搜索结果等）
  - 支持自定义图片和文案
  
- **Avatar组件（Avatar.vue）**
  - 头像展示
  - 支持圆形、方形
  - 加载失败显示默认头像
  
- **ImagePreview组件（ImagePreview.vue）**
  - 图片预览（放大、缩小、旋转）
  - 多图预览（左右切换）

**注意事项：**
- 使用Vue3 Composition API开发组件
- 使用`<script setup>`语法糖
- 组件Props定义使用TypeScript类型
- 组件Emits定义明确的事件类型
- 使用CSS Modules或Scoped CSS避免样式污染
- 响应式布局，适配移动端和PC端

---

## 第三周：用户服务后端开发（第3周）

### 3.1 数据库设计

**任务描述：**
设计用户模块相关的数据库表结构，包括用户表和用户关系表。

**数据表设计：**

#### 用户表（user）
```sql
CREATE TABLE `user` (
  `id` BIGINT NOT NULL COMMENT '用户ID（雪花算法生成）',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名（唯一，用于登录）',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `nickname` VARCHAR(50) NOT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `gender` TINYINT DEFAULT 0 COMMENT '性别（0-未知，1-男，2-女）',
  `birthday` DATE DEFAULT NULL COMMENT '生日',
  `bio` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
  `status` TINYINT DEFAULT 1 COMMENT '状态（0-禁用，1-正常）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

#### 用户关系表（user_relation）
```sql
CREATE TABLE `user_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `follower_id` BIGINT NOT NULL COMMENT '粉丝ID（关注者）',
  `followee_id` BIGINT NOT NULL COMMENT '被关注者ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_followee` (`follower_id`, `followee_id`),
  KEY `idx_follower` (`follower_id`),
  KEY `idx_followee` (`followee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关系表';
```

**技术要点：**

#### ID生成策略
- 使用雪花算法（Snowflake）生成分布式唯一ID
- ID由时间戳、机器ID、序列号组成
- 保证全局唯一、递增、高性能
- 自定义IdGenerator工具类

#### 密码加密
- 使用Spring Security的BCryptPasswordEncoder
- 每次加密结果不同（自动加盐）
- 验证密码使用matches方法
- 密码强度要求：8-20位，包含字母和数字

#### 索引设计
- username、email、phone建立唯一索引（保证唯一性）
- created_at建立普通索引（按时间查询）
- user_relation表follower_id和followee_id建立联合唯一索引（防止重复关注）
- follower_id和followee_id分别建立单列索引（分别查询粉丝和关注）

**注意事项：**
- 字段类型选择：VARCHAR长度合理设置，避免浪费空间
- 时间字段使用DATETIME，不要使用TIMESTAMP（范围更大）
- 所有表必须有主键
- 所有字段添加注释
- 字符集使用utf8mb4（支持emoji表情）
- 手机号和邮箱可以为空，但不能重复（UNIQUE约束）

---

### 3.2 用户注册与登录

**任务描述：**
实现用户注册、登录、JWT Token生成与验证等核心功能。

**实现步骤：**
1. 创建用户实体类和Mapper
2. 实现注册功能
3. 实现登录功能
4. 实现JWT Token功能

**技术要点：**

#### 用户注册
- **注册方式：** 手机号 + 短信验证码
- **验证码发送：**
  - 生成6位随机数字验证码
  - 调用第三方短信服务（阿里云、腾讯云）发送验证码
  - 验证码存储到Redis（key: sms:code:{phone}, value: code, expire: 5分钟）
  - 同一手机号1分钟内只能发送一次（防刷）
  
- **注册流程：**
  1. 验证手机号格式
  2. 检查手机号是否已注册
  3. 验证短信验证码（从Redis获取对比）
  4. 验证密码强度
  5. 生成用户ID（雪花算法）
  6. 密码加密（BCrypt）
  7. 写入数据库
  8. 删除Redis中的验证码（防止重复使用）
  9. 返回注册成功

#### 用户登录
- **登录方式：** 手机号/邮箱/用户名 + 密码
- **登录流程：**
  1. 根据账号查询用户（支持手机号、邮箱、用户名）
  2. 检查用户是否存在
  3. 检查用户状态（是否被禁用）
  4. 验证密码（BCrypt.matches）
  5. 生成Access Token和Refresh Token
  6. 将Token存储到Redis
  7. 返回Token和用户信息
  
- **登录失败处理：**
  - 记录登录失败次数（Redis计数器，key: login:fail:{account}）
  - 5次失败后锁定账号30分钟
  - 锁定期间禁止登录

#### JWT Token设计
- **Access Token：**
  - 有效期：30分钟
  - 存储用户ID、用户名
  - 使用HS256算法签名
  - 存储到Redis（key: token:access:{userId}, value: token, expire: 30分钟）
  
- **Refresh Token：**
  - 有效期：7天
  - 只存储用户ID
  - 使用HS256算法签名
  - 存储到Redis（key: token:refresh:{userId}, value: token, expire: 7天）
  
- **Token刷新：**
  - Access Token过期时，使用Refresh Token刷新
  - 验证Refresh Token有效性
  - 生成新的Access Token
  - 更新Redis中的Token

#### JWT工具类（JwtUtils）
- 生成Token：`generateToken(userId, username)`
- 解析Token：`parseToken(token)`
- 验证Token：`validateToken(token)`
- 刷新Token：`refreshToken(refreshToken)`

**注意事项：**
- 短信验证码不要存储到数据库，使用Redis即可
- 密码加密使用BCrypt，不要使用MD5（不安全）
- Token签名密钥从配置中心读取，不要硬编码
- 登录失败次数限制防止暴力破解
- Token存储到Redis后，可以实现强制下线功能（删除Redis中的Token）
- 同一用户多端登录需要考虑Token管理策略（单端登录、多端登录）

---

### 3.3 用户信息管理

**任务描述：**
实现用户信息查询、修改、密码修改等功能，并使用Redis缓存优化查询性能。

**实现步骤：**
1. 实现用户详情查询
2. 实现用户信息修改
3. 实现密码修改
4. 实现头像上传

**技术要点：**

#### 用户详情查询
- **查询内容：**
  - 基本信息：ID、用户名、昵称、头像、性别、生日、简介
  - 统计信息：动态数、关注数、粉丝数
  - 关系信息：是否已关注、是否关注我
  
- **缓存策略：**
  - Redis缓存用户基本信息（key: user:info:{userId}, expire: 1小时）
  - Redis缓存用户统计信息（key: user:stat:{userId}, fields: postCount, followCount, fansCount）
  - 查询流程：先查Redis，缓存未命中再查数据库，然后写入缓存
  
- **缓存更新策略：**
  - Cache Aside模式：先更新数据库，再删除缓存
  - 删除缓存而不是更新缓存（避免缓存与数据库不一致）

#### 用户信息修改
- **可修改字段：** 昵称、头像、性别、生日、简介
- **修改流程：**
  1. 验证字段合法性（昵称长度、简介长度等）
  2. 更新数据库
  3. 删除Redis缓存（user:info:{userId}）
  4. 返回更新后的用户信息
  
- **敏感信息修改：**
  - 手机号修改：需要验证旧手机号验证码 + 新手机号验证码
  - 邮箱修改：需要验证旧邮箱验证码 + 新邮箱验证码
  - 用户名修改：一般不允许修改（或限制修改次数）

#### 密码修改
- **修改流程：**
  1. 验证旧密码
  2. 验证新密码强度
  3. 验证新密码与确认密码一致
  4. 新密码加密
  5. 更新数据库
  6. 删除所有Token（强制重新登录）
  
- **忘记密码：**
  - 通过手机号验证码重置密码
  - 验证码验证通过后允许设置新密码

#### 头像上传
- **上传流程：**
  1. 前端上传图片到OSS（阿里云OSS、七牛云）
  2. 获取图片URL
  3. 调用后端接口更新头像URL
  4. 删除Redis缓存
  
- **图片限制：**
  - 格式：JPG、PNG、GIF
  - 大小：最大5MB
  - 尺寸：建议300x300

**注意事项：**
- 敏感信息返回时需要脱敏（手机号中间4位用*替换）
- 用户信息修改频率限制（1分钟最多修改3次，防止恶意修改）
- 密码修改后强制用户重新登录（安全考虑）
- 头像上传使用OSS而不是本地存储（节省服务器空间）
- 缓存键要有统一的命名规范（如：user:info:{userId}）
- 缓存过期时间设置合理（热点数据可以设置更长的过期时间）

---

### 3.4 关注与粉丝功能

**任务描述：**
实现用户关注、取消关注、查询关注列表、查询粉丝列表等功能，使用Redis优化性能。

**实现步骤：**
1. 实现关注功能
2. 实现取消关注功能
3. 实现关注列表查询
4. 实现粉丝列表查询

**技术要点：**

#### 关注功能
- **关注流程：**
  1. 验证不能关注自己
  2. 检查是否已经关注（从Redis Set查询）
  3. 写入数据库（user_relation表）
  4. 更新Redis Set（关注列表和粉丝列表）
  5. 更新Redis计数器（关注数和粉丝数）
  6. 发送关注通知（Kafka消息）
  
- **Redis数据结构：**
  - 用户关注列表：`key: user:follow:{userId}, value: Set<followeeId>`
  - 用户粉丝列表：`key: user:fans:{userId}, value: Set<followerId>`
  - 用户统计：`key: user:stat:{userId}, fields: {followCount, fansCount}`

#### 取消关注功能
- **取消关注流程：**
  1. 检查是否已经关注
  2. 删除数据库记录
  3. 从Redis Set中移除
  4. 更新Redis计数器（关注数和粉丝数）

#### Lua脚本保证原子性
- **关注Lua脚本：**
  ```lua
  -- 检查是否已关注
  local exists = redis.call('SISMEMBER', KEYS[1], ARGV[1])
  if exists == 1 then
    return 0  -- 已经关注
  end
  
  -- 添加到关注列表
  redis.call('SADD', KEYS[1], ARGV[1])
  -- 添加到粉丝列表
  redis.call('SADD', KEYS[2], ARGV[2])
  -- 关注数+1
  redis.call('HINCRBY', KEYS[3], 'followCount', 1)
  -- 粉丝数+1
  redis.call('HINCRBY', KEYS[4], 'fansCount', 1)
  
  return 1  -- 关注成功
  ```

#### 关注列表查询
- **查询流程：**
  1. 从Redis Set获取关注列表（SMEMBERS或SSCAN）
  2. 批量查询用户信息（MyBatis-Plus的listByIds）
  3. 返回用户列表（包含关注时间）
  
- **分页策略：**
  - 使用游标分页（cursor-based）
  - 每次返回20条数据
  - 支持按关注时间倒序排列

#### 粉丝列表查询
- 与关注列表查询类似
- 从Redis Set获取粉丝列表
- 批量查询用户信息

#### 共同关注查询
- 使用Redis Set交集运算（SINTER）
- 查询两个用户的共同关注

**注意事项：**
- 使用Lua脚本保证Redis多个操作的原子性
- 关注/取消关注同时更新数据库和缓存（双写一致性）
- Redis Set容量限制（单个用户关注上限10000）
- 定时任务将Redis数据同步到数据库（每小时）
- 大V用户的粉丝列表可能很大，查询时需要分页
- 防止用户快速重复点击（前端防抖 + 后端幂等性）
- 关注数和粉丝数可能存在误差，需要定期校对（数据库与Redis对比）

---

## 第四周：用户模块前端开发（第4周）

### 4.1 登录注册页面

**任务描述：**
实现用户登录和注册页面，包含表单验证、验证码倒计时等功能。

**实现步骤：**
1. 创建登录页面
2. 创建注册页面
3. 实现表单验证
4. 实现验证码功能

**技术要点：**

#### 登录页面（Login.vue）
- **页面布局：**
  - 左侧：产品介绍、Banner图（可选）
  - 右侧：登录表单
  
- **表单字段：**
  - 账号输入框（手机号/邮箱/用户名）
  - 密码输入框（带显示/隐藏切换）
  - 记住我（7天免登录）
  - 登录按钮
  - 注册链接、忘记密码链接
  
- **表单验证：**
  - 账号不能为空
  - 密码不能为空（6-20位）
  - 使用Element Plus的Form验证
  
- **登录逻辑：**
  1. 表单验证通过
  2. 调用登录接口
  3. 保存Token到localStorage
  4. 保存用户信息到Pinia Store
  5. 跳转到首页或来源页面

#### 注册页面（Register.vue）
- **表单字段：**
  - 手机号输入框
  - 验证码输入框 + 发送按钮
  - 密码输入框
  - 确认密码输入框
  - 同意协议复选框
  - 注册按钮
  - 已有账号登录链接
  
- **表单验证：**
  - 手机号格式验证（正则：/^1[3-9]\d{9}$/）
  - 验证码6位数字
  - 密码强度验证（8-20位，包含字母和数字）
  - 确认密码与密码一致
  - 必须同意协议

#### 验证码功能
- **发送验证码：**
  1. 验证手机号格式
  2. 调用发送验证码接口
  3. 按钮禁用，开始倒计时
  4. 倒计时结束后恢复按钮
  
- **倒计时实现：**
  ```typescript
  const countdown = ref(60)
  const isSending = ref(false)
  
  const sendCode = async () => {
    // 调用接口
    await sendSmsCode(phone.value)
    
    // 开始倒计时
    isSending.value = true
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value === 0) {
        clearInterval(timer)
        countdown.value = 60
        isSending.value = false
      }
    }, 1000)
  }
  ```

#### 密码输入框
- **显示/隐藏切换：**
  - 使用Element Plus的Input组件
  - 添加后缀图标（眼睛图标）
  - 点击切换type（password/text）

#### 记住我功能
- 勾选后保存Token到localStorage（7天过期）
- 未勾选保存到sessionStorage（关闭浏览器失效）

**注意事项：**
- 登录注册页面使用空白布局（BlankLayout）
- 表单验证失败时聚焦到第一个错误字段
- 登录失败显示错误提示（Toast）
- 验证码倒计时防止页面刷新丢失（可以存储到localStorage）
- 密码强度提示（弱、中、强）
- 响应式布局，移动端优化
- Loading状态（按钮loading、全屏loading）

---

### 4.2 个人主页

**任务描述：**
实现用户个人主页，展示用户信息、统计数据、编辑资料等功能。

**实现步骤：**
1. 创建个人主页组件
2. 实现用户信息展示
3. 实现编辑资料功能
4. 实现头像上传功能

**技术要点：**

#### 个人主页布局
- **顶部区域：**
  - 背景封面图
  - 用户头像（大头像）
  - 用户昵称、用户名
  - 个人简介
  - 编辑资料按钮（自己的主页才显示）
  - 关注/取消关注按钮（其他用户的主页）
  
- **统计区域：**
  - 动态数
  - 关注数
  - 粉丝数
  - 点击可跳转到对应列表
  
- **Tab区域：**
  - 动态Tab：用户发布的动态列表
  - 回复Tab：用户的评论回复
  - 点赞Tab：用户点赞的动态

#### 编辑资料弹窗
- **表单字段：**
  - 昵称（2-20字符）
  - 性别（男/女/保密）
  - 生日（日期选择器）
  - 个人简介（0-500字）
  
- **保存逻辑：**
  1. 表单验证
  2. 调用更新接口
  3. 更新Pinia Store中的用户信息
  4. 更新页面显示
  5. 关闭弹窗

#### 头像上传
- **上传流程：**
  1. 选择图片
  2. 图片裁剪（vue-cropper）
  3. 压缩图片（browser-image-compression）
  4. 上传到OSS
  5. 获取图片URL
  6. 调用后端接口更新头像
  7. 更新页面显示
  
- **图片裁剪：**
  - 使用vue-cropper组件
  - 裁剪比例1:1（正方形）
  - 支持缩放、旋转
  - 预览效果
  
- **图片压缩：**
  - 使用browser-image-compression库
  - 压缩到500KB以下
  - 保持图片质量

#### 用户信息展示
- **头像展示：**
  - 圆形头像
  - 加载失败显示默认头像
  - 支持点击放大预览
  
- **统计数字格式化：**
  - 小于1万直接显示
  - 大于1万显示1.2w格式
  - 大于1亿显示1.2亿格式

**注意事项：**
- 区分自己的主页和他人的主页（显示不同的操作按钮）
- 编辑资料弹窗使用Dialog组件
- 头像上传前压缩，减少上传时间
- 裁剪后的图片使用canvas导出
- 上传过程显示进度条
- 上传失败提示用户重试
- 统计数字实时更新（关注后关注数+1）

---

### 4.3 关注与粉丝页面

**任务描述：**
实现关注列表和粉丝列表页面，支持虚拟滚动优化长列表性能。

**实现步骤：**
1. 创建关注列表页面
2. 创建粉丝列表页面
3. 实现关注/取消关注功能
4. 实现虚拟滚动优化

**技术要点：**

#### 关注列表页面
- **列表项展示：**
  - 用户头像
  - 用户昵称、用户名
  - 个人简介（一行显示，超出省略）
  - 关注/取消关注按钮
  - 关注时间
  
- **加载更多：**
  - 下拉加载更多（无限滚动）
  - 使用IntersectionObserver监听滚动
  - 滚动到底部自动加载下一页

#### 粉丝列表页面
- 与关注列表类似
- 显示"回关"按钮（如果未关注对方）

#### 关注/取消关注功能
- **按钮状态：**
  - 未关注：显示"关注"按钮
  - 已关注：显示"已关注"按钮，鼠标悬停显示"取消关注"
  - 互相关注：显示"互相关注"标识
  
- **操作流程：**
  1. 点击按钮
  2. 防抖处理（300ms）
  3. 调用接口
  4. 乐观更新UI（立即更新按钮状态）
  5. 接口失败回滚UI
  
- **乐观更新：**
  ```typescript
  const handleFollow = async (userId: number) => {
    // 乐观更新
    const oldStatus = user.isFollowed
    user.isFollowed = !user.isFollowed
    
    try {
      await followUser(userId)
    } catch (error) {
      // 失败回滚
      user.isFollowed = oldStatus
      ElMessage.error('操作失败')
    }
  }
  ```

#### 虚拟滚动优化
- **为什么需要虚拟滚动：**
  - 关注/粉丝列表可能有成千上万条数据
  - 一次性渲染会导致页面卡顿
  - 虚拟滚动只渲染可视区域的数据
  
- **实现方案：**
  - 使用vue-virtual-scroller库
  - 或使用Element Plus的Virtualized List组件
  - 设置itemHeight（每项高度）
  - 自动计算可视区域渲染的数据

#### 搜索功能
- 搜索框输入用户名或昵称
- 防抖处理（500ms）
- 调用搜索接口过滤列表

**注意事项：**
- 关注/取消关注按钮防抖处理，避免重复点击
- 乐观更新提升用户体验（操作响应快）
- 虚拟滚动注意设置正确的itemHeight
- 列表为空时显示空状态组件
- 加载更多时显示Loading状态
- 点击用户项跳转到用户主页
- 移动端优化（列表项高度、字体大小）

---

## 第一个月总结

### 已完成内容
1. 后端微服务基础架构搭建（Maven多模块、Nacos、Gateway）
2. 前端Vue3项目初始化（Vite、TypeScript、路由、状态管理）
3. 用户模块完整实现（注册、登录、用户信息管理、关注功能）
4. 基础组件和布局开发

### 技术重点
1. Spring Cloud微服务架构
2. JWT Token双令牌机制
3. Redis缓存策略和Lua脚本
4. Vue3 Composition API
5. TypeScript类型系统

### 下个月计划
1. 动态模块开发（发布动态、动态列表）
2. Feed流服务开发（推拉结合模式）
3. 评论模块开发（多级评论）
4. 互动模块开发（点赞、收藏、分享）

---
