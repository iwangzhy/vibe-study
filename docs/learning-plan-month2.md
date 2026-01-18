# 第二个月学习计划 - 动态、Feed流与评论模块

> 时间：第5-8周  
> 目标：完成动态发布、Feed流服务、评论系统的前后端开发

---

## 第五周：动态服务后端开发（第5周）

### 5.1 数据库设计

**任务描述：**
设计动态模块相关的数据库表结构，支持文本、图片、视频等多种类型的动态。

**数据表设计：**

#### 动态表（post）
```sql
CREATE TABLE `post` (
  `id` BIGINT NOT NULL COMMENT '动态ID（雪花算法生成）',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `content` TEXT NOT NULL COMMENT '动态内容',
  `images` JSON DEFAULT NULL COMMENT '图片URL数组（最多9张）',
  `video` VARCHAR(255) DEFAULT NULL COMMENT '视频URL',
  `type` TINYINT NOT NULL DEFAULT 1 COMMENT '动态类型（1-文本，2-图片，3-视频）',
  `location` VARCHAR(100) DEFAULT NULL COMMENT '位置信息',
  `ip_location` VARCHAR(50) DEFAULT NULL COMMENT 'IP属地',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0-已删除，1-正常，2-审核中）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`, `created_at`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态表';
```

#### 动态统计表（post_stat）
```sql
CREATE TABLE `post_stat` (
  `post_id` BIGINT NOT NULL COMMENT '动态ID',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
  `share_count` INT NOT NULL DEFAULT 0 COMMENT '分享数',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态统计表';
```

#### 话题表（topic）
```sql
CREATE TABLE `topic` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '话题ID',
  `name` VARCHAR(100) NOT NULL COMMENT '话题名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '话题描述',
  `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '话题封面',
  `post_count` INT NOT NULL DEFAULT 0 COMMENT '动态数量',
  `follow_count` INT NOT NULL DEFAULT 0 COMMENT '关注数',
  `hot_score` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '热度分数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0-禁用，1-正常）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_hot_score` (`hot_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='话题表';
```

#### 动态话题关联表（post_topic）
```sql
CREATE TABLE `post_topic` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` BIGINT NOT NULL COMMENT '动态ID',
  `topic_id` BIGINT NOT NULL COMMENT '话题ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_topic` (`post_id`, `topic_id`),
  KEY `idx_topic_id` (`topic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态话题关联表';
```

**技术要点：**

#### 分库分表策略
- 使用ShardingSphere-JDBC实现分库分表
- 按user_id进行分表（取模算法）
- 分表数量：8张表（post_0 ~ post_7）
- 分表优势：提升写入性能，避免单表数据量过大

#### 软删除设计
- 使用deleted_at字段实现软删除
- 查询时添加条件：WHERE deleted_at IS NULL
- 软删除的好处：数据可恢复、数据统计完整

#### JSON字段存储
- images字段存储JSON数组：["url1", "url2", "url3"]
- MyBatis-Plus配置TypeHandler处理JSON字段
- 最多存储9张图片（参考微博）

#### 索引设计
- (user_id, created_at)组合索引：查询用户动态列表
- created_at索引：按时间倒序查询
- topic表的name唯一索引：防止话题重复
- topic表的hot_score索引：热门话题排序

**注意事项：**
- 动态内容使用TEXT类型（最大65535字节）
- 统计表单独设计，避免频繁更新主表
- 话题名称唯一，创建话题前先查询是否存在
- IP属地自动识别（根据用户IP获取地理位置）
- 分表后的分页查询需要注意性能问题

---

### 5.2 发布动态

**任务描述：**
实现动态发布功能，支持文本、图片、视频动态，包含敏感词过滤、@提及、#话题#解析等。

**实现步骤：**
1. 实现文本动态发布
2. 实现图片动态发布
3. 实现视频动态发布
4. 实现敏感词过滤
5. 实现@提及和#话题#解析

**技术要点：**

#### 发布流程
1. 接收前端提交的数据（内容、图片、视频、位置等）
2. 内容审核（敏感词过滤）
3. 解析@提及用户
4. 解析#话题#标签
5. 生成动态ID（雪花算法）
6. 写入数据库（post表 + post_stat表）
7. 发送Kafka消息（异步处理Feed流推送）
8. 返回动态详情

#### 敏感词过滤
- **DFA算法实现：**
  - 构建敏感词字典树（Trie树）
  - 快速匹配敏感词（时间复杂度O(n)）
  - 敏感词替换为***
  
- **敏感词库管理：**
  - 敏感词存储在Redis Set中（key: sensitive:words）
  - 支持动态添加和删除敏感词
  - 启动时加载敏感词到内存
  
- **过滤策略：**
  - 发现敏感词直接拒绝发布
  - 或自动替换敏感词后发布
  - 记录敏感词日志（用户ID、内容、敏感词）

#### @提及用户解析
- **解析规则：**
  - 正则表达式匹配：`@用户名`
  - 用户名格式：字母、数字、下划线，4-20位
  - 正则：`/@([a-zA-Z0-9_]{4,20})/g`
  
- **解析流程：**
  1. 提取所有@用户名
  2. 查询用户是否存在
  3. 存储提及关系（post_mention表）
  4. 发送提及通知（Kafka消息）

#### #话题#解析
- **解析规则：**
  - 正则表达式匹配：`#话题名#`
  - 话题名格式：中文、字母、数字，2-20字符
  - 正则：`/#([^#\s]{2,20})#/g`
  
- **解析流程：**
  1. 提取所有#话题#
  2. 查询话题是否存在，不存在则创建
  3. 存储动态话题关联（post_topic表）
  4. 更新话题统计（动态数+1）

#### 限流策略
- **发布频率限制：**
  - 使用Redis + Lua脚本实现滑动窗口限流
  - 限制：1分钟最多发布3条动态
  - key: `post:limit:{userId}`, value: List<timestamp>
  
- **Lua脚本实现：**
  ```lua
  local key = KEYS[1]
  local now = tonumber(ARGV[1])
  local ttl = tonumber(ARGV[2])
  local limit = tonumber(ARGV[3])
  
  -- 移除过期的时间戳
  redis.call('ZREMRANGEBYSCORE', key, 0, now - ttl * 1000)
  
  -- 获取当前数量
  local count = redis.call('ZCARD', key)
  
  if count < limit then
    redis.call('ZADD', key, now, now)
    redis.call('EXPIRE', key, ttl)
    return 1
  else
    return 0
  end
  ```

#### Kafka异步处理
- **消息内容：**
  - 动态ID
  - 用户ID
  - 动态类型
  - 发布时间
  
- **消费者处理：**
  - Feed流服务消费消息
  - 推送动态到粉丝收件箱
  - 搜索服务消费消息，同步到Elasticsearch

**注意事项：**
- 图片和视频先上传到OSS，获取URL后再提交
- 内容长度限制：文本最多5000字
- 图片最多9张，每张最大5MB
- 视频最大100MB，时长最多5分钟
- 发布失败需要回滚（删除已上传的图片、视频）
- IP属地自动识别，不需要用户填写
- 定时清理审核不通过的动态

---

### 5.3 动态查询

**任务描述：**
实现动态详情查询、用户动态列表查询、动态删除等功能，使用Redis缓存优化性能。

**实现步骤：**
1. 实现动态详情查询
2. 实现用户动态列表查询
3. 实现动态删除
4. 实现Redis缓存策略

**技术要点：**

#### 动态详情查询
- **查询内容：**
  - 动态基本信息（ID、内容、图片、视频、位置等）
  - 用户信息（昵称、头像）
  - 统计信息（点赞数、评论数、分享数、浏览数）
  - 话题信息（话题列表）
  - 当前用户交互状态（是否已点赞、是否已收藏）
  
- **数据聚合：**
  - 从post表查询动态基本信息
  - 从post_stat表查询统计信息
  - 通过OpenFeign调用user-service获取用户信息
  - 从Redis查询当前用户的点赞、收藏状态
  
- **服务降级：**
  - 使用Sentinel配置降级规则
  - 用户服务不可用时返回默认用户信息
  - 统计服务不可用时返回默认统计（0）

#### 用户动态列表查询
- **查询条件：**
  - 用户ID
  - 分页参数（cursor、pageSize）
  - 排序方式（时间倒序）
  
- **游标分页：**
  - 使用cursor-based pagination而非offset分页
  - cursor为上一页最后一条数据的创建时间
  - SQL: WHERE created_at < cursor ORDER BY created_at DESC LIMIT pageSize
  - 避免深分页性能问题
  
- **分表查询：**
  - ShardingSphere自动路由到对应分表
  - 查询条件必须包含分片键（user_id）

#### 动态删除
- **软删除流程：**
  1. 检查动态是否属于当前用户
  2. 更新deleted_at字段
  3. 删除Redis缓存
  4. 发送Kafka消息（通知Feed流服务删除）
  
- **硬删除：**
  - 定时任务清理30天前的软删除数据
  - 同时删除关联数据（统计、话题关联等）

#### Redis缓存策略
- **缓存热门动态：**
  - key: `post:info:{postId}`
  - value: 动态详情JSON
  - expire: 10分钟
  
- **缓存用户动态列表：**
  - key: `post:list:{userId}:page:{cursor}`
  - value: 动态ID列表
  - expire: 5分钟
  
- **缓存预热：**
  - 定时任务将热门动态加载到Redis
  - 热门标准：浏览数 > 10000或点赞数 > 1000
  
- **缓存更新策略：**
  - Cache Aside模式：先更新数据库，再删除缓存
  - 不使用缓存更新，避免并发问题

#### 布隆过滤器防止缓存穿透
- **使用场景：**
  - 查询不存在的动态ID
  - 恶意攻击（大量查询不存在的ID）
  
- **实现方案：**
  - 使用Redis的Bloom Filter
  - 动态发布时添加ID到布隆过滤器
  - 查询前先判断ID是否存在于布隆过滤器
  - 不存在直接返回，不查询数据库

**注意事项：**
- 动态详情返回时需要脱敏（如隐藏手机号）
- 用户信息调用失败不能影响主流程
- 缓存Key要有统一的命名规范
- 缓存过期时间要随机化，避免缓存雪崩
- 分页查询使用游标分页，性能更好
- 软删除的数据不显示在列表中
- 统计数据允许一定的延迟（最终一致性）

---

## 第六周：动态模块前端开发（第6周）

### 6.1 发布动态页面

**任务描述：**
实现动态发布页面，支持文本、图片、视频上传，@提及用户和#话题#自动补全。

**实现步骤：**
1. 创建发布动态页面
2. 实现富文本编辑器
3. 实现图片上传
4. 实现视频上传
5. 实现@和#自动补全

**技术要点：**

#### 发布动态页面布局
- **编辑区域：**
  - 富文本编辑框（多行文本输入）
  - 字数统计（0/5000）
  - @提及按钮
  - #话题#按钮
  - 添加图片按钮
  - 添加视频按钮
  - 添加位置按钮
  
- **预览区域：**
  - 图片预览（最多9张）
  - 视频预览
  - 位置展示
  - 可删除已添加的内容

#### 富文本编辑器
- **技术选型：**
  - Quill编辑器（轻量级）
  - 或Tiptap编辑器（基于ProseMirror）
  - 或使用Element Plus的Input type="textarea"
  
- **功能需求：**
  - 支持多行文本输入
  - 支持@用户自动补全
  - 支持#话题#自动补全
  - 支持emoji表情选择
  - 实时字数统计
  
- **@用户自动补全：**
  - 监听输入，检测@符号
  - 弹出用户搜索下拉框
  - 输入用户名搜索
  - 选择用户后插入@用户名
  
- **#话题#自动补全：**
  - 监听输入，检测#符号
  - 弹出话题搜索下拉框
  - 输入话题名搜索（热门话题优先）
  - 选择话题后插入#话题名#

#### 图片上传
- **上传流程：**
  1. 选择图片（支持多选，最多9张）
  2. 图片格式验证（JPG、PNG、GIF）
  3. 图片大小验证（每张最大5MB）
  4. 图片压缩（browser-image-compression）
  5. 上传到OSS
  6. 显示上传进度
  7. 获取图片URL
  8. 预览图片
  
- **图片预览：**
  - 缩略图展示（3列或4列）
  - 点击放大查看
  - 支持删除图片
  - 支持拖拽排序
  
- **图片压缩：**
  ```typescript
  import imageCompression from 'browser-image-compression'
  
  const compressImage = async (file: File) => {
    const options = {
      maxSizeMB: 1,
      maxWidthOrHeight: 1920,
      useWebWorker: true
    }
    return await imageCompression(file, options)
  }
  ```

#### 视频上传
- **上传流程：**
  1. 选择视频（只能选择1个）
  2. 视频格式验证（MP4、MOV）
  3. 视频大小验证（最大100MB）
  4. 视频时长验证（最多5分钟）
  5. 上传到OSS
  6. 显示上传进度
  7. 获取视频URL
  8. 生成视频封面（第一帧截图）
  
- **视频预览：**
  - 显示视频封面
  - 显示时长
  - 支持删除视频
  - 点击预览播放

#### 位置选择
- **实现方案：**
  - 调用高德地图或腾讯地图API
  - 定位当前位置
  - 搜索附近的地点
  - 选择地点后显示地址
  
- **位置展示：**
  - 图标 + 地址名称
  - 支持删除位置

#### 发布按钮
- **发布前验证：**
  - 内容不能为空
  - 字数不能超过5000
  - 图片或视频上传完成
  
- **发布流程：**
  1. 验证通过
  2. 组装数据（内容、图片URL、视频URL、位置等）
  3. 调用发布接口
  4. 显示Loading
  5. 发布成功跳转到动态详情页
  6. 发布失败提示错误信息

**注意事项：**
- 图片上传前先压缩，减少上传时间
- 上传进度条实时显示（使用Axios的onUploadProgress）
- 上传失败要提示用户重试
- 多张图片并发上传（Promise.all）
- 视频上传可能较慢，要有耐心提示
- 离开页面前提示保存草稿
- 草稿自动保存到localStorage
- @和#自动补全要防抖（300ms）

---

### 6.2 动态列表组件

**任务描述：**
实现动态卡片组件，展示动态内容、图片、视频、统计数据等，支持图片懒加载和视频懒加载。

**实现步骤：**
1. 创建动态卡片组件
2. 实现图片展示
3. 实现视频播放器
4. 实现懒加载优化

**技术要点：**

#### 动态卡片组件（PostCard.vue）
- **卡片布局：**
  - 顶部：用户信息（头像、昵称、时间、位置）
  - 中部：动态内容
    - 文本内容（支持展开/收起）
    - 图片（最多9张，瀑布流或网格布局）
    - 视频（封面 + 播放按钮）
    - 话题标签
  - 底部：统计信息（点赞数、评论数、分享数）+ 操作按钮
  
- **用户信息区域：**
  - 圆形头像（点击跳转用户主页）
  - 用户昵称（点击跳转用户主页）
  - 发布时间（格式化：刚刚、5分钟前、1小时前、日期）
  - 位置信息（图标 + 地址）
  - 更多按钮（举报、删除等）

#### 文本内容展示
- **长文本折叠：**
  - 内容超过3行自动折叠
  - 显示"展开全文"按钮
  - 点击展开后显示"收起"按钮
  
- **内容渲染：**
  - @用户名高亮显示（蓝色、可点击）
  - #话题#高亮显示（蓝色、可点击）
  - URL自动转换为链接
  - 换行符保留（white-space: pre-wrap）

#### 图片展示
- **布局方案：**
  - 1张图片：大图展示（最大宽度100%）
  - 2张图片：横向排列（每张50%宽度）
  - 3张图片：1+2布局或3列布局
  - 4张图片：2x2网格布局
  - 5-6张图片：2+3或3+3布局
  - 7-9张图片：3x3网格布局
  
- **图片预览：**
  - 点击图片放大查看
  - 支持左右切换
  - 支持缩放、旋转
  - 使用Element Plus的Image Preview组件
  
- **图片懒加载：**
  - 使用vue-lazyload插件
  - 或使用原生IntersectionObserver API
  - 图片进入可视区域才加载
  - 加载前显示占位图（loading.gif）

#### 视频播放器
- **视频封面：**
  - 显示视频封面图
  - 显示播放按钮（居中、半透明背景）
  - 显示视频时长（右下角）
  
- **视频播放：**
  - 点击封面播放视频
  - 使用HTML5 video标签
  - 或使用第三方播放器（Video.js、DPlayer）
  - 支持全屏播放
  - 支持进度条、音量控制
  
- **视频懒加载：**
  - 视频初始不加载（preload="none"）
  - 滚动到可视区域才设置src
  - 节省流量和提升性能

#### 统计信息与操作按钮
- **统计信息：**
  - 点赞数（1.2万格式化）
  - 评论数
  - 分享数
  
- **操作按钮：**
  - 点赞按钮（未点赞：灰色，已点赞：红色）
  - 评论按钮（点击跳转到动态详情页评论区）
  - 分享按钮（弹出分享弹窗）

#### 时间格式化
```typescript
const formatTime = (time: string) => {
  const now = Date.now()
  const postTime = new Date(time).getTime()
  const diff = now - postTime
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 2592000000) return `${Math.floor(diff / 86400000)}天前`
  
  return new Date(time).toLocaleDateString()
}
```

**注意事项：**
- 动态卡片组件要高度可复用
- 使用Props传入动态数据，使用Emits触发事件
- 图片懒加载减少首屏加载时间
- 视频不自动播放，节省流量
- 长文本折叠提升阅读体验
- @用户和#话题#要可点击跳转
- 时间格式化友好显示
- 统计数字格式化（万、亿）
- 操作按钮要有防抖处理

---

### 6.3 用户动态列表页

**任务描述：**
实现用户动态列表页面，展示用户发布的所有动态，支持下拉刷新和上拉加载更多。

**实现步骤：**
1. 创建动态列表页面
2. 实现动态列表渲染
3. 实现下拉刷新
4. 实现上拉加载更多

**技术要点：**

#### 动态列表页面（PostList.vue）
- **页面结构：**
  - 头部：Tab切换（全部、图片、视频）
  - 列表：动态卡片列表
  - 底部：加载更多提示
  
- **数据加载：**
  - 进入页面加载第一页数据
  - 显示Loading骨架屏
  - 数据加载完成显示列表
  - 空列表显示Empty组件

#### 动态列表渲染
- **列表循环：**
  ```vue
  <post-card
    v-for="post in postList"
    :key="post.id"
    :post="post"
    @like="handleLike"
    @comment="handleComment"
    @share="handleShare"
  />
  ```
  
- **虚拟滚动优化：**
  - 当列表数据量大时（>100条）
  - 使用vue-virtual-scroller组件
  - 只渲染可视区域的数据
  - 提升列表性能

#### 下拉刷新
- **实现方案：**
  - 使用第三方组件（vant的PullRefresh）
  - 或自己实现（监听touchstart、touchmove、touchend）
  
- **刷新逻辑：**
  1. 下拉触发刷新
  2. 显示Loading动画
  3. 重新请求第一页数据
  4. 替换列表数据
  5. 隐藏Loading
  6. 提示"刷新成功"

#### 上拉加载更多
- **实现方案：**
  - 使用IntersectionObserver监听列表底部
  - 或监听scroll事件
  
- **加载逻辑：**
  1. 滚动到底部触发加载
  2. 显示Loading动画
  3. 请求下一页数据（传入cursor）
  4. 追加到列表数据
  5. 隐藏Loading
  6. 如果没有更多数据，显示"没有更多了"
  
- **IntersectionObserver实现：**
  ```typescript
  const observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting && !loading.value && hasMore.value) {
      loadMore()
    }
  })
  
  onMounted(() => {
    observer.observe(loadMoreRef.value)
  })
  
  onUnmounted(() => {
    observer.disconnect()
  })
  ```

#### 骨架屏Loading
- **使用场景：**
  - 首次加载数据时
  - 下拉刷新时
  
- **实现方案：**
  - 使用Element Plus的Skeleton组件
  - 或自定义骨架屏组件
  - 模拟动态卡片的结构
  - 显示灰色占位块和闪烁动画

#### Tab切换
- **Tab选项：**
  - 全部：所有类型的动态
  - 图片：只显示图片动态
  - 视频：只显示视频动态
  
- **切换逻辑：**
  1. 点击Tab
  2. 清空列表数据
  3. 重置分页参数
  4. 请求对应类型的数据
  5. 显示新的列表

**注意事项：**
- 下拉刷新和上拉加载要有节流，避免重复请求
- 加载更多时要检查是否还有下一页
- 列表为空时显示友好的空状态提示
- 网络错误时显示重试按钮
- 骨架屏的高度要和实际卡片高度一致
- 虚拟滚动注意设置正确的itemHeight
- Tab切换要缓存之前的滚动位置（可选）
- 移动端优化（触摸滑动流畅度）

---

## 第七周：Feed流服务后端开发（第7周）

### 7.1 Feed流架构设计

**任务描述：**
设计Feed流的架构，采用推拉结合的模式，实现高效的动态推送和拉取。

**架构方案：**

#### Feed流模式对比
- **推模式（Push）：**
  - 用户发布动态时，推送到所有粉丝的收件箱
  - 优点：读取快（直接从收件箱读取）
  - 缺点：写入慢（粉丝多时写入量大）
  - 适合：普通用户（粉丝数 < 10000）
  
- **拉模式（Pull）：**
  - 用户打开首页时，实时拉取关注用户的最新动态
  - 优点：写入快（只写入作者的发件箱）
  - 缺点：读取慢（需要查询多个用户的动态）
  - 适合：大V用户（粉丝数 >= 10000）
  
- **推拉结合（Push-Pull）：**
  - 普通用户发布动态：推送到粉丝收件箱
  - 大V用户发布动态：只写入自己的发件箱
  - 用户读取Feed流：从收件箱读取 + 拉取大V用户的动态
  - 优点：平衡读写性能
  - 缺点：实现复杂

#### Redis数据结构设计
- **用户Feed收件箱（推模式）：**
  - key: `feed:inbox:{userId}`
  - type: Sorted Set
  - score: 动态发布时间戳
  - value: 动态ID
  - 容量限制：每个用户最多1000条
  
- **用户发件箱（拉模式）：**
  - key: `feed:outbox:{userId}`
  - type: Sorted Set
  - score: 动态发布时间戳
  - value: 动态ID
  - 容量限制：每个用户最多1000条
  
- **大V用户标识：**
  - key: `user:bigv`
  - type: Set
  - value: 大V用户ID集合
  - 定期更新（定时任务）

**技术要点：**

#### 大V用户判断
- **判断标准：**
  - 粉丝数 >= 10000
  - 或手动设置（管理后台）
  
- **定时任务更新：**
  - 每小时执行一次
  - 查询粉丝数 >= 10000的用户
  - 更新Redis Set（user:bigv）

#### Feed流推送策略
- **普通用户发布动态：**
  1. 写入自己的发件箱（outbox）
  2. 查询粉丝列表（分页查询，每次1000个）
  3. 批量推送到粉丝的收件箱（inbox）
  4. 使用Redis Pipeline提升性能
  
- **大V用户发布动态：**
  1. 只写入自己的发件箱（outbox）
  2. 不推送到粉丝收件箱

#### Feed流拉取策略
- **拉取流程：**
  1. 从收件箱（inbox）获取动态ID列表
  2. 获取关注的大V用户列表
  3. 从大V用户的发件箱（outbox）拉取动态ID
  4. 合并两部分动态ID，按时间戳排序
  5. 批量查询动态详情
  6. 返回Feed流列表
  
- **分页策略：**
  - 使用cursor-based pagination
  - cursor为上一页最后一条动态的时间戳
  - 每页20条数据

**注意事项：**
- 收件箱和发件箱要有容量限制，避免内存占用过大
- 使用Redis Sorted Set的ZREMRANGEBYRANK删除旧数据
- Feed流拉取要合并多个来源的数据
- 大V用户的判断标准可以灵活调整
- 定时任务要考虑性能，避免阻塞
- Redis Pipeline批量操作提升性能

---

### 7.2 Feed流推送

**任务描述：**
实现Feed流推送功能，监听Kafka动态发布消息，异步推送到粉丝收件箱。

**实现步骤：**
1. 创建Kafka消费者
2. 监听动态发布消息
3. 实现Feed流推送逻辑
4. 实现批量推送优化

**技术要点：**

#### Kafka消费者配置
- **Topic：** post-publish
- **消费者组：** feed-service-group
- **消费者数量：** 3个（并发消费）
- **消息格式：**
  ```json
  {
    "postId": 123456789,
    "userId": 987654321,
    "postType": 1,
    "timestamp": 1640000000000
  }
  ```

#### 推送流程
1. 消费Kafka消息
2. 判断用户是否为大V（从Redis Set查询）
3. 如果是大V，只写入发件箱，结束
4. 如果是普通用户，继续推送流程
5. 写入自己的发件箱
6. 查询粉丝列表（分页查询）
7. 批量推送到粉丝收件箱

#### 批量推送优化
- **分页查询粉丝：**
  - 每次查询1000个粉丝
  - 避免一次性加载大量数据导致OOM
  
- **Redis Pipeline批量写入：**
  ```java
  // 使用Pipeline批量写入
  RedisTemplate<String, String> redisTemplate;
  
  redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
      for (Long fanId : fanIds) {
          String key = "feed:inbox:" + fanId;
          connection.zAdd(key.getBytes(), timestamp, postId.getBytes());
          // 限制收件箱容量（保留最新1000条）
          connection.zRemRangeByRank(key.getBytes(), 0, -1001);
      }
      return null;
  });
  ```

#### 收件箱容量限制
- **容量限制：** 每个用户最多1000条
- **清理策略：** 使用ZREMRANGEBYRANK保留最新的1000条
- **清理时机：** 每次写入后立即清理

#### 异步处理
- **使用线程池：**
  - 粉丝列表分批处理（每批1000个）
  - 每批提交到线程池异步执行
  - 提升推送速度
  
- **线程池配置：**
  - 核心线程数：10
  - 最大线程数：20
  - 队列容量：500
  - 拒绝策略：CallerRunsPolicy

#### 推送失败处理
- **重试机制：**
  - 推送失败自动重试3次
  - 重试间隔：1秒、2秒、4秒（指数退避）
  
- **死信队列：**
  - 重试3次后仍失败，发送到死信队列
  - 定期人工处理死信队列

**注意事项：**
- Kafka消费者要保证幂等性（防止重复消费）
- 使用消息ID去重（Redis Set存储已处理的消息ID）
- 粉丝数量很多时推送可能较慢（异步处理）
- 收件箱容量限制防止内存占用过大
- 线程池要合理配置，避免OOM
- 推送失败要有监控和告警

---

### 7.3 Feed流拉取

**任务描述：**
实现Feed流拉取功能，从收件箱和大V发件箱合并数据，返回给前端展示。

**实现步骤：**
1. 实现收件箱数据拉取
2. 实现大V发件箱数据拉取
3. 实现数据合并和排序
4. 实现多级缓存优化

**技术要点：**

#### 收件箱数据拉取
- **拉取流程：**
  1. 从Redis Sorted Set获取收件箱数据
  2. 使用ZREVRANGEBYSCORE按时间戳倒序查询
  3. 传入cursor参数（上一页最后一条的时间戳）
  4. 查询pageSize + 1条数据（判断是否还有下一页）
  
- **Redis命令：**
  ```java
  // 拉取收件箱数据
  Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate
      .opsForZSet()
      .reverseRangeByScoreWithScores(
          "feed:inbox:" + userId,
          0,
          cursor,
          0,
          pageSize + 1
      );
  ```

#### 大V发件箱数据拉取
- **拉取流程：**
  1. 查询当前用户关注的大V列表（Redis Set交集）
  2. 从每个大V的发件箱拉取最新动态
  3. 每个大V拉取pageSize条数据
  4. 合并所有大V的动态
  
- **Redis命令：**
  ```java
  // 查询关注的大V列表
  Set<String> followBigV = redisTemplate
      .opsForSet()
      .intersect("user:follow:" + userId, "user:bigv");
  
  // 从每个大V的发件箱拉取数据
  for (String bigVId : followBigV) {
      Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate
          .opsForZSet()
          .reverseRangeByScoreWithScores(
              "feed:outbox:" + bigVId,
              0,
              cursor,
              0,
              pageSize
          );
      allPosts.addAll(tuples);
  }
  ```

#### 数据合并和排序
- **合并流程：**
  1. 合并收件箱数据 + 大V发件箱数据
  2. 按时间戳降序排序
  3. 取前pageSize条数据
  4. 提取动态ID列表
  5. 批量查询动态详情（MyBatis批量查询）
  6. 返回Feed流列表
  
- **排序实现：**
  ```java
  List<FeedItem> allFeeds = new ArrayList<>();
  allFeeds.addAll(inboxFeeds);
  allFeeds.addAll(bigVFeeds);
  
  // 按时间戳降序排序
  allFeeds.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
  
  // 取前pageSize条
  List<FeedItem> result = allFeeds.stream()
      .limit(pageSize)
      .collect(Collectors.toList());
  ```

#### 多级缓存优化
- **本地缓存（Caffeine）：**
  - 缓存用户的Feed流第一页
  - key: `feed:cache:{userId}:page:1`
  - expire: 1分钟
  - 减少Redis查询
  
- **Redis缓存：**
  - 缓存动态详情（post:info:{postId}）
  - expire: 10分钟
  - 减少数据库查询
  
- **缓存查询流程：**
  1. 查询Caffeine本地缓存
  2. 缓存命中直接返回
  3. 缓存未命中查询Redis
  4. 查询动态详情
  5. 写入本地缓存
  6. 返回结果

#### 推荐Feed流
- **推荐算法：**
  - 简单版：按热度排序
  - 热度计算：like_count * 1 + comment_count * 2 + share_count * 3
  - 查询热门动态（热度 > 100）
  - 按热度降序排列
  
- **推荐流程：**
  1. 从Elasticsearch查询热门动态
  2. 排除用户已看过的动态（Redis Set记录）
  3. 按热度降序排列
  4. 返回推荐列表

**注意事项：**
- 收件箱和发件箱数据要合并后再排序
- 大V用户较多时拉取可能较慢（限制大V数量）
- 本地缓存过期时间要短，避免数据不一致
- 动态详情批量查询使用IN语句，注意数量限制（最多100个）
- 推荐算法可以后续优化（机器学习、协同过滤等）
- Feed流要记录用户已读位置（cursor）

---

## 第八周：Feed流前端开发与评论模块后端开发（第8周）

### 8.1 首页Feed流

**任务描述：**
实现首页Feed流页面，展示关注和推荐的动态，支持下拉刷新和上拉加载更多。

**实现步骤：**
1. 创建首页Feed流页面
2. 实现Tab切换（关注、推荐）
3. 实现下拉刷新和上拉加载
4. 实现Feed流优化

**技术要点：**

#### 首页布局
- **顶部：** Tab切换（关注、推荐）
- **中部：** 动态列表（复用PostCard组件）
- **底部：** 加载更多提示
- **悬浮按钮：** 发布动态按钮（右下角）

#### Tab切换
- **关注Tab：**
  - 展示关注用户的动态
  - 调用Feed流拉取接口（/feed/timeline）
  
- **推荐Tab：**
  - 展示推荐的热门动态
  - 调用推荐接口（/feed/recommend）

#### 下拉刷新
- 清空列表数据
- 重置cursor为当前时间戳
- 请求第一页数据
- 替换列表数据

#### 上拉加载更多
- 传入cursor（上一页最后一条的时间戳）
- 请求下一页数据
- 追加到列表数据

#### Feed流缓存策略
- **客户端缓存：**
  - Tab切换时缓存已加载的数据
  - 使用Pinia Store存储
  - 避免重复请求
  
- **缓存更新：**
  - 下拉刷新时清空缓存
  - 发布动态后清空缓存
  - 点赞、评论后更新对应动态的缓存

#### 首屏优化
- **骨架屏：**
  - 首次加载显示骨架屏
  - 模拟3个动态卡片
  
- **预加载：**
  - 第一屏数据加载完成后
  - 预加载第二屏数据
  - 提升滚动流畅度

**注意事项：**
- Tab切换要缓存数据，避免重复请求
- 下拉刷新要有节流，避免频繁刷新
- 上拉加载要检查是否还有下一页
- Feed流为空时显示引导关注用户
- 发布动态按钮点击跳转到发布页面
- 移动端优化滑动体验

---

### 8.2 评论服务后端开发

**任务描述：**
实现评论功能，支持一级评论和二级回复，包含评论发布、查询、删除、点赞等功能。

**数据表设计：**

#### 评论表（comment）
```sql
CREATE TABLE `comment` (
  `id` BIGINT NOT NULL COMMENT '评论ID（雪花算法生成）',
  `post_id` BIGINT NOT NULL COMMENT '动态ID',
  `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父评论ID（一级评论为0）',
  `root_id` BIGINT NOT NULL DEFAULT 0 COMMENT '根评论ID（一级评论为0）',
  `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '回复的用户ID',
  `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `reply_count` INT NOT NULL DEFAULT 0 COMMENT '回复数',
  `floor` INT NOT NULL DEFAULT 0 COMMENT '楼层号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0-已删除，1-正常）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`, `created_at`),
  KEY `idx_root_id` (`root_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';
```

**技术要点：**

#### 评论数据结构
- **一级评论：** parent_id = 0, root_id = 0
- **二级回复：** parent_id = 一级评论ID, root_id = 一级评论ID
- **回复评论的回复：** parent_id = 被回复的评论ID, root_id = 一级评论ID

#### 发布评论
- **发布流程：**
  1. 验证内容不为空（最多500字）
  2. 内容审核（敏感词过滤）
  3. 生成评论ID
  4. 计算楼层号（一级评论才有楼层号）
  5. 写入数据库
  6. 异步更新动态评论数（Kafka消息）
  7. 发送评论通知（@提及的用户、动态作者）
  8. 返回评论详情

#### 查询评论列表
- **一级评论查询：**
  - 查询条件：post_id、parent_id = 0
  - 排序：按热度（点赞数）或时间
  - 分页：每页20条
  - 返回前3条二级回复
  
- **二级回复查询：**
  - 查询条件：root_id = 一级评论ID
  - 排序：按时间正序
  - 分页：每页10条

#### 热门评论
- **热门标准：**
  - 点赞数 > 10
  - 按点赞数降序排列
  - 最多返回3条

#### 评论删除
- **软删除：**
  - 更新status = 0
  - 删除后显示"该评论已删除"
  - 子评论仍然可见

#### 评论点赞
- 与动态点赞类似
- 使用Redis Set存储用户点赞的评论
- 使用Lua脚本保证原子性

**注意事项：**
- 评论内容限制500字
- 评论频率限制（1分钟最多5条）
- 楼层号自增（从1开始）
- 一级评论和二级回复分开查询
- 热门评论缓存到Redis（10分钟）
- 评论删除后子评论显示"回复已删除的评论"

---

## 第二个月总结

### 已完成内容
1. 动态模块完整实现（发布、查询、删除）
2. Feed流服务实现（推拉结合模式）
3. 评论系统后端实现（多级评论）
4. 前端动态列表、Feed流页面

### 技术重点
1. ShardingSphere分库分表
2. Kafka异步消息处理
3. Redis Sorted Set实现Feed流
4. DFA算法敏感词过滤
5. 推拉结合Feed流架构
6. 多级评论树形结构

### 下个月计划
1. 评论模块前端开发
2. 互动模块开发（点赞、收藏、分享）
3. 搜索模块开发（Elasticsearch）
4. 系统联调测试与优化

---
